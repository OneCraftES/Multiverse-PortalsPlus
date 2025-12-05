package org.mvplugins.multiverse.portals.action;

import org.bukkit.entity.Entity;
import org.mvplugins.multiverse.core.utils.result.Attempt;
import org.mvplugins.multiverse.portals.MVPortal;

public abstract class ActionHandler<T extends ActionHandlerType<T, H>, H extends ActionHandler<T, H>> {

    private final T handlerType;

    protected ActionHandler(T handlerType) {
        this.handlerType = handlerType;
    }

    public T getHandlerType() {
        return handlerType;
    }

    public abstract Attempt<Void, ActionFailureReason> runAction(MVPortal portal, Entity entity);
}
