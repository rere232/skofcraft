package ch.rere232.skofcraft.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;

/** One stretch of a {@link NuclearSubmarine}'s hull: everything that happens to it happens to the boat. */
public class SubmarinePart extends PartEntity<NuclearSubmarine> {
    private final EntityDimensions size;

    public SubmarinePart(NuclearSubmarine parent, float width, float height) {
        super(parent);
        this.size = EntityDimensions.scalable(width, height);
        refreshDimensions();
    }

    void follow(Vec3 position) {
        xo = getX();
        yo = getY();
        zo = getZ();
        setPos(position.x, position.y, position.z);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // A blast far from the middle of the boat still has to count, so these go through; the boat dedupes them
        return !isInvulnerableTo(source) && (source.is(DamageTypeTags.IS_EXPLOSION) || source.getDirectEntity() != null)
                && getParent().hurt(source, amount);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        return getParent().interact(player, hand);
    }

    @Override
    public boolean isPickable() {
        return !getParent().isRemoved();
    }

    @Override
    public boolean canBeCollidedWith() {
        return getParent().isDeckSafe();
    }

    @Override
    public boolean is(Entity entity) {
        return this == entity || getParent() == entity;
    }

    @Override
    public ItemStack getPickResult() {
        return getParent().getPickResult();
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return size;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
