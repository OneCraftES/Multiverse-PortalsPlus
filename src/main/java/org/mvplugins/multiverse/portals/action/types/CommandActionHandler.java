package org.mvplugins.multiverse.portals.action.types;

import org.bukkit.entity.Entity;
import org.mvplugins.multiverse.core.utils.result.Attempt;
import org.mvplugins.multiverse.portals.MVPortal;
import org.mvplugins.multiverse.portals.action.ActionFailureReason;
import org.mvplugins.multiverse.portals.action.ActionHandler;

final class CommandActionHandler extends ActionHandler<CommandActionHandlerType, CommandActionHandler> {

    private final CommandRunner commandRunner;

    CommandActionHandler(CommandActionHandlerType handlerType, CommandRunner commandRunner) {
        super(handlerType);
        this.commandRunner = commandRunner;
    }

    @Override
    public Attempt<Void, ActionFailureReason> runAction(MVPortal portal, Entity entity) {
        commandRunner.runCommand(entity);
        return Attempt.success(null);
    }
}
