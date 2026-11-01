package ch.rere232.skofcraft.carrier;

import ch.rere232.skofcraft.registry.SkofcraftBlocks;
import ch.rere232.skofcraft.registry.SkofcraftItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The carrier as built by the deploy item, in the ship's own frame: bow toward north (-Z), origin on the keel
 * at the middle of the ship. x runs port (-) to starboard (+), y up from the keel, z from bow (-36) to stern (35).
 * Air entries are spaces inside the hull that must stay dry. Below the hangar floor the lower deck holds the
 * crew compartments and the reactor room; see {@link #buildLowerDeck}.
 */
public final class CarrierLayout {
    public static final int HALF_WIDTH = 8;
    public static final int BOW_Z = -36;
    public static final int STERN_Z = 35;
    public static final int HEIGHT = 25;
    /** Local y of the top water block; the keel sits this many blocks under the surface. */
    public static final int WATERLINE = 3;
    public static final int HANGAR_FLOOR = 4;
    public static final int DECK = 9;
    public static final BlockPos HELM = new BlockPos(6, DECK + 5, 5);

    private static Map<BlockPos, BlockState> blocks;
    /** Starting contents of the containers aboard, as block entity data. Filled while the layout is built. */
    private static final Map<BlockPos, CompoundTag> contents = new HashMap<>();

    private CarrierLayout() {}

    public static synchronized Map<BlockPos, BlockState> blocks() {
        if (blocks == null) {
            blocks = build();
        }
        return blocks;
    }

    public static synchronized Map<BlockPos, CompoundTag> contents() {
        blocks();
        return contents;
    }

    public static int hullHalfWidth(int z) {
        return z < -26 ? Math.max(1, 5 - (int) Math.ceil((-26 - z) * 0.45D)) : 5;
    }

    private static int deckHalfWidth(int z) {
        return z < -28 ? Math.max(2, 8 - (-28 - z)) : HALF_WIDTH;
    }

    private static Map<BlockPos, BlockState> build() {
        Map<BlockPos, BlockState> map = new LinkedHashMap<>();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState antifouling = Blocks.RED_TERRACOTTA.defaultBlockState();
        BlockState hull = Blocks.GRAY_CONCRETE.defaultBlockState();
        BlockState floor = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
        BlockState light = Blocks.SEA_LANTERN.defaultBlockState();
        BlockState deck = Blocks.SMOOTH_BASALT.defaultBlockState();
        BlockState white = Blocks.WHITE_CONCRETE.defaultBlockState();
        BlockState yellow = Blocks.YELLOW_CONCRETE.defaultBlockState();
        BlockState island = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();

        for (int z = BOW_Z; z <= STERN_Z; z++) {
            int hw = hullHalfWidth(z);
            boolean end = z == BOW_Z || z == STERN_Z;
            for (int x = -hw; x <= hw; x++) {
                for (int y = 0; y < DECK; y++) {
                    boolean side = Math.abs(x) == hw;
                    if (y == 0 && side && hw > 1) {
                        continue; // rounded bilge
                    }
                    BlockState wall = y <= WATERLINE ? antifouling : hull;
                    if (end || side || y == 0) {
                        boolean lamp = side && !end && (y == 2 || y == 7) && Math.floorMod(z, 8) == 0;
                        map.put(new BlockPos(x, y, z), lamp ? light : wall);
                    } else if (y == HANGAR_FLOOR) {
                        map.put(new BlockPos(x, y, z), floor);
                    } else {
                        map.put(new BlockPos(x, y, z), air);
                    }
                }
            }
            // Sponsons under the overhanging flight deck
            int dw = deckHalfWidth(z);
            for (int x = -dw; x <= dw; x++) {
                if (Math.abs(x) > hw) {
                    map.put(new BlockPos(x, DECK - 1, z), hull);
                }
                map.put(new BlockPos(x, DECK, z), deck);
            }
        }

        // Landing area: centre line, edge lines and four arresting wires near the stern
        for (int z = 6; z <= STERN_Z; z++) {
            if (Math.floorMod(z, 4) < 2) {
                map.put(new BlockPos(-2, DECK, z), white);
            }
            map.put(new BlockPos(-7, DECK, z), white);
            map.put(new BlockPos(3, DECK, z), white);
        }
        BlockState wire = SkofcraftBlocks.ARRESTING_WIRE.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);
        for (int z : new int[] {22, 25, 28, 31}) {
            for (int x = -6; x <= 2; x++) {
                map.put(new BlockPos(x, DECK, z), wire);
            }
        }

        // Two bow catapults launching toward the bow, with yellow foul lines
        BlockState catapult = SkofcraftBlocks.CATAPULT.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);
        for (int x : new int[] {-3, 3}) {
            for (int z = -33; z <= -12; z++) {
                map.put(new BlockPos(x, DECK, z), catapult);
            }
            map.put(new BlockPos(x - 1, DECK, -11), yellow);
            map.put(new BlockPos(x, DECK, -11), yellow);
            map.put(new BlockPos(x + 1, DECK, -11), yellow);
        }

        // Ladders: flight deck -> hangar -> reactor room, against the port hull wall
        BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.EAST);
        for (int y = 1; y < DECK; y++) {
            map.put(new BlockPos(-4, y, 30), ladder);
        }
        map.put(new BlockPos(-4, DECK, 30), Blocks.SPRUCE_TRAPDOOR.defaultBlockState()
                .setValue(TrapDoorBlock.HALF, Half.TOP).setValue(TrapDoorBlock.FACING, Direction.EAST));

        buildLowerDeck(map);
        buildHangar(map);
        buildIsland(map, island, air);
        buildDefenses(map);
        return map;
    }

    // ---- interior ----

    private static final int PASSAGE_X = -4;
    private static final int ROOM_MIN_X = -2;
    private static final int ROOM_MAX_X = 4;

    /**
     * Lower deck (y 1-3): a passageway along the port side, reached by the ladder at the stern, with compartments
     * to starboard. Bow to stern: chain locker, crew berthing, mess, reactor room, workshop, sick bay, captain's cabin.
     */
    private static void buildLowerDeck(Map<BlockPos, BlockState> map) {
        BlockState bulkhead = Blocks.POLISHED_ANDESITE.defaultBlockState();
        for (int z = -24; z <= STERN_Z - 1; z++) {
            wall(map, PASSAGE_X + 1, z, bulkhead);
        }
        for (int z : new int[] {-25, -13, -5, 5, 17, 26}) {
            for (int x = ROOM_MIN_X; x <= ROOM_MAX_X; x++) {
                wall(map, x, z, bulkhead);
            }
        }
        for (int x = -4; x <= 4; x++) {
            wall(map, x, -26, bulkhead);
        }
        door(map, -3, -26, Direction.NORTH);
        for (int z : new int[] {-19, -9, 0, 11, 21, 29}) {
            door(map, PASSAGE_X + 1, z, Direction.EAST);
        }
        for (int z : new int[] {-22, -14, -6, 2, 10, 18, 26, 33}) {
            lantern(map, PASSAGE_X, 3, z);
        }

        // Chain locker in the bow
        for (int y = 1; y <= 3; y++) {
            map.put(new BlockPos(0, y, -31), Blocks.CHAIN.defaultBlockState());
            map.put(new BlockPos(0, y, -29), Blocks.CHAIN.defaultBlockState());
        }
        for (int x : new int[] {-2, 2}) {
            barrel(map, new BlockPos(x, 1, -28), new ItemStack(Items.CHAIN, 32), new ItemStack(Items.LEAD, 8));
        }
        lantern(map, 0, 3, -27);

        // Crew berthing: bunk beds two high along both sides
        BlockState bunk = Blocks.BLUE_BED.defaultBlockState();
        for (int z : new int[] {-23, -21, -19, -17, -15}) {
            for (int y = 1; y <= 2; y++) {
                bed(map, bunk, new BlockPos(ROOM_MAX_X, y, z), Direction.EAST);
                if (z != -19) {
                    bed(map, bunk, new BlockPos(ROOM_MIN_X, y, z), Direction.WEST);
                }
            }
        }
        for (int x = 0; x <= 2; x += 2) {
            barrel(map, new BlockPos(x, 1, -24));
        }
        lantern(map, 1, 3, -22);
        lantern(map, 1, 3, -16);

        // Mess: a long table with benches and the galley along the starboard side
        for (int z = -11; z <= -7; z++) {
            map.put(new BlockPos(1, 1, z), Blocks.SPRUCE_FENCE.defaultBlockState());
            map.put(new BlockPos(1, 2, z), Blocks.SPRUCE_PRESSURE_PLATE.defaultBlockState());
            map.put(new BlockPos(0, 1, z), Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST));
            map.put(new BlockPos(2, 1, z), Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST));
        }
        BlockPos galley = new BlockPos(ROOM_MAX_X, 1, 0);
        map.put(galley.offset(0, 0, -12), Blocks.SMOKER.defaultBlockState().setValue(AbstractFurnaceBlock.FACING, Direction.WEST));
        map.put(galley.offset(0, 0, -11), Blocks.FURNACE.defaultBlockState().setValue(AbstractFurnaceBlock.FACING, Direction.WEST));
        barrel(map, galley.offset(0, 0, -10), new ItemStack(Items.BREAD, 32), new ItemStack(Items.COOKED_BEEF, 32),
                new ItemStack(Items.BAKED_POTATO, 32), new ItemStack(Items.COOKED_COD, 32));
        map.put(galley.offset(0, 0, -9), Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        barrel(map, galley.offset(0, 0, -8), new ItemStack(Items.PUMPKIN_PIE, 16), new ItemStack(Items.COOKIE, 32),
                new ItemStack(Items.APPLE, 16), new ItemStack(SkofcraftItems.SKOFEE.get(), 8));
        map.put(galley.offset(0, 0, -7), Blocks.CRAFTING_TABLE.defaultBlockState());
        map.put(galley.offset(0, 0, -6), Blocks.SMOKER.defaultBlockState().setValue(AbstractFurnaceBlock.FACING, Direction.WEST));
        lantern(map, 1, 3, -12);
        lantern(map, -1, 3, -9);
        lantern(map, 3, 3, -9);

        // Reactor room: two reactors on pedestals with control rods, a coolant column and the control desk
        BlockState reactor = SkofcraftBlocks.NUCLEAR_REACTOR.get().defaultBlockState();
        for (int x : new int[] {-1, 3}) {
            for (int z = -1; z <= 1; z++) {
                map.put(new BlockPos(x, 1, z), Blocks.IRON_BLOCK.defaultBlockState());
            }
            map.put(new BlockPos(x, 2, 0), reactor);
            map.put(new BlockPos(x, 3, 0), Blocks.CHAIN.defaultBlockState());
        }
        map.put(new BlockPos(1, 1, 0), Blocks.SEA_LANTERN.defaultBlockState());
        map.put(new BlockPos(1, 2, 0), Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState());
        map.put(new BlockPos(1, 3, 0), Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState());
        for (int x = ROOM_MIN_X; x <= ROOM_MAX_X; x++) {
            BlockPos desk = new BlockPos(x, 1, -4);
            if (x % 2 == 0) {
                map.put(desk, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, true));
                map.put(desk.below(), Blocks.REDSTONE_BLOCK.defaultBlockState()); // keeps the lamp lit
            } else {
                map.put(desk, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
            }
        }
        for (int z : new int[] {-5, 5}) {
            map.put(new BlockPos(1, 2, z), Blocks.CYAN_STAINED_GLASS.defaultBlockState());
        }

        // Workshop: aircraft workbench and tools, spare parts and fuel
        map.put(new BlockPos(ROOM_MAX_X, 1, 8), SkofcraftBlocks.AIRCRAFT_WORKBENCH.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));
        map.put(new BlockPos(ROOM_MAX_X, 1, 10), Blocks.ANVIL.defaultBlockState().setValue(AnvilBlock.FACING, Direction.NORTH));
        map.put(new BlockPos(ROOM_MAX_X, 1, 12), Blocks.SMITHING_TABLE.defaultBlockState());
        map.put(new BlockPos(ROOM_MAX_X, 1, 14), Blocks.CRAFTING_TABLE.defaultBlockState());
        chest(map, new BlockPos(ROOM_MIN_X, 1, 7), Direction.EAST,
                new ItemStack(SkofcraftItems.GEAR.get(), 16), new ItemStack(SkofcraftItems.AIRCRAFT_PLATE.get(), 16));
        chest(map, new BlockPos(ROOM_MIN_X, 1, 15), Direction.EAST);
        lantern(map, 1, 3, 8);
        lantern(map, 1, 3, 14);

        // Sick bay
        for (int z : new int[] {19, 22, 25}) {
            bed(map, Blocks.WHITE_BED.defaultBlockState(), new BlockPos(ROOM_MAX_X, 1, z), Direction.EAST);
        }
        map.put(new BlockPos(ROOM_MIN_X, 1, 24), Blocks.BREWING_STAND.defaultBlockState());
        map.put(new BlockPos(ROOM_MIN_X, 1, 25), Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        barrel(map, new BlockPos(ROOM_MIN_X, 1, 18), new ItemStack(Items.GOLDEN_APPLE, 4),
                PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.HEALING),
                PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.HEALING),
                PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.REGENERATION),
                new ItemStack(Items.MILK_BUCKET));
        lantern(map, 1, 3, 21);

        // Captain's cabin at the stern
        for (int x = ROOM_MIN_X; x <= ROOM_MAX_X; x++) {
            for (int z = 27; z <= STERN_Z - 1; z++) {
                map.put(new BlockPos(x, 1, z), Blocks.RED_CARPET.defaultBlockState());
            }
        }
        bed(map, Blocks.RED_BED.defaultBlockState(), new BlockPos(ROOM_MAX_X, 1, 33), Direction.EAST);
        for (int x = ROOM_MIN_X; x <= 2; x++) {
            map.put(new BlockPos(x, 1, STERN_Z - 1), Blocks.BOOKSHELF.defaultBlockState());
            map.put(new BlockPos(x, 2, STERN_Z - 1), Blocks.BOOKSHELF.defaultBlockState());
        }
        map.put(new BlockPos(1, 1, 30), Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, Direction.WEST));
        map.put(new BlockPos(0, 1, 30), Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST));
        chest(map, new BlockPos(ROOM_MAX_X, 1, 28), Direction.WEST, new ItemStack(Items.SPYGLASS), new ItemStack(Items.COMPASS),
                new ItemStack(Items.CLOCK), new ItemStack(SkofcraftItems.CIGAR.get(), 4));
        lantern(map, 1, 3, 31);
    }

    /** Hangar deck (y 5-8): lit from the flight deck above, with a refuelling station amidships. */
    private static void buildHangar(Map<BlockPos, BlockState> map) {
        for (int z = -24; z <= 32; z += 8) {
            lantern(map, -3, DECK - 1, z);
            lantern(map, 3, DECK - 1, z);
        }
        ItemStack fuel = new ItemStack(SkofcraftItems.JET_FUEL_CANISTER.get(), 4);
        chest(map, new BlockPos(4, HANGAR_FLOOR + 1, -1), Direction.WEST, fuel.copy());
        chest(map, new BlockPos(4, HANGAR_FLOOR + 1, 1), Direction.WEST, fuel.copy(),
                new ItemStack(SkofcraftItems.EMPTY_FUEL_CANISTER.get(), 4));
    }

    private static void wall(Map<BlockPos, BlockState> map, int x, int z, BlockState state) {
        for (int y = 1; y <= 3; y++) {
            map.put(new BlockPos(x, y, z), state);
        }
    }

    private static void door(Map<BlockPos, BlockState> map, int x, int z, Direction facing) {
        BlockState door = Blocks.SPRUCE_DOOR.defaultBlockState().setValue(DoorBlock.FACING, facing);
        map.put(new BlockPos(x, 1, z), door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        map.put(new BlockPos(x, 2, z), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    private static void lantern(Map<BlockPos, BlockState> map, int x, int y, int z) {
        map.put(new BlockPos(x, y, z), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
    }

    /** A bed with its head at {@code head}, pointing {@code facing}. */
    private static void bed(Map<BlockPos, BlockState> map, BlockState bed, BlockPos head, Direction facing) {
        BlockState state = bed.setValue(BedBlock.FACING, facing);
        map.put(head, state.setValue(BedBlock.PART, BedPart.HEAD));
        map.put(head.relative(facing.getOpposite()), state.setValue(BedBlock.PART, BedPart.FOOT));
    }

    private static void barrel(Map<BlockPos, BlockState> map, BlockPos pos, ItemStack... items) {
        map.put(pos, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP));
        fill(pos, items);
    }

    private static void chest(Map<BlockPos, BlockState> map, BlockPos pos, Direction facing, ItemStack... items) {
        map.put(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing));
        fill(pos, items);
    }

    private static void fill(BlockPos pos, ItemStack... items) {
        if (items.length == 0) {
            return;
        }
        ListTag list = new ListTag();
        for (int slot = 0; slot < items.length; slot++) {
            CompoundTag item = items[slot].save(new CompoundTag());
            item.putByte("Slot", (byte) slot);
            list.add(item);
        }
        CompoundTag tag = new CompoundTag();
        tag.put("Items", list);
        contents.put(pos, tag);
    }

    /** Gatling cannons and missile launchers on the four deck corners and the island roof, clear of the runways. */
    private static void buildDefenses(Map<BlockPos, BlockState> map) {
        BlockState ciws = SkofcraftBlocks.CIWS.get().defaultBlockState();
        BlockState sam = SkofcraftBlocks.SAM_LAUNCHER.get().defaultBlockState();
        map.put(new BlockPos(-8, DECK + 1, -26), ciws);
        map.put(new BlockPos(8, DECK + 1, -26), sam);
        map.put(new BlockPos(-8, DECK + 1, 34), sam);
        map.put(new BlockPos(8, DECK + 1, 34), ciws);
        map.put(new BlockPos(6, DECK + 8, 12), ciws);
    }

    /** Starboard island: two levels, the bridge with the helm on top, and a mast. */
    private static void buildIsland(Map<BlockPos, BlockState> map, BlockState wall, BlockState air) {
        BlockState glass = Blocks.GLASS.defaultBlockState();
        for (int y = DECK + 1; y <= DECK + 7; y++) {
            for (int x = 5; x <= 8; x++) {
                for (int z = 2; z <= 13; z++) {
                    boolean edge = x == 5 || x == 8 || z == 2 || z == 13;
                    boolean corner = (x == 5 || x == 8) && (z == 2 || z == 13);
                    BlockState state;
                    if (y == DECK + 4 || y == DECK + 7) {
                        state = wall; // bridge floor and roof
                    } else if (!edge) {
                        state = air;
                    } else if (y >= DECK + 5 && !corner) {
                        state = glass; // bridge windows
                    } else {
                        state = wall;
                    }
                    map.put(new BlockPos(x, y, z), state);
                }
            }
        }
        // Doorway on the port side of the island
        map.put(new BlockPos(5, DECK + 1, 8), air);
        map.put(new BlockPos(5, DECK + 2, 8), air);
        BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH);
        for (int y = DECK + 1; y <= DECK + 4; y++) {
            map.put(new BlockPos(6, y, 3), ladder);
        }
        map.put(HELM, SkofcraftBlocks.CARRIER_HELM.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        map.put(HELM.east(), SkofcraftBlocks.RADAR.get().defaultBlockState());

        for (int y = DECK + 8; y <= DECK + 12; y++) {
            map.put(new BlockPos(7, y, 8), Blocks.IRON_BARS.defaultBlockState());
        }
        for (int x = 6; x <= 8; x++) {
            map.put(new BlockPos(x, DECK + 11, 9), Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState());
        }
        map.put(new BlockPos(7, DECK + 13, 8), Blocks.LIGHTNING_ROD.defaultBlockState());
    }
}
