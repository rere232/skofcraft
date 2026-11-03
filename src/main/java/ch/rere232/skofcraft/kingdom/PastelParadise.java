package ch.rere232.skofcraft.kingdom;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.advancement.SkofcraftAdvancements;
import ch.rere232.skofcraft.config.SkofcraftConfig;
import ch.rere232.skofcraft.entity.BoutiqueKeeper;
import ch.rere232.skofcraft.entity.Femboy;
import ch.rere232.skofcraft.entity.Gloom;
import ch.rere232.skofcraft.gums.GumSlotData;
import ch.rere232.skofcraft.hr.MandatoryHr;
import ch.rere232.skofcraft.item.ClothingItem;
import ch.rere232.skofcraft.phone.PhoneAddiction;
import ch.rere232.skofcraft.registry.SkofcraftEffects;
import ch.rere232.skofcraft.registry.SkofcraftBlocks;
import ch.rere232.skofcraft.registry.SkofcraftEntities;
import ch.rere232.skofcraft.registry.SkofcraftItems;
import ch.rere232.skofcraft.registry.SkofcraftPastel;
import ch.rere232.skofcraft.registry.SkofcraftSounds;
import ch.rere232.skofcraft.worldgen.PastelStructures;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemHandlerHelper;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * Soft islands in a pink sky. You get in by wearing the whole fit plus ears and a tail, and hugging a Blahaj. You leave
 * whenever you like by sneaking and hugging one. There is nothing below the islands, but Blahaj catches anyone
 * who falls off.
 * <p>
 * It is where you go to recover from the rest of the mod. While you are here your phone and nicotine habits fade, faster
 * in a hot spring, and a long enough stay sends you home Well Rested. Headpats earn Heart Tokens, which pay the shops and
 * the therapist who takes incidents off your HR record. The only danger is what you brought with you: Gloom turn up
 * around anyone with a phone habit, and The Algorithm comes if you offer your phone at a Heart Altar.
 */
