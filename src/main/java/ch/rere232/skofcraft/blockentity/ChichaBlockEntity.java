package ch.rere232.skofcraft.blockentity;

import ch.rere232.skofcraft.block.ChichaBlock;
import ch.rere232.skofcraft.item.ChichaPipeItem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** Holds the chicha's tobacco, flavor and which player has each of the four hoses. */
public class ChichaBlockEntity extends BlockEntity {
    public static final int TUBES = 4;
    public static final int RANGE = 4;
    public static final int MAX_PUFFS = 40;

    private final UUID[] users = new UUID[TUBES];
    private int puffs = 0;
    private String flavor = "";

    public ChichaBlockEntity(BlockPos pos, BlockState state) {
        super(SkofcraftBlockEntities.CHICHA.get(), pos, state);
    }

    /** Releases hoses whose player left, moved out of range or no longer holds the hose. */
    public void tick() {
        if (level == null || level.isClientSide || level.getGameTime() % 20 != 0) {
            return;
        }
        for (int i = 0; i < TUBES; i++) {
            if (users[i] == null) {
                continue;
            }
            Player player = level.getPlayerByUUID(users[i]);
            if (player == null || !isInRange(player) || !hasPipe(player, i)) {
                release(i);
            }
        }
    }

    public boolean isInRange(Player player) {
        return player.distanceToSqr(Vec3.atCenterOf(worldPosition)) <= (double) RANGE * RANGE;
    }

    private boolean hasPipe(Player player, int tube) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (ChichaPipeItem.matches(inventory.getItem(slot), worldPosition, tube)) {
                return true;
            }
        }
        return false;
    }

    public int freeTube() {
        for (int i = 0; i < TUBES; i++) {
            if (users[i] == null) {
                return i;
            }
        }
        return -1;
    }

    public int tubeOf(UUID id) {
        for (int i = 0; i < TUBES; i++) {
            if (id.equals(users[i])) {
                return i;
            }
        }
        return -1;
    }

    public boolean isUser(int tube, UUID id) {
        return tube >= 0 && tube < TUBES && id.equals(users[tube]);
    }

    @Nullable
    public UUID getUser(int tube) {
        return users[tube];
    }

    public void assign(int tube, UUID id) {
        users[tube] = id;
        syncState();
    }

    public void release(int tube) {
        users[tube] = null;
        syncState();
    }

    private void syncState() {
        setChanged();
        if (level == null) {
            return;
        }
        BlockState state = getBlockState();
        for (int i = 0; i < TUBES; i++) {
            state = state.setValue(ChichaBlock.TUBES[i], users[i] == null);
        }
        level.setBlock(worldPosition, state, 3);
        level.sendBlockUpdated(worldPosition, state, state, 3);
    }

    public int getPuffs() {
        return puffs;
    }

    /** Adds puffs up to the capacity and returns how many were added. */
    public int addPuffs(int amount) {
        int added = Math.min(amount, MAX_PUFFS - puffs);
        if (added > 0) {
            puffs += added;
            setChanged();
        }
        return added;
    }

    public boolean consumePuff() {
        if (puffs <= 0) {
            return false;
        }
        puffs--;
        setChanged();
        return true;
    }

    public String getFlavor() {
        return flavor;
    }

    public void setFlavor(String flavorId) {
        this.flavor = flavorId;
        setChanged();
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        puffs = tag.getInt("Puffs");
        flavor = tag.getString("Flavor");
        for (int i = 0; i < TUBES; i++) {
            users[i] = tag.hasUUID("User" + i) ? tag.getUUID("User" + i) : null;
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Puffs", puffs);
        tag.putString("Flavor", flavor);
        for (int i = 0; i < TUBES; i++) {
            if (users[i] != null) {
                tag.putUUID("User" + i, users[i]);
            }
        }
    }

    @Override
    public @Nullable ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(RANGE + 1);
    }
}
