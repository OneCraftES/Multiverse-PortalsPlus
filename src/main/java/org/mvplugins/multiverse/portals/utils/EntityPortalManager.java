package org.mvplugins.multiverse.portals.utils;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.util.Vector;
import org.jvnet.hk2.annotations.Service;
import org.mvplugins.multiverse.core.destination.DestinationInstance;
import org.mvplugins.multiverse.external.jakarta.inject.Inject;
import org.mvplugins.multiverse.portals.MVPortal;

@Service
public class EntityPortalManager {

    @Inject
    public EntityPortalManager() {
    }

    public boolean teleportEntity(Entity entity, MVPortal portal) {
        if (entity == null || portal == null) {
            return false;
        }

        DestinationInstance<?, ?> destination = portal.getDestination();
        if (destination == null) {
            return false;
        }

        if (entity instanceof Vehicle vehicle) {
            return this.handleVehicleTeleport(vehicle, destination);
        }

        Location targetLocation = destination.getLocation(entity).getOrNull();
        if (targetLocation == null) {
            return false;
        }

        Vector velocity = entity.getVelocity();
        boolean success = entity.teleport(targetLocation);
        if (success) {
            entity.setVelocity(velocity);
        }
        return success;
    }

    private boolean handleVehicleTeleport(Vehicle vehicle, DestinationInstance<?, ?> destination) {
        Location targetLocation = destination.getLocation(vehicle).getOrNull();
        if (targetLocation == null) {
            return false;
        }

        // 1. Eject all passengers
        List<Entity> passengers = List.copyOf(vehicle.getPassengers());
        for (Entity passenger : passengers) {
            vehicle.removePassenger(passenger);
        }

        // 2. Teleport vehicle
        Vector velocity = vehicle.getVelocity();
        boolean success = vehicle.teleport(targetLocation);
        if (!success) {
            return false;
        }
        vehicle.setVelocity(velocity);

        // 3. Teleport and remount passengers
        for (Entity passenger : passengers) {
            passenger.teleport(targetLocation);
            vehicle.addPassenger(passenger);
        }

        return true;
    }

    public boolean canUsePortal(Entity entity) {
        if (entity == null) {
            return false;
        }
        return !(entity instanceof Player);
    }
}
