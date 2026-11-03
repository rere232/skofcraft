package ch.rere232.skofcraft.kingdom;

import ch.rere232.skofcraft.registry.SkofcraftBlocks;
import ch.rere232.skofcraft.registry.SkofcraftKingdom;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The castle at the middle of the Kingdom: one long hall, a red carpet, an empty throne and, in front of it, the sword
 * almost nobody can pull. Built the first time anyone arrives, right where they are about to land.
 */
final class SnusKingdomCastle {
    private static final int HALF_WIDTH = 11;
    private static final int FRONT = 12;
    private static final int BACK = -16;
    private static final int WALL_HEIGHT = 8;
    private static final int MIN_FLOOR = 66;
    private static final int MAX_FLOOR = 200;
    private static final int ARRIVAL_Z = 6;

    private SnusKingdomCastle() {}

    /** Builds the castle if it isn't there yet and returns where to put someone waking up on its floor. */
    static BlockPos arrival(ServerLevel level, SnusKingdom.Data data) {
        if (!data.built) {
            build(level, data);
        }
        return new BlockPos(0, data.floorY + 1, ARRIVAL_Z);
    }

    static BlockPos regentSpot(SnusKingdom.Data data) {
        return new BlockPos(3, data.floorY + 1, -8);
    }

    private static void build(ServerLevel level, SnusKingdom.Data data) {
        for (int chunkX = -1; chunkX <= 0; chunkX++) {
            for (int chunkZ = -2; chunkZ <= 0; chunkZ++) {
                level.getChunk(chunkX, chunkZ);
            }
        }
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 0, 0);
        int floor = Math.max(MIN_FLOOR, Math.min(MAX_FLOOR, surface));

        BlockState snus = SkofcraftKingdom.PACKED_SNUS.get().defaultBlockState();
        BlockState tin = SkofcraftKingdom.TIN_PLATE.get().defaultBlockState();
        BlockState lid = SkofcraftKingdom.TIN_LID.get().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        for (int x = -HALF_WIDTH; x <= HALF_WIDTH; x++) {
            for (int z = BACK; z <= FRONT; z++) {
                boolean wall = Math.abs(x) == HALF_WIDTH || z == BACK || z == FRONT;
                boolean corner = Math.abs(x) == HALF_WIDTH && (z == BACK || z == FRONT);

                // Down to the ground, so the hall stands on a hillside or out in the SKOFee just the same
                for (int y = floor - 1; y > level.getMinBuildHeight(); y--) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState below = level.getBlockState(pos);
                    if (!below.isAir() && below.getFluidState().isEmpty()) {
                        break;
                    }
                    set(level, pos, snus);
                }
                set(level, new BlockPos(x, floor, z), (x + z & 1) == 0 ? snus : lid);

                for (int y = 1; y < WALL_HEIGHT; y++) {
                    set(level, new BlockPos(x, floor + y, z), wall ? (corner ? tin : snus) : air);
                }
                set(level, new BlockPos(x, floor + WALL_HEIGHT, z), wall ? tin : lid);
                set(level, new BlockPos(x, floor + WALL_HEIGHT + 1, z), wall && (x + z & 1) == 0 ? snus : air);
                for (int y = WALL_HEIGHT + 2; y < WALL_HEIGHT + 24; y++) {
                    set(level, new BlockPos(x, floor + y, z), air);
                }
            }
        }

        // The door
        for (int x = -1; x <= 1; x++) {
            for (int y = 1; y <= 3; y++) {
                set(level, new BlockPos(x, floor + y, FRONT), air);
            }
        }

        // Pillars and light
        for (int z = -10; z <= 8; z += 6) {
            for (int x = -7; x <= 7; x += 14) {
                for (int y = 1; y < WALL_HEIGHT; y++) {
                    set(level, new BlockPos(x, floor + y, z), y == WALL_HEIGHT - 2 ? Blocks.SHROOMLIGHT.defaultBlockState() : tin);
                }
            }
        }

        // Windows
        for (int z = -12; z <= 8; z += 5) {
            for (int y = 3; y <= 5; y++) {
                set(level, new BlockPos(-HALF_WIDTH, floor + y, z), Blocks.GLASS.defaultBlockState());
                set(level, new BlockPos(HALF_WIDTH, floor + y, z), Blocks.GLASS.defaultBlockState());
            }
        }

        // The carpet, from the door to the sword
        for (int z = FRONT - 1; z >= -8; z--) {
            for (int x = -1; x <= 1; x++) {
                set(level, new BlockPos(x, floor + 1, z), Blocks.RED_CARPET.defaultBlockState());
            }
        }

        // The dais and the empty throne
        for (int x = -4; x <= 4; x++) {
            for (int z = BACK + 1; z <= -11; z++) {
                set(level, new BlockPos(x, floor + 1, z), Blocks.GOLD_BLOCK.defaultBlockState());
                if (Math.abs(x) <= 2 && z <= -12) {
                    set(level, new BlockPos(x, floor + 2, z), Blocks.GOLD_BLOCK.defaultBlockState());
                }
            }
        }
        set(level, new BlockPos(0, floor + 3, BACK + 2), Blocks.QUARTZ_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH));
        for (int y = 3; y <= 5; y++) {
            set(level, new BlockPos(0, floor + y, BACK + 1), Blocks.GOLD_BLOCK.defaultBlockState());
        }
        set(level, new BlockPos(-1, floor + 3, BACK + 2), Blocks.GOLD_BLOCK.defaultBlockState());
        set(level, new BlockPos(1, floor + 3, BACK + 2), Blocks.GOLD_BLOCK.defaultBlockState());

        set(level, new BlockPos(0, floor + 1, -9), SkofcraftBlocks.EXCALIBUR.get().defaultBlockState());

        data.built = true;
        data.floorY = floor;
        data.setDirty();
        SnusKingdom.spawnCourt(level, new BlockPos[] {
                new BlockPos(-5, floor + 1, -4), new BlockPos(5, floor + 1, -2),
                new BlockPos(-5, floor + 1, 3), new BlockPos(5, floor + 1, 5)});
    }

    private static void set(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
    }
}
