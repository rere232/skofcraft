package ch.rere232.skofcraft.carrier;

import ch.rere232.skofcraft.registry.SkofcraftBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * The blocks of a carrier in the ship's own frame (see {@link CarrierLayout}), with block entity data.
 * Lifted out of the world when the carrier gets underway and written back when it stops.
 */
public final class CarrierBlocks {
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private final List<BlockPos> positions = new ArrayList<>();
    private final List<BlockState> states = new ArrayList<>();
    private final Map<Integer, CompoundTag> blockEntities = new HashMap<>();

    public static CarrierBlocks fromLayout() {
        CarrierBlocks blocks = new CarrierBlocks();
        Map<BlockPos, CompoundTag> contents = CarrierLayout.contents();
        CarrierLayout.blocks().forEach((pos, state) -> {
            CompoundTag tag = contents.get(pos);
            if (tag != null) {
                blocks.blockEntities.put(blocks.size(), tag.copy());
            }
            blocks.add(pos, state);
        });
        return blocks;
    }

    private void add(BlockPos local, BlockState state) {
        positions.add(local);
        states.add(state);
    }

    public int size() {
        return positions.size();
    }

    public BlockPos localPos(int i) {
        return positions.get(i);
    }

    public BlockState state(int i) {
        return states.get(i);
    }

    public boolean contains(Block block) {
        return states.stream().anyMatch(state -> state.is(block));
    }

