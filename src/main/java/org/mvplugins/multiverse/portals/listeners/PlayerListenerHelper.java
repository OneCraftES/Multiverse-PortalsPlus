package org.mvplugins.multiverse.portals.listeners;

import java.util.Date;

import com.dumptruckman.minecraft.util.Logging;
import org.mvplugins.multiverse.core.destination.DestinationInstance;
import org.mvplugins.multiverse.core.teleportation.AsyncSafetyTeleporter;
import org.mvplugins.multiverse.external.jakarta.inject.Inject;
import org.mvplugins.multiverse.external.jetbrains.annotations.NotNull;
import org.jvnet.hk2.annotations.Service;
import org.mvplugins.multiverse.portals.PortalPlayerSession;
import org.bukkit.Location;
import org.bukkit.entity.Player;

@Service
final class PlayerListenerHelper {

    private final AsyncSafetyTeleporter safetyTeleporter;
    private final org.mvplugins.multiverse.portals.utils.LeashManager leashManager;

    @Inject
    PlayerListenerHelper(@NotNull AsyncSafetyTeleporter safetyTeleporter,
            @NotNull org.mvplugins.multiverse.portals.utils.LeashManager leashManager) {
        this.safetyTeleporter = safetyTeleporter;
        this.leashManager = leashManager;
    }

    void stateSuccess(String playerName, String worldName) {
        Logging.fine(String.format(
                "MV-Portals is allowing Player '%s' to use the portal '%s'.",
                playerName, worldName));
    }

    void stateFailure(String playerName, String portalName) {
        Logging.fine(String.format(
                "MV-Portals is DENYING Player '%s' access to use the portal '%s'.",
                playerName, portalName));
    }

    void performTeleport(Player player, Location to, PortalPlayerSession ps, DestinationInstance<?, ?> destination,
            boolean checkSafety) {
        Location sourceLoc = player.getLocation();
        org.mvplugins.multiverse.portals.utils.LeashManager.EntityGraph graph = null;
        try {
            graph = this.leashManager.collectWholeChain(player, sourceLoc);
            if (graph != null) {
                this.leashManager.toggleLeashProtection(graph.entities, true);
            }
        } catch (Exception e) {
            Logging.warning("Failed to collect leash chain for player " + player.getName() + ": " + e.getMessage());
        }

        final org.mvplugins.multiverse.portals.utils.LeashManager.EntityGraph finalGraph = graph;

        safetyTeleporter.to(destination)
                .checkSafety(checkSafety && destination.checkTeleportSafety())
                .teleportSingle(player)
                .onSuccess(() -> {
                    try {
                        ps.playerDidTeleport(to);
                        ps.setTeleportTime(new Date());
                        this.stateSuccess(player.getDisplayName(), destination.toString());
                        // Teleport leashed entities (and mounts)
                        if (finalGraph != null) {
                            this.leashManager.teleportChain(finalGraph, player, sourceLoc, player.getLocation());
                        }
                    } finally {
                        if (finalGraph != null) {
                            this.leashManager.toggleLeashProtection(finalGraph.entities, false);
                        }
                    }
                })
                .onFailure(reason -> {
                    if (finalGraph != null) {
                        this.leashManager.toggleLeashProtection(finalGraph.entities, false);
                    }
                    Logging.fine(
                            "Failed to teleport player '%s' to destination '%s'. Reason: %s",
                            player.getDisplayName(), destination, reason);
                });
    }
}
