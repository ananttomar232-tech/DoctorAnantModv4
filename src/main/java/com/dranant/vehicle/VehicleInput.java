package com.dranant.vehicle;

/**
 * Extra driving inputs for the local driver (vehicles are simulated on the driving client, like boats).
 * Written every client tick by {@code client.VehicleClientEvents}; stays false on a dedicated server.
 */
public final class VehicleInput {
    /** Jump key: handbrake / drift. */
    public static volatile boolean brake;
    /** Sprint key: nitro (car) / turbo boost (bike). */
    public static volatile boolean boost;

    private VehicleInput() {}
}
