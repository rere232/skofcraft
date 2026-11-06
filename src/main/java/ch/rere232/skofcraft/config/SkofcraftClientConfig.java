package ch.rere232.skofcraft.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Settings that only matter on this computer, in skofcraft-client.toml. */
public final class SkofcraftClientConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue AUTO_DOWNLOAD_MEDIA_TOOLS = BUILDER
            .comment("Download yt-dlp and Deno into .minecraft/skofcraft/media by themselves the first time a DJ Mixer needs them.",
                    "Turn off to install them yourself (on the PATH) or with the Install button on the mixer.")
            .define("autoDownloadMediaTools", true);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private SkofcraftClientConfig() {}

    public static boolean autoDownloadMediaTools() {
        try {
            return AUTO_DOWNLOAD_MEDIA_TOOLS.get();
        } catch (IllegalStateException e) {
            return true; // config not loaded yet
        }
    }
}
