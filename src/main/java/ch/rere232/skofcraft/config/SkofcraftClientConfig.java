package ch.rere232.skofcraft.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Settings that only matter on this computer, in skofcraft-client.toml. */
public final class SkofcraftClientConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue AUTO_DOWNLOAD_MEDIA_TOOLS = BUILDER
            .comment("Download yt-dlp and Deno into .minecraft/skofcraft/media by themselves the first time a DJ Mixer needs them.",
                    "Turn off to install them yourself (on the PATH) or with the Install button on the mixer.")
            .define("autoDownloadMediaTools", true);

    public static final ForgeConfigSpec.DoubleValue BASS_SCREEN_SHAKE = BUILDER
            .comment("How hard the screen shakes on bass hits from a DJ Mixer: 0 turns it off, 1 is normal, 2 is double.",
                    "Vanilla's Screen Effect Scale accessibility slider applies on top of this.")
            .defineInRange("bassScreenShake", 1.0D, 0.0D, 2.0D);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private SkofcraftClientConfig() {}

    public static double bassScreenShake() {
        try {
            return BASS_SCREEN_SHAKE.get();
        } catch (IllegalStateException e) {
            return 1.0D; // config not loaded yet
        }
    }

    public static boolean autoDownloadMediaTools() {
        try {
            return AUTO_DOWNLOAD_MEDIA_TOOLS.get();
        } catch (IllegalStateException e) {
            return true; // config not loaded yet
        }
    }
}
