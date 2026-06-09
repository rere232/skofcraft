package ch.rere232.skofcraft.gums;

import ch.rere232.skofcraft.item.GumConsumableItem;
import ch.rere232.skofcraft.network.GumSyncS2CPacket;
import ch.rere232.skofcraft.network.SkofcraftNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

public class GumEvents {
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }
        GumSlotData.tick(event.player);

        if (event.player instanceof ServerPlayer serverPlayer && serverPlayer.tickCount % 5 == 0) {
            syncToClient(serverPlayer);
        }
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            syncToClient(serverPlayer);
        }
    }

    private void syncToClient(ServerPlayer serverPlayer) {
        GumSlotData data = GumSlotData.get(serverPlayer);
        int[] ticks = new int[GumSlotData.SLOT_COUNT];
        String[] itemIds = new String[GumSlotData.SLOT_COUNT];
        String[] flavorIds = new String[GumSlotData.SLOT_COUNT];

        for (int i = 0; i < GumSlotData.SLOT_COUNT; i++) {
            ticks[i] = data.getRemainingTicks(i);
            ItemStack item = data.getSlotItem(i);
            itemIds[i] = item.isEmpty() ? "" : net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item.getItem()).toString();
            flavorIds[i] = item.isEmpty() ? "" : GumConsumableItem.getFlavorId(item);
        }

        SkofcraftNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new GumSyncS2CPacket(ticks, itemIds, flavorIds));
    }
}
