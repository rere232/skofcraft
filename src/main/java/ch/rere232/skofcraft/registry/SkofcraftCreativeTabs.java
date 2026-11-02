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
                output.accept(SkofcraftItems.QUEST_BOOK.get());
                output.accept(SkofcraftItems.TOBACCO_SEEDS.get());
                output.accept(SkofcraftItems.TOBACCO_LEAF_FRESH.get());
                output.accept(SkofcraftItems.TOBACCO_LEAF_DRY.get());
                output.accept(SkofcraftItems.TOBACCO_DUST.get());

                output.accept(SkofcraftItems.SKOFEE_SEEDS.get());
                output.accept(SkofcraftItems.SKOFEE_BEANS_FRESH.get());
                output.accept(SkofcraftItems.SKOFEE_BEANS_DRY.get());
                output.accept(SkofcraftItems.SKOFEE_BEANS_ROASTED.get());
                output.accept(SkofcraftItems.SKOFEE_GROUND.get());
                output.accept(SkofcraftItems.SKOFEE_CUP.get());
                output.accept(SkofcraftItems.SKOFEE.get());
                output.accept(SkofcraftItems.SKOFEE_LATTE.get());
                output.accept(SkofcraftItems.EMPTY_CAN.get());
                output.accept(SkofcraftItems.WHITE_MONSTER.get());
                output.accept(SkofcraftItems.RED_BULL.get());
                output.accept(SkofcraftItems.ONE_MILLION_BEER.get());
                output.accept(SkofcraftItems.SKOFEE_MACHINE.get());

                output.accept(SkofcraftItems.VEGETAL_FIBER.get());
                output.accept(SkofcraftItems.NICOTINE_EXTRACT.get());
                output.accept(SkofcraftItems.NICOTINE_CRYSTALS.get());
                output.accept(SkofcraftItems.NICOTINE_MIX.get());

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
                output.accept(SkofcraftItems.CHEWING_TOBACCO.get());
                output.accept(SkofcraftItems.SNUFF.get());
                output.accept(SkofcraftItems.EMPTY_VAPE.get());
                output.accept(SkofcraftItems.VAPE.get());
                output.accept(SkofcraftItems.CIGARETTE.get());
                output.accept(SkofcraftItems.CIGARILLO.get());
                output.accept(SkofcraftItems.CIGAR.get());
                output.accept(SkofcraftItems.EMPTY_CIGARETTE_PACK.get());
                output.accept(SkofcraftItems.CIGARETTE_PACK.get());
                output.accept(SkofcraftItems.CHICHA.get());
                output.accept(SkofcraftItems.TOBACCONIST_COUNTER.get());
                output.accept(SkofcraftItems.ADDICTOL.get());
                output.accept(SkofcraftItems.PHONE.get());
                output.accept(SkofcraftItems.BLAHAJ.get());
                output.accept(SkofcraftItems.CHOKER.get());
                output.accept(SkofcraftItems.SKIRT.get());
                output.accept(SkofcraftItems.THIGH_HIGHS.get());
                output.accept(SkofcraftItems.CROP_TOP.get());
                output.accept(SkofcraftItems.HAND_WARMERS.get());
                output.accept(SkofcraftItems.FACE_MASK.get());
                output.accept(SkofcraftItems.CAT_EARS.get());
                output.accept(SkofcraftItems.CAT_TAIL.get());
                output.accept(SkofcraftItems.OUTFIT_BOX.get());
                output.accept(SkofcraftItems.ESTROGEN_PILL.get());
                output.accept(SkofcraftItems.FEMBOY_SPAWN_EGG.get());
                output.accept(SkofcraftItems.ANIME_CREW_SPAWN_EGG.get());
                output.accept(SkofcraftItems.BIG_RED_BUTTON.get());
                output.accept(SkofcraftItems.EVIL_PRESSURE_PLATE.get());
                output.accept(SkofcraftItems.EVIL_TRIPWIRE.get());
                output.accept(SkofcraftItems.EVIL_LEVER.get());
                output.accept(SkofcraftItems.EXCALIBUR.get());
                output.accept(SkofcraftItems.AIRCRAFT_WORKBENCH.get());
                output.accept(SkofcraftItems.GEAR.get());
                output.accept(SkofcraftItems.AIRCRAFT_PLATE.get());
                output.accept(SkofcraftItems.TURBINE_BLADE.get());
                output.accept(SkofcraftItems.ROTOR_BLADE.get());
                output.accept(SkofcraftItems.AVIONICS.get());
                output.accept(SkofcraftItems.JET_ENGINE.get());
                output.accept(SkofcraftItems.WEAPON_SYSTEM.get());
                output.accept(SkofcraftItems.EMPTY_FUEL_CANISTER.get());
                output.accept(SkofcraftItems.JET_FUEL_CANISTER.get());
                output.accept(SkofcraftItems.APACHE.get());
                output.accept(SkofcraftItems.F14.get());
                output.accept(SkofcraftItems.MAUS.get());
                output.accept(SkofcraftItems.TIGER_2.get());
                output.accept(SkofcraftItems.LEOPARD_1.get());
                output.accept(SkofcraftItems.POSTBUS.get());
                output.accept(SkofcraftItems.ITEM_SCANNER.get());
                output.accept(SkofcraftItems.HR_CERTIFICATE.get());
                output.accept(SkofcraftItems.NUCLEAR_REACTOR.get());
                output.accept(SkofcraftItems.CARRIER_HELM.get());
                output.accept(SkofcraftItems.RADAR.get());
                output.accept(SkofcraftItems.CIWS.get());
                output.accept(SkofcraftItems.SAM_LAUNCHER.get());
                output.accept(SkofcraftItems.LAUNCH_PAD.get());
                output.accept(SkofcraftItems.RECON_SATELLITE.get());
                output.accept(SkofcraftItems.STRIKE_SATELLITE.get());
                output.accept(SkofcraftItems.ORBITAL_DESIGNATOR.get());
                output.accept(SkofcraftItems.DRACONIC_WARHEAD.get());
                output.accept(SkofcraftItems.ICBM_GUIDANCE.get());
                output.accept(SkofcraftItems.ICBM_BOOSTER.get());
                output.accept(SkofcraftItems.GIGA_ICBM.get());
                output.accept(SkofcraftItems.DOOMSDAY_MISSILE.get());
                output.accept(SkofcraftItems.COMPOSITE_PLATE.get());
                output.accept(SkofcraftItems.TACTICAL_CORE.get());
                output.accept(SkofcraftItems.GAS_FILTER.get());
                output.accept(SkofcraftItems.TACTICAL_HELMET.get());
                output.accept(SkofcraftItems.TACTICAL_CHESTPLATE.get());
                output.accept(SkofcraftItems.TACTICAL_LEGGINGS.get());
                output.accept(SkofcraftItems.TACTICAL_BOOTS.get());
                output.accept(SkofcraftItems.TACTICAL_GLOVES.get());
                output.accept(SkofcraftItems.CATAPULT.get());
                output.accept(SkofcraftItems.ARRESTING_WIRE.get());
                output.accept(SkofcraftItems.AIRCRAFT_CARRIER.get());
                output.accept(SkofcraftItems.DAMAGE_CONTROL_KIT.get());
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
