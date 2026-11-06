package ch.rere232.skofcraft.client.audio;

import ch.rere232.skofcraft.registry.SkofcraftSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.concurrent.CompletableFuture;

/**
 * The song coming out of a mixer's speakers, played through Minecraft's own sound engine in the Jukebox/Note Blocks
 * category. It is stereo, so OpenAL cannot place it in the world; the session works the loudness out from the
 * distance to the nearest speaker instead.
 */
@OnlyIn(Dist.CLIENT)
final class MixerSound extends AbstractTickableSoundInstance {
    private final PcmStream stream;
    private volatile float targetVolume;

    MixerSound(PcmStream stream, float volume) {
        super(SkofcraftSounds.DJ_MIXER_STREAM.get(), SoundSource.RECORDS, SoundInstance.createUnseededRandom());
        this.stream = stream;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.looping = false;
        this.delay = 0;
        this.x = 0.0D;
        this.y = 0.0D;
        this.z = 0.0D;
        this.volume = volume;
        this.targetVolume = volume;
    }

    void setTargetVolume(float volume) {
        this.targetVolume = volume;
    }

    void finish() {
        stop();
    }

    @Override
    public void tick() {
        volume += (targetVolume - volume) * 0.3F;
        if (Math.abs(targetVolume - volume) < 0.002F) {
            volume = targetVolume;
        }
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public CompletableFuture<AudioStream> getStream(SoundBufferLibrary library, Sound sound, boolean looping) {
        return CompletableFuture.completedFuture(stream);
    }
}
