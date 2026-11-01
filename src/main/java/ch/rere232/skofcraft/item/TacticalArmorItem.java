package ch.rere232.skofcraft.item;

import ch.rere232.skofcraft.SkofcraftMod;
import ch.rere232.skofcraft.advancement.SkofcraftAdvancements;
import ch.rere232.skofcraft.registry.SkofcraftEffects;
import ch.rere232.skofcraft.registry.SkofcraftItems;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Tactical armor: a high-tech military set beyond netherite.
 * <ul>
 *     <li>Helmet: gas mask and thermal visor. Night vision, water breathing (rebreather), and immunity to poison,
 *     wither, nausea, blindness, darkness, hunger and spit.</li>
 *     <li>Plate carrier: +4 hearts, projectiles do 60% less.</li>
 *     <li>Leggings: +15% speed.</li>
 *     <li>Boots: step up full blocks, fall damage cut by 80%.</li>
 *     <li>Gloves (Curios hands slot, see {@link TacticalGlovesItem}): +3 attack damage, +15% attack speed, +2 armor.</li>
 *     <li>Full set (with the gloves): no fire or fall damage, explosions do 70% less, and an automatic medkit that heals you and adds
 *     absorption when you drop low, every {@link #MEDKIT_COOLDOWN} ticks.</li>
 * </ul>
 */
public class TacticalArmorItem extends ArmorItem {
    public static final int MEDKIT_COOLDOWN = 20 * 90;
    private static final float MEDKIT_THRESHOLD = 0.35F;
    private static final String MEDKIT_READY_AT = "SkofcraftMedkitReadyAt";
    private static final int EFFECT_REFRESH = 260;
    private static final List<MobEffect> FILTERED = List.of(MobEffects.POISON, MobEffects.WITHER, MobEffects.CONFUSION,
            MobEffects.BLINDNESS, MobEffects.DARKNESS, MobEffects.HUNGER);
    private static final UUID HEALTH_ID = UUID.fromString("4f2a9c0e-7d1b-4e5a-9c3f-1a2b3c4d5e01");
    private static final UUID SPEED_ID = UUID.fromString("4f2a9c0e-7d1b-4e5a-9c3f-1a2b3c4d5e02");
    private static final UUID STEP_ID = UUID.fromString("4f2a9c0e-7d1b-4e5a-9c3f-1a2b3c4d5e03");

    public static final ArmorMaterial MATERIAL = new ArmorMaterial() {
        @Override
        public int getDurabilityForType(Type type) {
            // Netherite is 37 times these per-slot bases
            int base = switch (type) {
                case HELMET -> 11;
                case CHESTPLATE -> 16;
                case LEGGINGS -> 15;
                case BOOTS -> 13;
            };
            return base * 60;
        }

        @Override
        public int getDefenseForType(Type type) {
            return switch (type) {
                case HELMET -> 5;
                case CHESTPLATE -> 10;
                case LEGGINGS -> 8;
                case BOOTS -> 5;
            };
        }

        @Override
        public int getEnchantmentValue() {
            return 20;
        }

        @Override
        public SoundEvent getEquipSound() {
            return SoundEvents.ARMOR_EQUIP_NETHERITE;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(SkofcraftItems.COMPOSITE_PLATE.get());
        }

        @Override
        public String getName() {
            return SkofcraftMod.MODID + ":tactical";
        }

        @Override
        public float getToughness() {
            return 5.0F;
        }

        @Override
        public float getKnockbackResistance() {
            return 0.25F;
        }
    };

    @Nullable
    private Multimap<Attribute, AttributeModifier> modifiers;

    public TacticalArmorItem(Type type, Properties properties) {
        super(MATERIAL, type, properties);
    }

    // ---------------------------------------------------------------- what is worn

    public static boolean wearing(LivingEntity entity, Type type) {
        ItemStack stack = entity.getItemBySlot(type.getSlot());
        return stack.getItem() instanceof TacticalArmorItem armor && armor.getType() == type;
    }

    /** All four armor pieces, plus the gloves in the Curios hands slot. */
    public static boolean fullSet(LivingEntity entity) {
        for (Type type : Type.values()) {
            if (!wearing(entity, type)) {
                return false;
            }
        }
        return CuriosApi.getCuriosInventory(entity)
                .map(curios -> curios.isEquipped(SkofcraftItems.TACTICAL_GLOVES.get()))
                .orElse(false);
    }

    // ---------------------------------------------------------------- stats

    /** The usual armor values, plus each piece's extra: health on the chest, speed on the legs, step height on the boots. */
    @Override
    public @NotNull Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(@NotNull EquipmentSlot slot) {
        if (slot != getType().getSlot()) {
            return super.getDefaultAttributeModifiers(slot);
        }
        if (modifiers == null) {
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
            builder.putAll(super.getDefaultAttributeModifiers(slot));
            switch (getType()) {
                case CHESTPLATE -> builder.put(Attributes.MAX_HEALTH,
                        new AttributeModifier(HEALTH_ID, "Tactical armor health", 8.0D, AttributeModifier.Operation.ADDITION));
                case LEGGINGS -> builder.put(Attributes.MOVEMENT_SPEED,
                        new AttributeModifier(SPEED_ID, "Tactical armor speed", 0.15D, AttributeModifier.Operation.MULTIPLY_BASE));
                case BOOTS -> builder.put(ForgeMod.STEP_HEIGHT_ADDITION.get(),
                        new AttributeModifier(STEP_ID, "Tactical armor step", 0.6D, AttributeModifier.Operation.ADDITION));
                default -> {
                }
            }
            modifiers = builder.build();
        }
        return modifiers;
    }

    // ---------------------------------------------------------------- worn effects

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof Player player) || player.getItemBySlot(getType().getSlot()) != stack) {
            return;
        }
        if (getType() == Type.HELMET) {
            gasMask(player);
        } else if (getType() == Type.CHESTPLATE && fullSet(player)) {
            medkit(player, level);
            if (player.tickCount % 20 == 0) {
                SkofcraftAdvancements.award(player, SkofcraftAdvancements.FULLY_KITTED);
            }
        }
    }

    /** Filters out what you breathe or get in the eyes; the visor sees in the dark and the rebreather works underwater. */
    private static void gasMask(Player player) {
        for (MobEffect effect : FILTERED) {
            if (player.hasEffect(effect)) {
                player.removeEffect(effect);
            }
        }
        MobEffect spit = SkofcraftEffects.SPIT_BLIND.get();
        if (player.hasEffect(spit)) {
            player.removeEffect(spit);
        }
        keep(player, MobEffects.NIGHT_VISION);
        keep(player, MobEffects.WATER_BREATHING);
    }

    private static void keep(Player player, MobEffect effect) {
        MobEffectInstance current = player.getEffect(effect);
        // Night vision flickers under 200 ticks, so top it up well before that
        if (current == null || current.getDuration() < 220) {
            player.addEffect(new MobEffectInstance(effect, EFFECT_REFRESH, 0, true, false, false));
        }
    }

    /** Patches you up when you drop low, then needs time to recharge. */
    private static void medkit(Player player, Level level) {
        if (player.getHealth() > player.getMaxHealth() * MEDKIT_THRESHOLD) {
            return;
        }
        CompoundTag data = player.getPersistentData();
        long now = level.getGameTime();
        if (now < data.getLong(MEDKIT_READY_AT)) {
            return;
        }
        data.putLong(MEDKIT_READY_AT, now + MEDKIT_COOLDOWN);
        player.heal(10.0F);
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 30, 1));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 8, 1));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0F, 1.6F);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.displayClientMessage(Component.translatable("message.skofcraft.tactical_medkit").withStyle(ChatFormatting.AQUA), true);
        }
    }

    // ---------------------------------------------------------------- looks

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return SkofcraftMod.MODID + ":textures/models/armor/tactical_layer_" + (slot == EquipmentSlot.LEGS ? 2 : 1) + ".png";
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public @NotNull net.minecraft.client.model.HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack,
                                                                                         EquipmentSlot slot,
                                                                                         net.minecraft.client.model.HumanoidModel<?> original) {
                return ch.rere232.skofcraft.client.TacticalArmorModel.get(slot);
            }
        });
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.skofcraft.tactical_" + getType().getName()).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.skofcraft.tactical_set").withStyle(ChatFormatting.DARK_AQUA));
    }

    // ---------------------------------------------------------------- damage

    @Mod.EventBusSubscriber(modid = SkofcraftMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class Events {
        private Events() {}

        @SubscribeEvent
        public static void onHurt(LivingHurtEvent event) {
            LivingEntity entity = event.getEntity();
            var source = event.getSource();
            boolean full = fullSet(entity);
            if (full && (source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_FALL))) {
                if (source.is(DamageTypeTags.IS_FIRE)) {
                    entity.clearFire();
                }
                event.setCanceled(true);
                return;
            }
            float amount = event.getAmount();
            if (source.is(DamageTypeTags.IS_PROJECTILE) && wearing(entity, Type.CHESTPLATE)) {
                amount *= 0.4F;
            }
            if (source.is(DamageTypeTags.IS_FALL) && wearing(entity, Type.BOOTS)) {
                amount *= 0.2F;
            }
            if (source.is(DamageTypeTags.IS_EXPLOSION) && full) {
                amount *= 0.3F;
            }
            event.setAmount(amount);
        }
    }
}
