package org.mvplugins.multiverse.portals.action.types;

import org.bukkit.command.CommandSender;
import org.jvnet.hk2.annotations.Service;
import org.mvplugins.multiverse.core.utils.result.Attempt;
import org.mvplugins.multiverse.external.jakarta.inject.Inject;
import org.mvplugins.multiverse.portals.action.ActionFailureReason;
import org.mvplugins.multiverse.portals.action.ActionHandlerType;

import java.util.Collection;

@Service
final class CommandActionHandlerType extends ActionHandlerType<CommandActionHandlerType, CommandActionHandler> {

    @Inject
    CommandActionHandlerType() {
        super("command");
    }

    @Override
    public Attempt<CommandActionHandler, ActionFailureReason> parseHandler(String action) {
        return Attempt.success(new CommandActionHandler(this, CommandRunner.fromString(action)));
    }

    @Override
    public Collection<String> suggestActions(CommandSender sender, String input) {
        //todo possibly use command map to suggest tab completion
        return super.suggestActions(sender, input);
    }
}
