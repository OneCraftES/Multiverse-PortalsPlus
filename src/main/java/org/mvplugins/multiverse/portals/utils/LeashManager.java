package org.mvplugins.multiverse.portals.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;
import org.jvnet.hk2.annotations.Service;
import org.mvplugins.multiverse.external.jakarta.inject.Inject;

@Service
public class LeashManager implements org.bukkit.event.Listener {

    @Inject
    public LeashManager() {
    }

    private final java.util.Set<java.util.UUID> pendingTeleport = java.util.Collections
            .newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    public static class EntityGraph {
        public final java.util.Set<Entity> entities;
        public final java.util.Map<java.util.UUID, java.util.UUID> leashMap; // EntityUUID -> HolderUUID
        public final java.util.Map<java.util.UUID, java.util.UUID> mountMap; // PassengerUUID -> VehicleUUID

        public EntityGraph(java.util.Set<Entity> entities, java.util.Map<java.util.UUID, java.util.UUID> leashMap,
                java.util.Map<java.util.UUID, java.util.UUID> mountMap) {
            this.entities = entities;
            this.leashMap = leashMap;
            this.mountMap = mountMap;
        }
    }

    /**
     * Scans for the entire graph of connected entities (leash chain/web) starting
     * from the source.
     * Use reflection to verify "Leashable" status to avoid build path issues.
     *
     * @param startEntity The entity to start scanning from.
     * @param source      The location to scan around.
     * @return A set of all entities connected in the leash chain.
     */
    public EntityGraph collectWholeChain(Entity startEntity, Location source) {
        java.util.Set<Entity> visited = java.util.Collections
                .newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());
        java.util.Queue<Entity> queue = new java.util.LinkedList<>();

        // Relationship maps for snapshotting (UUIDs)
        java.util.Map<java.util.UUID, java.util.UUID> leashMap = new java.util.HashMap<>();
        java.util.Map<java.util.UUID, java.util.UUID> mountMap = new java.util.HashMap<>();

        // Start from the triggering entity
        queue.add(startEntity);
        visited.add(startEntity);

        while (!queue.isEmpty()) {
            Entity current = queue.poll();
            Location currentLoc = current.getLocation();

            // --- 0. Snapshot Current State ---
            // Leash State
            if (isLeashedToAny(current)) {
                Entity holder = getLeashHolder(current);
                if (holder != null) {
                    leashMap.put(current.getUniqueId(), holder.getUniqueId());
                    if (!visited.contains(holder)) {
                        visited.add(holder);
                        queue.add(holder);
                    }
                }
            }
            // Mount State
            Entity vehicle = current.getVehicle();
            if (vehicle != null) {
                mountMap.put(current.getUniqueId(), vehicle.getUniqueId());
                if (!visited.contains(vehicle)) {
                    visited.add(vehicle);
                    queue.add(vehicle);
                }
            }

            // --- Directions ---

            // 1. Find entities held by current (Downwards Leash)
            double radius = 15.0;
            try {
                Collection<Entity> nearby = currentLoc.getWorld().getNearbyEntities(currentLoc, radius, radius, radius);
                for (Entity e : nearby) {
                    if (!visited.contains(e) && isLeashedTo(e, current)) {
                        // e is held by current
                        leashMap.put(e.getUniqueId(), current.getUniqueId());
                        visited.add(e);
                        queue.add(e);
                    }
                }
            } catch (Exception e) {
            }

            // 2. Find passengers (Downwards Mount)
            for (Entity passenger : current.getPassengers()) {
                mountMap.put(passenger.getUniqueId(), current.getUniqueId());
                if (!visited.contains(passenger)) {
                    visited.add(passenger);
                    queue.add(passenger);
                }
            }
        }

