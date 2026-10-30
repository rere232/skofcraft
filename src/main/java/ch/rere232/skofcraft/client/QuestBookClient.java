package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.screen.QuestBookScreen;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class QuestBookClient {
    private QuestBookClient() {}

    public static void open() {
        Minecraft.getInstance().setScreen(new QuestBookScreen());
    }
}
