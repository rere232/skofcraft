package ch.rere232.skofcraft.kingdom;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.advancement.SkofcraftAdvancements;
import ch.rere232.skofcraft.config.SkofcraftConfig;
import ch.rere232.skofcraft.entity.InterimRegent;
import ch.rere232.skofcraft.entity.KingdomNoble;
import ch.rere232.skofcraft.gums.GumSlotData;
import ch.rere232.skofcraft.hr.MandatoryHr;
import ch.rere232.skofcraft.item.GumConsumableItem;
import ch.rere232.skofcraft.registry.SkofcraftEntities;
import ch.rere232.skofcraft.registry.SkofcraftSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The homeland. Four Extra Strong pouches in the gum at once and you pass out and wake up here. You stay for as long as
 * there is anything in your gum: when the last one runs out, withdrawal throws you back where you collapsed. Addictol
 * does it on the spot. It only happens once per overdose, or waking up with four pouches still in would send you
 * straight back.
 */
@Mod.EventBusSubscriber(modid = SkofcraftMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SnusKingdom {
    public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "snus_kingdom"));

    /** Extra Strong is strength 2; Premium also counts. */
    private static final int OVERDOSE_STRENGTH = 2;
    private static final int CHECK_INTERVAL = 20;
    private static final int STUN_TICKS = 80;
    private static final int DAZE_TICKS = 200;

    private static final String RECORD = "SkofcraftKingdom";
    private static final String OVERDOSED = "Overdosed";

    private SnusKingdom() {}

    public static boolean isIn(Player player) {
        return player.level().dimension() == LEVEL;
    }

    private static CompoundTag record(Player player) {
        return DimensionTrip.record(player, RECORD);
    }

    private static boolean isOverdosing(Player player) {
        GumSlotData gum = GumSlotData.get(player);
        for (int slot = 0; slot < GumSlotData.SLOT_COUNT; slot++) {
            if (gum.getRemainingTicks(slot) <= 0 || GumConsumableItem.getStrength(gum.getSlotItem(slot)) < OVERDOSE_STRENGTH) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasAnythingInGum(Player player) {
        GumSlotData gum = GumSlotData.get(player);
        for (int slot = 0; slot < GumSlotData.SLOT_COUNT; slot++) {
            if (gum.getRemainingTicks(slot) > 0) {
                return true;
            }
        }
        return false;
    }

    /** Someone flying a drone is somewhere else already, and their body has to stay put until they come back. */
    private static boolean isFlyingDrone(ServerPlayer player) {
        return !player.serverLevel().getEntities(SkofcraftEntities.FPV_DRONE.get(), drone -> drone.getPilot() == player).isEmpty();
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || player.tickCount % CHECK_INTERVAL != 0 || !player.isAlive() || player.isSpectator()
                || MandatoryHr.isInTraining(player)) {
            return;
        }
        boolean overdosing = isOverdosing(player);
        if (!overdosing) {
            record(player).remove(OVERDOSED);
        }
        if (isIn(player)) {
            if (!hasAnythingInGum(player) && !player.isCreative()) {
                wakeUp(player, "withdrawal");
                SkofcraftAdvancements.award(player, SkofcraftAdvancements.RUDE_AWAKENING);
            }
        } else if (overdosing && SkofcraftConfig.enableSnusKingdom && !record(player).getBoolean(OVERDOSED)
                && !PastelParadise.isIn(player) && !isFlyingDrone(player)) {
            record(player).putBoolean(OVERDOSED, true);
            passOut(player);
        }
    }

    public static void passOut(ServerPlayer player) {
        ServerLevel kingdom = player.server.getLevel(LEVEL);
        if (kingdom == null) {
            return;
        }
        DimensionTrip.remember(player, record(player));

        player.level().playSound(null, player.blockPosition(), SkofcraftSounds.TRIP_THUD.get(), SoundSource.PLAYERS, 1.0F, 0.6F);
        Data data = Data.get(kingdom);
        BlockPos arrival = SnusKingdomCastle.arrival(kingdom, data);
        ensureRegent(kingdom, data);
        player.stopRiding();
        player.teleportTo(kingdom, arrival.getX() + 0.5D, arrival.getY(), arrival.getZ() + 0.5D, 180.0F, 0.0F);
        stun(player);
        player.displayClientMessage(Component.translatable("message.skofcraft.kingdom_arrive").withStyle(ChatFormatting.GOLD), false);
        SkofcraftAdvancements.award(player, SkofcraftAdvancements.LONG_LIVE_THE_INTERIM);
    }

    /** Back where they collapsed. */
    public static void wakeUp(ServerPlayer player, String reason) {
        DimensionTrip.goBack(player, record(player));
        stun(player);
        player.level().playSound(null, player.blockPosition(), SkofcraftSounds.TRIP_THUD.get(), SoundSource.PLAYERS, 1.0F, 0.8F);
        player.displayClientMessage(Component.translatable("message.skofcraft.kingdom_wake." + reason).withStyle(ChatFormatting.GRAY), false);
    }

    private static void stun(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, STUN_TICKS, 0, true, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, DAZE_TICKS, 0, true, false, true));
    }

    /** Tracked in saved data because the regent's chunk may not have finished loading their entity yet. */
    private static void ensureRegent(ServerLevel kingdom, Data data) {
        if (data.regentAlive) {
            return;
        }
        InterimRegent regent = SkofcraftEntities.INTERIM_REGENT.get().create(kingdom);
        if (regent != null) {
            BlockPos pos = SnusKingdomCastle.regentSpot(data);
            regent.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 180.0F, 0.0F);
            regent.setCustomName(data.crowned ? Component.translatable("entity.skofcraft.interim_regent.former")
                    : Component.translatable("entity.skofcraft.interim_regent.numbered", data.regentsAppointed + 1));
            regent.setCustomNameVisible(true);
            kingdom.addFreshEntity(regent);
            data.regentsAppointed++;
            data.regentAlive = true;
            data.setDirty();
        }
    }

    /** The sword is out. After 900 years the search committee can finally stop meeting. */
    public static void crown(ServerPlayer player, BlockPos stone) {
        ServerLevel level = player.serverLevel();
        Data data = Data.get(level);
        data.crowned = true;
        data.setDirty();
        for (InterimRegent regent : level.getEntitiesOfClass(InterimRegent.class, new AABB(stone).inflate(64.0D))) {
            regent.setCustomName(Component.translatable("entity.skofcraft.interim_regent.former"));
        }
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, stone.getX() + 0.5D, stone.getY() + 1.0D, stone.getZ() + 0.5D, 60, 0.5D, 1.0D, 0.5D, 0.3D);
        player.server.getPlayerList().broadcastSystemMessage(
                Component.translatable("message.skofcraft.excalibur_pulled", player.getDisplayName()).withStyle(ChatFormatting.GOLD), false);
        SkofcraftAdvancements.award(player, SkofcraftAdvancements.RIGHTFUL);
    }

    public static boolean isCrowned(ServerLevel kingdom) {
        return Data.get(kingdom).crowned;
    }

    public static void onRegentDied(ServerLevel level) {
        Data data = Data.get(level);
        data.regentAlive = false;
        data.setDirty();
    }

    /** Spawns the court the first time the castle goes up. */
    static void spawnCourt(ServerLevel kingdom, BlockPos[] spots) {
        for (BlockPos spot : spots) {
            KingdomNoble noble = SkofcraftEntities.KINGDOM_NOBLE.get().create(kingdom);
            if (noble != null) {
                noble.moveTo(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D, kingdom.random.nextFloat() * 360.0F, 0.0F);
                noble.setPersistenceRequired();
                kingdom.addFreshEntity(noble);
            }
        }
    }

    /** {@code /snuskingdom}: goes there or comes back, for operators who'd rather not overdose to look around. */
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("snuskingdom")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    if (isIn(player)) {
                        wakeUp(player, "command");
                    } else {
                        passOut(player);
                    }
                    return 1;
                }));
    }

    /** What the Kingdom remembers: where the castle is, and whose turn it is on the throne. */
    static final class Data extends SavedData {
        private static final String NAME = "skofcraft_snus_kingdom";
        boolean built;
        int floorY;
        boolean regentAlive;
        int regentsAppointed;
        boolean crowned;

        static Data get(ServerLevel kingdom) {
            return kingdom.getDataStorage().computeIfAbsent(Data::load, Data::new, NAME);
        }

        private static Data load(CompoundTag tag) {
            Data data = new Data();
            data.built = tag.getBoolean("Built");
            data.floorY = tag.getInt("FloorY");
            data.regentAlive = tag.getBoolean("RegentAlive");
            data.regentsAppointed = tag.getInt("RegentsAppointed");
            data.crowned = tag.getBoolean("Crowned");
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            tag.putBoolean("Built", built);
            tag.putInt("FloorY", floorY);
            tag.putBoolean("RegentAlive", regentAlive);
            tag.putInt("RegentsAppointed", regentsAppointed);
            tag.putBoolean("Crowned", crowned);
            return tag;
        }
    }
}