        return new EntityGraph(visited, leashMap, mountMap);
    }

    /**
     * Teleports the entire collected chain relative to the trigger entity.
     *
     * @param graph   The snapshot graph of connected entities.
     * @param trigger The entity that triggered the teleport (source reference).
     * @param source  The source location.
     * @param target  The target location.
     */
    public void teleportChain(EntityGraph graph, Entity trigger, Location source, Location target) {
        if (graph == null || graph.entities.isEmpty())
            return;

        // Protect ALL entities in the chain
        toggleLeashProtection(graph.entities, true);

        try {
            // We iterate safely. The 'trigger' entity is usually teleported by the caller
            for (Entity entity : graph.entities) {
                if (entity.equals(trigger))
                    continue; // Skip trigger, caller handles it

                Location entityLoc = entity.getLocation();
                Vector offset = entityLoc.toVector().subtract(source.toVector());
                Location newLoc = target.clone().add(offset);
                newLoc.setWorld(target.getWorld());

                teleportEntityOnly(entity, newLoc);
            }

            // Restore Links (Leashes and Mounts)
            // Use delayed task to ensure worlds are loaded and entities valid
            org.bukkit.plugin.Plugin plugin = org.bukkit.Bukkit.getPluginManager().getPlugin("Multiverse-Portals");
            if (plugin != null) {
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    // Restore Leashes
                    for (java.util.Map.Entry<java.util.UUID, java.util.UUID> entry : graph.leashMap.entrySet()) {
                        Entity leashable = org.bukkit.Bukkit.getEntity(entry.getKey());
                        Entity holder = org.bukkit.Bukkit.getEntity(entry.getValue());

                        if (leashable != null && holder != null && leashable.isValid() && holder.isValid()) {
                            setLeashHolder(leashable, holder);
                        }
                    }
                    // Restore Mounts
                    for (java.util.Map.Entry<java.util.UUID, java.util.UUID> entry : graph.mountMap.entrySet()) {
                        Entity passenger = org.bukkit.Bukkit.getEntity(entry.getKey());
                        Entity vehicle = org.bukkit.Bukkit.getEntity(entry.getValue());

                        if (passenger != null && vehicle != null && passenger.isValid() && vehicle.isValid()
                                && passenger.getVehicle() == null) {
                            vehicle.addPassenger(passenger);
                        }
                    }
                }, 10L);
            }

        } finally {
            toggleLeashProtection(graph.entities, false);
        }
    }

    private void teleportEntityOnly(Entity entity, Location target) {
        // Eject first just in case
        entity.leaveVehicle();
        for (Entity p : new ArrayList<>(entity.getPassengers())) {
            entity.removePassenger(p);
        }
        entity.teleport(target);
    }

    // ... (Keep toggleLeashProtection and onEntityUnleash) ...

    public void toggleLeashProtection(Collection<Entity> entities, boolean enable) {
        if (entities == null || entities.isEmpty())
            return;
        for (Entity entity : entities) {
            if (enable) {
                this.pendingTeleport.add(entity.getUniqueId());
            } else {
                this.pendingTeleport.remove(entity.getUniqueId());
            }
        }
    }

    @org.bukkit.event.EventHandler
    public void onEntityUnleash(org.bukkit.event.entity.EntityUnleashEvent event) {
        if (this.pendingTeleport.contains(event.getEntity().getUniqueId())) {
            try {
                java.lang.reflect.Method setDropLeash = event.getClass().getMethod("setDropLeash", boolean.class);
                setDropLeash.invoke(event, false);
            } catch (Exception e) {
                event.setCancelled(true);
            }
        }
    }

    // Reflection Helpers

    private Entity getLeashHolder(Entity entity) {
        try {
            java.lang.reflect.Method getLeashHolderMethod = entity.getClass().getMethod("getLeashHolder");
            Object holder = getLeashHolderMethod.invoke(entity);
            if (holder instanceof Entity) {
                return (Entity) holder;
            }
        } catch (Exception e) {
        }
        return null;
    }

    private boolean isLeashedToAny(Entity entity) {
        try {
            java.lang.reflect.Method isLeashedMethod = entity.getClass().getMethod("isLeashed");
            return (boolean) isLeashedMethod.invoke(entity);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isLeashedTo(Entity entity, Entity holder) {
        try {
            if (!isLeashedToAny(entity))
                return false;
            Entity currentHolder = getLeashHolder(entity);
            return holder.equals(currentHolder);
        } catch (Exception e) {
            return false;
        }
    }

    private void setLeashHolder(Entity entity, Entity holder) {
        try {
            java.lang.reflect.Method setLeashHolderMethod = entity.getClass().getMethod("setLeashHolder", Entity.class);
            setLeashHolderMethod.invoke(entity, holder);
        } catch (Exception e) {
        }
    }
}
