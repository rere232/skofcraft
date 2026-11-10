package ch.rere232.skofcraft.client.audio;

import ch.rere232.skofcraft.config.SkofcraftClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ViewportEvent;

/**
 * A short camera jolt on every bass hit. It kicks in hard and dies out within a fifth of a second, scaled by how
 * loud the music is where the player stands, the client config and vanilla's Screen Effect Scale slider.
 */
@OnlyIn(Dist.CLIENT)
final class BassShake {
    /** Degrees of pitch at full strength; yaw and roll get a bit less. */
    private static final float MAX_ANGLE = 1.4F;
    /** Per second: the jolt halves in about 60 ms. */
    private static final float DECAY = 12.0F;

    private static float trauma;
    private static long lastNanos;

    private BassShake() {}

    static void kick(float strength) {
        trauma = Math.max(trauma, Mth.clamp(strength, 0.0F, 1.0F));
    }

    static void apply(ViewportEvent.ComputeCameraAngles event) {
        long now = System.nanoTime();
        float dt = lastNanos == 0L ? 0.0F : Math.min(0.1F, (now - lastNanos) / 1.0E9F);
        lastNanos = now;
        trauma *= (float) Math.exp(-DECAY * dt);
        if (trauma < 0.01F) {
            trauma = 0.0F;
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        float scale = (float) (SkofcraftClientConfig.bassScreenShake() * mc.options.screenEffectScale().get());
        if (scale <= 0.0F) {
            return;
        }
        float amount = trauma * trauma * MAX_ANGLE * scale;
        float t = now / 1.0E9F;
        event.setPitch(event.getPitch() + amount * (Mth.sin(t * 41.0F) * 0.7F + Mth.sin(t * 67.0F) * 0.3F));
        event.setYaw(event.getYaw() + amount * 0.5F * Mth.sin(t * 53.0F + 1.3F));
        event.setRoll(event.getRoll() + amount * 0.6F * Mth.sin(t * 37.0F + 2.1F));
    }
}
