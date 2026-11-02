package ch.rere232.skofcraft.worldgen;

import ch.rere232.skofcraft.registry.SkofcraftStructures;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

import java.util.Optional;

/**
 * A single jigsaw piece that only builds on dry, fairly level ground. Vanilla's jigsaw structures look at one column and
 * trust it, which puts a house on the edge of a cliff or a sea bluff often enough to notice.
 */
public class FlatGroundStructure extends Structure {
    public static final Codec<FlatGroundStructure> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            settingsCodec(instance),
            StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(structure -> structure.startPool),
            Codec.intRange(0, 64).fieldOf("max_height_range").forGetter(structure -> structure.maxHeightRange),
            Codec.intRange(1, 64).fieldOf("site_radius").forGetter(structure -> structure.siteRadius)
    ).apply(instance, FlatGroundStructure::new));

    private static final int SAMPLES_PER_SIDE = 5;
    private static final int MAX_PIECE_DISTANCE = 80;

    private final Holder<StructureTemplatePool> startPool;
    private final int maxHeightRange;
    private final int siteRadius;

    public FlatGroundStructure(StructureSettings settings, Holder<StructureTemplatePool> startPool, int maxHeightRange, int siteRadius) {
        super(settings);
        this.startPool = startPool;
        this.maxHeightRange = maxHeightRange;
        this.siteRadius = siteRadius;
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        if (!isGoodSite(context, chunk.getMinBlockX(), chunk.getMinBlockZ())) {
            return Optional.empty();
        }
        BlockPos start = new BlockPos(chunk.getMinBlockX(), 0, chunk.getMinBlockZ());
        return JigsawPlacement.addPieces(context, startPool, Optional.empty(), 1, start, false,
                Optional.of(Heightmap.Types.WORLD_SURFACE_WG), MAX_PIECE_DISTANCE);
    }

    /** The piece can turn any way around its start corner, so this looks at the whole square it might land on. */
    private boolean isGoodSite(GenerationContext context, int x, int z) {
        int step = siteRadius * 2 / (SAMPLES_PER_SIDE - 1);
        int lowest = Integer.MAX_VALUE;
        int highest = Integer.MIN_VALUE;
        for (int dx = -siteRadius; dx <= siteRadius; dx += step) {
            for (int dz = -siteRadius; dz <= siteRadius; dz += step) {
                int surface = height(context, x + dx, z + dz, Heightmap.Types.WORLD_SURFACE_WG);
                // Water is the only thing between the two
                if (surface != height(context, x + dx, z + dz, Heightmap.Types.OCEAN_FLOOR_WG)) {
                    return false;
                }
                lowest = Math.min(lowest, surface);
                highest = Math.max(highest, surface);
                if (highest - lowest > maxHeightRange) {
                    return false;
                }
            }
        }
        return true;
    }

    private static int height(GenerationContext context, int x, int z, Heightmap.Types type) {
        return context.chunkGenerator().getBaseHeight(x, z, type, context.heightAccessor(), context.randomState());
    }

    @Override
    public StructureType<?> type() {
        return SkofcraftStructures.FLAT_GROUND.get();
    }
}
