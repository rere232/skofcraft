package ch.rere232.skofcraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

/** Feeds energy into every neighbour that accepts FE. It never runs out: it is a reactor. */
public class NuclearReactorBlockEntity extends BlockEntity {
    public static final int OUTPUT = 2000;

    public NuclearReactorBlockEntity(BlockPos pos, BlockState state) {
        super(SkofcraftBlockEntities.NUCLEAR_REACTOR.get(), pos, state);
    }

    public void tick() {
        if (level == null) {
            return;
        }
        for (Direction direction : Direction.values()) {
            BlockEntity neighbour = level.getBlockEntity(worldPosition.relative(direction));
            if (neighbour == null) {
                continue;
            }
            neighbour.getCapability(ForgeCapabilities.ENERGY, direction.getOpposite()).ifPresent(storage -> {
                if (storage.canReceive()) {
                    storage.receiveEnergy(OUTPUT, false);
                }
            });
        }
    }
}
