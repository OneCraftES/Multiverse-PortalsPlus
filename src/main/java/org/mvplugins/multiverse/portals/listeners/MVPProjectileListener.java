package org.mvplugins.multiverse.portals.listeners;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.dumptruckman.minecraft.util.Logging;
import org.bukkit.Location;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jvnet.hk2.annotations.Service;
import org.mvplugins.multiverse.external.jakarta.inject.Inject;
import org.mvplugins.multiverse.core.destination.DestinationInstance;
import org.mvplugins.multiverse.portals.MVPortal;
import org.mvplugins.multiverse.portals.MultiversePortals;
import org.mvplugins.multiverse.portals.utils.EntityPortalManager;
import org.mvplugins.multiverse.portals.utils.PortalManager;
import org.mvplugins.multiverse.portals.utils.PortalVisualizer;

@Service
public class MVPProjectileListener implements Listener {

    private final MultiversePortals plugin;
    private final EntityPortalManager entityPortalManager;
    private final PortalManager portalManager;
    private final PortalVisualizer portalVisualizer;
    private final Map<Entity, BukkitTask> trackedProjectiles;

    @Inject
    public MVPProjectileListener(
            MultiversePortals plugin,
            EntityPortalManager entityPortalManager,
            PortalManager portalManager,
            PortalVisualizer portalVisualizer) {
        this.plugin = plugin;
        this.entityPortalManager = entityPortalManager;
        this.portalManager = portalManager;
        this.portalVisualizer = portalVisualizer;
        this.trackedProjectiles = new ConcurrentHashMap<>();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        final Projectile projectile = event.getEntity();
        if (!(projectile instanceof EnderPearl)) {
            return;
        }
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!projectile.isValid() || projectile.isDead()) {
                    this.cancel();
                    trackedProjectiles.remove(projectile);
                    return;
                }
                Location loc = projectile.getLocation();
                MVPortal portal = portalManager.getPortal(loc);

                // TODO: Check if entity is allowed in portal settings if we implement that
                if (portal != null && portal.getTeleportNonPlayers()) {
                    this.cancel();
                    trackedProjectiles.remove(projectile);
                    handleProjectilePortal(projectile, portal);
                }
            }
        }.runTaskTimer(this.plugin, 0L, 1L);
        this.trackedProjectiles.put(projectile, task);
    }

    private void handleProjectilePortal(Projectile projectile, MVPortal portal) {
        DestinationInstance<?, ?> destination = portal.getDestination();
        Location destLoc = null;
        if (destination != null) {
            destLoc = destination.getLocation(projectile).getOrNull();
        }

        if (destLoc != null) {
            Logging.finer("Projectile %s entering portal: %s", projectile.getType(), portal.getName());

            this.portalVisualizer.displayReactiveEffect(projectile, portal);

            this.entityPortalManager.teleportEntity(projectile, portal);

            this.portalVisualizer.displayDestinationEffect(destLoc, projectile, portal);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        BukkitTask task = this.trackedProjectiles.remove(projectile);
        if (task != null) {
            task.cancel();
        }
    }

    public void cleanup() {
        this.trackedProjectiles.values().forEach(BukkitTask::cancel);
        this.trackedProjectiles.clear();
    }
}
