package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.registry.SkofcraftSounds;
import ch.rere232.skofcraft.SkofcraftMod;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Client side of a reactor meltdown: a white flash, camera shake, a boom that arrives late from far away, a fireball
 * and ground shockwave made of particles, and the mushroom cloud itself, drawn as about a thousand big billboarded
 * smoke puffs laid out as a stem, a rolling ring cap with a dome on top, and a dust skirt at the base. The cloud
 * rises for twelve seconds, glows from inside while hot, then hangs in the sky for about a minute.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = SkofcraftMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class NuclearBlastClient {
    private static final int LIFETIME = 1400;
    private static final int FADE_TICKS = 400;
    private static final int FIREBALL_TICKS = 30;
    private static final int SHOCKWAVE_TICKS = 60;
    private static final int RISE_TICKS = 240;
    private static final int SKIRT_TICKS = 90;
    /** Ticks over which the fire inside the cloud cools to smoke. */
    private static final int COOL_TICKS = 300;
    /** Height of the middle of the cap above the blast once fully risen. */
    private static final double CLOUD_HEIGHT = 110.0D;
    /** Blocks per tick: sound is heard later the further away you are. */
    private static final double SOUND_SPEED = 17.0D;
    private static final double FLASH_RANGE = 400.0D;
    private static final double SHAKE_RANGE = 220.0D;
    private static final int SHAKE_TICKS = 90;
    /** A second meltdown this close to a fresh cloud (the other reactor aboard) joins it instead of drawing another. */
    private static final double MERGE_DISTANCE = 40.0D;
    private static final int MERGE_TICKS = 60;

    private static final int STEM = 0;
    private static final int SKIRT = 1;
    private static final int TORUS = 2;
    private static final int DOME = 3;
    private static final int[] PUFF_COUNTS = {320, 140, 520, 260};

    private static final ParticleOptions EMBER = new DustParticleOptions(new Vector3f(1.0F, 0.45F, 0.1F), 4.0F);

    private static final List<Blast> BLASTS = new ArrayList<>();
    private static ClientLevel blastLevel;
    private static float flash;
    /** Ticks the flash stays at full strength before it starts to fade. */
    private static int flashHold;
    private static float flashFade = 0.92F;
    /** Pure white for the ICBM; the reactor flash is a warmer white. */
    private static int flashColor = 0xFFF8E8;
    private static int shakeTicks;
    private static float shakeStrength;

    private NuclearBlastClient() {}

    /** One smoke puff, fixed in the cloud's own shape coordinates; where that lands moves as the cloud grows. */
    private record Puff(int part, float a, float b, float c, float size, float grey, float phase, int variant) {}

    private static final class Blast {
        final double x;
        final double y;
        final double z;
        final int boomDelay;
        final float scale;
        final boolean draconic;
        final List<Puff> puffs = new ArrayList<>();
        int age;

        Blast(double x, double y, double z, int boomDelay, float scale, boolean draconic, RandomSource random) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.boomDelay = boomDelay;
            this.scale = scale;
            this.draconic = draconic;
            // A bigger cloud gets more puffs so it stays as dense
            float density = Math.min(2.0F, scale);
            for (int part = 0; part < PUFF_COUNTS.length; part++) {
                for (int i = 0; i < (int) (PUFF_COUNTS[part] * density); i++) {
                    puffs.add(newPuff(part, random));
                }
            }
        }

        private static Puff newPuff(int part, RandomSource random) {
            float tau = Mth.TWO_PI;
            float grey = 0.55F + random.nextFloat() * 0.2F;
            float phase = random.nextFloat() * tau;
            int variant = random.nextInt(4);
            return switch (part) {
                // a: height along the stem, b: angle, c: how far out from the axis
                case STEM -> new Puff(part, random.nextFloat(), random.nextFloat() * tau, (float) Math.sqrt(random.nextFloat()),
                        8.0F + random.nextFloat() * 5.0F, grey, phase, variant);
                // a: angle, b: fraction of the skirt radius, c: height above the ground
                case SKIRT -> new Puff(part, random.nextFloat() * tau, 0.35F + random.nextFloat() * 0.65F, random.nextFloat(),
                        9.0F + random.nextFloat() * 6.0F, grey + 0.05F, phase, variant);
                // a: angle round the ring, b: angle round the tube, c: depth into the tube
                case TORUS -> new Puff(part, random.nextFloat() * tau, random.nextFloat() * tau, 0.55F + random.nextFloat() * 0.45F,
                        12.0F + random.nextFloat() * 7.0F, grey, phase, variant);
                // a: angle, b: height up the dome, c: how far out
                default -> new Puff(part, random.nextFloat() * tau, (float) Math.sqrt(random.nextFloat()), (float) Math.sqrt(random.nextFloat()),
                        12.0F + random.nextFloat() * 5.0F, grey + 0.08F, phase, variant);
            };
        }
    }

    public static void start(double x, double y, double z) {
        start(x, y, z, 1.0F, false);
    }

    public static void start(double x, double y, double z, float scale, boolean draconic) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        if (mc.level != blastLevel) {
            BLASTS.clear();
            blastLevel = mc.level;
        }
        for (Blast blast : BLASTS) {
            double dx = blast.x - x;
            double dy = blast.y - y;
            double dz = blast.z - z;
            if (blast.age < MERGE_TICKS && dx * dx + dy * dy + dz * dz < MERGE_DISTANCE * MERGE_DISTANCE) {
                return;
            }
        }
        double distance = mc.player.position().distanceTo(new Vec3(x, y, z));
        double flashRange = FLASH_RANGE * scale;
        double shakeRange = SHAKE_RANGE * scale;
        if (distance < flashRange) {
            if (scale > 2.0F) {
                // Giga blast: the whole sky goes white wherever you are, holds, then fades slowly
                float strength = (float) Mth.clamp(1.4D - distance / flashRange, 0.6D, 1.0D);
                flash = Math.max(flash, strength);
                flashHold = Math.max(flashHold, (int) (30 * strength));
                flashFade = 0.965F;
                flashColor = 0xFFFFFF;
            } else {
                flash = Math.max(flash, (float) Mth.clamp(1.0D - distance / flashRange, 0.25D, 1.0D));
                if (flashHold <= 0) {
                    flashFade = 0.92F;
                    flashColor = 0xFFF8E8;
                }
            }
        }
        if (distance < shakeRange) {
            shakeStrength = Math.max(shakeTicks > 0 ? shakeStrength : 0.0F, (float) (1.0D - distance / shakeRange) * 4.0F * Math.min(2.0F, scale));
            shakeTicks = (int) (SHAKE_TICKS * Math.min(2.0F, scale));
        }
        BLASTS.add(new Blast(x, y, z, (int) (distance / SOUND_SPEED), scale, draconic, mc.level.random));
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.END || mc.isPaused()) {
            return;
        }
        if (mc.level == null || mc.level != blastLevel) {
            BLASTS.clear();
            flash = 0.0F;
            flashHold = 0;
            shakeTicks = 0;
            return;
        }
        if (flashHold > 0) {
            flashHold--;
        } else {
            flash *= flashFade;
        }
        if (shakeTicks > 0) {
            shakeTicks--;
        }
        Iterator<Blast> iterator = BLASTS.iterator();
        while (iterator.hasNext()) {
            Blast blast = iterator.next();
            tickParticles(mc, mc.level, blast);
            if (++blast.age > LIFETIME) {
                iterator.remove();
            }
        }
    }

    /** The parts that stay particles: the fireball, the ground shockwave and embers falling out of the hot cap. */
    private static void tickParticles(Minecraft mc, ClientLevel level, Blast b) {
        RandomSource random = level.random;
        int age = b.age;
        if (age == b.boomDelay) {
            boom(mc, level, b);
        }
        if (age <= FIREBALL_TICKS) {
            double radius = (4.0D + age * 0.5D) * b.scale;
            double cy = b.y + age * 0.4D * b.scale;
            for (int i = 0; i < 40; i++) {
                Vec3 dir = randomDirection(random);
                ParticleOptions type = i % 3 == 0 ? ParticleTypes.EXPLOSION
                        : (i % 3 == 1 ? (b.draconic ? ParticleTypes.DRAGON_BREATH : ParticleTypes.FLAME) : ParticleTypes.LAVA);
                spawn(level, type, b.x + dir.x * radius, cy + dir.y * radius, b.z + dir.z * radius,
                        dir.x * 0.2D, dir.y * 0.2D, dir.z * 0.2D);
            }
        }
        if (age <= SHOCKWAVE_TICKS) {
            double ring = age * 3.0D * b.scale;
            int count = 40 + age * 2;
            for (int i = 0; i < count; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double cos = Math.cos(angle);
                double sin = Math.sin(angle);
                spawn(level, ParticleTypes.CLOUD, b.x + cos * ring, b.y + random.nextDouble() * 3.0D, b.z + sin * ring,
                        cos * 1.0D, 0.03D, sin * 1.0D);
            }
        }
        float heat = heat(age);
        if (heat > 0.2F && age > 10) {
            double rise = smoothstep(Math.min(1.0D, age / (double) RISE_TICKS));
            double top = b.y + (capHeight(b, rise) - b.y) * b.scale;
            double major = capMajor(rise, 1.0D) * b.scale;
            for (int i = 0; i < 12; i++) {
                double r = Math.sqrt(random.nextDouble()) * major * 1.2D;
                double angle = random.nextDouble() * Math.PI * 2.0D;
                spawn(level, EMBER, b.x + Math.cos(angle) * r, top - random.nextDouble() * 10.0D, b.z + Math.sin(angle) * r,
                        0.0D, -0.05D, 0.0D);
            }
        }
    }

    private static void boom(Minecraft mc, ClientLevel level, Blast b) {
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 at = camera.getPosition();
        double distance = at.distanceTo(new Vec3(b.x, b.y, b.z));
        float volume = (float) Mth.clamp(1.2D - distance / 500.0D, 0.25D, 1.0D);
        level.playLocalSound(at.x, at.y, at.z, SkofcraftSounds.NUKE_BOOM.get(), SoundSource.BLOCKS, volume, 1.0F, false);
        level.playLocalSound(at.x, at.y, at.z, SkofcraftSounds.NUKE_RUMBLE.get(), SoundSource.WEATHER, volume, 1.0F, false);
        level.playLocalSound(at.x, at.y, at.z, SkofcraftSounds.SHOCKWAVE.get(), SoundSource.BLOCKS, volume * 0.8F, 1.0F, false);
    }

    // ---------------------------------------------------------------- cloud shape

    private static float heat(double age) {
        return (float) Mth.clamp(1.0D - age / COOL_TICKS, 0.0D, 1.0D);
    }

    private static double capHeight(Blast b, double rise) {
        return b.y + 10.0D + CLOUD_HEIGHT * rise;
    }

    private static double capMajor(double rise, double spread) {
        return (6.0D + 26.0D * rise) * spread;
    }

    private static double capMinor(double rise, double spread) {
        return (7.0D + 10.0D * rise) * spread;
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        // After weather, which comes after the vanilla clouds: they depth-test against the puffs instead of painting over them
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER || BLASTS.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Camera camera = event.getCamera();
        Vec3 cam = camera.getPosition();
        Vector3f left = camera.getLeftVector();
        Vector3f up = camera.getUpVector();
        // Vertices are camera-relative and untransformed; the view rotation goes on the model-view matrix below
        Matrix4f pose = new Matrix4f();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(CloudRenderType.PUFFS);
        float partialTick = event.getPartialTick();

        for (Blast blast : BLASTS) {
            double t = blast.age + partialTick;
            double rise = smoothstep(Math.min(1.0D, t / RISE_TICKS));
            double skirtRise = smoothstep(Math.min(1.0D, t / SKIRT_TICKS));
            // Once risen the cloud keeps spreading slowly as it drifts apart
            double spread = 1.0D + Math.max(0.0D, t - RISE_TICKS) / 1500.0D;
            double major = capMajor(rise, spread);
            double minor = capMinor(rise, spread);
            double top = capHeight(blast, rise);
            float heat = heat(t);
            float alpha = (float) (Math.min(1.0D, t / 6.0D) * Mth.clamp((LIFETIME - t) / FADE_TICKS, 0.0D, 1.0D));
            if (alpha <= 0.0F) {
                continue;
            }
            for (Puff puff : blast.puffs) {
                drawPuff(consumer, pose, cam, left, up, blast, puff, t, rise, skirtRise, spread, major, minor, top, heat, alpha);
            }
        }

        // At this stage vanilla has already pushed the view rotation onto the model-view stack for clouds and
        // weather, so set it explicitly rather than stacking it a second time
        PoseStack modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.setIdentity();
        modelView.mulPoseMatrix(event.getPoseStack().last().pose());
        RenderSystem.applyModelViewMatrix();
        // Fog would hide a cloud past the render distance, so turn it off for the puffs
        float fogStart = RenderSystem.getShaderFogStart();
        float fogEnd = RenderSystem.getShaderFogEnd();
        RenderSystem.setShaderFogStart(1.0E6F);
        RenderSystem.setShaderFogEnd(1.0E6F + 1.0F);
        buffers.endBatch(CloudRenderType.PUFFS);
        RenderSystem.setShaderFogStart(fogStart);
        RenderSystem.setShaderFogEnd(fogEnd);
        modelView.popPose();
        RenderSystem.applyModelViewMatrix();
    }

    private static void drawPuff(VertexConsumer consumer, Matrix4f pose, Vec3 cam, Vector3f left, Vector3f up, Blast b, Puff p,
                                 double t, double rise, double skirtRise, double spread, double major, double minor, double top,
                                 float heat, float alpha) {
        double wobble = Math.sin(t * 0.04D + p.phase());
        double px;
        double py;
        double pz;
        double size = p.size() * (0.45D + 0.55D * rise) * spread * (1.0D + 0.06D * wobble);
        float shade;
        float localHeat;
        float puffAlpha = alpha * 0.9F;
        switch (p.part()) {
            case STEM -> {
                double h = p.a();
                double capBase = top - minor * 0.6D;
                double radius = (9.0D - 3.0D * h) * (0.5D + 0.5D * rise) * spread;
                if (h > 0.8D) {
                    radius += (h - 0.8D) / 0.2D * major * 0.45D; // flares out into the cap
                }
                double r = radius * p.c();
                px = b.x + Math.cos(p.b()) * r;
                pz = b.z + Math.sin(p.b()) * r;
                py = b.y + h * (capBase - b.y);
                shade = 0.8F;
                localHeat = heat * (float) Math.sqrt(1.0D - h);
            }
            case SKIRT -> {
                double r = (10.0D + 50.0D * skirtRise) * spread * p.b();
                px = b.x + Math.cos(p.a()) * r;
                pz = b.z + Math.sin(p.a()) * r;
                py = b.y + p.c() * 5.0D;
                size = p.size() * (0.4D + 0.6D * skirtRise) * spread;
                shade = 0.88F;
                localHeat = heat * 0.3F;
                puffAlpha = alpha * 0.75F;
            }
            case TORUS -> {
                // Rolling: up the inside, out over the top, down the outside
                double phi = p.b() - t * 0.006D;
                double ring = major + minor * p.c() * Math.cos(phi);
                px = b.x + Math.cos(p.a()) * ring;
                pz = b.z + Math.sin(p.a()) * ring;
                py = top + minor * p.c() * Math.sin(phi) * 0.75D;
                shade = 0.72F + 0.28F * (float) Math.sin(phi);
                localHeat = heat * (0.55F + 0.45F * (float) -Math.cos(phi));
            }
            default -> {
                double v = p.b();
                double r = major * 0.9D * Math.sqrt(1.0D - v * v) * p.c();
                px = b.x + Math.cos(p.a()) * r;
                pz = b.z + Math.sin(p.a()) * r;
                py = top + minor * 0.3D + v * minor * 0.9D;
                shade = 1.0F;
                localHeat = heat * 0.7F;
            }
        }

        float grey = p.grey() * (0.85F + 0.15F * shade) * shade;
        float glow = Mth.clamp(localHeat * 2.0F, 0.0F, 1.0F);
        float red = Mth.lerp(glow, grey, 1.0F);
        float green = Mth.lerp(glow, grey * 0.95F, 0.5F + 0.35F * localHeat);
        float blue = Mth.lerp(glow, grey * 0.9F, 0.1F + 0.2F * localHeat);
        if (b.draconic) {
            // Dragon fire: magenta-hot, cooling to a violet-tinged smoke
            red = Mth.lerp(glow, grey * 0.92F, 0.85F + 0.15F * localHeat);
            green = Mth.lerp(glow, grey * 0.85F, 0.25F + 0.3F * localHeat);
            blue = Mth.lerp(glow, grey * 1.0F, 1.0F);
        }
        // Scale the whole shape about the blast point
        px = b.x + (px - b.x) * b.scale;
        py = b.y + (py - b.y) * b.scale;
        pz = b.z + (pz - b.z) * b.scale;
        size *= b.scale;
        int light = LightTexture.pack((int) (15.0F * Mth.clamp(localHeat * 1.8F, 0.0F, 1.0F)), 15);

        float x = (float) (px - cam.x);
        float y = (float) (py - cam.y);
        float z = (float) (pz - cam.z);
        float half = (float) (size * 0.5D);
        float lx = left.x() * half;
        float ly = left.y() * half;
        float lz = left.z() * half;
        float ux = up.x() * half;
        float uy = up.y() * half;
        float uz = up.z() * half;
        float u0 = (p.variant() % 2) * 0.5F;
        float v0 = (p.variant() / 2) * 0.5F;
        float u1 = u0 + 0.5F;
        float v1 = v0 + 0.5F;
        consumer.vertex(pose, x + lx - ux, y + ly - uy, z + lz - uz).uv(u1, v1).color(red, green, blue, puffAlpha).uv2(light).endVertex();
        consumer.vertex(pose, x + lx + ux, y + ly + uy, z + lz + uz).uv(u1, v0).color(red, green, blue, puffAlpha).uv2(light).endVertex();
        consumer.vertex(pose, x - lx + ux, y - ly + uy, z - lz + uz).uv(u0, v0).color(red, green, blue, puffAlpha).uv2(light).endVertex();
        consumer.vertex(pose, x - lx - ux, y - ly - uy, z - lz - uz).uv(u0, v1).color(red, green, blue, puffAlpha).uv2(light).endVertex();
    }

    // ---------------------------------------------------------------- helpers

    private static void spawn(ClientLevel level, ParticleOptions type, double x, double y, double z, double vx, double vy, double vz) {
        level.addAlwaysVisibleParticle(type, true, x, y, z, vx, vy, vz);
    }

    private static Vec3 randomDirection(RandomSource random) {
        double y = random.nextDouble() * 2.0D - 1.0D;
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double r = Math.sqrt(1.0D - y * y);
        return new Vec3(Math.cos(angle) * r, y, Math.sin(angle) * r);
    }

    private static double smoothstep(double t) {
        return t * t * (3.0D - 2.0D * t);
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (shakeTicks <= 0) {
            return;
        }
        double t = (Minecraft.getInstance().level == null ? 0L : Minecraft.getInstance().level.getGameTime()) + event.getPartialTick();
        float amplitude = shakeStrength * shakeTicks / SHAKE_TICKS;
        event.setPitch(event.getPitch() + (float) Math.sin(t * 1.9D) * amplitude);
        event.setYaw(event.getYaw() + (float) Math.sin(t * 1.3D + 2.0D) * amplitude * 0.6F);
        event.setRoll(event.getRoll() + (float) Math.sin(t * 2.7D + 1.0D) * amplitude);
    }

    /** White flash over the whole screen, fading out. */
    public static void renderFlash(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
        if (flash < 0.01F) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.fill(0, 0, width, height, ((int) (Mth.clamp(flash, 0.0F, 1.0F) * 255.0F) << 24) | flashColor);
        RenderSystem.disableBlend();
    }
}
