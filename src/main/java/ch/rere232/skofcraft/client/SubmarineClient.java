package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.entity.NuclearSubmarine;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderBlockScreenEffectEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.joml.Vector3f;

import java.lang.reflect.Method;

/** What it looks like from inside: a dry view, floodlights to see further, and a chase camera that fits a 48-block boat. */
@Mod.EventBusSubscriber(modid = SkofcraftMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class SubmarineClient {
    private static final float FOG_NEAR = 8.0F;
    private static final float FOG_FAR = 128.0F;
    private static final double CHASE_DISTANCE = 34.0D;
    private static final double CHASE_HEIGHT = 8.0D;
    private static final Method SET_POSITION = ObfuscationReflectionHelper.findMethod(Camera.class, "m_90584_", double.class, double.class, double.class);

    private SubmarineClient() {}

    private static boolean aboard() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.getVehicle() instanceof NuclearSubmarine;
    }

    @SubscribeEvent
    public static void onFog(ViewportEvent.RenderFog event) {
        if (event.getType() == FogType.WATER && aboard()) {
            event.setNearPlaneDistance(FOG_NEAR);
            event.setFarPlaneDistance(FOG_FAR);
            event.setCanceled(true);
        }
    }

    /** You're inside, so no water on the screen. */
    @SubscribeEvent
    public static void onScreenEffect(RenderBlockScreenEffectEvent event) {
        if (event.getOverlayType() == RenderBlockScreenEffectEvent.OverlayType.WATER && aboard()) {
            event.setCanceled(true);
        }
    }

    /** Vanilla's third person stops 4 blocks back, which is inside the sail. Pull out until the whole boat fits. */
    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        Camera camera = event.getCamera();
        if (!aboard() || !camera.isDetached()) {
            return;
        }
        Entity player = mc.player;
        Vector3f forward = camera.getLookVector();
        Vec3 focus = player.getEyePosition((float) event.getPartialTick());
        Vec3 wanted = focus.add(-forward.x() * CHASE_DISTANCE, -forward.y() * CHASE_DISTANCE + CHASE_HEIGHT, -forward.z() * CHASE_DISTANCE);
        HitResult hit = mc.level.clip(new ClipContext(focus, wanted, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player));
        Vec3 position = hit.getType() == HitResult.Type.MISS ? wanted : hit.getLocation().add(forward.x(), forward.y(), forward.z());
        try {
            SET_POSITION.invoke(camera, position.x, position.y, position.z);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Couldn't move the submarine camera", e);
        }
    }
}
