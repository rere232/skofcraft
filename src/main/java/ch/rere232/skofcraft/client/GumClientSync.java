package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.gums.GumSlotData;
import net.minecraft.client.Minecraft;

public class GumClientSync {
    public static void handle(int[] ticks, String[] itemIds, String[] flavorIds) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            GumSlotData.applySyncData(mc.player, ticks, itemIds, flavorIds);
        }
    }
}
