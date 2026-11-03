package ch.rere232.skofcraft.worldgen;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.entity.BoutiqueKeeper;
import ch.rere232.skofcraft.registry.SkofcraftBlocks;
import ch.rere232.skofcraft.registry.SkofcraftEntities;
import ch.rere232.skofcraft.registry.SkofcraftPastel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * The buildings of Pastel Paradise, drawn block by block and placed like any other feature. A feature may reach 16
 * blocks past its own chunk and starts anywhere inside it, so nothing here goes more than 12 blocks out from its center.
 */
public final class PastelStructures {
    private static final int MAX_FOUNDATION = 10;
    private static final int MAX_SLOPE = 3;

    private PastelStructures() {}

    /** A chest with a loot table from data/skofcraft/loot_tables/chests. */
    public static void chest(WorldGenLevel level, RandomSource random, BlockPos pos, String table) {
        level.setBlock(pos, Blocks.CHEST.defaultBlockState(), Block.UPDATE_CLIENTS);
        RandomizableContainerBlockEntity.setLootTable(level, random, pos,
                ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "chests/" + table));
    }

    /** Stands on flat ground on an island, with its footprint cleared and propped up where the edge drops away. */
    private abstract static class Building extends Feature<NoneFeatureConfiguration> {
        private final int halfX;
        private final int halfZ;
        private final int height;

        Building(int halfX, int halfZ, int height) {
            super(NoneFeatureConfiguration.CODEC);
            this.halfX = halfX;
            this.halfZ = halfZ;
            this.height = height;
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            WorldGenLevel level = context.level();
            BlockPos origin = context.origin();
            if (!level.getBlockState(origin.below()).is(Blocks.GRASS_BLOCK)) {
                return false;
            }
            for (int x = -halfX; x <= halfX; x += halfX) {
                for (int z = -halfZ; z <= halfZ; z += halfZ) {
                    int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, origin.getX() + x, origin.getZ() + z);
                    if (Math.abs(surface - origin.getY()) > MAX_SLOPE) {
                        return false;
                    }
                }
            }
            BlockState ground = SkofcraftPastel.MARSHMALLOW.get().defaultBlockState();
            for (int x = -halfX; x <= halfX; x++) {
                for (int z = -halfZ; z <= halfZ; z++) {
                    BlockPos.MutableBlockPos cursor = origin.offset(x, -1, z).mutable();
                    for (int i = 0; i < MAX_FOUNDATION && level.getBlockState(cursor).isAir(); i++) {
                        level.setBlock(cursor, ground, Block.UPDATE_CLIENTS);
                        cursor.move(0, -1, 0);
                    }
                    for (int y = 0; y <= height; y++) {
                        level.setBlock(origin.offset(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            }
            build(level, origin, context.random());
            return true;
        }

        abstract void build(WorldGenLevel level, BlockPos origin, RandomSource random);

        static void box(WorldGenLevel level, BlockPos origin, int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
            for (int x = x1; x <= x2; x++) {
                for (int y = y1; y <= y2; y++) {
                    for (int z = z1; z <= z2; z++) {
                        level.setBlock(origin.offset(x, y, z), state, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }

        static BlockState bricks() {
            return SkofcraftPastel.HEART_BRICKS.get().defaultBlockState();
        }

        static BlockState planks() {
            return SkofcraftPastel.PASTEL_PLANKS.get().defaultBlockState();
        }

        static BlockState lantern() {
            return SkofcraftPastel.HEART_LANTERN.get().defaultBlockState();
        }

        static BlockState blahaj() {
            return SkofcraftBlocks.BLAHAJ.get().defaultBlockState();
        }
    }

    /** One room, a pointed pink roof, and somebody's Blahaj. */
    public static class Cottage extends Building {
        public Cottage() {
            super(5, 5, 9);
        }

        @Override
        void build(WorldGenLevel level, BlockPos origin, RandomSource random) {
            box(level, origin, -4, -1, -4, 4, -1, 4, planks());
            box(level, origin, -4, 0, -4, 4, 3, 4, bricks());
            box(level, origin, -3, 0, -3, 3, 3, 3, Blocks.AIR.defaultBlockState());
            for (int layer = 0; layer <= 4; layer++) {
                int size = 5 - layer;
                box(level, origin, -size, 4 + layer, -size, size, 4 + layer, size,
                        layer % 2 == 0 ? Blocks.PINK_WOOL.defaultBlockState() : Blocks.WHITE_WOOL.defaultBlockState());
            }
            box(level, origin, 0, 0, 4, 0, 1, 4, Blocks.AIR.defaultBlockState());
            for (int side = -4; side <= 4; side += 8) {
                box(level, origin, side, 1, -1, side, 2, 1, Blocks.PINK_STAINED_GLASS.defaultBlockState());
            }
            box(level, origin, -3, 0, -3, 3, 0, 3, Blocks.PINK_CARPET.defaultBlockState());
            level.setBlock(origin.offset(0, 3, 0), lantern(), Block.UPDATE_CLIENTS);
            level.setBlock(origin.offset(2, 0, -3), blahaj(), Block.UPDATE_CLIENTS);
            chest(level, random, origin.offset(-2, 0, -3), "pastel_cottage");
        }
    }

    /** Open on every side, so the regulars can be patted from any direction. */
    public static class Cafe extends Building {
        public Cafe() {
            super(7, 6, 7);
        }

        @Override
        void build(WorldGenLevel level, BlockPos origin, RandomSource random) {
            box(level, origin, -6, -1, -5, 6, -1, 5, planks());
            for (int x = -6; x <= 6; x += 12) {
                for (int z = -5; z <= 5; z += 10) {
                    box(level, origin, x, 0, z, x, 3, z, planks());
                }
            }
            for (int x = -6; x <= 6; x++) {
                box(level, origin, x, 4, -5, x, 4, 5,
                        (x + 6) % 2 == 0 ? Blocks.MAGENTA_WOOL.defaultBlockState() : Blocks.WHITE_WOOL.defaultBlockState());
            }
            level.setBlock(origin.offset(0, 3, 0), lantern(), Block.UPDATE_CLIENTS);
            // The counter along the back, with the machine on it
            box(level, origin, -4, 0, -4, 4, 0, -4, planks());
            level.setBlock(origin.offset(0, 1, -4), SkofcraftBlocks.SKOFEE_MACHINE.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            chest(level, random, origin.offset(3, 1, -4), "headpat_cafe");
            for (int x = -3; x <= 3; x += 6) {
                for (int z = 0; z <= 3; z += 3) {
                    level.setBlock(origin.offset(x, 0, z), Blocks.OAK_FENCE.defaultBlockState(), Block.UPDATE_CLIENTS);
                    level.setBlock(origin.offset(x, 1, z), Blocks.PINK_CARPET.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
            level.setBlock(origin.offset(-5, 0, -4), blahaj(), Block.UPDATE_CLIENTS);
        }
    }

    /** Racks of clothes behind a striped front. The only place The Rightful Fit is at all likely to turn up. */
    public static class Boutique extends Building {
        public Boutique() {
            super(8, 6, 8);
        }

        @Override
        void build(WorldGenLevel level, BlockPos origin, RandomSource random) {
            box(level, origin, -7, -1, -5, 7, -1, 5, planks());
            BlockState[] stripes = {Blocks.LIGHT_BLUE_WOOL.defaultBlockState(), Blocks.PINK_WOOL.defaultBlockState(),
                    Blocks.WHITE_WOOL.defaultBlockState(), Blocks.PINK_WOOL.defaultBlockState(), Blocks.LIGHT_BLUE_WOOL.defaultBlockState()};
            for (int y = 0; y < stripes.length; y++) {
                box(level, origin, -7, y, -5, 7, y, 5, stripes[y]);
            }
            box(level, origin, -6, 0, -4, 6, 4, 4, Blocks.AIR.defaultBlockState());
            box(level, origin, -7, 5, -5, 7, 5, 5, bricks());
            box(level, origin, -1, 0, 5, 1, 2, 5, Blocks.AIR.defaultBlockState());
            box(level, origin, -5, 1, 5, -3, 3, 5, Blocks.GLASS.defaultBlockState());
            box(level, origin, 3, 1, 5, 5, 3, 5, Blocks.GLASS.defaultBlockState());
            box(level, origin, -6, 0, -4, 6, 0, 4, Blocks.WHITE_CARPET.defaultBlockState());
            box(level, origin, -6, 0, -4, 6, 0, -4, planks());
            chest(level, random, origin.offset(-3, 1, -4), "pastel_boutique");
            chest(level, random, origin.offset(3, 1, -4), "pastel_boutique");
            level.setBlock(origin.offset(-4, 4, 0), lantern(), Block.UPDATE_CLIENTS);
            level.setBlock(origin.offset(4, 4, 0), lantern(), Block.UPDATE_CLIENTS);
            level.setBlock(origin.offset(0, 1, -4), blahaj(), Block.UPDATE_CLIENTS);
            BoutiqueKeeper keeper = SkofcraftEntities.BOUTIQUE_KEEPER.get().create(level.getLevel());
            if (keeper != null) {
                keeper.moveTo(origin.getX() + 0.5D, origin.getY() + 1.0D, origin.getZ() + 0.5D, random.nextFloat() * 360.0F, 0.0F);
                keeper.setPersistenceRequired();
                level.addFreshEntityWithPassengers(keeper);
            }
        }
    }

    /** Tall and narrow, with a ladder up the inside and a stash at the top. */
    public static class Tower extends Building {
        private static final int TOP = 11;

        public Tower() {
            super(4, 4, TOP + 4);
        }

        @Override
        void build(WorldGenLevel level, BlockPos origin, RandomSource random) {
            box(level, origin, -3, -1, -3, 3, TOP, 3, bricks());
            box(level, origin, -2, 0, -2, 2, TOP - 1, 2, Blocks.AIR.defaultBlockState());
            box(level, origin, 0, 0, 3, 0, 1, 3, Blocks.AIR.defaultBlockState());
            for (int y = 2; y < TOP; y += 3) {
                box(level, origin, -3, y, 0, -3, y, 0, Blocks.PINK_STAINED_GLASS.defaultBlockState());
                box(level, origin, 3, y, 0, 3, y, 0, Blocks.PINK_STAINED_GLASS.defaultBlockState());
            }
            BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH);
            box(level, origin, 0, 0, -2, 0, TOP, -2, ladder);
            box(level, origin, -2, TOP - 4, -1, 2, TOP - 4, 2, planks());
            level.setBlock(origin.offset(0, TOP - 1, 0), lantern(), Block.UPDATE_CLIENTS);
            chest(level, random, origin.offset(1, TOP - 3, 1), "plush_tower");
            level.setBlock(origin.offset(1, TOP - 3, -1), SkofcraftPastel.HEART_ALTAR.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            level.setBlock(origin.offset(-1, TOP - 3, 1), blahaj(), Block.UPDATE_CLIENTS);
            // Roof access through the ladder hole, with a parapet of pink wool
            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z <= 3; z++) {
                    if ((Math.abs(x) == 3 || Math.abs(z) == 3) && (x + z & 1) == 0) {
                        level.setBlock(origin.offset(x, TOP + 1, z), Blocks.PINK_WOOL.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }

    /** A warm pool with a brick rim. Sitting in one doubles how fast Paradise does you good. */
    public static class HotSpring extends Building {
        public HotSpring() {
            super(5, 5, 3);
        }

        @Override
        void build(WorldGenLevel level, BlockPos origin, RandomSource random) {
            box(level, origin, -4, -3, -4, 4, -3, 4, SkofcraftPastel.MARSHMALLOW.get().defaultBlockState());
            box(level, origin, -4, -2, -4, 4, -1, 4, bricks());
            box(level, origin, -3, -2, -3, 3, -1, 3, Blocks.WATER.defaultBlockState());
            box(level, origin, -4, 0, -4, -4, 0, -4, lantern());
            box(level, origin, 4, 0, 4, 4, 0, 4, lantern());
        }
    }

    /** A puff of cotton candy, drifting in the open air. You can land on these. */
    public static class CottonCloud extends Feature<NoneFeatureConfiguration> {
        public CottonCloud() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            WorldGenLevel level = context.level();
            RandomSource random = context.random();
            BlockPos origin = context.origin();
            int radiusX = 5 + random.nextInt(6);
            int radiusZ = 4 + random.nextInt(5);
            int radiusY = 1 + random.nextInt(2);
            BlockState state = SkofcraftPastel.COTTON_CANDY_CLOUD.get().defaultBlockState();
            for (int x = -radiusX; x <= radiusX; x++) {
                for (int z = -radiusZ; z <= radiusZ; z++) {
                    for (int y = -radiusY; y <= radiusY; y++) {
                        double spread = (double) (x * x) / (radiusX * radiusX) + (double) (z * z) / (radiusZ * radiusZ)
                                + (double) (y * y) / (radiusY * radiusY);
                        BlockPos pos = origin.offset(x, y, z);
                        if (spread <= 1.0D && level.getBlockState(pos).isAir()) {
                            level.setBlock(pos, state, Block.UPDATE_CLIENTS);
                        }
                    }
                }
            }
            return true;
        }
    }
}
