package ch.rere232.skofcraft.phone;

import ch.rere232.skofcraft.config.SkofcraftConfig;
import ch.rere232.skofcraft.network.PhoneAddictionS2CPacket;
import ch.rere232.skofcraft.network.SkofcraftNetwork;
import ch.rere232.skofcraft.registry.SkofcraftItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.network.PacketDistributor;

/**
 * Phone addiction: screen time builds it, time away from the phone lets it fade, and a heavy habit
 * brings phantom notifications and withdrawal. Standing on grass in daylight speeds up recovery.
 */
public final class PhoneAddiction {
    public static final int MAX = 100;
    /** Highest app id the client may report; 0 is the home screen. */
    public static final int APP_COUNT = 5;

    private static final String ROOT = "SkofcraftPhone";
    private static final String LEVEL = "Level";
    private static final String IDLE = "IdleTicks";
    private static final String GRASS = "GrassTicks";
    private static final String LAST_USE = "LastUse";

    /** Addiction gained per usage beat: home, Scrollr, Amazooon, Flappy Block, Lucky Creeper. */
    private static final int[] APP_GAIN = {0, 1, 1, 1, 2};
    private static final int MIN_BEAT_TICKS = 50;
    private static final int DECAY_IDLE_TICKS = 20 * 60 * 2;
    private static final int GRASS_DECAY_TICKS = 200;

    private static final int PHANTOM_LEVEL = 40;
    private static final int PHANTOM_IDLE_TICKS = 20 * 60;
    private static final int FATIGUE_LEVEL = 60;
    private static final int FATIGUE_IDLE_TICKS = 20 * 120;
    private static final int SHAKES_LEVEL = 80;
    private static final int SHAKES_IDLE_TICKS = 20 * 180;
    private static final int PHANTOM_MESSAGES = 4;

    private PhoneAddiction() {}

    private static CompoundTag tag(Player player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT)) {
            persistent.put(ROOT, new CompoundTag());
        }
        return persistent.getCompound(ROOT);
    }

    public static int level(Player player) {
        return tag(player).getInt(LEVEL);
    }

    private static void setLevel(ServerPlayer player, int level) {
        tag(player).putInt(LEVEL, Math.max(0, Math.min(MAX, level)));
        sync(player);
    }

    /** Eases the habit, e.g. with a hug from a plushie. */
    public static void reduce(ServerPlayer player, int amount) {
        setLevel(player, level(player) - amount);
    }

    public static void sync(ServerPlayer player) {
        SkofcraftNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new PhoneAddictionS2CPacket(level(player)));
    }

    /** Wipes the habit, e.g. with Addictol. */
    public static void reset(Player player) {
        CompoundTag tag = tag(player);
        tag.putInt(LEVEL, 0);
        tag.putInt(IDLE, 0);
        tag.putInt(GRASS, 0);
        if (player instanceof ServerPlayer serverPlayer) {
            sync(serverPlayer);
        }
    }

    /** Usage beat sent by the open phone: always eases withdrawal, and feeds the habit at most every 2.5 seconds. */
    public static void onUse(ServerPlayer player, int app) {
        if (app < 0 || app >= APP_COUNT || !player.isAlive()
                || !player.getInventory().hasAnyMatching(stack -> stack.is(SkofcraftItems.PHONE.get()))) {
            return;
        }
        CompoundTag tag = tag(player);
        tag.putInt(IDLE, 0);

        long now = player.level().getGameTime();
        long last = tag.contains(LAST_USE) ? tag.getLong(LAST_USE) : Long.MIN_VALUE / 2;
        if (now - last >= 0 && now - last < MIN_BEAT_TICKS) {
            return;
        }
        tag.putLong(LAST_USE, now);
        if (SkofcraftConfig.enableAddiction && APP_GAIN[app] > 0) {
            setLevel(player, level(player) + APP_GAIN[app]);
        }
    }

    public static void tick(ServerPlayer player) {
        int level = level(player);
        if (level <= 0) {
            return;
        }
        CompoundTag tag = tag(player);
        int idle = tag.getInt(IDLE) + 1;
        tag.putInt(IDLE, idle);

        if (idle % DECAY_IDLE_TICKS == 0) {
            level--;
            setLevel(player, level);
        }
        if (player.tickCount % 20 == 0 && touchingGrass(player)) {
            int grass = tag.getInt(GRASS) + 20;
            if (grass >= GRASS_DECAY_TICKS) {
                grass = 0;
                level--;
                setLevel(player, level);
                if (player.level() instanceof ServerLevel server) {
                    server.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0D,
                            player.getZ(), 4, 0.3D, 0.4D, 0.3D, 0.0D);
                }
            }
            tag.putInt(GRASS, grass);
        }

        if (SkofcraftConfig.enableAddiction && level > 0) {
            withdrawal(player, level, idle);
        }
    }

    private static boolean touchingGrass(ServerPlayer player) {
        BlockPos pos = player.blockPosition();
        return player.onGround() && player.level().isDay() && player.level().canSeeSky(pos)
                && player.level().getBlockState(pos.below()).is(Blocks.GRASS_BLOCK);
    }

    private static void withdrawal(ServerPlayer player, int level, int idle) {
        if (level < PHANTOM_LEVEL || idle < PHANTOM_IDLE_TICKS || player.tickCount % 20 != 0) {
            return;
        }
        if (player.getRandom().nextInt(15) == 0) {
            player.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.get(), SoundSource.PLAYERS, 0.7F, 1.5F);
            player.displayClientMessage(Component.translatable(
                    "message.skofcraft.phone_phantom." + player.getRandom().nextInt(PHANTOM_MESSAGES)), true);
        }
        if (!SkofcraftConfig.enableNegativeEffects) {
            return;
        }
        if (level >= FATIGUE_LEVEL && idle >= FATIGUE_IDLE_TICKS) {
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 0, true, false, true));
        }
        if (level >= SHAKES_LEVEL && idle >= SHAKES_IDLE_TICKS) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0, true, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 0, true, false, true));
        }
    }
}
