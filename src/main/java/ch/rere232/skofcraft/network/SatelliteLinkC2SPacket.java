package ch.rere232.skofcraft.network;

import ch.rere232.skofcraft.registry.SkofcraftSounds;
import ch.rere232.skofcraft.entity.OrbitalRod;
import ch.rere232.skofcraft.registry.SkofcraftItems;
import ch.rere232.skofcraft.space.IcbmTargets;
import ch.rere232.skofcraft.space.OrbitData;
import ch.rere232.skofcraft.space.SatelliteType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/** From the phone's map app: ask for the satellite status, or call a strike on a map position. */
public class SatelliteLinkC2SPacket {
    public static final int STATUS = 0;
    public static final int STRIKE = 1;
    public static final int MARK = 2;

    private final int action;
    private final int x;
    private final int z;

    public SatelliteLinkC2SPacket(int action, int x, int z) {
        this.action = action;
        this.x = x;
        this.z = z;
    }

    public static void encode(SatelliteLinkC2SPacket packet, FriendlyByteBuf buf) {
        buf.writeByte(packet.action);
        buf.writeInt(packet.x);
        buf.writeInt(packet.z);
    }

    public static SatelliteLinkC2SPacket decode(FriendlyByteBuf buf) {
        return new SatelliteLinkC2SPacket(buf.readByte(), buf.readInt(), buf.readInt());
    }

    public static void handle(SatelliteLinkC2SPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !player.getInventory().hasAnyMatching(stack -> stack.is(SkofcraftItems.PHONE.get()))) {
                return;
            }
            ServerLevel level = player.serverLevel();
            OrbitData orbit = OrbitData.get(level);
            Component message = null;
            boolean struck = false;
            if (packet.action == MARK && orbit.count(SatelliteType.RECON) > 0) {
                IcbmTargets.set(player, packet.x, packet.z);
                message = Component.translatable("message.skofcraft.icbm_marked", packet.x, packet.z).withStyle(ChatFormatting.LIGHT_PURPLE);
            }
            if (packet.action == STRIKE) {
                message = strike(level, orbit, player, packet.x, packet.z);
                struck = message == null;
                if (struck) {
                    message = Component.translatable("message.skofcraft.strike_inbound_map", packet.x, packet.z, OrbitalRod.WARNING_TICKS / 20)
                            .withStyle(ChatFormatting.RED);
                }
            }
            reply(player, orbit, message, struck, packet.x, packet.z);
        });
        context.setPacketHandled(true);
    }

    /** Fires a strike; returns why it could not, or null if the rod is on its way. */
    @Nullable
    private static Component strike(ServerLevel level, OrbitData orbit, ServerPlayer player, int x, int z) {
        if (orbit.count(SatelliteType.RECON) <= 0) {
            return Component.translatable("phone.skofcraft.map.locked");
        }
        if (orbit.strikeSatellitesFor(player).isEmpty()) {
            return Component.translatable("message.skofcraft.strike_no_satellite");
        }
        long now = level.getGameTime();
        OrbitData.Satellite satellite = orbit.readyStrike(player, now);
        if (satellite == null) {
            return Component.translatable("message.skofcraft.strike_recharging", secondsToNext(orbit, player, now));
        }
        BlockPos column = new BlockPos(x, 0, z);
        if (!level.hasChunkAt(column)) {
            return Component.translatable("message.skofcraft.strike_unloaded");
        }
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        orbit.fire(satellite, now);
        OrbitalRod.call(level, new Vec3(x + 0.5D, y, z + 0.5D), player);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SkofcraftSounds.DESIGNATOR_FIRE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        return null;
    }

    private static long secondsToNext(OrbitData orbit, ServerPlayer player, long now) {
        List<OrbitData.Satellite> usable = orbit.strikeSatellitesFor(player);
        long wait = Long.MAX_VALUE;
        for (OrbitData.Satellite satellite : usable) {
            wait = Math.min(wait, satellite.readyAt() - now);
        }
        return usable.isEmpty() ? 0 : Math.max(0L, (wait + 19) / 20);
    }

    private static void reply(ServerPlayer player, OrbitData orbit, @Nullable Component message, boolean struck, int x, int z) {
        long now = player.level().getGameTime();
        List<OrbitData.Satellite> strikes = orbit.strikeSatellitesFor(player);
        int ready = 0;
        for (OrbitData.Satellite satellite : strikes) {
            if (satellite.readyAt() <= now) {
                ready++;
            }
        }
        SkofcraftNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SatelliteLinkS2CPacket(
                orbit.count(SatelliteType.RECON), strikes.size(), ready, (int) secondsToNext(orbit, player, now), message, struck, x, z));
    }
}
