package org.mvplugins.multiverse.portals.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.mvplugins.multiverse.portals.MVPortal;

public class MVPortalRemovedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final MVPortal portal;

    public MVPortalRemovedEvent(MVPortal portal) {
        this.portal = portal;
    }

    public MVPortal getPortal() {
        return portal;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
