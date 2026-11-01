package ch.rere232.skofcraft.entity;

import ch.rere232.skofcraft.registry.SkofcraftEntities;
import ch.rere232.skofcraft.space.GigaBlast;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * The Giga ICBM on its way up: a longer countdown with sirens, the same climb as a satellite launcher, and above the
 * build limit it leaves the world and comes back down as an {@link IcbmWarhead} over its target.
 */
public class GigaIcbm extends LaunchRocket {
    public static final int COUNTDOWN = 200;
    private static final double SIREN_RANGE = 256.0D;

    private int targetX;
    private int targetZ;

    public GigaIcbm(EntityType<? extends GigaIcbm> type, Level level) {
        super(type, level);
    }

    public static GigaIcbm create(ServerLevel level, BlockPos pad, double padTop, BlockPos target, UUID owner) {
        GigaIcbm missile = new GigaIcbm(SkofcraftEntities.GIGA_ICBM.get(), level);
        missile.setPos(pad.getX() + 0.5D, pad.getY() + padTop, pad.getZ() + 0.5D);
        missile.targetX = target.getX();
        missile.targetZ = target.getZ();
        missile.owner = owner;
        return missile;
    }

    @Override
    public int countdown() {
        return COUNTDOWN;
    }

    @Override
    protected void onCountdown(ServerLevel level, int timer) {
        if (timer % 50 == 1) {
            for (ServerPlayer player : level.players()) {
                if (player.distanceToSqr(this) < SIREN_RANGE * SIREN_RANGE) {
                    player.playNotifySound(SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 1.5F, 0.8F);
                }
            }
        }
    }

    @Override
    protected void liftoff(ServerLevel level) {
        super.liftoff(level);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 10.0F, 0.6F);
        Component alert = Component.translatable("message.skofcraft.icbm_launched", getBlockX(), getBlockZ())
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD);
        for (ServerPlayer player : level.players()) {
            player.displayClientMessage(alert, false);
        }
    }

    @Override
    protected void reachOrbit(ServerLevel level) {
        int ticks = GigaBlast.scheduleReentry(level, getX(), getZ(), targetX, targetZ, owner);
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
        if (player != null) {
            player.displayClientMessage(Component.translatable("message.skofcraft.icbm_midcourse", targetX, targetZ, ticks / 20)
                    .withStyle(ChatFormatting.LIGHT_PURPLE), false);
        }
        discard();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        targetX = tag.getInt("TargetX");
        targetZ = tag.getInt("TargetZ");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("TargetX", targetX);
        tag.putInt("TargetZ", targetZ);
    }
}
