package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.entity.FpvDrone;
import ch.rere232.skofcraft.keybind.SkofcraftKeybinds;
import ch.rere232.skofcraft.network.FpvDroneStateC2SPacket;
import ch.rere232.skofcraft.network.SkofcraftNetwork;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Being on the link. The camera moves to the drone, which on its own stops vanilla from walking the player or sending
 * their position and rotation, so the mouse keeps turning the local player and that rotation steers the drone.
 */
@Mod.EventBusSubscriber(modid = SkofcraftMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class FpvDroneClient {
    /** The wide, slightly fisheye look of a real FPV lens. */
    private static final double BASE_FOV = 105.0D;
    private static final double FOV_PER_SPEED = 6.0D;
    private static final float ROLL_PER_STRAFE = 22.0F;
    private static final float ROLL_PER_TURN = 1.2F;
    private static final float MAX_ROLL = 40.0F;
    private static final int NO_SIGNAL_TICKS = 24;

    private static FpvDrone linked;
    private static float bodyYaw;
    private static float bodyPitch;
    private static CameraType bodyCameraType;
    private static float lastYaw;
    private static float roll;
    private static float rollO;
    private static float strafe;
    private static boolean sneakReleased;
    private static int noSignalTicks;
    /** The real pitch while the body is drawn with the one it had when the link came up. */
    private static float renderPitch;
    private static float renderPitchO;

    private FpvDroneClient() {}

    static FpvDrone linked() {
        return linked;
    }

    static int noSignalTicks() {
        return noSignalTicks;
    }

    public static boolean isLocalPilot(FpvDrone drone) {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && drone.getPilotId() == mc.player.getId();
    }

    /** Runs on every drone in the client's world. */
    public static void onDroneTick(FpvDrone drone) {
        if (!drone.soundStarted) {
            drone.soundStarted = true;
            Minecraft.getInstance().getSoundManager().play(new FpvDroneSound(drone));
        }
        if (linked == null && isLocalPilot(drone)) {
            bind(drone);
        }
    }

    private static void bind(FpvDrone drone) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        linked = drone;
        bodyYaw = player.getYRot();
        bodyPitch = player.getXRot();
        bodyCameraType = mc.options.getCameraType();
        steerTo(player, drone.getYRot(), drone.getXRot());
        lastYaw = drone.getYRot();
        roll = 0.0F;
        rollO = 0.0F;
        strafe = 0.0F;
        sneakReleased = false;
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        mc.setCameraEntity(drone);
    }

    private static void unbind(Minecraft mc) {
        boolean lost = linked.isRemoved();
        linked = null;
        if (bodyCameraType != null) {
            mc.options.setCameraType(bodyCameraType);
        }
        if (mc.player != null) {
            mc.setCameraEntity(mc.player);
            steerTo(mc.player, bodyYaw, bodyPitch);
        }
        // A controlled disconnect just hands you your eyes back; the rest is the feed cutting out
        noSignalTicks = lost || mc.player == null ? NO_SIGNAL_TICKS : NO_SIGNAL_TICKS / 3;
    }

    private static void steerTo(LocalPlayer player, float yaw, float pitch) {
        player.setYRot(yaw);
        player.setXRot(pitch);
        player.yRotO = yaw;
        player.xRotO = pitch;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (noSignalTicks > 0 && !mc.isPaused()) {
            noSignalTicks--;
        }
        if (linked == null) {
            return;
        }
        if (mc.player == null || mc.level == null || linked.isRemoved() || linked.level() != mc.level || !isLocalPilot(linked)) {
            unbind(mc);
            return;
        }
        // Vanilla hands the camera back on some events (respawning, for one), and F5 has no business here
        if (mc.getCameraEntity() != linked) {
            mc.setCameraEntity(linked);
        }
        if (!mc.options.getCameraType().isFirstPerson()) {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        }
        rollO = roll;
        float turn = Mth.wrapDegrees(mc.player.getYRot() - lastYaw);
        lastYaw = mc.player.getYRot();
        float wanted = Mth.clamp(-strafe * ROLL_PER_STRAFE + turn * ROLL_PER_TURN, -MAX_ROLL, MAX_ROLL);
        roll += (wanted - roll) * 0.25F;
    }

    /** The sticks, read from the keys since the player's own input is zeroed below. */
    public static FpvDrone.Input input(FpvDrone drone) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null) {
            strafe = 0.0F;
            return new FpvDrone.Input(0.0F, 0.0F, 0.0F, drone.getYRot(), drone.getXRot());
        }
        float forward = axis(mc.options.keyUp.isDown(), mc.options.keyDown.isDown());
        strafe = axis(mc.options.keyLeft.isDown(), mc.options.keyRight.isDown());
        float vertical = axis(mc.options.keyJump.isDown(), SkofcraftKeybinds.HELICOPTER_DESCEND.isDown());
        return new FpvDrone.Input(forward, strafe, vertical, player.getYRot(), player.getXRot());
    }

    private static float axis(boolean positive, boolean negative) {
        return (positive ? 1.0F : 0.0F) - (negative ? 1.0F : 0.0F);
    }

    /** Sends this tick's flight to the server, along with whatever the pilot just decided. */
    public static void report(FpvDrone drone, boolean impact, boolean flooded) {
        Minecraft mc = Minecraft.getInstance();
        int flags = (impact ? FpvDrone.FLAG_IMPACT : 0) | (drone.onGround() ? FpvDrone.FLAG_ON_GROUND : 0);
        if (mc.screen == null && mc.mouseHandler.isMouseGrabbed() && drone.isArmed() && mc.options.keyAttack.isDown()) {
            flags |= FpvDrone.FLAG_DETONATE;
        }
        // Must be let go first: the launch may well have happened mid-sneak
        boolean sneaking = mc.screen == null && mc.options.keyShift.isDown();
        sneakReleased |= !sneaking;
        if ((sneaking && sneakReleased) || outOfRange(drone) || (flooded && !drone.isArmed())) {
            flags |= FpvDrone.FLAG_DISCONNECT;
        }
        SkofcraftNetwork.CHANNEL.sendToServer(new FpvDroneStateC2SPacket(drone.getId(), drone.getX(), drone.getY(), drone.getZ(),
                drone.getYRot(), drone.getXRot(), flags));
    }

    /** How far the drone can get before this client runs out of chunks to show, whatever the server would allow. */
    static int signalRange(FpvDrone drone) {
        return Math.min(drone.getSignalRange(), Minecraft.getInstance().options.getEffectiveRenderDistance() * 16);
    }

    /** From 0 next to the pilot to 1 at the edge of the signal. */
    static float signalUsed(FpvDrone drone) {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? 0.0F : (float) (drone.horizontalDistanceTo(mc.player) / Math.max(1, signalRange(drone)));
    }

    private static boolean outOfRange(FpvDrone drone) {
        return signalUsed(drone) >= 1.0F;
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (linked != null) {
            Input input = event.getInput();
            input.forwardImpulse = 0.0F;
            input.leftImpulse = 0.0F;
            input.up = false;
            input.down = false;
            input.left = false;
            input.right = false;
            input.jumping = false;
            input.shiftKeyDown = false;
        }
    }

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (linked != null) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    /** Applied per frame from the player's rotation: the drone's own only changes per tick, which would judder. */
    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (linked != null && mc.player != null) {
            float partial = (float) event.getPartialTick();
            event.setYaw(mc.player.getViewYRot(partial));
            event.setPitch(mc.player.getViewXRot(partial));
            event.setRoll(event.getRoll() + Mth.lerp(partial, rollO, roll));
        }
    }

    @SubscribeEvent
    public static void onFov(ViewportEvent.ComputeFov event) {
        if (linked != null && event.usedConfiguredFov()) {
            event.setFOV(BASE_FOV + linked.position().subtract(linked.xo, linked.yo, linked.zo).length() * FOV_PER_SPEED);
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (linked != null) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
        if (linked != null && (event.getOverlay() == VanillaGuiOverlay.CROSSHAIR.type() || event.getOverlay() == VanillaGuiOverlay.EXPERIENCE_BAR.type())) {
            event.setCanceled(true);
        }
    }

    /** Your body shouldn't spin around when you look back at it from the air. */
    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        if (linked != null && event.getEntity() == Minecraft.getInstance().player) {
            LocalPlayer player = Minecraft.getInstance().player;
            renderPitch = player.getXRot();
            renderPitchO = player.xRotO;
            player.setXRot(bodyPitch);
            player.xRotO = bodyPitch;
            player.yBodyRot = bodyYaw;
            player.yBodyRotO = bodyYaw;
            player.yHeadRot = bodyYaw;
            player.yHeadRotO = bodyYaw;
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        if (linked != null && event.getEntity() == Minecraft.getInstance().player) {
            LocalPlayer player = Minecraft.getInstance().player;
            player.setXRot(renderPitch);
            player.xRotO = renderPitchO;
        }
    }
}
