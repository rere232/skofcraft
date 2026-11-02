package ch.rere232.skofcraft.entity;

import ch.rere232.skofcraft.registry.SkofcraftSounds;
import ch.rere232.skofcraft.advancement.SkofcraftAdvancements;
import ch.rere232.skofcraft.registry.SkofcraftItems;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Aircraft fuel, counted in ticks of engine time. The engines burn one tick of fuel per tick while someone
 * is at the controls; a Jet Fuel Canister adds {@link #PER_CANISTER}. The tank is kept on the item when the
 * aircraft is picked up.
 */
public final class AircraftFuel {
    public static final int PER_CANISTER = 20 * 60 * 4;
    public static final String TAG = "Fuel";

    private AircraftFuel() {}

    /** Refuels from a Jet Fuel Canister held in {@code hand}; returns false if the player is not holding one. */
    public static boolean tryRefuel(FueledVehicle aircraft, Entity entity, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(SkofcraftItems.JET_FUEL_CANISTER.get())) {
            return false;
        }
        if (entity.level().isClientSide) {
            return true;
        }
        if (aircraft.getFuel() >= aircraft.getMaxFuel()) {
            player.displayClientMessage(Component.translatable("message.skofcraft.aircraft_tank_full"), true);
            return true;
        }
        aircraft.setFuel(Math.min(aircraft.getMaxFuel(), aircraft.getFuel() + PER_CANISTER));
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
            ItemStack empty = new ItemStack(SkofcraftItems.EMPTY_FUEL_CANISTER.get());
            if (held.isEmpty()) {
                player.setItemInHand(hand, empty);
            } else if (!player.getInventory().add(empty)) {
                player.drop(empty, false);
            }
        }
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SkofcraftSounds.FUEL_PUMP.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        player.displayClientMessage(Component.translatable("message.skofcraft.aircraft_refueled", percent(aircraft)), true);
        SkofcraftAdvancements.award(player, SkofcraftAdvancements.REFUEL);
        return true;
    }

    /** Server only: burns one tick of fuel and warns the pilot when the tank runs low or dry. */
    public static void burn(FueledVehicle aircraft, LivingEntity pilot) {
        int fuel = aircraft.getFuel();
        if (fuel <= 0) {
            return;
        }
        fuel--;
        aircraft.setFuel(fuel);
        if (!(pilot instanceof Player player)) {
            return;
        }
        if (fuel == 0) {
            player.displayClientMessage(Component.translatable("message.skofcraft.aircraft_out_of_fuel").withStyle(ChatFormatting.RED), true);
            Entity vehicle = player.getVehicle();
            if (vehicle != null && !vehicle.onGround()) {
                SkofcraftAdvancements.award(player, SkofcraftAdvancements.RUNNING_ON_FUMES);
            }
        } else if (fuel == aircraft.getMaxFuel() / 10) {
            player.displayClientMessage(Component.translatable("message.skofcraft.aircraft_low_fuel").withStyle(ChatFormatting.GOLD), true);
        }
    }

    public static int percent(FueledVehicle aircraft) {
        return Math.round(100.0F * aircraft.getFuel() / aircraft.getMaxFuel());
    }

    public static ItemStack withFuel(ItemStack stack, int fuel) {
        if (fuel > 0) {
            stack.getOrCreateTag().putInt(TAG, fuel);
        }
        return stack;
    }

    public static int fuelOf(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0 : Math.max(0, tag.getInt(TAG));
    }

    public static Component tooltip(ItemStack stack, int maxFuel) {
        int fuel = Math.min(fuelOf(stack), maxFuel);
        return Component.translatable("tooltip.skofcraft.aircraft_fuel", Math.round(100.0F * fuel / maxFuel))
                .withStyle(fuel > 0 ? ChatFormatting.GOLD : ChatFormatting.RED);
    }
}
