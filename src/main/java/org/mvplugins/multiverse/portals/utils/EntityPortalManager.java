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

import org.mvplugins.multiverse.portals.MultiversePortals;
import org.jetbrains.annotations.NotNull;

@Service
public class EntityPortalManager {

    private final MultiversePortals plugin;
    private final PortalVisualizer portalVisualizer;
    private final LeashManager leashManager;

    @Inject
    public EntityPortalManager(@NotNull MultiversePortals plugin,
            @NotNull PortalVisualizer portalVisualizer,
            @NotNull LeashManager leashManager) {
        this.plugin = plugin;
        this.portalVisualizer = portalVisualizer;
        this.leashManager = leashManager;
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

        Location sourceLoc = entity.getLocation();
        // Collect whole chain/graph BEFORE teleporting the holder
        org.mvplugins.multiverse.portals.utils.LeashManager.EntityGraph graph = this.leashManager
                .collectWholeChain(entity, sourceLoc);

        // Protect chain
        this.leashManager.toggleLeashProtection(graph.entities, true);

        try {
            Location targetLocation = destination.getLocation(entity).getOrNull();
            if (targetLocation == null) {
                return false;
            }

            Vector velocity = entity.getVelocity();
            boolean success = entity.teleport(targetLocation);
            if (success) {
                entity.setVelocity(velocity);
                // Teleport the rest of the chain
                this.leashManager.teleportChain(graph, entity, sourceLoc, targetLocation);
                // Visual Effects
                this.portalVisualizer.displayReactiveEffect(entity, portal);
            }
            return success;
        } finally {
            this.leashManager.toggleLeashProtection(graph.entities, false);
        }
    }

    private boolean handleVehicleTeleport(Vehicle vehicle, DestinationInstance<?, ?> destination) {
        Location targetLocation = destination.getLocation(vehicle).getOrNull();
        if (targetLocation == null) {
            return false;
        }

        Location sourceLoc = vehicle.getLocation();
        org.mvplugins.multiverse.portals.utils.LeashManager.EntityGraph graph = this.leashManager
                .collectWholeChain(vehicle, sourceLoc);

        // Protect them from breaking due to distance
        this.leashManager.toggleLeashProtection(graph.entities, true);

        try {
            // Must eject passengers before teleporting vehicle
            List<Entity> passengers = List.copyOf(vehicle.getPassengers());
            for (Entity passenger : passengers) {
                vehicle.removePassenger(passenger);
            }

            // Teleport vehicle
            Vector velocity = vehicle.getVelocity();
            boolean success = vehicle.teleport(targetLocation);
            if (!success) {
                return false;
            }
            vehicle.setVelocity(velocity);

            // Teleport the rest (passengers + leashes) using the graph
            this.leashManager.teleportChain(graph, vehicle, sourceLoc, targetLocation);

            return true;
        } finally {
            this.leashManager.toggleLeashProtection(graph.entities, false);
        }
    }

    public boolean canUsePortal(Entity entity) {
        if (entity == null) {
            return false;
        }
        return !(entity instanceof Player);
    }
}
