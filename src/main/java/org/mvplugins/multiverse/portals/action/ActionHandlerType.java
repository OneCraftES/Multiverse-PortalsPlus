package org.mvplugins.multiverse.portals.action;

import org.bukkit.command.CommandSender;
import org.jvnet.hk2.annotations.Contract;
import org.mvplugins.multiverse.core.utils.result.Attempt;
import org.mvplugins.multiverse.external.jetbrains.annotations.NotNull;
import org.mvplugins.multiverse.external.vavr.control.Option;

import java.util.Collection;
import java.util.Collections;

@Contract
public abstract class ActionHandlerType<T extends ActionHandlerType<T, H>, H extends ActionHandler<T, H>> {

    private final String name;

    protected ActionHandlerType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract Attempt<H, ActionFailureReason> parseHandler(String action);

    public @NotNull Collection<String> suggestActions(CommandSender sender, String input) {
        return Collections.emptyList();
    }
}
