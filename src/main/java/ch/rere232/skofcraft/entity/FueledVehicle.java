package ch.rere232.skofcraft.entity;

/** Anything with a tank that takes Jet Fuel Canisters; see {@link AircraftFuel}. */
public interface FueledVehicle {
    /** Fuel in ticks of engine time. */
    int getFuel();

    int getMaxFuel();

    void setFuel(int fuel);
}
