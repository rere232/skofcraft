package ch.rere232.skofcraft.carrier;

import ch.rere232.skofcraft.entity.AircraftCarrier;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Keeps the crew aboard while the carrier is underway: Shift orders all stop instead of jumping overboard. */
public class CarrierEvents {
    @SubscribeEvent
    public void onDismount(EntityMountEvent event) {
        if (!event.isDismounting() || event.getLevel().isClientSide
                || !(event.getEntityBeingMounted() instanceof AircraftCarrier carrier)
                || !carrier.holdsAboard(event.getEntityMounting())) {
            return;
        }
        event.setCanceled(true);
        carrier.onLeaveAttempt(event.getEntityMounting());
    }
}
