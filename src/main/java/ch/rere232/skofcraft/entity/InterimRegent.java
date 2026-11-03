package ch.rere232.skofcraft.entity;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.advancement.SkofcraftAdvancements;
import ch.rere232.skofcraft.kingdom.SnusKingdom;
import ch.rere232.skofcraft.registry.SkofcraftItems;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Temporarily in charge until someone pulls Excalibur from the stone. Has been temporarily in charge for 900 years.
 * Stays by the throne. Kill one and the office just appoints another interim.
 */
public class InterimRegent extends KingdomNoble {
    private static final int GREETINGS = 6;
    private static final int FORMER_GREETINGS = 3;

    public InterimRegent(EntityType<? extends InterimRegent> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    @Override
    protected void addWanderGoals() {}

    @Override
    protected void greet(Player player) {
        boolean crowned = level() instanceof ServerLevel kingdom && SnusKingdom.LEVEL == kingdom.dimension() && SnusKingdom.isCrowned(kingdom);
        player.displayClientMessage(Component.translatable(crowned
                ? "message.skofcraft.regent_former." + getRandom().nextInt(FORMER_GREETINGS)
                : "message.skofcraft.regent." + getRandom().nextInt(GREETINGS)), false);
    }

    @Override
    protected void updateTrades() {
        getOffers().add(sell(new ItemStack(Items.EMERALD, 24), new ItemStack(SkofcraftItems.FLAVOR_ROYAL_RESERVE.get())));
        getOffers().add(sell(new ItemStack(Items.EMERALD, 8), new ItemStack(SkofcraftItems.FLAVOR_REGENTS_BLEND.get())));
        Item knockoff = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath(SkofcraftMod.MODID, "knockoff_excalibur"));
        getOffers().add(sell(new ItemStack(Items.EMERALD, 16), new ItemStack(knockoff)));
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel serverLevel) {
            SnusKingdom.onRegentDied(serverLevel);
            if (source.getEntity() instanceof Player player) {
                SkofcraftAdvancements.award(player, SkofcraftAdvancements.REGICIDE);
            }
        }
    }
}
