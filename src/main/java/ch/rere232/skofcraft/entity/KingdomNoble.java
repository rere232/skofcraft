package ch.rere232.skofcraft.entity;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.registry.SkofcraftEntities;
import ch.rere232.skofcraft.registry.SkofcraftItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.InteractGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.LookAtTradingPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.TradeWithPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * A Tobacconist of noble birth. Lives in the Snus Kingdom and trades in flavors you can't get anywhere else. Being an
 * {@link AbstractVillager}, killing one goes on your HR record like any other colleague.
 */
public class KingdomNoble extends AbstractVillager {
    private static final int STOCK = 8;
    private static final float PRICE_MULTIPLIER = 0.05F;
    private static final int TRADE_XP = 5;
    private static final int RESTOCK_INTERVAL = 24000;

    public KingdomNoble(EntityType<? extends KingdomNoble> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0D).add(Attributes.MOVEMENT_SPEED, 0.45D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 0.5D));
        goalSelector.addGoal(1, new LookAtTradingPlayerGoal(this));
        addWanderGoals();
        goalSelector.addGoal(9, new InteractGoal(this, Player.class, 3.0F, 1.0F));
        goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0F));
    }

    protected void addWanderGoals() {
        goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.35D));
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !isAlive() || isTrading() || getOffers().isEmpty()) {
            return super.mobInteract(player, hand);
        }
        if (!level().isClientSide) {
            greet(player);
            setTradingPlayer(player);
            openTradingScreen(player, getDisplayName(), 1);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    protected void greet(Player player) {}

    /** Only real villagers restock by themselves, and only at a workstation. */
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (tickCount % RESTOCK_INTERVAL == 0 && !isTrading()) {
            getOffers().forEach(MerchantOffer::resetUses);
        }
    }

    @Override
    protected void updateTrades() {
        getOffers().add(sell(new ItemStack(Items.EMERALD, 8), new ItemStack(SkofcraftItems.FLAVOR_REGENTS_BLEND.get())));
        getOffers().add(sell(new ItemStack(Items.EMERALD, 4), new ItemStack(SkofcraftItems.ADDICTOL.get())));
        getOffers().add(sell(new ItemStack(SkofcraftItems.TOBACCO_LEAF_DRY.get(), 12), new ItemStack(Items.EMERALD)));
        getOffers().add(sell(new ItemStack(Items.GOLD_INGOT, 2), new ItemStack(Items.EMERALD)));
    }

    protected static MerchantOffer sell(ItemStack cost, ItemStack result) {
        return new MerchantOffer(cost, result, STOCK, TRADE_XP, PRICE_MULTIPLIER);
    }

    @Override
    protected void rewardTradeXp(MerchantOffer offer) {}

    @Override
    public boolean showProgressBar() {
        return false;
    }

    /** Nobles only ever turn up fully grown, and the Kingdom doesn't make new ones. */
    @Override
    public boolean isBaby() {
        return false;
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Mod.EventBusSubscriber(modid = SkofcraftMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class Events {
        @SubscribeEvent
        public static void onAttributes(EntityAttributeCreationEvent event) {
            event.put(SkofcraftEntities.KINGDOM_NOBLE.get(), createAttributes().build());
            event.put(SkofcraftEntities.INTERIM_REGENT.get(), createAttributes().build());
        }

        @SubscribeEvent
        public static void onSpawnPlacements(SpawnPlacementRegisterEvent event) {
            event.register(SkofcraftEntities.KINGDOM_NOBLE.get(), SpawnPlacements.Type.ON_GROUND,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mob::checkMobSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
        }
    }
}
