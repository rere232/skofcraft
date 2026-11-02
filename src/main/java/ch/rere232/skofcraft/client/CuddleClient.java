package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.cuddle.CuddleManager;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.player.Input;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Quaternionf;

import java.util.HashMap;
import java.util.Map;

/**
 * Who is cuddling whom, as far as this client knows, and the part of the animation that moves whole players: they turn
 * to face each other, step closer than their hitboxes allow, lean in and sway together. The arms are in
 * {@link CuddlePlayerModel}. The rotations are only changed for the length of the render, so looking around stays free.
 */
@Mod.EventBusSubscriber(modid = SkofcraftMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class CuddleClient {
    private static final float FADE_IN_TICKS = 12.0F;
    private static final float FADE_OUT_TICKS = 8.0F;
    /** How far each player is drawn towards the other, in blocks. */
    private static final double CLOSE_IN = 0.08D;
    private static final float LEAN_RADIANS = 4.0F * Mth.DEG_TO_RAD;
    private static final float SWAY_RADIANS = 2.5F * Mth.DEG_TO_RAD;
    private static final float SWAY_SPEED = 0.05F;

    private static final class Entry {
        final int partner;
        /** The one who asked has their arms over the shoulders; the one who accepted has them around the waist. */
        final boolean over;
        final long start;
        long end = -1L;

        Entry(int partner, boolean over, long start) {
            this.partner = partner;
            this.over = over;
            this.start = start;
        }

        float blend(float time) {
            float in = (time - start) / FADE_IN_TICKS;
            float out = end < 0L ? 1.0F : 1.0F - (time - end) / FADE_OUT_TICKS;
            float amount = Mth.clamp(Math.min(in, out), 0.0F, 1.0F);
            return amount * amount * (3.0F - 2.0F * amount);
        }
    }

    /** What the model should do for the player being drawn right now. */
    public record Pose(Player player, boolean over, float blend, float time) {}

    private static final Map<Integer, Entry> ENTRIES = new HashMap<>();
    private static Pose current;
    private static float savedBodyRot;
    private static float savedBodyRotO;
    private static float savedHeadRot;
    private static float savedHeadRotO;
    private static CameraType cameraBefore;

    private CuddleClient() {}

    public static Pose currentPose() {
        return current;
    }

    public static void sync(int first, int second, boolean active) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        long now = mc.level.getGameTime();
        if (active) {
            ENTRIES.computeIfAbsent(first, id -> new Entry(second, true, now)).end = -1L;
            ENTRIES.computeIfAbsent(second, id -> new Entry(first, false, now)).end = -1L;
        } else {
            for (int id : new int[] {first, second}) {
                Entry entry = ENTRIES.get(id);
                if (entry != null && entry.end < 0L) {
                    entry.end = now;
                }
            }
        }
        if (mc.player != null && (mc.player.getId() == first || mc.player.getId() == second)) {
            setThirdPerson(mc, active);
        }
    }

    /** Your own arms are the whole point, so the camera steps back for the cuddle, and returns afterwards. */
    private static void setThirdPerson(Minecraft mc, boolean active) {
        if (active && cameraBefore == null) {
            cameraBefore = mc.options.getCameraType();
            if (cameraBefore.isFirstPerson()) {
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                mc.levelRenderer.needsUpdate();
            }
        } else if (!active && cameraBefore != null) {
            mc.options.setCameraType(cameraBefore);
            mc.levelRenderer.needsUpdate();
            cameraBefore = null;
        }
    }

    private static boolean localPlayerCuddling() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return false;
        }
        Entry entry = ENTRIES.get(mc.player.getId());
        return entry != null && entry.end < 0L;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.END || mc.level == null || ENTRIES.isEmpty()) {
            return;
        }
        long now = mc.level.getGameTime();
        ENTRIES.entrySet().removeIf(pair -> pair.getValue().end >= 0L && now - pair.getValue().end > FADE_OUT_TICKS);
    }

    /** Hold still. Sneaking is left alone: it is how you let go. */
    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (localPlayerCuddling()) {
            Input input = event.getInput();
            input.up = false;
            input.down = false;
            input.left = false;
            input.right = false;
            input.jumping = false;
            input.forwardImpulse = 0.0F;
            input.leftImpulse = 0.0F;
        }
    }

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if ((event.isAttack() || event.isUseItem()) && localPlayerCuddling()) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderPre(RenderPlayerEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = event.getEntity();
        Entry entry = ENTRIES.get(player.getId());
        if (entry == null || mc.level == null || (player == mc.player && mc.screen instanceof EffectRenderingInventoryScreen<?>)) {
            return;
        }
        Entity partner = mc.level.getEntity(entry.partner);
        if (partner == null) {
            return;
        }
        float partial = event.getPartialTick();
        float time = mc.level.getGameTime() + partial;
        float blend = entry.blend(time);
        Vec3 here = player.getPosition(partial);
        Vec3 there = partner.getPosition(partial);
        Vec3 toward = new Vec3(there.x - here.x, 0.0D, there.z - here.z);
        if (toward.lengthSqr() < 1.0E-4D) {
            return;
        }
        toward = toward.normalize();

        savedBodyRot = player.yBodyRot;
        savedBodyRotO = player.yBodyRotO;
        savedHeadRot = player.yHeadRot;
        savedHeadRotO = player.yHeadRotO;
        float facing = (float) (Mth.atan2(toward.z, toward.x) * Mth.RAD_TO_DEG) - 90.0F;
        float body = Mth.rotLerp(blend, Mth.rotLerp(partial, player.yBodyRotO, player.yBodyRot), facing);
        player.yBodyRot = body;
        player.yBodyRotO = body;
        player.yHeadRot = body;
        player.yHeadRotO = body;

        event.getPoseStack().pushPose();
        event.getPoseStack().translate(toward.x * CLOSE_IN * blend, 0.0D, toward.z * CLOSE_IN * blend);
        // Leaning is around each player's own sideways axis. Swaying has to be the same rotation for both of them,
        // so its axis always points from the one who asked to the one who accepted.
        event.getPoseStack().mulPose(new Quaternionf().rotationAxis(LEAN_RADIANS * blend, (float) toward.z, 0.0F, (float) -toward.x));
        float sign = entry.over ? 1.0F : -1.0F;
        event.getPoseStack().mulPose(new Quaternionf().rotationAxis(Mth.sin(time * SWAY_SPEED) * SWAY_RADIANS * blend,
                (float) toward.x * sign, 0.0F, (float) toward.z * sign));
        current = new Pose(player, entry.over, blend, time - entry.start);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderPost(RenderPlayerEvent.Post event) {
        if (current != null && current.player() == event.getEntity()) {
            Player player = current.player();
            player.yBodyRot = savedBodyRot;
            player.yBodyRotO = savedBodyRotO;
            player.yHeadRot = savedHeadRot;
            player.yHeadRotO = savedHeadRotO;
            event.getPoseStack().popPose();
            current = null;
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ENTRIES.clear();
        current = null;
        if (cameraBefore != null) {
            Minecraft.getInstance().options.setCameraType(cameraBefore);
            cameraBefore = null;
        }
    }
}
