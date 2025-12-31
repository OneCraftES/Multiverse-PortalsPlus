package org.mvplugins.multiverse.portals.listeners;

import com.dumptruckman.minecraft.util.Logging;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.jvnet.hk2.annotations.Service;
import org.mvplugins.multiverse.external.jakarta.inject.Inject;
import org.mvplugins.multiverse.portals.MVPortal;
import org.mvplugins.multiverse.portals.MultiversePortals;
import org.mvplugins.multiverse.portals.utils.EntityPortalManager;
import org.mvplugins.multiverse.portals.utils.PortalManager;
import org.mvplugins.multiverse.portals.utils.PortalVisualizer;

@Service
public class MVPEntityListener implements Listener {

    private final MultiversePortals plugin;
    private final EntityPortalManager entityPortalManager;
    private final PortalManager portalManager;
    private final PortalVisualizer portalVisualizer;

    @Inject
    public MVPEntityListener(
            MultiversePortals plugin,
            EntityPortalManager entityPortalManager,
            PortalManager portalManager,
            PortalVisualizer portalVisualizer) {
        this.plugin = plugin;
        this.entityPortalManager = entityPortalManager;
        this.portalManager = portalManager;
        this.portalVisualizer = portalVisualizer;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityPortal(EntityPortalEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Entity entity = event.getEntity();
        if (entity instanceof Player) {
            return; // Handled by player listeners
        }

        Location location = event.getFrom();
        MVPortal portal = this.portalManager.getPortal(location);

        if (portal == null) {
            return;
        }

        if (!portal.getTeleportNonPlayers()) {
            return;
        }

        // Check if entity is allowed in portal settings (future feature)
        // if (!portal.getEntityPortalSettings().isEntityAllowed(entity.getType()))
        // return;

        if (this.entityPortalManager.canUsePortal(entity)) {
            event.setCancelled(true);

            Logging.finer("Entity %s using portal: %s", entity.getType(), portal.getName());

            if (event.getFrom().getWorld() != event.getTo().getWorld()
                    || event.getFrom().distance(event.getTo()) > 0.0) {
                this.portalVisualizer.displayReactiveEffect(entity, portal);

                // Destination location is needed for effect, simpler to just dispatch teleport
                // and handle effect if needed
                // But EntityPortalManager abstracts destination lookup.
                // We'll trust EntityPortalManager to do the teleport.
                // For destination effect, we'd need to know where it went, or calculate it
                // ourselves.
                // MV4 calculated destLoc to show effect.

                var destInstance = portal.getDestination();
                Location destLoc = destInstance != null ? destInstance.getLocation(entity).getOrNull() : null;

                boolean success = this.entityPortalManager.teleportEntity(entity, portal);

                if (success && destLoc != null) {
                    this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
                        this.portalVisualizer.displayDestinationEffect(destLoc, entity, portal);
                    });
                }
            } else {
                this.entityPortalManager.teleportEntity(entity, portal);
            }
        }
    }
}
