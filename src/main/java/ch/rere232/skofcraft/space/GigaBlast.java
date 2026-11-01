package ch.rere232.skofcraft.space;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.advancement.SkofcraftAdvancements;
import ch.rere232.skofcraft.block.NuclearReactorBlock;
import ch.rere232.skofcraft.entity.IcbmWarhead;
import ch.rere232.skofcraft.network.MushroomCloudS2CPacket;
import ch.rere232.skofcraft.network.SkofcraftNetwork;
import ch.rere232.skofcraft.registry.SkofcraftBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * The Giga ICBM's detonation, done by hand rather than with a vanilla explosion. A shockwave ring sweeps outwards at
 * {@link #RING_SPEED} blocks per tick; as it passes each column it
 * <ul>
 *     <li>inside {@link #CRATER_RADIUS}: vaporises everything down to a bowl {@link #CRATER_DEPTH} deep, lined with
 *     fused rock, obsidian and pools of lava,</li>
 *     <li>out to {@link #RIM_RADIUS}: throws up a ridge of ejecta,</li>
 *     <li>out to {@link #SCORCH_RADIUS}: strips leaves and plants, burns down wood (less and less further out, leaving
 *     dead trunks), scorches grass, fuses sand to glass and sets fires,</li>
 *     <li>out to {@link #DAMAGE_RADIUS}: kills or flings and burns whatever is standing there.</li>
 * </ul>
 * The crater edge is jagged, nuclear reactors caught in it melt down too, and a huge purple mushroom cloud rises.
 * This class also runs the warhead's re-entry: warnings, keeping the target loaded, and spawning it over the target.
 */
public final class GigaBlast {
    public static final double CRATER_RADIUS = 42.0D;
    public static final double CRATER_DEPTH = 24.0D;
    public static final double RIM_RADIUS = CRATER_RADIUS * 1.35D;
    public static final double SCORCH_RADIUS = CRATER_RADIUS * 2.7D;
    public static final double DAMAGE_RADIUS = CRATER_RADIUS * 3.3D;
    private static final double RING_SPEED = 3.0D;
    private static final double LIP_HEIGHT = 7.0D;
    private static final double LIP_PEAK = CRATER_RADIUS * 1.12D;
    private static final double LIP_WIDTH = CRATER_RADIUS * 0.25D;
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final float CLOUD_SCALE = 2.4F;
    private static final double CLOUD_RANGE = 2000.0D;
    private static final double WARNING_RANGE = 1024.0D;
    private static final int WARNING_TICKS = 160;
    private static final int MIN_FLIGHT_TICKS = WARNING_TICKS + 60;
    private static final int MAX_FLIGHT_TICKS = 900;
    private static final int TICKET_RADIUS = 9;
    private static final TicketType<ChunkPos> TICKET = TicketType.create(SkofcraftMod.MODID + ":icbm", Comparator.comparingLong(ChunkPos::toLong), 1200);

    /** Column offsets out to the scorch radius, nearest first: the ring works through them in order. */
    private static int[][] columns;

    private static final List<Reentry> REENTRIES = new ArrayList<>();
    private static final List<Blast> BLASTS = new ArrayList<>();

    private GigaBlast() {}

    private static final class Reentry {
        final ServerLevel level;
        final int x;
        final int z;
        final UUID owner;
        final long warnAt;
        final long spawnAt;
        boolean warned;

        Reentry(ServerLevel level, int x, int z, UUID owner, long spawnAt) {
            this.level = level;
            this.x = x;
            this.z = z;
            this.owner = owner;
            this.spawnAt = spawnAt;
            this.warnAt = spawnAt - WARNING_TICKS;
        }
    }

    private static final class Blast {
        final ServerLevel level;
        final int cx;
        final int cy;
        final int cz;
        final UUID owner;
        final RandomSource random;
        final float[] phases = new float[3];
        double ring;
        int next;

        Blast(ServerLevel level, BlockPos center, UUID owner) {
            this.level = level;
            this.cx = center.getX();
            this.cy = center.getY();
            this.cz = center.getZ();
            this.owner = owner;
            this.random = RandomSource.create(level.getRandom().nextLong());
            for (int i = 0; i < phases.length; i++) {
                phases[i] = random.nextFloat() * Mth.TWO_PI;
            }
        }

        /** The crater is not a perfect circle: its radius wobbles with direction. */
        double shape(double angle) {
            return 1.0D + 0.10D * Math.sin(3.0D * angle + phases[0]) + 0.06D * Math.sin(7.0D * angle + phases[1])
                    + 0.03D * Math.sin(17.0D * angle + phases[2]);
        }
    }

    // ---------------------------------------------------------------- re-entry

    /** The missile has left the atmosphere over (fromX, fromZ); returns how many ticks until the warhead appears over the target. */
    public static int scheduleReentry(ServerLevel level, double fromX, double fromZ, int x, int z, UUID owner) {
        double distance = Math.sqrt((x - fromX) * (x - fromX) + (z - fromZ) * (z - fromZ));
        int ticks = Mth.clamp(MIN_FLIGHT_TICKS + (int) (distance / 40.0D), MIN_FLIGHT_TICKS, MAX_FLIGHT_TICKS);
        REENTRIES.add(new Reentry(level, x, z, owner, level.getGameTime() + ticks));
        return ticks;
    }

    private static void keepLoaded(ServerLevel level, int x, int z) {
        ChunkPos chunk = new ChunkPos(new BlockPos(x, 0, z));
        level.getChunkSource().addRegionTicket(TICKET, chunk, TICKET_RADIUS, chunk);
    }

    private static void warn(Reentry reentry) {
        reentry.warned = true;
        keepLoaded(reentry.level, reentry.x, reentry.z);
        Component title = Component.translatable("message.skofcraft.icbm_incoming").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD);
        for (ServerPlayer player : reentry.level.players()) {
            double dx = player.getX() - reentry.x;
            double dz = player.getZ() - reentry.z;
            if (dx * dx + dz * dz > WARNING_RANGE * WARNING_RANGE) {
                continue;
            }
            int distance = (int) Math.sqrt(dx * dx + dz * dz);
            player.connection.send(new ClientboundSetTitlesAnimationPacket(5, 80, 20));
            player.connection.send(new ClientboundSetTitleTextPacket(title));
            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("message.skofcraft.icbm_incoming_sub",
                    reentry.x, reentry.z, distance, (WARNING_TICKS + 80) / 20).withStyle(ChatFormatting.RED)));
            player.playNotifySound(SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 2.0F, 0.6F);
            player.playNotifySound(SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.8F, 0.5F);
        }
    }

    private static void spawnWarhead(Reentry reentry) {
        ServerLevel level = reentry.level;
        keepLoaded(level, reentry.x, reentry.z);
        level.getChunk(reentry.x >> 4, reentry.z >> 4);
        level.addFreshEntity(IcbmWarhead.create(level, reentry.x + 0.5D, reentry.z + 0.5D, reentry.owner));
    }

    // ---------------------------------------------------------------- detonation

    public static void detonate(ServerLevel level, BlockPos center, UUID owner) {
        keepLoaded(level, center.getX(), center.getZ());
        double x = center.getX() + 0.5D;
        double y = center.getY();
        double z = center.getZ() + 0.5D;
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(x, y, z) < CLOUD_RANGE * CLOUD_RANGE) {
                SkofcraftNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new MushroomCloudS2CPacket(x, y, z, CLOUD_SCALE, true));
            }
        }
        level.playSound(null, x, y, z, SoundEvents.ENDER_DRAGON_DEATH, SoundSource.HOSTILE, 30.0F, 0.5F);
        level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 30.0F, 0.3F);
        ServerPlayer caller = level.getServer().getPlayerList().getPlayer(owner);
        if (caller != null) {
            SkofcraftAdvancements.award(caller, SkofcraftAdvancements.GIGA_ICBM);
        }
        BLASTS.add(new Blast(level, center, owner));
    }

    private static int[][] columns() {
        if (columns == null) {
            int r = Mth.ceil(SCORCH_RADIUS);
            List<int[]> list = new ArrayList<>();
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dz * dz <= SCORCH_RADIUS * SCORCH_RADIUS) {
                        list.add(new int[] {dx, dz});
                    }
                }
            }
            columns = list.toArray(new int[0][]);
            Arrays.sort(columns, Comparator.comparingInt(c -> c[0] * c[0] + c[1] * c[1]));
        }
        return columns;
    }

    /** Advances the shockwave one tick; returns false once it has died out. */
    private static boolean tick(Blast blast) {
        double previous = blast.ring;
        blast.ring += RING_SPEED;
        int[][] all = columns();
        double reach = blast.ring * blast.ring;
        while (blast.next < all.length) {
            int[] column = all[blast.next];
            if (column[0] * column[0] + column[1] * column[1] > reach) {
                break;
            }
            blast.next++;
            column(blast, column[0], column[1]);
        }
        if (previous < DAMAGE_RADIUS) {
            hitEntities(blast, previous, Math.min(blast.ring, DAMAGE_RADIUS));
            shockwaveParticles(blast);
        }
        return previous < DAMAGE_RADIUS || blast.next < all.length;
    }

    private static void column(Blast blast, int dx, int dz) {
        double distance = Math.sqrt(dx * dx + dz * dz);
        double effective = distance / (blast.shape(Math.atan2(dz, dx)) + blast.random.nextDouble() * 0.03D);
        int x = blast.cx + dx;
        int z = blast.cz + dz;
        ServerLevel level = blast.level;
        int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
        if (effective < CRATER_RADIUS) {
            crater(blast, x, z, effective, surface);
        } else if (effective < SCORCH_RADIUS) {
            double intensity = effective < RIM_RADIUS ? 1.0D : 1.0D - (effective - RIM_RADIUS) / (SCORCH_RADIUS - RIM_RADIUS);
            int ground = scorch(blast, x, z, surface, intensity);
            if (effective < RIM_RADIUS && ground > level.getMinBuildHeight()) {
                lip(blast, x, z, effective, ground);
            }
        }
    }

    private static boolean unbreakable(BlockState state) {
        return state.getDestroySpeed(null, null) < 0.0F;
    }

    private static void crater(Blast blast, int x, int z, double effective, int surface) {
        ServerLevel level = blast.level;
        double fraction = effective / CRATER_RADIUS;
        int bottom = blast.cy - (int) (CRATER_DEPTH * Math.sqrt(1.0D - fraction * fraction)) - blast.random.nextInt(2);
        bottom = Math.max(bottom, level.getMinBuildHeight() + 1);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = surface; y > bottom; y--) {
            BlockState state = level.getBlockState(pos.set(x, y, z));
            if (state.isAir() || unbreakable(state)) {
                continue;
            }
            if (state.is(SkofcraftBlocks.NUCLEAR_REACTOR.get())) {
                NuclearReactorBlock.scheduleMeltdown(level, pos.immutable());
                continue;
            }
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS);
        }
        // Fused rock lining the bowl, with lava pooling near the middle
        for (int y = bottom; y >= bottom - 2; y--) {
            BlockState state = level.getBlockState(pos.set(x, y, z));
            if (state.isAir() || unbreakable(state) || !state.getFluidState().isEmpty()) {
                continue;
            }
            if (y == bottom && fraction < 0.35D && blast.random.nextInt(9) == 0) {
                level.setBlock(pos, Blocks.LAVA.defaultBlockState(), Block.UPDATE_ALL);
            } else {
                level.setBlock(pos, lining(blast.random), FLAGS);
            }
        }
    }

    private static BlockState lining(RandomSource random) {
        float r = random.nextFloat();
        if (r < 0.32F) {
            return Blocks.BLACKSTONE.defaultBlockState();
        } else if (r < 0.52F) {
            return Blocks.BASALT.defaultBlockState();
        } else if (r < 0.68F) {
            return Blocks.MAGMA_BLOCK.defaultBlockState();
        } else if (r < 0.84F) {
            return Blocks.OBSIDIAN.defaultBlockState();
        } else if (r < 0.92F) {
            return Blocks.CRYING_OBSIDIAN.defaultBlockState();
        }
        return Blocks.TUFF.defaultBlockState();
    }

    /** Ejecta piled up around the crater, highest just outside the edge. */
    private static void lip(Blast blast, int x, int z, double effective, int ground) {
        double height = LIP_HEIGHT * (1.0D - Math.abs(effective - LIP_PEAK) / LIP_WIDTH);
        int blocks = (int) Math.round(height + blast.random.nextDouble() - 0.5D);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 1; i <= blocks; i++) {
            pos.set(x, ground + i, z);
            if (!blast.level.getBlockState(pos).isAir()) {
                continue;
            }
            float r = blast.random.nextFloat();
            BlockState ejecta = r < 0.35F ? Blocks.COBBLED_DEEPSLATE.defaultBlockState()
                    : r < 0.6F ? Blocks.BLACKSTONE.defaultBlockState()
                    : r < 0.8F ? Blocks.COARSE_DIRT.defaultBlockState()
                    : r < 0.93F ? Blocks.TUFF.defaultBlockState() : Blocks.MAGMA_BLOCK.defaultBlockState();
            blast.level.setBlock(pos, ejecta, FLAGS);
        }
    }

    private static boolean vegetation(BlockState state) {
        return state.is(BlockTags.LEAVES) || state.canBeReplaced() || state.getBlock() instanceof BushBlock
                || state.is(BlockTags.FLOWERS) || state.is(Tags.Blocks.GLASS) || state.is(Tags.Blocks.GLASS_PANES)
                || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.POWDER_SNOW) || state.is(BlockTags.WOOL_CARPETS);
    }

    /** Burns a column down to bare ground and scorches it; returns the ground's y, or the minimum build height if none. */
    private static int scorch(Blast blast, int x, int z, int surface, double intensity) {
        ServerLevel level = blast.level;
        RandomSource random = blast.random;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int floor = Math.max(level.getMinBuildHeight(), surface - 48);
        for (int y = surface; y > floor; y--) {
            BlockState state = level.getBlockState(pos.set(x, y, z));
            if (state.isAir()) {
                continue;
            }
            if (unbreakable(state) || !state.getFluidState().isEmpty()) {
                return y;
            }
            if (state.is(SkofcraftBlocks.NUCLEAR_REACTOR.get())) {
                if (intensity > 0.8D) {
                    NuclearReactorBlock.scheduleMeltdown(level, pos.immutable());
                }
                return y;
            }
            if (vegetation(state)) {
                // At the very edge some leaves hang on
                if (!state.is(BlockTags.LEAVES) || random.nextDouble() < 0.35D + intensity) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS);
                    continue;
                }
                return y;
            }
            if (state.isFlammable(level, pos, Direction.UP) && random.nextDouble() < intensity * 0.85D) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS);
                continue;
            }
            scorchGround(level, random, pos, state, intensity);
            return y;
        }
        return level.getMinBuildHeight();
    }

    private static void scorchGround(ServerLevel level, RandomSource random, BlockPos.MutableBlockPos pos, BlockState state, double intensity) {
        if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM) || state.is(Blocks.MOSS_BLOCK)) {
            level.setBlock(pos, random.nextFloat() < 0.6F ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.DIRT.defaultBlockState(), FLAGS);
        } else if (state.is(BlockTags.SAND) && intensity > 0.6D && random.nextDouble() < intensity * 0.6D) {
            // Desert sand fused to glass by the heat
            level.setBlock(pos, Blocks.GLASS.defaultBlockState(), FLAGS);
        }
        BlockPos above = pos.above();
        if (random.nextDouble() < 0.05D + intensity * 0.3D && level.getBlockState(above).isAir()
                && BaseFireBlock.canBePlacedAt(level, above, Direction.UP)) {
            level.setBlock(above, BaseFireBlock.getState(level, above), Block.UPDATE_ALL);
        }
    }

    private static void hitEntities(Blast blast, double inner, double outer) {
        ServerLevel level = blast.level;
        AABB box = new AABB(blast.cx - outer, blast.cy - 80.0D, blast.cz - outer, blast.cx + outer, blast.cy + 200.0D, blast.cz + outer);
        Entity owner = level.getServer().getPlayerList().getPlayer(blast.owner);
        DamageSource source = level.damageSources().explosion(null, owner);
        double kill = CRATER_RADIUS * 0.9D;
        for (Entity entity : level.getEntities((Entity) null, box, Entity::isAlive)) {
            double dx = entity.getX() - (blast.cx + 0.5D);
            double dz = entity.getZ() - (blast.cz + 0.5D);
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance < inner || distance >= outer) {
                continue;
            }
            if (distance < kill) {
                if (entity instanceof ItemEntity || entity instanceof ExperienceOrb) {
                    entity.discard();
                    continue;
                }
                entity.setSecondsOnFire(20);
                entity.hurt(source, 1000.0F);
                continue;
            }
            double falloff = 1.0D - (distance - kill) / (DAMAGE_RADIUS - kill);
            entity.setSecondsOnFire(3 + (int) (12 * falloff));
            entity.hurt(source, (float) (4.0D + 80.0D * falloff * falloff));
            double push = 0.6D + 3.0D * falloff;
            entity.push(dx / Math.max(distance, 0.1D) * push, 0.4D + 0.8D * falloff, dz / Math.max(distance, 0.1D) * push);
            entity.hurtMarked = true;
        }
    }

    /** A wall of dust and fire racing out along the ground, seen from far away. */
    private static void shockwaveParticles(Blast blast) {
        ServerLevel level = blast.level;
        double ring = blast.ring;
        int points = 24 + (int) (ring / 2.0D);
        double cx = blast.cx + 0.5D;
        double cz = blast.cz + 0.5D;
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(cx, blast.cy, cz) > WARNING_RANGE * WARNING_RANGE) {
                continue;
            }
            for (int i = 0; i < points; i++) {
                double angle = blast.random.nextDouble() * Mth.TWO_PI;
                double px = cx + Math.cos(angle) * ring;
                double pz = cz + Math.sin(angle) * ring;
                double py = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(px), Mth.floor(pz)) + 0.5D;
                ParticleOptions type = i % 6 == 0 ? ParticleTypes.EXPLOSION : (i % 3 == 0 ? ParticleTypes.FLAME
                        : (i % 3 == 1 ? ParticleTypes.CAMPFIRE_COSY_SMOKE : ParticleTypes.DRAGON_BREATH));
                level.sendParticles(player, type, true, px, py, pz, 1, 0.5D, 1.0D, 0.5D, 0.05D);
            }
            if (ring < CRATER_RADIUS) {
                level.sendParticles(player, ParticleTypes.EXPLOSION_EMITTER, true, cx, blast.cy + 2.0D, cz, 3, ring * 0.5D, 4.0D, ring * 0.5D, 0.0D);
            }
        }
        if (blast.ring < DAMAGE_RADIUS && ((int) (blast.ring / RING_SPEED)) % 4 == 0) {
            level.playSound(null, cx, blast.cy, cz, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 16.0F, 0.4F + blast.random.nextFloat() * 0.2F);
        }
    }

    // ---------------------------------------------------------------- events

    @Mod.EventBusSubscriber(modid = SkofcraftMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class Events {
        private Events() {}

        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            if (!REENTRIES.isEmpty()) {
                List<Reentry> due = new ArrayList<>();
                for (Reentry reentry : REENTRIES) {
                    long now = reentry.level.getGameTime();
                    if (!reentry.warned && now >= reentry.warnAt) {
                        warn(reentry);
                    }
                    if (now >= reentry.spawnAt) {
                        due.add(reentry);
                    }
                }
                REENTRIES.removeAll(due);
                due.forEach(GigaBlast::spawnWarhead);
            }
            if (!BLASTS.isEmpty()) {
                // Copy: a reactor caught in the crater may not add a blast, but stay safe against changes while iterating
                List<Blast> active = new ArrayList<>(BLASTS);
                for (Blast blast : active) {
                    if (!tick(blast)) {
                        BLASTS.remove(blast);
                    }
                }
            }
        }

        @SubscribeEvent
        public static void onServerStopped(ServerStoppedEvent event) {
            REENTRIES.clear();
            BLASTS.clear();
        }
    }
}
