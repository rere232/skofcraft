package ch.rere232.skofcraft.menu;

import ch.rere232.skofcraft.recipe.AircraftAssemblyRecipe;
import ch.rere232.skofcraft.registry.SkofcraftBlocks;
import ch.rere232.skofcraft.registry.SkofcraftRecipes;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Optional;

/** 5x4 crafting grid plus a result slot; the grid is emptied back to the player when the screen closes. */
public class AircraftWorkbenchMenu extends AbstractContainerMenu {
    public static final int GRID_WIDTH = AircraftAssemblyRecipe.MAX_WIDTH;
    public static final int GRID_HEIGHT = AircraftAssemblyRecipe.MAX_HEIGHT;
    private static final int GRID_SIZE = GRID_WIDTH * GRID_HEIGHT;
    private static final int RESULT_SLOT = GRID_SIZE;
    private static final int INV_START = RESULT_SLOT + 1;
    private static final int HOTBAR_START = INV_START + 27;
    private static final int INV_END = HOTBAR_START + 9;
    public static final int GRID_X = 8;
    public static final int GRID_Y = 18;
    public static final int RESULT_X = 134;
    public static final int RESULT_Y = GRID_Y + (GRID_HEIGHT * 18 - 16) / 2;
    public static final int INVENTORY_Y = GRID_Y + GRID_HEIGHT * 18 + 18;

    private final CraftingContainer craftSlots = new TransientCraftingContainer(this, GRID_WIDTH, GRID_HEIGHT);
    private final ResultContainer resultSlots = new ResultContainer();
    private final ContainerLevelAccess access;
    private final Player player;

    public AircraftWorkbenchMenu(int windowId, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(windowId, playerInventory, ContainerLevelAccess.NULL);
    }

    public AircraftWorkbenchMenu(int windowId, Inventory playerInventory, ContainerLevelAccess access) {
        super(SkofcraftMenus.AIRCRAFT_WORKBENCH.get(), windowId);
        this.access = access;
        this.player = playerInventory.player;

        for (int row = 0; row < GRID_HEIGHT; row++) {
            for (int col = 0; col < GRID_WIDTH; col++) {
                addSlot(new Slot(craftSlots, col + row * GRID_WIDTH, GRID_X + col * 18, GRID_Y + row * 18));
            }
        }
        addSlot(new AssemblyResultSlot(RESULT_X, RESULT_Y));

        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, INVENTORY_Y + i * 18));
            }
        }
        for (int i = 0; i < 9; ++i) {
            addSlot(new Slot(playerInventory, i, 8 + i * 18, INVENTORY_Y + 58));
        }
    }

    private Optional<AircraftAssemblyRecipe> findRecipe(Level level) {
        return level.getRecipeManager().getRecipeFor(SkofcraftRecipes.AIRCRAFT_ASSEMBLY.get(), craftSlots, level);
    }

    @Override
    public void slotsChanged(Container container) {
        access.execute((level, pos) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return;
            }
            ItemStack result = findRecipe(level).map(recipe -> recipe.assemble(craftSlots, level.registryAccess())).orElse(ItemStack.EMPTY);
            resultSlots.setItem(0, result);
            setRemoteSlot(RESULT_SLOT, result);
            serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(containerId, incrementStateId(), RESULT_SLOT, result));
        });
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return moved;
        }
        ItemStack stack = slot.getItem();
        moved = stack.copy();
        if (index == RESULT_SLOT) {
            access.execute((level, pos) -> stack.getItem().onCraftedBy(stack, level, player));
            if (!moveItemStackTo(stack, INV_START, INV_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, moved);
        } else if (index >= INV_START && index < HOTBAR_START) {
            if (!moveItemStackTo(stack, HOTBAR_START, INV_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= HOTBAR_START) {
            if (!moveItemStackTo(stack, INV_START, HOTBAR_START, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, INV_START, INV_END, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == moved.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        if (index == RESULT_SLOT) {
            player.drop(stack, false);
        }
        return moved;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != resultSlots && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        access.execute((level, pos) -> clearContainer(player, craftSlots));
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, SkofcraftBlocks.AIRCRAFT_WORKBENCH.get());
    }

    /** Takes one of each grid item per craft and puts back container items such as the bucket from a Lava Bucket. */
    private class AssemblyResultSlot extends Slot {
        AssemblyResultSlot(int x, int y) {
            super(resultSlots, 0, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            stack.onCraftedBy(player.level(), player, stack.getCount());
            NonNullList<ItemStack> remaining = findRecipe(player.level())
                    .map(recipe -> recipe.getRemainingItems(craftSlots))
                    .orElseGet(() -> NonNullList.withSize(GRID_SIZE, ItemStack.EMPTY));
            for (int i = 0; i < GRID_SIZE; i++) {
                if (!craftSlots.getItem(i).isEmpty()) {
                    craftSlots.removeItem(i, 1);
                }
                ItemStack leftover = remaining.get(i);
                if (leftover.isEmpty()) {
                    continue;
                }
                if (craftSlots.getItem(i).isEmpty()) {
                    craftSlots.setItem(i, leftover);
                } else if (!player.getInventory().add(leftover)) {
                    player.drop(leftover, false);
                }
            }
        }
    }
}
