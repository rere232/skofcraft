package ch.rere232.skofcraft.block;

import ch.rere232.skofcraft.advancement.SkofcraftAdvancements;
import ch.rere232.skofcraft.blockentity.NuclearReactorBlockEntity;
import ch.rere232.skofcraft.blockentity.SkofcraftBlockEntities;
import ch.rere232.skofcraft.network.MushroomCloudS2CPacket;
import ch.rere232.skofcraft.network.SkofcraftNetwork;
import ch.rere232.skofcraft.registry.SkofcraftBlocks;
import ch.rere232.skofcraft.SkofcraftMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Naval reactor: pushes {@link NuclearReactorBlockEntity#OUTPUT} FE per tick into every adjacent machine, and a
 * carrier will not sail without one. Any explosion that reaches it, even one far too weak to break it, sets off a
 * meltdown: a giant blast a few ticks later, then rings of secondary blasts racing outward across the deck and
 * through the hull. Any of them that reaches another reactor sets it off too.
 */
public class NuclearReactorBlock extends BaseEntityBlock {
    public static final float MELTDOWN_POWER = 24.0F;
    private static final double MELTDOWN_WITNESS_RADIUS = 128.0D;
    /** Rings of secondary blasts after the core goes: ring i is i * RING_SPACING out and goes off i * RING_DELAY ticks later. */
    private static final int RINGS = 4;
    private static final double RING_SPACING = 10.0D;
    private static final int RING_DELAY = 5;
    private static final int RING_BASE_BLASTS = 6;
    private static final float SECONDARY_POWER = 7.0F;
    /** Secondary blasts climb to the surface (the flight deck on a carrier) unless it is further up than this. */
    private static final int MAX_SURFACE_CLIMB = 32;
    /** Ticks between the triggering blast and the meltdown, so chained reactors go off one after another. */
    private static final int MELTDOWN_DELAY = 6;
    /** Reach of an explosion that destroyed no blocks (water, or a blast that does not break blocks). */
    private static final double DEFAULT_BLAST_REACH = 4.0D;
    private static final double MAX_BLAST_REACH = 24.0D;

    /** A blast waiting for its tick: the reactor core itself, or one of the secondary blasts around it. */
    private record PendingBlast(ServerLevel level, BlockPos pos, long dueTick, boolean core) {}

    private static final List<PendingBlast> PENDING = new ArrayList<>();

    public NuclearReactorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NuclearReactorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, SkofcraftBlockEntities.NUCLEAR_REACTOR.get(), (l, p, s, reactor) -> reactor.tick());
    }

    @Override
    public void wasExploded(Level level, BlockPos pos, Explosion explosion) {
        if (level instanceof ServerLevel serverLevel) {
            scheduleMeltdown(serverLevel, pos);
        }
    }

    /** Takes the reactor out now, so nothing triggers it twice, and blows it a few ticks later. */
    public static void scheduleMeltdown(ServerLevel level, BlockPos pos) {
        if (level.getBlockState(pos).is(SkofcraftBlocks.NUCLEAR_REACTOR.get())) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        level.playSound(null, pos, SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.BLOCKS, 4.0F, 0.5F);
        PENDING.add(new PendingBlast(level, pos, level.getGameTime() + MELTDOWN_DELAY, true));
    }

    private static void meltdown(ServerLevel level, BlockPos pos) {
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;
        level.explode(null, x, y, z, MELTDOWN_POWER, true, Level.ExplosionInteraction.BLOCK);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 24, 6.0D, 4.0D, 6.0D, 0.0D);
        level.sendParticles(ParticleTypes.FLASH, x, y, z, 6, 2.0D, 2.0D, 2.0D, 0.0D);
        level.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, x, y + 4.0D, z, 120, 6.0D, 10.0D, 6.0D, 0.05D);
        level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 16.0F, 0.4F);
        level.playSound(null, x, y, z, SoundEvents.WITHER_DEATH, SoundSource.BLOCKS, 8.0F, 0.5F);
        SkofcraftNetwork.CHANNEL.send(PacketDistributor.NEAR.with(PacketDistributor.TargetPoint.p(x, y, z, MushroomCloudS2CPacket.RANGE, level.dimension())),
                new MushroomCloudS2CPacket(x, y, z));
        for (ServerPlayer player : level.players()) {
            if (player.blockPosition().closerThan(pos, MELTDOWN_WITNESS_RADIUS)) {
                SkofcraftAdvancements.award(player, SkofcraftAdvancements.MELTDOWN);
            }
        }
        scheduleRings(level, pos);
    }

    /** Spreads secondary blasts in growing rings, alternating between the surface above and the reactor's own level. */
    private static void scheduleRings(ServerLevel level, BlockPos pos) {
        RandomSource random = level.getRandom();
        long now = level.getGameTime();
        for (int ring = 1; ring <= RINGS; ring++) {
            int blasts = RING_BASE_BLASTS + ring * 2;
            double offset = random.nextDouble() * Math.PI * 2.0D;
            for (int i = 0; i < blasts; i++) {
                double angle = offset + i * Math.PI * 2.0D / blasts + (random.nextDouble() - 0.5D) * 0.4D;
                double distance = ring * RING_SPACING + (random.nextDouble() - 0.5D) * RING_SPACING * 0.6D;
                int x = pos.getX() + Mth.floor(Math.sin(angle) * distance);
                int z = pos.getZ() + Mth.floor(Math.cos(angle) * distance);
                int y = pos.getY();
                if (i % 2 == 0) {
                    int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                    if (surface > y && surface - y <= MAX_SURFACE_CLIMB) {
                        y = surface;
                    }
                }
                PENDING.add(new PendingBlast(level, new BlockPos(x, y, z), now + (long) ring * RING_DELAY + random.nextInt(RING_DELAY), false));
            }
        }
    }

    private static void secondaryBlast(ServerLevel level, BlockPos pos) {
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;
        float power = SECONDARY_POWER + level.getRandom().nextFloat() * 3.0F;
        level.explode(null, x, y, z, power, true, Level.ExplosionInteraction.BLOCK);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 3, 1.5D, 1.0D, 1.5D, 0.0D);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y + 1.0D, z, 30, 2.0D, 3.0D, 2.0D, 0.05D);
    }

    /** Reactors resist almost any blast, so look for them around every explosion rather than in what it destroyed. */
    @Mod.EventBusSubscriber(modid = SkofcraftMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class Events {
        private Events() {}

        @SubscribeEvent
        public static void onDetonate(ExplosionEvent.Detonate event) {
            if (!(event.getLevel() instanceof ServerLevel level)) {
                return;
            }
            Vec3 center = event.getExplosion().getPosition();
            // Explosion has no public radius in 1.20.1: estimate its reach from the farthest block it broke
            double reach = DEFAULT_BLAST_REACH;
            for (BlockPos broken : event.getAffectedBlocks()) {
                reach = Math.max(reach, Math.sqrt(broken.distToCenterSqr(center)) + 1.5D);
            }
            reach = Math.min(reach, MAX_BLAST_REACH);

            int r = Mth.ceil(reach);
            BlockPos origin = BlockPos.containing(center);
            List<BlockPos> reactors = new ArrayList<>();
            for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-r, -r, -r), origin.offset(r, r, r))) {
                if (pos.distToCenterSqr(center) <= reach * reach && level.getBlockState(pos).is(SkofcraftBlocks.NUCLEAR_REACTOR.get())) {
                    reactors.add(pos.immutable());
                }
            }
            reactors.forEach(pos -> scheduleMeltdown(level, pos));
        }

        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END || PENDING.isEmpty()) {
                return;
            }
            // Copy first: a blast can set off more reactors, which adds to the list
            List<PendingBlast> due = new ArrayList<>();
            PENDING.removeIf(pending -> {
                boolean ready = pending.level().getGameTime() >= pending.dueTick();
                if (ready) {
                    due.add(pending);
                }
                return ready;
            });
            for (PendingBlast pending : due) {
                if (pending.core()) {
                    meltdown(pending.level(), pending.pos());
                } else {
                    secondaryBlast(pending.level(), pending.pos());
                }
            }
        }

        @SubscribeEvent
        public static void onServerStopped(ServerStoppedEvent event) {
            PENDING.clear();
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.GLOW, pos.getX() + random.nextDouble(), pos.getY() + 1.05D,
                    pos.getZ() + random.nextDouble(), 0.0D, 0.02D, 0.0D);
        }
    }
}
