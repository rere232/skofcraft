package ch.rere232.skofcraft.worldgen;

import ch.rere232.skofcraft.registry.SkofcraftKingdom;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.function.Supplier;

/**
 * The scenery of the Snus Kingdom. A feature may reach 16 blocks past its own chunk, and starts anywhere inside it, so
 * nothing here goes more than 12 blocks out from its center.
 */
public final class KingdomFeatures {
    private static final int MAX_FOUNDATION = 12;
    private static final int FIELD_RADIUS = 7;

    private KingdomFeatures() {}

    private abstract static class Scenery extends Feature<NoneFeatureConfiguration> {
        Scenery() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            WorldGenLevel level = context.level();
            BlockPos origin = context.origin();
            // Nothing grows in the middle of a SKOFee river
            if (!level.getFluidState(origin).isEmpty() || !level.getFluidState(origin.below()).isEmpty()) {
                return false;
            }
            build(level, origin, context.random());
            return true;
        }

        abstract void build(WorldGenLevel level, BlockPos origin, RandomSource random);

        /** So nothing hangs over a slope: fills down from the block under {@code top} until it hits the ground. */
        static void foundation(WorldGenLevel level, BlockPos top, BlockState state) {
            BlockPos.MutableBlockPos cursor = top.mutable();
            for (int i = 0; i < MAX_FOUNDATION; i++) {
                cursor.move(0, -1, 0);
                BlockState below = level.getBlockState(cursor);
                if (!below.isAir() && below.getFluidState().isEmpty()) {
                    return;
                }
                level.setBlock(cursor, state, Block.UPDATE_CLIENTS);
            }
        }
    }

    /** A snus tin the size of a house, half sunk into the ground. */
    public static class GiantSnusTin extends Scenery {
        @Override
        void build(WorldGenLevel level, BlockPos origin, RandomSource random) {
            int radius = 5 + random.nextInt(7);
            int height = 3 + radius / 2 + random.nextInt(3);
            BlockState side = SkofcraftKingdom.TIN_PLATE.get().defaultBlockState();
            BlockState top = SkofcraftKingdom.TIN_LID.get().defaultBlockState();
            BlockState inside = SkofcraftKingdom.PACKED_SNUS.get().defaultBlockState();
            int squared = radius * radius;
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    int distance = x * x + z * z;
                    if (distance > squared) {
                        continue;
                    }
                    boolean edge = distance > (radius - 1) * (radius - 1);
                    for (int y = -2; y < height; y++) {
                        BlockState state = y == height - 1 ? top : edge ? side : inside;
                        level.setBlock(origin.offset(x, y, z), state, Block.UPDATE_CLIENTS);
                    }
                    foundation(level, origin.offset(x, -2, z), side);
                }
            }
        }
    }

    /** Grows wild, wrapped and lit. Nobody knows who lit them. */
    public static class CigarTree extends Scenery {
        private static final int[][] CROSS = {{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}};

        @Override
        void build(WorldGenLevel level, BlockPos origin, RandomSource random) {
            int height = 7 + random.nextInt(8);
            BlockState wrapper = SkofcraftKingdom.CIGAR_WRAPPER.get().defaultBlockState();
            BlockState ember = SkofcraftKingdom.CIGAR_EMBER.get().defaultBlockState();
            for (int[] offset : CROSS) {
                BlockPos base = origin.offset(offset[0], 0, offset[1]);
                foundation(level, base, wrapper);
                for (int y = 0; y < height; y++) {
                    level.setBlock(base.above(y), y >= height - 2 ? ember : wrapper, Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    /** A used pouch, the size of a boulder. */
    public static class PouchBoulder extends Scenery {
        @Override
        void build(WorldGenLevel level, BlockPos origin, RandomSource random) {
            int radiusX = 3 + random.nextInt(4);
            int radiusZ = 2 + random.nextInt(3);
            int radiusY = 2 + random.nextInt(2);
            BlockState fabric = SkofcraftKingdom.POUCH_FABRIC.get().defaultBlockState();
            for (int x = -radiusX; x <= radiusX; x++) {
                for (int z = -radiusZ; z <= radiusZ; z++) {
                    for (int y = -1; y <= radiusY; y++) {
                        double spread = (double) (x * x) / (radiusX * radiusX) + (double) (z * z) / (radiusZ * radiusZ)
                                + (double) (y * y) / (radiusY * radiusY);
                        if (spread <= 1.0D) {
                            level.setBlock(origin.offset(x, y, z), fabric, Block.UPDATE_CLIENTS);
                        }
                    }
                }
            }
        }
    }

    /** A patch of crops, already planted on tilled ground. Crops stop their own farmland from drying out. */
    public static class CropField extends Scenery {
        private final Supplier<Block> crop;

        public CropField(Supplier<Block> crop) {
            this.crop = crop;
        }

        @Override
        void build(WorldGenLevel level, BlockPos origin, RandomSource random) {
            if (!(crop.get() instanceof CropBlock plant)) {
                return;
            }
            BlockState farmland = Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE);
            for (int x = -FIELD_RADIUS; x <= FIELD_RADIUS; x++) {
                for (int z = -FIELD_RADIUS; z <= FIELD_RADIUS; z++) {
                    int worldX = origin.getX() + x;
                    int worldZ = origin.getZ() + z;
                    int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, worldX, worldZ);
                    BlockPos air = new BlockPos(worldX, y, worldZ);
                    BlockPos ground = air.below();
                    BlockState under = level.getBlockState(ground);
                    if (Math.abs(y - origin.getY()) > 3 || !under.getFluidState().isEmpty() || !under.isSolid()
                            || under.is(SkofcraftKingdom.TIN_PLATE.get()) || under.is(SkofcraftKingdom.TIN_LID.get())
                            || under.is(SkofcraftKingdom.POUCH_FABRIC.get()) || under.is(SkofcraftKingdom.CIGAR_WRAPPER.get())) {
                        continue;
                    }
                    level.setBlock(ground, farmland, Block.UPDATE_CLIENTS);
                    level.setBlock(air, plant.getStateForAge(3 + random.nextInt(plant.getMaxAge() - 2)), Block.UPDATE_CLIENTS);
                }
            }
        }
    }
}
