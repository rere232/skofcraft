package ch.rere232.skofcraft.compat.jei;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * One machine step as JEI shows it. Each input slot holds a list of stacks JEI cycles through (empty for a blank slot).
 * When {@code linkedInput} is set, that input and the output cycle together: a flavor and the flavored product it makes.
 */
public record MachineRecipe(List<List<ItemStack>> inputs, List<ItemStack> outputs, int linkedInput) {
    public static MachineRecipe of(ItemStack output, ItemStack... inputs) {
        return new MachineRecipe(java.util.Arrays.stream(inputs).map(s -> s.isEmpty() ? List.<ItemStack>of() : List.of(s)).toList(),
                List.of(output), -1);
    }
}
