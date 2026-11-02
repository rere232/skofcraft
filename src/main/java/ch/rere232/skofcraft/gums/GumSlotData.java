package ch.rere232.skofcraft.gums;

import ch.rere232.skofcraft.registry.SkofcraftSounds;
import ch.rere232.skofcraft.config.SkofcraftConfig;
import ch.rere232.skofcraft.entity.SpitProjectile;
import ch.rere232.skofcraft.item.GumConsumableItem;
import ch.rere232.skofcraft.registry.SkofcraftItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class GumSlotData {
    public static final int SLOT_COUNT = 4;

    private static final String ROOT = "SkofcraftGums";
    private static final String SLOT_PREFIX = "Slot";
    private static final String ITEM_PREFIX = "SlotItem";
    private static final String FLAVOR_PREFIX = "SlotFlavor";
    private static final String STRENGTH_PREFIX = "SlotStrength";
    private static final String SPIT_LAST_KEY = "SpitLast";
    private static final String CHEW_TICKS_KEY = "ChewTicks";

    private static final int SPIT_COOLDOWN_TICKS = 20 * 30;
    private static final int SPIT_WARN_TICKS = 20 * 60 * 4 + 20 * 30;
    private static final int SPIT_LIMIT_TICKS = 20 * 60 * 5;
    private static final String TOLERANCE_KEY = "Tolerance";
    private static final String ADDICTION_KEY = "Addiction";
    private static final String CRAVING_TICKS_KEY = "CravingTicks";

    public static final int MAX_TOLERANCE = 100;
    public static final int MAX_ADDICTION = 100;

    private final Player player;

    public GumSlotData(Player player) {
        this.player = player;
    }

    public static GumSlotData get(Player player) {
        return new GumSlotData(player);
    }

    public ItemStack getSlotItem(int index) {
        CompoundTag rootTag = getRootTag();
        int ticks = rootTag.getInt(SLOT_PREFIX + index);
        if (ticks > 0) {
            String itemId = rootTag.getString(ITEM_PREFIX + index);
            ResourceLocation key = ResourceLocation.tryParse(itemId);
            if (key != null) {
                Item item = BuiltInRegistries.ITEM.get(key);
                if (item != net.minecraft.world.item.Items.AIR) {
                    ItemStack stack = new ItemStack(item);
                    String flavorId = rootTag.getString(FLAVOR_PREFIX + index);
                    if (!flavorId.isEmpty()) {
                        stack.getOrCreateTag().putString(GumConsumableItem.TAG_FLAVOR, flavorId);
                    }
                    GumConsumableItem.setStrength(stack, rootTag.getInt(STRENGTH_PREFIX + index));
                    return stack;
                }
            }
            return new ItemStack(SkofcraftItems.SNUS_POUCH.get());
        }
        return ItemStack.EMPTY;
    }

    public int getRemainingTicks(int index) {
        CompoundTag rootTag = getRootTag();
        return rootTag.getInt(SLOT_PREFIX + index);
    }

    /** True while a chewing tobacco plug is in one of the gum slots. */
    public boolean hasActiveChew() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (getRemainingTicks(i) > 0 && getSlotItem(i).getItem() == SkofcraftItems.CHEWING_TOBACCO.get()) {
                return true;
            }
        }
        return false;
    }

    /** Spits a projectile if the player is chewing and the 30 second cooldown is over. */
    public static void trySpit(ServerPlayer player) {
        GumSlotData data = get(player);
        if (!data.hasActiveChew()) {
            player.displayClientMessage(Component.translatable("message.skofcraft.spit_nothing"), true);
            return;
        }

        CompoundTag rootTag = getRootTag(player);
        long now = player.level().getGameTime();
        long last = rootTag.contains(SPIT_LAST_KEY) ? rootTag.getLong(SPIT_LAST_KEY) : now - SPIT_COOLDOWN_TICKS;
        long remaining = SPIT_COOLDOWN_TICKS - (now - last);
        if (remaining > 0) {
            player.displayClientMessage(Component.translatable("message.skofcraft.spit_cooldown", (remaining + 19) / 20), true);
            return;
        }

        rootTag.putLong(SPIT_LAST_KEY, now);
        rootTag.putInt(CHEW_TICKS_KEY, 0);

        SpitProjectile spit = new SpitProjectile(player.level(), player);
        spit.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.2F, 1.0F);
        player.level().addFreshEntity(spit);
        player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(), SkofcraftSounds.SPIT.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
    }

    /** Chewing needs a spit at least every 5 minutes, otherwise nausea and slowness set in. */
    private static void updateChewTimer(Player player, CompoundTag rootTag) {
        if (!get(player).hasActiveChew()) {
            rootTag.putInt(CHEW_TICKS_KEY, 0);
            return;
        }
        int ticks = rootTag.getInt(CHEW_TICKS_KEY) + 1;
        rootTag.putInt(CHEW_TICKS_KEY, ticks);
        if (ticks == SPIT_WARN_TICKS) {
            player.displayClientMessage(Component.translatable("message.skofcraft.spit_soon"), true);
        } else if (ticks == SPIT_LIMIT_TICKS) {
            player.displayClientMessage(Component.translatable("message.skofcraft.spit_now"), true);
        }
        if (ticks >= SPIT_LIMIT_TICKS && ticks % 40 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0, true, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 0, true, false, true));
        }
    }

    public int getTolerance() {
        return getRootTag().getInt(TOLERANCE_KEY);
    }

    public int getAddiction() {
        return getRootTag().getInt(ADDICTION_KEY);
    }

    public void removeSlot(int index) {
        CompoundTag rootTag = getRootTag();
        rootTag.putInt(SLOT_PREFIX + index, 0);
        rootTag.remove(ITEM_PREFIX + index);
        rootTag.remove(FLAVOR_PREFIX + index);
        rootTag.remove(STRENGTH_PREFIX + index);
    }

    public void tryInsert(int slotIndex, ItemStack stack) {
        if (!(stack.getItem() instanceof GumConsumableItem consumable)) {
            return;
        }

        CompoundTag rootTag = getRootTag();
        String key = SLOT_PREFIX + slotIndex;
        int ticks = rootTag.getInt(key);
        if (ticks <= 0) {
            int durationTicks = consumable.getDurationMinutes() * 60 * 20;
            rootTag.putInt(key, durationTicks);
            rootTag.putString(ITEM_PREFIX + slotIndex, BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            String flavorId = GumConsumableItem.getFlavorId(stack);
            if (!flavorId.isEmpty()) {
                rootTag.putString(FLAVOR_PREFIX + slotIndex, flavorId);
            }
            rootTag.putInt(STRENGTH_PREFIX + slotIndex, GumConsumableItem.getStrength(stack));
            onProductInserted(rootTag, GumConsumableItem.getStrength(stack));
        }
    }

    public static boolean tryInsert(Player player, ItemStack stack) {
        if (!(stack.getItem() instanceof GumConsumableItem consumable)) {
            return false;
        }

        CompoundTag rootTag = getRootTag(player);
        for (int i = 0; i < SLOT_COUNT; i++) {
            String key = SLOT_PREFIX + i;
            int ticks = rootTag.getInt(key);
            if (ticks <= 0) {
                int durationTicks = consumable.getDurationMinutes() * 60 * 20;
                rootTag.putInt(key, durationTicks);
                rootTag.putString(ITEM_PREFIX + i, BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
                String flavorId = GumConsumableItem.getFlavorId(stack);
                if (!flavorId.isEmpty()) {
                    rootTag.putString(FLAVOR_PREFIX + i, flavorId);
                }
                rootTag.putInt(STRENGTH_PREFIX + i, GumConsumableItem.getStrength(stack));
                onProductInserted(rootTag, GumConsumableItem.getStrength(stack));
                return true;
            }
        }
        return false;
    }

    public static void tick(Player player) {
        CompoundTag rootTag = getRootTag(player);
        int active = 0;
        for (int i = 0; i < SLOT_COUNT; i++) {
            String key = SLOT_PREFIX + i;
            int ticks = rootTag.getInt(key);
            if (ticks > 0) {
                int nextTicks = ticks - 1;
                rootTag.putInt(key, nextTicks);
                if (nextTicks <= 0) {
                    rootTag.remove(ITEM_PREFIX + i);
                    rootTag.remove(FLAVOR_PREFIX + i);
                    rootTag.remove(STRENGTH_PREFIX + i);
                }
                active++;
            }
        }

        if (!player.level().isClientSide) {
            updateChewTimer(player, rootTag);
            if (active > 0) {
                rootTag.putInt(CRAVING_TICKS_KEY, 0);
                if (SkofcraftConfig.enableTolerance && player.tickCount % 200 == 0) {
                    int tolerance = rootTag.getInt(TOLERANCE_KEY);
                    rootTag.putInt(TOLERANCE_KEY, Math.min(MAX_TOLERANCE, tolerance + 1));
                }
                GumStatusEffects.apply(player, get(player), SkofcraftConfig.enableNegativeEffects, SkofcraftConfig.enableTolerance, rootTag.getInt(TOLERANCE_KEY));
                return;
            }

            if (SkofcraftConfig.enableTolerance && player.tickCount % 200 == 0) {
                int tolerance = rootTag.getInt(TOLERANCE_KEY);
                rootTag.putInt(TOLERANCE_KEY, Math.max(0, tolerance - 1));
            }

            if (SkofcraftConfig.enableAddiction) {
                int addiction = rootTag.getInt(ADDICTION_KEY);
                int cravingTicks = rootTag.getInt(CRAVING_TICKS_KEY) + 1;
                rootTag.putInt(CRAVING_TICKS_KEY, cravingTicks);

                if (player.tickCount % 1200 == 0) {
                    rootTag.putInt(ADDICTION_KEY, Math.max(0, addiction - 1));
                }

                GumStatusEffects.applyCraving(player, addiction, cravingTicks, SkofcraftConfig.enableNegativeEffects);
            } else {
                rootTag.putInt(CRAVING_TICKS_KEY, 0);
            }
        }
    }

    /** Wipes tolerance, addiction and cravings back to zero. */
    public static void resetDependence(Player player) {
        CompoundTag rootTag = getRootTag(player);
        rootTag.putInt(TOLERANCE_KEY, 0);
        rootTag.putInt(ADDICTION_KEY, 0);
        rootTag.putInt(CRAVING_TICKS_KEY, 0);
    }

    /** Smoking is very addictive: the strength rises from cigarette to cigar. */
    public static void onSmoked(Player player, int strength) {
        onProductInserted(getRootTag(player), strength);
    }

    /** A vape puff builds tolerance and addiction a little, more with stronger liquid. */
    public static void onVapePuff(Player player, int strength) {
        CompoundTag rootTag = getRootTag(player);
        rootTag.putInt(CRAVING_TICKS_KEY, 0);
        if (SkofcraftConfig.enableTolerance) {
            rootTag.putInt(TOLERANCE_KEY, Math.min(MAX_TOLERANCE, rootTag.getInt(TOLERANCE_KEY) + 1 + strength));
        }
        if (SkofcraftConfig.enableAddiction) {
            rootTag.putInt(ADDICTION_KEY, Math.min(MAX_ADDICTION, rootTag.getInt(ADDICTION_KEY) + 1 + strength));
        }
    }

    /** A pinch of snuff counts as a strong product for tolerance and addiction. */
    public static void onSnuffUsed(Player player) {
        onProductInserted(getRootTag(player), 1);
    }

    private static void onProductInserted(CompoundTag rootTag, int strength) {
        rootTag.putInt(CRAVING_TICKS_KEY, 0);

        if (SkofcraftConfig.enableTolerance) {
            int tolerance = rootTag.getInt(TOLERANCE_KEY);
            rootTag.putInt(TOLERANCE_KEY, Math.min(MAX_TOLERANCE, tolerance + 2 + strength));
        }

        if (SkofcraftConfig.enableAddiction) {
            int addiction = rootTag.getInt(ADDICTION_KEY);
            rootTag.putInt(ADDICTION_KEY, Math.min(MAX_ADDICTION, addiction + 3 + strength));
        }
    }

    public static void applySyncData(Player player, int[] ticks, String[] itemIds, String[] flavorIds, int[] strengths, int tolerance, int addiction) {
        if (ticks == null || itemIds == null || flavorIds == null || strengths == null || ticks.length < SLOT_COUNT || itemIds.length < SLOT_COUNT || flavorIds.length < SLOT_COUNT || strengths.length < SLOT_COUNT) {
            return;
        }
        CompoundTag rootTag = getRootTag(player);
        rootTag.putInt(TOLERANCE_KEY, tolerance);
        rootTag.putInt(ADDICTION_KEY, addiction);
        for (int i = 0; i < SLOT_COUNT; i++) {
            String tickKey = SLOT_PREFIX + i;
            String itemKey = ITEM_PREFIX + i;
            int value = Math.max(0, ticks[i]);
            rootTag.putInt(tickKey, value);
            if (value > 0 && !itemIds[i].isEmpty()) {
                rootTag.putString(itemKey, itemIds[i]);
                rootTag.putInt(STRENGTH_PREFIX + i, strengths[i]);
                if (!flavorIds[i].isEmpty()) {
                    rootTag.putString(FLAVOR_PREFIX + i, flavorIds[i]);
                } else {
                    rootTag.remove(FLAVOR_PREFIX + i);
                }
            } else {
                rootTag.remove(itemKey);
                rootTag.remove(FLAVOR_PREFIX + i);
                rootTag.remove(STRENGTH_PREFIX + i);
            }
        }
    }

    private CompoundTag getRootTag() {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT)) {
            persistent.put(ROOT, new CompoundTag());
        }
        return persistent.getCompound(ROOT);
    }

    private static CompoundTag getRootTag(Player player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT)) {
            persistent.put(ROOT, new CompoundTag());
        }
        return persistent.getCompound(ROOT);
    }
}
