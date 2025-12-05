package org.mvplugins.multiverse.portals.action.types;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.mvplugins.multiverse.core.utils.REPatterns;

abstract class CommandRunner {

    static CommandRunner fromString(String command) {
        String[] split = REPatterns.COLON.split(command, 2);
        return switch ((split.length == 2) ? split[0] : "") {
            case "op" -> new Op(split[1]);
            case "console" -> new Console(split[1]);
            default -> new Self(command);
        };
    }

    private final String cmdStr;

    CommandRunner(String cmdStr) {
        this.cmdStr = cmdStr;
    }

    void runCommand(CommandSender sender) {
        runCommand(sender, parseCmdStr(sender));
    }

    private String parseCmdStr(CommandSender sender) {
        String parsedCmd = cmdStr;
        if (sender instanceof Entity entity) {
            parsedCmd = parsedCmd.replace("%world%", entity.getWorld().getName());
        }
        if (sender instanceof OfflinePlayer player) {
            parsedCmd = parsedCmd.replace("%player%", String.valueOf(player.getName()));
            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                parsedCmd = PlaceholderAPI.setPlaceholders(player, parsedCmd);
            }
        }
        return parsedCmd;
    }

    protected abstract void runCommand(CommandSender sender, String cmd);

    private static class Self extends CommandRunner {

        private Self(String cmdStr) {
            super(cmdStr);
        }

        @Override
        protected void runCommand(CommandSender sender, String cmd) {
            Bukkit.dispatchCommand(sender, cmd);
        }
    }

    private static class Op extends CommandRunner {

        private Op(String cmdStr) {
            super(cmdStr);
        }

        @Override
        protected void runCommand(CommandSender sender, String cmd) {
            boolean shouldRemoveOp = false;
            if (!sender.isOp()) {
                sender.setOp(true);
                shouldRemoveOp = true;
            }
            Bukkit.dispatchCommand(sender, cmd);
            if (shouldRemoveOp) {
                sender.setOp(false);
            }
        }
    }

    private static class Console extends CommandRunner {

        private Console(String cmdStr) {
            super(cmdStr);
        }

        @Override
        protected void runCommand(CommandSender sender, String cmd) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }
    }
}
