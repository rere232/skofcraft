package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.screen.DjMixerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class DjMixerClient {
    private DjMixerClient() {}

    public static void open(BlockPos pos) {
        Minecraft.getInstance().setScreen(new DjMixerScreen(pos));
    }
}
