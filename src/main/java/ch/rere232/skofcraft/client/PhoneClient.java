package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.screen.PhoneScreen;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class PhoneClient {
    private static int addiction;

    private PhoneClient() {}

    public static void open() {
        Minecraft.getInstance().setScreen(new PhoneScreen());
    }

    public static void setAddiction(int level) {
        addiction = level;
    }

    /** Phone addiction 0..100 as last synced by the server. */
    public static int addiction() {
        return addiction;
    }
}