    public static Rotation rotationFor(Direction facing) {
        return switch (facing) {
            case EAST -> Rotation.CLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    public static BlockPos toWorld(BlockPos origin, Direction facing, BlockPos local) {
        return origin.offset(local.rotate(rotationFor(facing)));
    }

    /** True if the layout could be built here: open water under the hull and nothing solid in the way. */
    public static boolean canDeploy(Level level, BlockPos origin, Direction facing) {
        for (Map.Entry<BlockPos, BlockState> entry : CarrierLayout.blocks().entrySet()) {
            BlockPos local = entry.getKey();
            BlockPos world = toWorld(origin, facing, local);
            BlockState current = level.getBlockState(world);
            if (local.getY() == 0 && !entry.getValue().isAir() && !current.getFluidState().is(FluidTags.WATER)) {
                return false;
            }
            if (!isOpen(level, world, current)) {
                return false;
            }
        }
        return true;
    }

    /**
     * How many of the layout's solid blocks are in place around {@code origin}, from 0 to 1. Guards the helm
     * against lifting random terrain when it is placed outside a carrier.
     */
    public static float integrity(Level level, BlockPos origin, Direction facing) {
        int solid = 0;
        int intact = 0;
        Rotation rotation = rotationFor(facing);
        for (Map.Entry<BlockPos, BlockState> entry : CarrierLayout.blocks().entrySet()) {
            if (entry.getValue().isAir()) {
                continue;
            }
            solid++;
            if (level.getBlockState(toWorld(origin, facing, entry.getKey())).is(entry.getValue().rotate(rotation).getBlock())) {
                intact++;
            }
        }
        return solid == 0 ? 0.0F : intact / (float) solid;
    }

    /** What a repair did: blocks rebuilt, flooded spaces pumped dry, and damage left for lack of budget or parts. */
    public record RepairResult(int rebuilt, int pumped, int remaining) {}

    /**
     * Rebuilds the carrier at {@code origin} to its original layout: missing blocks are put back (spending one point
     * of {@code budget} each) and water in the dry compartments is pumped out for free. Nothing players built is
     * overwritten. Blocks that store items or are worth more than the repair costs (the reactors, the helm, iron and
     * redstone blocks, workstations) are not recreated, so a kit cannot be used to duplicate them.
     */
    public static RepairResult repair(Level level, BlockPos origin, Direction facing, int budget) {
        Rotation rotation = rotationFor(facing);
        int rebuilt = 0;
        int pumped = 0;
        int remaining = 0;
        for (Map.Entry<BlockPos, BlockState> entry : CarrierLayout.blocks().entrySet()) {
            BlockPos world = toWorld(origin, facing, entry.getKey());
            BlockState target = entry.getValue().rotate(rotation);
            BlockState current = level.getBlockState(world);
            if (target.isAir()) {
                if (!current.getFluidState().isEmpty() && current.getCollisionShape(level, world).isEmpty()) {
                    level.setBlock(world, target, FLAGS);
                    pumped++;
                }
                continue;
            }
            if (current.is(target.getBlock()) || !isOpen(level, world, current) || !isRepairable(target)) {
                continue;
            }
            if (rebuilt >= budget) {
                remaining++;
                continue;
            }
            level.setBlock(world, target, FLAGS);
            rebuilt++;
        }
        return new RepairResult(rebuilt, pumped, remaining);
    }

    private static boolean isRepairable(BlockState state) {
        return !state.hasBlockEntity()
                && !state.is(SkofcraftBlocks.NUCLEAR_REACTOR.get())
                && !state.is(SkofcraftBlocks.CARRIER_HELM.get())
                && !state.is(SkofcraftBlocks.AIRCRAFT_WORKBENCH.get())
                && !state.is(Blocks.IRON_BLOCK)
                && !state.is(Blocks.REDSTONE_BLOCK)
                && !state.is(Blocks.ANVIL)
                && !state.is(Blocks.SMITHING_TABLE)
                && !state.is(Blocks.CRAFTING_TABLE)
                && !state.is(Blocks.BOOKSHELF)
                && !state.is(Blocks.LECTERN)
                && !state.is(Blocks.WATER_CAULDRON);
    }

    private static boolean isOpen(Level level, BlockPos pos, BlockState state) {
        return state.isAir() || state.canBeReplaced() || !state.getFluidState().isEmpty() && state.getCollisionShape(level, pos).isEmpty();
    }

    /**
     * Reads the carrier at {@code origin}: every block of the layout's hull (air included, so flooded rooms come
     * back dry) plus anything players built above the waterline inside the carrier's bounds.
     */
    public static CarrierBlocks capture(Level level, BlockPos origin, Direction facing) {
        CarrierBlocks blocks = new CarrierBlocks();
        Map<BlockPos, BlockState> layout = CarrierLayout.blocks();
        Rotation toLocal = inverse(rotationFor(facing));
        for (int x = -CarrierLayout.HALF_WIDTH; x <= CarrierLayout.HALF_WIDTH; x++) {
            for (int y = 0; y < CarrierLayout.HEIGHT; y++) {
                for (int z = CarrierLayout.BOW_Z; z <= CarrierLayout.STERN_Z; z++) {
                    BlockPos local = new BlockPos(x, y, z);
                    BlockPos world = toWorld(origin, facing, local);
                    BlockState state = level.getBlockState(world);
                    boolean inLayout = layout.containsKey(local);
                    boolean empty = state.isAir() || state.getFluidState().is(FluidTags.WATER) && state.getCollisionShape(level, world).isEmpty();
                    if (empty) {
                        if (inLayout && layout.get(local).isAir()) {
                            blocks.add(local, Blocks.AIR.defaultBlockState());
                        }
                        continue;
                    }
                    if (!inLayout && y <= CarrierLayout.WATERLINE) {
                        continue; // sea bed and kelp around the hull
                    }
                    int index = blocks.size();
                    blocks.add(local, state.rotate(toLocal));
                    BlockEntity blockEntity = level.getBlockEntity(world);
                    if (blockEntity != null) {
                        blocks.blockEntities.put(index, blockEntity.saveWithoutMetadata());
                    }
                }
            }
        }
        return blocks;
    }

    private static Rotation inverse(Rotation rotation) {
        return switch (rotation) {
            case CLOCKWISE_90 -> Rotation.COUNTERCLOCKWISE_90;
            case COUNTERCLOCKWISE_90 -> Rotation.CLOCKWISE_90;
            default -> rotation;
        };
    }

    /** Takes the captured blocks out of the world, leaving sea below the waterline and air above. */
    public void remove(Level level, BlockPos origin, Direction facing) {
        BlockState water = Blocks.WATER.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        // No neighbour updates, so torches, ladders and the like do not pop off while their wall is gone
        for (int i = 0; i < size(); i++) {
            BlockPos world = toWorld(origin, facing, positions.get(i));
            if (blockEntities.containsKey(i)) {
                level.removeBlockEntity(world);
            }
            level.setBlock(world, positions.get(i).getY() <= CarrierLayout.WATERLINE ? water : air, FLAGS);
        }
    }

    /** Writes the blocks back; anything solid now in the way keeps its place and the carrier's block drops. */
    public void place(Level level, BlockPos origin, Direction facing) {
        Rotation rotation = rotationFor(facing);
        for (int i = 0; i < size(); i++) {
            BlockPos world = toWorld(origin, facing, positions.get(i));
            BlockState state = states.get(i).rotate(rotation);
            BlockState current = level.getBlockState(world);
            if (!isOpen(level, world, current)) {
                if (!state.isAir()) {
                    Block.popResource(level, world, new ItemStack(state.getBlock()));
                }
                continue;
            }
            level.setBlock(world, state, FLAGS);
            CompoundTag tag = blockEntities.get(i);
            BlockEntity blockEntity = tag == null ? null : level.getBlockEntity(world);
            if (blockEntity != null) {
                blockEntity.load(tag);
                blockEntity.setChanged();
            }
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        Map<BlockState, Integer> paletteIndex = new IdentityHashMap<>();
        ListTag palette = new ListTag();
        int[] packed = new int[size()];
        int[] indices = new int[size()];
        for (int i = 0; i < size(); i++) {
            BlockState state = states.get(i);
            Integer index = paletteIndex.get(state);
            if (index == null) {
                index = palette.size();
                paletteIndex.put(state, index);
                palette.add(NbtUtils.writeBlockState(state));
            }
            packed[i] = pack(positions.get(i));
            indices[i] = index;
        }
        tag.put("Palette", palette);
        tag.putIntArray("Positions", packed);
        tag.putIntArray("States", indices);
        ListTag entities = new ListTag();
        blockEntities.forEach((index, data) -> {
            CompoundTag entry = new CompoundTag();
            entry.putInt("Index", index);
            entry.put("Data", data);
            entities.add(entry);
        });
        tag.put("BlockEntities", entities);
        return tag;
    }

    public static CarrierBlocks load(Level level, CompoundTag tag) {
        CarrierBlocks blocks = new CarrierBlocks();
        ListTag paletteTag = tag.getList("Palette", Tag.TAG_COMPOUND);
        List<BlockState> palette = new ArrayList<>();
        for (int i = 0; i < paletteTag.size(); i++) {
            palette.add(NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), paletteTag.getCompound(i)));
        }
        int[] packed = tag.getIntArray("Positions");
        int[] indices = tag.getIntArray("States");
        for (int i = 0; i < Math.min(packed.length, indices.length); i++) {
            blocks.add(unpack(packed[i]), palette.get(indices[i]));
        }
        ListTag entities = tag.getList("BlockEntities", Tag.TAG_COMPOUND);
        for (int i = 0; i < entities.size(); i++) {
            CompoundTag entry = entities.getCompound(i);
            blocks.blockEntities.put(entry.getInt("Index"), entry.getCompound("Data"));
        }
        return blocks;
    }

    /** Client copy for rendering: only visible blocks, no block entity data. */
    public void write(FriendlyByteBuf buf) {
        int count = 0;
        for (BlockState state : states) {
            if (!state.isAir()) {
                count++;
            }
        }
        buf.writeVarInt(count);
        for (int i = 0; i < size(); i++) {
            if (!states.get(i).isAir()) {
                buf.writeInt(pack(positions.get(i)));
                buf.writeVarInt(Block.getId(states.get(i)));
            }
        }
    }

    public static CarrierBlocks read(FriendlyByteBuf buf) {
        CarrierBlocks blocks = new CarrierBlocks();
        int count = buf.readVarInt();
        for (int i = 0; i < count; i++) {
            BlockPos local = unpack(buf.readInt());
            blocks.add(local, Block.stateById(buf.readVarInt()));
        }
        return blocks;
    }

    private static int pack(BlockPos pos) {
        return ((pos.getX() + 512) & 1023) << 20 | ((pos.getY() + 512) & 1023) << 10 | ((pos.getZ() + 512) & 1023);
    }

    private static BlockPos unpack(int packed) {
        return new BlockPos((packed >> 20 & 1023) - 512, (packed >> 10 & 1023) - 512, (packed & 1023) - 512);
    }
}
