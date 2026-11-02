package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.entity.FpvDrone;
import ch.rere232.skofcraft.registry.SkofcraftSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

/** The motors. Heard from far off, since being able to hear one coming is the whole horror of them. */
public class FpvDroneSound extends AbstractTickableSoundInstance {
    private static final float FLYING_VOLUME = 2.5F;
    private static final float ONBOARD_VOLUME = 0.35F;
    private static final float IDLE_VOLUME = 0.25F;

    private final FpvDrone drone;

    public FpvDroneSound(FpvDrone drone) {
        super(SkofcraftSounds.FPV_MOTOR.get(), SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.drone = drone;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0F;
        this.x = drone.getX();
        this.y = drone.getY();
        this.z = drone.getZ();
    }

    @Override
    public void tick() {
        if (drone.isRemoved()) {
            stop();
            return;
        }
        x = drone.getX();
        y = drone.getY();
        z = drone.getZ();
        boolean powered = drone.isLinked() && drone.getBattery() > 0;
        float wanted = !powered ? 0.0F : drone.onGround() ? IDLE_VOLUME : FpvDroneClient.isLocalPilot(drone) ? ONBOARD_VOLUME : FLYING_VOLUME;
        volume += (wanted - volume) * 0.2F;
        double speed = drone.position().subtract(drone.xo, drone.yo, drone.zo).length();
        float wantedPitch = 0.8F + Mth.clamp((float) speed * 0.5F, 0.0F, 0.9F);
        pitch += (wantedPitch - pitch) * 0.2F;
    }
}
