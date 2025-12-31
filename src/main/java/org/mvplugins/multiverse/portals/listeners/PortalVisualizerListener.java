package org.mvplugins.multiverse.portals.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jvnet.hk2.annotations.Service;
import org.mvplugins.multiverse.external.jakarta.inject.Inject;
import org.mvplugins.multiverse.portals.event.MVPortalCreatedEvent;
import org.mvplugins.multiverse.portals.event.MVPortalRemovedEvent;
import org.mvplugins.multiverse.portals.utils.PortalVisualizer;

@Service
public class PortalVisualizerListener implements Listener {

    private final PortalVisualizer portalVisualizer;

    @Inject
    public PortalVisualizerListener(PortalVisualizer portalVisualizer) {
        this.portalVisualizer = portalVisualizer;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPortalCreated(MVPortalCreatedEvent event) {
        this.portalVisualizer.startVisualizer(event.getPortal());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPortalRemoved(MVPortalRemovedEvent event) {
        this.portalVisualizer.stopVisualizer(event.getPortal());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPortalModified(org.mvplugins.multiverse.portals.event.MVPortalModifiedEvent event) {
        this.portalVisualizer.refreshVisualizer(event.getPortal());
    }
}
