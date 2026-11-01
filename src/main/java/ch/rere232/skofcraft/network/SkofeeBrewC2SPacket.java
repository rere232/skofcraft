package ch.rere232.skofcraft.network;

import ch.rere232.skofcraft.block.SkofeeMachineBlock;
import ch.rere232.skofcraft.blockentity.SkofeeMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Sent when the player finishes the SKOFee Machine minigame; the score (0-100) sets the cup quality. */
public class SkofeeBrewC2SPacket {
    private final BlockPos pos;
    private final int score;

    public SkofeeBrewC2SPacket(BlockPos pos, int score) {
        this.pos = pos;
        this.score = score;
    }

    public static void encode(SkofeeBrewC2SPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos);
        buf.writeVarInt(packet.score);
    }

    public static SkofeeBrewC2SPacket decode(FriendlyByteBuf buf) {
        return new SkofeeBrewC2SPacket(buf.readBlockPos(), buf.readVarInt());
    }

    public static void handle(SkofeeBrewC2SPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || player.distanceToSqr(packet.pos.getCenter()) > 64.0D
                    || !player.level().isLoaded(packet.pos)
                    || !(player.level().getBlockEntity(packet.pos) instanceof SkofeeMachineBlockEntity machine)) {
                return;
            }
            SkofeeMachineBlock.pour(player, packet.pos, machine, Math.max(0, Math.min(100, packet.score)));
        });
        context.setPacketHandled(true);
    }
}
