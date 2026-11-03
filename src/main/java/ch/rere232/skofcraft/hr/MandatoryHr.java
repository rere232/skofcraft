package ch.rere232.skofcraft.hr;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.advancement.SkofcraftAdvancements;
import ch.rere232.skofcraft.config.SkofcraftConfig;
import ch.rere232.skofcraft.entity.HrRepresentative;
import ch.rere232.skofcraft.network.HrTrainingS2CPacket;
import ch.rere232.skofcraft.network.SkofcraftNetwork;
import ch.rere232.skofcraft.registry.SkofcraftEntities;
import ch.rere232.skofcraft.registry.SkofcraftItems;
import ch.rere232.skofcraft.registry.SkofcraftSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Keeps each player's record. The third war crime brings a {@link HrRepresentative}, who sits them through training
 * they can't skip. The record is kept where it survives death, and unfinished training resumes on the next login, so
 * neither dying nor leaving gets anyone out of it. Players can't be hurt while they're in training.
 */
@Mod.EventBusSubscriber(modid = SkofcraftMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MandatoryHr {
    public static final int CRIMES_BEFORE_TRAINING = 3;
    public static final int CERTIFIED_CRIMES_BEFORE_TRAINING = 30;
    /** A little under what the slides take on the client, so a lagging server can't refuse an honest finish. */
    private static final long MIN_TRAINING_TICKS = 700L;
    private static final int CHECK_INTERVAL = 40;
    private static final int SEARCH_RADIUS = 128;
    private static final int SPAWN_ATTEMPTS = 10;
    private static final int SPAWN_MIN = 10;
    private static final int SPAWN_MAX = 16;
    private static final int SPAWN_MAX_HEIGHT_DIFFERENCE = 8;
    private static final String CRIMES = "SkofcraftWarCrimes";
    private static final String TRAINING = "SkofcraftHrTraining";
    private static final String CERTIFIED = "SkofcraftHrCertified";
    private static final Map<UUID, Long> TRAINING_STARTED = new HashMap<>();

    private MandatoryHr() {}

    /** Forge copies this tag across death; the rest of the player's data is lost. */
    private static CompoundTag record(Player player) {
        CompoundTag root = player.getPersistentData();
        CompoundTag persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
        root.put(Player.PERSISTED_NBT_TAG, persisted);
        return persisted;
    }

    private static int crimesBeforeTraining(Player player) {
        return record(player).getBoolean(CERTIFIED) ? CERTIFIED_CRIMES_BEFORE_TRAINING : CRIMES_BEFORE_TRAINING;
    }

    public static boolean isDue(Player player) {
        return record(player).getInt(CRIMES) >= crimesBeforeTraining(player);
    }

    public static int incidents(Player player) {
        return record(player).getInt(CRIMES);
    }

    /** One incident fewer on the record. Anyone already in training still has to finish it. */
    public static void forgiveOne(Player player) {
        CompoundTag record = record(player);
        record.putInt(CRIMES, Math.max(0, record.getInt(CRIMES) - 1));
    }

    /**
     * Raises the player's limit for good. Returns false if they had already signed. A representative already on the way
     * goes home by itself, because the player is no longer due.
     */
    public static boolean certify(Player player) {
        CompoundTag record = record(player);
        if (record.getBoolean(CERTIFIED)) {
            return false;
        }
        record.putBoolean(CERTIFIED, true);
        return true;
    }

    public static boolean isInTraining(Player player) {
        return record(player).getBoolean(TRAINING);
    }

    /** Logs a war crime. {@code reasonKey} is the translation key of what it was. */
    public static void recordCrime(ServerPlayer player, String reasonKey) {
        if (!SkofcraftConfig.enableMandatoryHr || isInTraining(player)) {
            return;
        }
        CompoundTag record = record(player);
        int crimes = record.getInt(CRIMES) + 1;
        record.putInt(CRIMES, crimes);
        int limit = crimesBeforeTraining(player);
        player.displayClientMessage(Component.translatable("hr.skofcraft.logged",
                Math.min(crimes, limit), limit, Component.translatable(reasonKey)).withStyle(ChatFormatting.GRAY), false);
    }

    public static void startTraining(ServerPlayer player) {
        if (isInTraining(player)) {
            return;
        }
        record(player).putBoolean(TRAINING, true);
        player.stopRiding();
        player.closeContainer();
        openScreen(player);
    }

    private static void openScreen(ServerPlayer player) {
        TRAINING_STARTED.put(player.getUUID(), player.level().getGameTime());
        SkofcraftNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new HrTrainingS2CPacket(true));
    }

    /** The client says it finished. Too early and it's ignored; the button still works when pressed again. */
    public static void complete(ServerPlayer player) {
        Long started = TRAINING_STARTED.get(player.getUUID());
        if (!isInTraining(player) || started == null || player.level().getGameTime() - started < MIN_TRAINING_TICKS) {
            return;
        }
        finish(player);
        ItemStack certificate = new ItemStack(SkofcraftItems.HR_CERTIFICATE.get());
        if (!player.getInventory().add(certificate)) {
            player.drop(certificate, false);
        }
        SkofcraftAdvancements.award(player, SkofcraftAdvancements.MANDATORY_FUN);
        player.displayClientMessage(Component.translatable("hr.skofcraft.completed").withStyle(ChatFormatting.GREEN), false);
    }

    private static void finish(ServerPlayer player) {
        CompoundTag record = record(player);
        record.putBoolean(TRAINING, false);
        record.putInt(CRIMES, 0);
        TRAINING_STARTED.remove(player.getUUID());
        SkofcraftNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new HrTrainingS2CPacket(false));
    }

    private static void summon(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        HrRepresentative representative = SkofcraftEntities.HR_REPRESENTATIVE.get().create(level);
        if (representative == null) {
            return;
        }
        representative.setOwner(player);
        BlockPos spot = findSpot(level, player);
        representative.moveTo(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
        level.addFreshEntity(representative);
        player.displayClientMessage(Component.translatable("hr.skofcraft.summoned").withStyle(ChatFormatting.YELLOW), false);
        player.playNotifySound(SkofcraftSounds.HR_CHIME.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
    }

    /** Far enough to watch them walk over, if the ground allows it. Otherwise right next to the player. */
    private static BlockPos findSpot(ServerLevel level, ServerPlayer player) {
        for (int i = 0; i < SPAWN_ATTEMPTS; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            int distance = SPAWN_MIN + level.random.nextInt(SPAWN_MAX - SPAWN_MIN + 1);
            int x = player.getBlockX() + (int) (Math.cos(angle) * distance);
            int z = player.getBlockZ() + (int) (Math.sin(angle) * distance);
            BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
            if (Math.abs(pos.getY() - player.getBlockY()) <= SPAWN_MAX_HEIGHT_DIFFERENCE && level.getFluidState(pos).isEmpty()
                    && level.noCollision(SkofcraftEntities.HR_REPRESENTATIVE.get().getAABB(x + 0.5D, pos.getY(), z + 0.5D))) {
                return pos;
            }
        }
        return player.blockPosition();
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || player.tickCount % CHECK_INTERVAL != 0 || !SkofcraftConfig.enableMandatoryHr
                || !isDue(player) || isInTraining(player) || !player.isAlive() || player.isSpectator()) {
            return;
        }
        boolean coming = !player.serverLevel().getEntitiesOfClass(HrRepresentative.class,
                player.getBoundingBox().inflate(SEARCH_RADIUS), representative -> representative.isFor(player)).isEmpty();
        if (!coming) {
            summon(player);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && isInTraining(player)) {
            if (SkofcraftConfig.enableMandatoryHr) {
                openScreen(player);
            } else {
                finish(player);
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        TRAINING_STARTED.remove(event.getEntity().getUUID());
    }

    /** They can't see the screen behind the slides, so nothing gets to kill them while they're stuck in it. */
    @SubscribeEvent
    public static void onAttacked(LivingAttackEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && isInTraining(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof AbstractVillager && event.getSource().getEntity() instanceof ServerPlayer player) {
            recordCrime(player, "hr.skofcraft.crime.villager");
        }
    }

    /** {@code /mandatoryhr}: sends HR to whoever runs it, for operators who want to see it without committing anything. */
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("mandatoryhr")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    record(player).putInt(CRIMES, crimesBeforeTraining(player));
                    context.getSource().sendSuccess(() -> Component.translatable("hr.skofcraft.command"), false);
                    return 1;
                }));
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        TRAINING_STARTED.clear();
    }
}
