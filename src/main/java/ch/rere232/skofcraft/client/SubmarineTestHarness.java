package ch.rere232.skofcraft.client;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.entity.GigaIcbm;
import ch.rere232.skofcraft.entity.NuclearSubmarine;
import ch.rere232.skofcraft.entity.PilotedAircraft;
import ch.rere232.skofcraft.entity.SubmarinePart;
import ch.rere232.skofcraft.entity.Torpedo;
import ch.rere232.skofcraft.keybind.SkofcraftKeybinds;
import ch.rere232.skofcraft.network.ApacheFireC2SPacket;
import ch.rere232.skofcraft.network.SkofcraftNetwork;
import ch.rere232.skofcraft.radar.RadarCategory;
import ch.rere232.skofcraft.registry.SkofcraftEntities;
import ch.rere232.skofcraft.registry.SkofcraftItems;
import ch.rere232.skofcraft.space.IcbmTargets;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.function.BiConsumer;
import java.util.stream.StreamSupport;

/** TEMPORARY end-to-end test, never committed. */
@Mod.EventBusSubscriber(modid = SkofcraftMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class SubmarineTestHarness {
    private static final int START = 160;
    private static final int TOP = 150;
    private static boolean opened;
    private static int ticks;
    private static int cx;
    private static int cz;
    private static int boatId = -1;
    private static int loadedId = -1;
    private static int secondId = -1;
    private static BlockPos cube;

    private static void log(String text) {
        System.out.println("[harness] t=" + (ticks - START) + " " + text);
    }

    @SubscribeEvent
    public static void onScreen(ScreenEvent.Init.Post event) {
        if (!opened && event.getScreen() instanceof TitleScreen) {
            opened = true;
            Minecraft mc = Minecraft.getInstance();
            mc.options.pauseOnLostFocus = false;
            mc.createWorldOpenFlows().loadLevel(event.getScreen(), "sub_test");
        }
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, "sub_" + name + ".png", mc.getMainRenderTarget(), c -> {});
    }

    private static void onServer(Minecraft mc, BiConsumer<ServerLevel, ServerPlayer> work) {
        MinecraftServer server = mc.getSingleplayerServer();
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayers().get(0);
            work.accept(player.serverLevel(), player);
        });
    }

    private static NuclearSubmarine serverBoat(ServerLevel level) {
        return level.getEntity(boatId) instanceof NuclearSubmarine boat ? boat : null;
    }

    private static void wall(ServerLevel level, boolean present) {
        for (int x = cx - 5; x <= cx + 5; x++) {
            for (int y = TOP - 8; y <= TOP + 6; y++) {
                level.setBlockAndUpdate(new BlockPos(x, y, cz + 47), present ? Blocks.STONE.defaultBlockState() : y <= TOP ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState());
            }
        }
    }

    private static int stoneInCube(ServerLevel level) {
        int count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(cube.offset(-4, -4, -4), cube.offset(4, 4, 4))) {
            if (level.getBlockState(pos).is(Blocks.STONE)) {
                count++;
            }
        }
        return count;
    }

    private static long count(Minecraft mc, Class<?> type) {
        return StreamSupport.stream(mc.level.entitiesForRendering().spliterator(), false).filter(type::isInstance).count();
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.END || mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) {
            return;
        }
        ticks++;
        if (ticks == 40) {
            cx = mc.player.getBlockX();
            cz = mc.player.getBlockZ();
            onServer(mc, (level, player) -> {
                level.setDayTime(6000);
                player.setGameMode(GameType.CREATIVE);
                for (int x = cx - 30; x <= cx + 30; x++) {
                    for (int z = cz - 40; z <= cz + 180; z++) {
                        for (int y = 110; y <= TOP + 30; y++) {
                            level.setBlock(new BlockPos(x, y, z), y <= TOP ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
                        }
                    }
                }
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
                player.teleportTo(cx + 4.5D, TOP + 3.0D, cz + 15.5D);
            });
        }
        int t = ticks - START;
        if (t < 0) {
            return;
        }
        if (t == 0) {
            onServer(mc, (level, player) -> {
                player.setYRot(0.0F);
                player.setXRot(90.0F);
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(SkofcraftItems.NUCLEAR_SUBMARINE.get()));
                var result = SkofcraftItems.NUCLEAR_SUBMARINE.get().use(level, player, InteractionHand.MAIN_HAND);
                var placed = level.getEntitiesOfClass(NuclearSubmarine.class, player.getBoundingBox().inflate(120.0D));
                log("item use=" + result.getResult() + " placed=" + placed.size() + " left=" + player.getMainHandItem());
                placed.forEach(Entity::discard);
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                double y = TOP + 1.0D - NuclearSubmarine.WATERLINE;
                NuclearSubmarine boat = SkofcraftEntities.NUCLEAR_SUBMARINE.get().create(level);
                boat.place(cx + 0.5D, y, cz + 0.5D, 0.0F);
                level.addFreshEntity(boat);
                boatId = boat.getId();
                log("radar surfaced=" + RadarCategory.of(boat));
                wall(level, true);
                // Comes into the world the way a saved boat does: no place() call
                NuclearSubmarine loaded = SkofcraftEntities.NUCLEAR_SUBMARINE.get().create(level);
                loaded.setPos(cx - 20.5D, y, cz + 0.5D);
                level.addFreshEntity(loaded);
                loadedId = loaded.getId();
            });
        }
        if (t == 15) {
            onServer(mc, (level, player) -> {
                NuclearSubmarine loaded = (NuclearSubmarine) level.getEntity(loadedId);
                log("after load hull=" + loaded.getHealth() + " (want 400)");
                // At the stern, far from the middle of the boat, with several parts in range
                level.explode(null, loaded.getX(), loaded.getY() + 2.0D, loaded.getZ() - 22.0D, 6.0F, net.minecraft.world.level.Level.ExplosionInteraction.NONE);
                log("after stern blast hull=" + loaded.getHealth());
                loaded.discard();
            });
        }
        if (t < 25) {
            mc.player.setYRot(90.0F);
            mc.player.setXRot(35.0F);
        }
        if (t == 25) {
            Entity picked = mc.hitResult instanceof EntityHitResult hit ? hit.getEntity() : null;
            log("picked=" + (picked == null ? "null" : picked.getClass().getSimpleName()));
            if (picked instanceof SubmarinePart) {
                log("interact=" + mc.gameMode.interact(mc.player, picked, InteractionHand.MAIN_HAND));
            }
        }
        if (t == 35) {
            log("riding=" + mc.player.isPassenger() + " piloted=" + (AircraftClient.piloted() != null));
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
        if (!(mc.player.getVehicle() instanceof NuclearSubmarine boat)) {
            // The keys below are only updated while aboard, so this one would stay held and throw us off the next boat
            mc.options.keyShift.setDown(false);
            if (t == 35 || t == 100 || t == 400) {
                log("NOT ABOARD");
            }
            if (t == 850) {
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                mc.player.setYRot(90.0F);
                mc.player.setXRot(25.0F);
                onServer(mc, (level, player) -> log("server deckSafe=" + (serverBoat(level) == null ? "gone" : serverBoat(level).isDeckSafe())
                        + " standing y=" + player.getY() + " boatY=" + (serverBoat(level) == null ? 0 : serverBoat(level).getY())));
            }
            if (t == 855) {
                shot(mc, "deck_view");
            }
            if (t == 860) {
                onServer(mc, (level, player) -> {
                    NuclearSubmarine second = SkofcraftEntities.NUCLEAR_SUBMARINE.get().create(level);
                    second.place(cx + 15.5D, TOP + 1.0D - NuclearSubmarine.WATERLINE, cz + 0.5D, 0.0F);
                    level.addFreshEntity(second);
                    secondId = second.getId();
                    log("second boat boarded=" + player.startRiding(second, true));
                });
            }
            if (t == 990) {
                onServer(mc, (level, player) -> log("after leaving at speed hull=" + ((NuclearSubmarine) level.getEntity(secondId)).getHealth() + " (want 400)"));
            }
            if (t == 995) {
                onServer(mc, (level, player) -> log("reboard=" + player.startRiding(level.getEntity(secondId), true)));
            }
            if (t == 1020) {
                onServer(mc, (level, player) -> {
                    player.getAbilities().invulnerable = true;
                    player.teleportTo(cx + 25.0D, TOP + 40.0D, cz + 0.5D);
                    NuclearSubmarine sub = serverBoat(level);
                    if (sub != null) {
                        log("hurt=" + sub.hurt(level.damageSources().magic(), 1000.0F) + " removed=" + sub.isRemoved());
                    }
                });
            }
            if (t == 1090) {
                log("survived the meltdown. done");
                shot(mc, "after_meltdown");
            }
            if (t == 1095) {
                mc.stop();
            }
            return;
        }

        mc.options.keyUp.setDown((t >= 60 && t < 170) || (t >= 870 && t < 970));
        SkofcraftKeybinds.HELICOPTER_DESCEND.setDown(t >= 215 && t < 330);
        mc.options.keyJump.setDown(t >= 380 && t < 520);
        mc.options.keyShift.setDown((t >= 830 && t < 835) || (t >= 972 && t < 977) || (t >= 1010 && t < 1015));

        if (t == 971) {
            log("second boat underway throttle=" + boat.getThrottle() + " speed=" + boat.shownSpeed);
        }
        if (t == 1005) {
            log("reboarded throttle=" + boat.getThrottle() + " speed=" + boat.shownSpeed + " (want 0, 0)");
        }

        if (t < 60) {
            mc.player.setYRot(90.0F);
            mc.player.setXRot(20.0F);
        } else if (t < 355) {
            mc.player.setYRot(boat.getYRot() + 120.0F);
            mc.player.setXRot(15.0F);
        } else if (t < 380) {
            mc.player.setYRot(boat.getYRot());
            mc.player.setXRot(0.0F);
        } else if (t >= 525 && t < 545) {
            mc.player.setYRot(boat.getYRot());
            mc.player.setXRot(0.0F);
        } else if (t >= 590 && t < 800) {
            mc.player.setYRot(boat.getYRot() + 60.0F);
            mc.player.setXRot(45.0F);
        } else {
            mc.player.setYRot(boat.getYRot() + 90.0F);
            mc.player.setXRot(15.0F);
        }

        if (t == 50) {
            shot(mc, "surfaced_side");
        }
        if (t == 120) {
            log("underway z=" + boat.getZ() + " throttle=" + boat.getThrottle() + " speed=" + boat.shownSpeed);
            shot(mc, "underway");
        }
        if (t == 190) {
            log("after ram z=" + boat.getZ() + " throttle=" + boat.getThrottle() + " hull=" + boat.getHealth() + "/" + boat.getMaxHealth());
            onServer(mc, (level, player) -> wall(level, false));
        }
        if (t == 335) {
            log("dived depth=" + boat.depth() + " submerged=" + boat.isSubmerged() + " air=" + mc.player.getAirSupply());
            onServer(mc, (level, player) -> log("radar submerged=" + RadarCategory.of(serverBoat(level))));
            shot(mc, "submerged_third");
        }
        if (t == 340) {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        }
        if (t == 345) {
            shot(mc, "submerged_first");
            SkofcraftNetwork.CHANNEL.sendToServer(new ApacheFireC2SPacket(PilotedAircraft.WEAPON_GUN));
        }
        if (t == 355) {
            shot(mc, "sonar_ping");
            log("sincePing=" + (mc.level.getGameTime() - boat.getLastPing()));
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            onServer(mc, (level, player) -> {
                NuclearSubmarine sub = serverBoat(level);
                Boat target = EntityType.BOAT.create(level);
                target.setPos(sub.getX(), TOP + 1.0D, sub.getZ() + 55.0D);
                level.addFreshEntity(target);
                log("target at " + target.position() + " sub at " + sub.position());
            });
        }
        if (t == 370) {
            SkofcraftNetwork.CHANNEL.sendToServer(new ApacheFireC2SPacket(PilotedAircraft.WEAPON_MISSILE));
        }
        if (t == 385) {
            shot(mc, "torpedo");
        }
        if (t > 370 && t <= 470 && t % 5 == 0) {
            onServer(mc, (level, player) -> {
                for (Torpedo fish : level.getEntitiesOfClass(Torpedo.class, player.getBoundingBox().inflate(300.0D))) {
                    log("fish age=" + fish.tickCount + " pos=" + fish.position() + " v=" + fish.getDeltaMovement());
                }
                for (Boat b : level.getEntitiesOfClass(Boat.class, player.getBoundingBox().inflate(300.0D))) {
                    log("boat pos=" + b.position());
                }
            });
        }
        if (t > 370 && t <= 510 && t % 20 == 0) {
            log("torpedoes=" + count(mc, Torpedo.class) + " boats=" + count(mc, Boat.class));
        }
        if (t == 520) {
            log("surfaced? depth=" + boat.depth() + " y=" + boat.getY());
            onServer(mc, (level, player) -> {
                NuclearSubmarine sub = serverBoat(level);
                cube = BlockPos.containing(sub.getX(), sub.getY() + 2.0D, sub.getZ() + 45.0D);
                for (BlockPos pos : BlockPos.betweenClosed(cube.offset(-3, -1, -3), cube.offset(3, 3, 3))) {
                    level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
                }
                log("stone before=" + stoneInCube(level));
            });
        }
        if (t == 535) {
            SkofcraftNetwork.CHANNEL.sendToServer(new ApacheFireC2SPacket(PilotedAircraft.WEAPON_MISSILE));
        }
        if (t == 590) {
            onServer(mc, (level, player) -> {
                log("stone after=" + stoneInCube(level) + " water at cube=" + level.getBlockState(cube));
                NuclearSubmarine sub = serverBoat(level);
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(SkofcraftItems.GIGA_ICBM.get()));
                log("load=" + sub.interact(player, InteractionHand.MAIN_HAND) + " missiles=" + sub.getMissileCount());
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                IcbmTargets.set(player, cx + 3000, cz + 3000);
            });
        }
        if (t == 600) {
            log("before launch depth=" + boat.depth() + " missiles=" + boat.getMissileCount() + " speed=" + boat.shownSpeed);
            SkofcraftNetwork.CHANNEL.sendToServer(new ApacheFireC2SPacket(NuclearSubmarine.WEAPON_ICBM));
        }
        if (t == 660) {
            log("launching=" + boat.isLaunching() + " hatch=" + boat.getOpenHatch() + " missiles=" + boat.getMissileCount() + " rockets=" + count(mc, GigaIcbm.class));
            shot(mc, "hatch_open");
        }
        if (t == 815) {
            for (Entity e : mc.level.entitiesForRendering()) {
                if (e instanceof GigaIcbm rocket) {
                    log("rocket y=" + rocket.getY() + " boatY=" + boat.getY());
                }
            }
            shot(mc, "liftoff");
        }
    }
}
