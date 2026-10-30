package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.keybind.SkofcraftKeybinds;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Reads the pilot's jump key, which the common entity cannot see. */
@OnlyIn(Dist.CLIENT)
public final class ApacheClientInput {
    private ApacheClientInput() {}

    public static boolean isJumping(LivingEntity pilot) {
        return pilot instanceof LocalPlayer player && player.input.jumping;
    }

    public static boolean isDescending(LivingEntity pilot) {
        return pilot instanceof LocalPlayer && SkofcraftKeybinds.HELICOPTER_DESCEND.isDown();
    }
}
