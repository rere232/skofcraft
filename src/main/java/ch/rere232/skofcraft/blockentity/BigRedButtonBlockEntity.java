package ch.rere232.skofcraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Holds nothing: only there so the client can draw the giant warning above the button. */
public class BigRedButtonBlockEntity extends BlockEntity {
    public BigRedButtonBlockEntity(BlockPos pos, BlockState state) {
        super(SkofcraftBlockEntities.BIG_RED_BUTTON.get(), pos, state);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(2.5D, 0.0D, 2.5D).expandTowards(0.0D, 3.0D, 0.0D);
    }
}