@Mod.EventBusSubscriber(modid = SkofcraftMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PastelParadise {
    public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "pastel_paradise"));

    private static final String RECORD = "SkofcraftPastel";
    private static final int CHECK_INTERVAL = 10;
    private static final int ISLAND_FLOOR = 110;
    private static final int ISLAND_RADIUS = 14;
    private static final int RESCUE_SLOW_FALL_TICKS = 200;
    private static final int RECOVERY_INTERVAL = 200;
    private static final int STAY_FOR_WELL_RESTED = 20 * 60 * 5;
    private static final int WELL_RESTED_TICKS = 20 * 60 * 20;
    private static final int PAT_TOKEN_COOLDOWN = 20 * 30;
    private static final int GLOOM_ADDICTION = 25;
    private static final int GLOOM_CROWD = 4;
    private static final int GLOOM_RANGE = 16;
    private static final String STAY = "StayTicks";

    private PastelParadise() {}

    public static boolean isIn(Player player) {
        return player.level().dimension() == LEVEL;
    }

    /** A hug that got past the Blahaj's cooldown. */
    public static void onHug(ServerPlayer player) {
        if (isIn(player)) {
            if (player.isShiftKeyDown()) {
                leave(player);
            }
        } else if (SkofcraftConfig.enablePastelParadise && !SnusKingdom.isIn(player) && !MandatoryHr.isInTraining(player)
                && isDressedForIt(player)) {
            enter(player);
        }
    }

    /** All six pieces of the fit, plus ears and a tail. The cat and bunny or fox versions all count. */
    private static boolean isDressedForIt(Player player) {
        return ClothingItem.isWearingFullOutfit(player) && CuriosApi.getCuriosInventory(player)
                .map(curios -> (curios.isEquipped(SkofcraftItems.CAT_EARS.get()) || curios.isEquipped(SkofcraftPastel.BUNNY_EARS.get()))
                        && (curios.isEquipped(SkofcraftItems.CAT_TAIL.get()) || curios.isEquipped(SkofcraftPastel.FOX_TAIL.get())))
                .orElse(false);
    }

    public static void enter(ServerPlayer player) {
        ServerLevel paradise = player.server.getLevel(LEVEL);
        if (paradise == null) {
            return;
        }
        DimensionTrip.remember(player, DimensionTrip.record(player, RECORD));
        buildIslandOnce(paradise);
        player.stopRiding();
        player.teleportTo(paradise, 0.5D, ISLAND_FLOOR + 1.0D, 6.5D, 180.0F, 0.0F);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, true, false, true));
        paradise.playSound(null, player.blockPosition(), SkofcraftSounds.PLUSH_SOFT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        player.displayClientMessage(Component.translatable("message.skofcraft.pastel_arrive").withStyle(ChatFormatting.LIGHT_PURPLE), false);
        SkofcraftAdvancements.award(player, SkofcraftAdvancements.SOFT_LANDING);
    }

    public static void leave(ServerPlayer player) {
        CompoundTag record = DimensionTrip.record(player, RECORD);
        boolean rested = record.getInt(STAY) >= STAY_FOR_WELL_RESTED;
        record.remove(STAY);
        DimensionTrip.goBack(player, record);
        player.displayClientMessage(Component.translatable("message.skofcraft.pastel_leave").withStyle(ChatFormatting.LIGHT_PURPLE), false);
        if (rested) {
            player.addEffect(new MobEffectInstance(SkofcraftEffects.WELL_RESTED.get(), WELL_RESTED_TICKS, 0));
            player.displayClientMessage(Component.translatable("message.skofcraft.pastel_rested").withStyle(ChatFormatting.LIGHT_PURPLE), false);
            SkofcraftAdvancements.award(player, SkofcraftAdvancements.INNER_PEACE);
        }
    }

    /** A headpat, here and only here, earns a Heart Token. At most one every 30 seconds per player. */
    public static void onPat(Player player) {
        Item token = SkofcraftPastel.HEART_TOKEN.get();
        if (isIn(player) && !player.getCooldowns().isOnCooldown(token)) {
            player.getCooldowns().addCooldown(token, PAT_TOKEN_COOLDOWN);
            ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(token));
        }
    }

    /** The slow good that being here does you, and the one bad thing that follows you in. */
    private static void recover(ServerPlayer player, ServerLevel paradise) {
        int ease = player.isInWater() ? 2 : 1;
        PhoneAddiction.reduce(player, ease);
        GumSlotData.ease(player, ease);
        CompoundTag record = DimensionTrip.record(player, RECORD);
        record.putInt(STAY, record.getInt(STAY) + RECOVERY_INTERVAL);
        if (PhoneAddiction.level(player) >= GLOOM_ADDICTION && !player.isCreative() && paradise.random.nextBoolean()) {
            spawnGloom(player, paradise);
        }
    }

    private static void spawnGloom(ServerPlayer player, ServerLevel paradise) {
        if (paradise.getEntitiesOfClass(Gloom.class, player.getBoundingBox().inflate(32.0D)).size() >= GLOOM_CROWD) {
            return;
        }
        for (int attempt = 0; attempt < 8; attempt++) {
            int x = player.getBlockX() + paradise.random.nextInt(GLOOM_RANGE * 2 + 1) - GLOOM_RANGE;
            int z = player.getBlockZ() + paradise.random.nextInt(GLOOM_RANGE * 2 + 1) - GLOOM_RANGE;
            int y = paradise.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (Math.abs(y - player.getBlockY()) > 8 || pos.closerThan(player.blockPosition(), 6.0D)
                    || !paradise.getBlockState(pos.below()).isFaceSturdy(paradise, pos.below(), Direction.UP)) {
                continue;
            }
            Gloom gloom = SkofcraftEntities.GLOOM.get().create(paradise);
            if (gloom != null) {
                gloom.moveTo(x + 0.5D, y, z + 0.5D, paradise.random.nextFloat() * 360.0F, 0.0F);
                gloom.setTarget(player);
                paradise.addFreshEntity(gloom);
            }
            return;
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || !isIn(player)) {
            return;
        }
        ServerLevel paradise = player.serverLevel();
        if (player.tickCount % RECOVERY_INTERVAL == 0) {
            recover(player, paradise);
        }
        if (player.tickCount % CHECK_INTERVAL != 0 || player.getY() > player.level().getMinBuildHeight()) {
            return;
        }
        buildIslandOnce(paradise);
        player.teleportTo(paradise, 0.5D, ISLAND_FLOOR + 12.0D, 6.5D, player.getYRot(), player.getXRot());
        player.setDeltaMovement(0.0D, 0.0D, 0.0D);
        player.hurtMarked = true;
        player.resetFallDistance();
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, RESCUE_SLOW_FALL_TICKS, 0, true, false, true));
        paradise.playSound(null, player.blockPosition(), SkofcraftSounds.BLAHAJ_SQUEAK.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        paradise.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.0D, player.getZ(), 10, 0.5D, 0.5D, 0.5D, 0.0D);
        player.displayClientMessage(Component.translatable("message.skofcraft.pastel_rescued").withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }

    /** A guaranteed place to land, because the noise might put open sky at the middle of the world. */
    private static void buildIslandOnce(ServerLevel level) {
        Data data = Data.get(level);
        if (data.built) {
            addAmenities(level, data);
            return;
        }
        for (int chunkX = -1; chunkX <= 0; chunkX++) {
            for (int chunkZ = -1; chunkZ <= 0; chunkZ++) {
                level.getChunk(chunkX, chunkZ);
            }
        }
        BlockState marshmallow = SkofcraftPastel.MARSHMALLOW.get().defaultBlockState();
        BlockState bricks = SkofcraftPastel.HEART_BRICKS.get().defaultBlockState();
        BlockState lantern = SkofcraftPastel.HEART_LANTERN.get().defaultBlockState();
        int squared = ISLAND_RADIUS * ISLAND_RADIUS;
        for (int x = -ISLAND_RADIUS; x <= ISLAND_RADIUS; x++) {
            for (int z = -ISLAND_RADIUS; z <= ISLAND_RADIUS; z++) {
                int distance = x * x + z * z;
                if (distance > squared) {
                    continue;
                }
                int depth = 2 + (squared - distance) / 25;
                for (int y = 1; y <= depth; y++) {
                    set(level, new BlockPos(x, ISLAND_FLOOR - y, z), marshmallow);
                }
                set(level, new BlockPos(x, ISLAND_FLOOR, z), Blocks.GRASS_BLOCK.defaultBlockState());
                for (int y = 1; y <= 14; y++) {
                    set(level, new BlockPos(x, ISLAND_FLOOR + y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
        for (int x = -4; x <= 4; x += 8) {
            for (int z = -4; z <= 4; z += 8) {
                for (int y = 1; y <= 4; y++) {
                    set(level, new BlockPos(x, ISLAND_FLOOR + y, z), bricks);
                }
                set(level, new BlockPos(x, ISLAND_FLOOR + 5, z), lantern);
            }
        }
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                set(level, new BlockPos(x, ISLAND_FLOOR + 1, z), Blocks.PINK_CARPET.defaultBlockState());
            }
        }
        set(level, new BlockPos(0, ISLAND_FLOOR + 1, 0), SkofcraftBlocks.BLAHAJ.get().defaultBlockState());
        PastelStructures.chest(level, level.random, new BlockPos(0, ISLAND_FLOOR + 1, -3), "pastel_cottage");
        for (int side = -1; side <= 1; side += 2) {
            Femboy resident = SkofcraftEntities.FEMBOY.get().create(level);
            if (resident != null) {
                resident.moveTo(side * 6 + 0.5D, ISLAND_FLOOR + 1.0D, 2.5D, level.random.nextFloat() * 360.0F, 0.0F);
                level.addFreshEntity(resident);
            }
        }
        data.built = true;
        data.setDirty();
        addAmenities(level, data);
    }

    /** Separate from the island so a pavilion built before these existed still gets them. */
    private static void addAmenities(ServerLevel level, Data data) {
        if (data.amenities) {
            return;
        }
        level.getChunk(0, -1);
        level.getChunk(-1, -1);
        set(level, new BlockPos(3, ISLAND_FLOOR + 1, -3), SkofcraftPastel.THERAPY_COUCH.get().defaultBlockState());
        BoutiqueKeeper keeper = SkofcraftEntities.BOUTIQUE_KEEPER.get().create(level);
        if (keeper != null) {
            keeper.moveTo(-2.5D, ISLAND_FLOOR + 1.0D, -2.5D, 180.0F, 0.0F);
            keeper.setPersistenceRequired();
            level.addFreshEntity(keeper);
        }
        data.amenities = true;
        data.setDirty();
    }

    private static void set(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
    }

    /** {@code /pastelparadise}: goes there or comes back, for operators who want to look around. */
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("pastelparadise")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    if (isIn(player)) {
                        leave(player);
                    } else {
                        enter(player);
                    }
                    return 1;
                }));
    }

    private static final class Data extends SavedData {
        private static final String NAME = "skofcraft_pastel_paradise";
        boolean built;
        boolean amenities;

        static Data get(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(Data::load, Data::new, NAME);
        }

        private static Data load(CompoundTag tag) {
            Data data = new Data();
            data.built = tag.getBoolean("Built");
            data.amenities = tag.getBoolean("Amenities");
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            tag.putBoolean("Built", built);
            tag.putBoolean("Amenities", amenities);
            return tag;
        }
    }
}
