package ch.rere232.skofcraft.registry;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.effect.EstrogenEffect;
import ch.rere232.skofcraft.effect.SpitBlindEffect;
import ch.rere232.skofcraft.effect.TrippedEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class SkofcraftEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, SkofcraftMod.MODID);

    public static final RegistryObject<MobEffect> SPIT_BLIND = EFFECTS.register("spit_blind", SpitBlindEffect::new);
    public static final RegistryObject<MobEffect> ESTROGEN = EFFECTS.register("estrogen", EstrogenEffect::new);
    public static final RegistryObject<MobEffect> WELL_RESTED = EFFECTS.register("well_rested", ch.rere232.skofcraft.effect.WellRestedEffect::new);
    public static final RegistryObject<MobEffect> TRIPPED = EFFECTS.register("tripped", TrippedEffect::new);

    private SkofcraftEffects() {}
}
