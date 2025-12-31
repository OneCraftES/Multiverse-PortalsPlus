package org.mvplugins.multiverse.portals.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;
import org.jvnet.hk2.annotations.Service;
import org.mvplugins.multiverse.external.jakarta.inject.Inject;
import org.mvplugins.multiverse.portals.MVPortal;
import org.mvplugins.multiverse.portals.MultiversePortals;
import org.mvplugins.multiverse.portals.utils.EntityPortalManager;
import org.mvplugins.multiverse.portals.utils.PortalManager;
import org.mvplugins.multiverse.portals.utils.PortalVisualizer;

@Service
public class MVPEntityMoveListener implements Listener {

    private final MultiversePortals plugin;
    private final EntityPortalManager entityPortalManager;
    private final PortalManager portalManager;
    private final PortalVisualizer portalVisualizer;
    private BukkitTask checkTask;

    @Inject
    public MVPEntityMoveListener(
            MultiversePortals plugin,
            EntityPortalManager entityPortalManager,
            PortalManager portalManager,
            PortalVisualizer portalVisualizer) {
        this.plugin = plugin;
        this.entityPortalManager = entityPortalManager;
        this.portalManager = portalManager;
        this.portalVisualizer = portalVisualizer;

        startTask();
    }

    private void startTask() {
        this.checkTask = this.plugin.getServer().getScheduler().runTaskTimer(this.plugin, () -> {
            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (entity instanceof Player || !entity.isValid()) {
                        continue;
                    }

                    Location loc = entity.getLocation();
                    MVPortal portal = this.portalManager.getPortal(loc);

                    if (portal == null) {
                        continue;
                    }

                    if (!portal.getTeleportNonPlayers()) {
                        continue;
                    }

                    if (this.entityPortalManager.canUsePortal(entity)) {
                        // Visuals
                        this.portalVisualizer.displayReactiveEffect(entity, portal);

                        var destInstance = portal.getDestination();
                        Location destLoc = destInstance != null ? destInstance.getLocation(entity).getOrNull() : null;

                        boolean success = this.entityPortalManager.teleportEntity(entity, portal);

                        if (success && destLoc != null) {
                            // Destination effect
                            this.portalVisualizer.displayDestinationEffect(destLoc, entity, portal);
                        }
                    }
                }
            }
        }, 20L, 10L);
    }

    public void stopTask() {
        if (this.checkTask != null && !this.checkTask.isCancelled()) {
            this.checkTask.cancel();
        }
    }
}
