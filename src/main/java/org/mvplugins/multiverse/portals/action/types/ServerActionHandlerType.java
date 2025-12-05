package org.mvplugins.multiverse.portals.action.types;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jvnet.hk2.annotations.Service;
import org.mvplugins.multiverse.core.utils.result.Attempt;
import org.mvplugins.multiverse.external.jakarta.inject.Inject;
import org.mvplugins.multiverse.portals.MultiversePortals;
import org.mvplugins.multiverse.portals.action.ActionFailureReason;
import org.mvplugins.multiverse.portals.action.ActionHandlerType;
import org.mvplugins.multiverse.portals.utils.BungeeServerList;

import java.util.Collection;

@Service
final class ServerActionHandlerType extends ActionHandlerType<ServerActionHandlerType, ServerActionHandler> {

    private final MultiversePortals plugin;
    private final BungeeServerList bungeeServerList;

    @Inject
    ServerActionHandlerType(@NotNull MultiversePortals plugin, @NotNull BungeeServerList bungeeServerList) {
        super("server");
        this.plugin = plugin;
        this.bungeeServerList = bungeeServerList;
    }

    @Override
    public Attempt<ServerActionHandler, ActionFailureReason> parseHandler(String action) {
        return Attempt.success(new ServerActionHandler(this, plugin, action));
    }

    @Override
    public Collection<String> suggestActions(CommandSender sender, String input) {
        return bungeeServerList.getServerNames();
    }
}
