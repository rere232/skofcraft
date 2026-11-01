package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.SkofcraftMod;
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Crashes the game for real once the big red button was pressed. The exception is thrown from the client tick, so
 * Minecraft handles it like any other crash: the world is saved, a crash report is written, and the game closes.
 */
@Mod.EventBusSubscriber(modid = SkofcraftMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ButtonCrashClient {
    /** A few ticks so the click is heard and the button is seen going down. */
    private static final int DELAY_TICKS = 6;
    private static int countdown = -1;

    private ButtonCrashClient() {}

    public static void arm() {
        if (countdown < 0) {
            countdown = DELAY_TICKS;
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || countdown < 0 || countdown-- > 0) {
            return;
        }
        throw new ReportedException(new CrashReport("Pressed the button you were told not to press",
                new IllegalStateException("It said \"Do NOT press this button\". In giant red letters.")));
    }
}
