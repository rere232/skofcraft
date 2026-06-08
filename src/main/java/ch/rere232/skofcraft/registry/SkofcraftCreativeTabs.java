package ch.rere232.skofcraft.registry;

import ch.rere232.skofcraft.SkofcraftMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class SkofcraftCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SkofcraftMod.MODID);

    public static final RegistryObject<CreativeModeTab> SKOFCRAFT_TAB = CREATIVE_MODE_TABS.register("skofcraft", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.skofcraft.skofcraft"))
            .icon(() -> new ItemStack(SkofcraftItems.FILLED_SNUS_BOX.get()))
            .displayItems((parameters, output) -> {
                output.accept(SkofcraftItems.TOBACCO_SEEDS.get());
                output.accept(SkofcraftItems.TOBACCO_LEAF_FRESH.get());
                output.accept(SkofcraftItems.TOBACCO_LEAF_DRY.get());
                output.accept(SkofcraftItems.TOBACCO_DUST.get());

                output.accept(SkofcraftItems.VEGETAL_FIBER.get());
                output.accept(SkofcraftItems.NEUTRAL_BASE.get());
                output.accept(SkofcraftItems.NICOTINE_EXTRACT.get());
                output.accept(SkofcraftItems.NICOTINE_CRYSTALS.get());

                output.accept(SkofcraftItems.FLAVOR_MINT.get());
                output.accept(SkofcraftItems.FLAVOR_ICE_MINT.get());
                output.accept(SkofcraftItems.FLAVOR_BERRY.get());
                output.accept(SkofcraftItems.FLAVOR_CITRUS.get());
                output.accept(SkofcraftItems.FLAVOR_LICORICE.get());
                output.accept(SkofcraftItems.FLAVOR_COLA.get());
                output.accept(SkofcraftItems.FLAVOR_COFFEE.get());
                output.accept(SkofcraftItems.FLAVOR_NATURE.get());

                output.accept(SkofcraftItems.EMPTY_POUCH.get());
                output.accept(SkofcraftItems.SNUS_POUCH.get());
                output.accept(SkofcraftItems.NICOTINE_POUCH.get());
                output.accept(SkofcraftItems.EMPTY_SNUS_BOX.get());
                output.accept(SkofcraftItems.FILLED_SNUS_BOX.get());
                output.accept(SkofcraftItems.EMPTY_POUCH_BOX.get());
                output.accept(SkofcraftItems.FILLED_POUCH_BOX.get());

                output.accept(SkofcraftItems.MANUAL_DRYER.get());
                output.accept(SkofcraftItems.MANUAL_GRINDER.get());
                output.accept(SkofcraftItems.MANUAL_MIXER.get());
                output.accept(SkofcraftItems.MANUAL_POUCH_PRESS.get());

                output.accept(SkofcraftItems.FE_DRYER.get());
                output.accept(SkofcraftItems.FE_GRINDER.get());
                output.accept(SkofcraftItems.FE_MIXER.get());
                output.accept(SkofcraftItems.FE_PRESS.get());
                output.accept(SkofcraftItems.FE_NICOTINE_EXTRACTOR.get());
                output.accept(SkofcraftItems.FE_PACKAGER.get());

                output.accept(SkofcraftItems.INDUSTRIAL_SNUS_LINE.get());
                output.accept(SkofcraftItems.INDUSTRIAL_POUCH_LINE.get());
                output.accept(SkofcraftItems.INDUSTRIAL_PACKAGER.get());
                output.accept(SkofcraftItems.INDUSTRIAL_EXTRACTOR.get());
            })
            .build());
}
