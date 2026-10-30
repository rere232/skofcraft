package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.registry.SkofcraftEffects;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/** Brown splash drawn over the screen while the player has spit in their eyes. */
public final class SpitOverlay {
    private static final ResourceLocation SPLASH = ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "textures/misc/spit_splash.png");

    private SpitOverlay() {}

    public static void render(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        MobEffectInstance effect = player.getEffect(SkofcraftEffects.SPIT_BLIND.get());
        if (effect == null) {
            return;
        }

        float fade = Mth.clamp(effect.getDuration() / 20.0F, 0.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.fill(0, 0, width, height, ((int) (fade * 90) << 24) | 0x4A2E14);
        g.setColor(1.0F, 1.0F, 1.0F, fade * 0.95F);
        g.blit(SPLASH, 0, 0, width, height, 0.0F, 0.0F, 256, 256, 256, 256);
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }
}
