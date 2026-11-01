package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.screen.SkofeeBrewScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class SkofeeClient {
    private SkofeeClient() {}

    public static void openBrew(BlockPos pos) {
        Minecraft.getInstance().setScreen(new SkofeeBrewScreen(pos));
    }
}
