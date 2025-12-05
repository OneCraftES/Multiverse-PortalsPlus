package org.mvplugins.multiverse.portals.utils;

import org.mvplugins.multiverse.core.economy.MVEconomist;
import org.mvplugins.multiverse.core.world.MultiverseWorld;
import org.mvplugins.multiverse.core.world.WorldManager;
import org.mvplugins.multiverse.external.jakarta.inject.Inject;
import org.mvplugins.multiverse.external.jetbrains.annotations.NotNull;
import org.jvnet.hk2.annotations.Service;
import org.mvplugins.multiverse.portals.MVPortal;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

@Service
public class DisplayUtils {

    private final WorldManager worldManager;
    private final MVEconomist economist;

    @Inject
    DisplayUtils(
            @NotNull WorldManager worldManager,
            @NotNull MVEconomist economist) {
        this.worldManager = worldManager;
        this.economist = economist;
    }

    public void showStaticInfo(CommandSender sender, MVPortal portal, String message) {
        sender.sendMessage(ChatColor.AQUA + "--- " + message + ChatColor.DARK_AQUA + portal.getName() + ChatColor.AQUA + " ---");
        String[] locParts = portal.getPortalLocation().toString().split(":");
        sender.sendMessage("Coords: " + ChatColor.GOLD + locParts[1] + ChatColor.WHITE + " to " + ChatColor.GOLD + locParts[2] + ChatColor.WHITE + " in " + ChatColor.GOLD + portal.getWorld().getName() );
        sender.sendMessage("Action Type: " + ChatColor.GOLD + portal.getActionType());
        if (portal.getAction().isEmpty()) {
            sender.sendMessage("Action: " + ChatColor.RED + ChatColor.ITALIC + "NOT SET!");
            return;
        }
        if (portal.getActionType().equalsIgnoreCase("multiverse-destination")) {
            sender.sendMessage("Action: " + ChatColor.GOLD + formatActionAsMVDestination(portal));
        } else {
            sender.sendMessage("Action: " + ChatColor.GOLD + portal.getAction());
        }
        sender.sendMessage("Check Destination Safety: " + formatBoolean(portal.getCheckDestinationSafety()));
        sender.sendMessage("Teleport Non Players: " + formatBoolean(portal.getTeleportNonPlayers()));
    }

    private String formatBoolean(Boolean bool) {
        return bool ? ChatColor.GREEN + "true" : ChatColor.RED + "false";
    }

    public String formatActionAsMVDestination(MVPortal portal) {
        String[] split = portal.getAction().split(":", 2);
        String destination = split.length == 2 ? split[1] : "";
        String destType = split.length == 2 ? split[0] : "";
        if (destType.equals("w")) {
            MultiverseWorld destWorld = worldManager.getWorld(destination).getOrNull();
            if (destWorld != null) {
                return "(World) " + ChatColor.DARK_AQUA + destination;
            }
        }
        if (destType.equals("p")) {
            // todo: I think should use instance check instead of destType prefix
            // String targetWorldName = portalManager.getPortal(portal.getDestination().getName()).getWorld().getName();
            // destination = "(Portal) " + ChatColor.DARK_AQUA + portal.getDestination().getName() + ChatColor.GRAY + " (" + targetWorldName + ")";
        }
        if (destType.equals("e")) {
            String destinationWorld = portal.getAction().split(":")[1];
            String destPart = portal.getAction().split(":")[2];
            String[] targetParts = destPart.split(",");
            int x, y, z;
            try {
                x = (int) Double.parseDouble(targetParts[0]);
                y = (int) Double.parseDouble(targetParts[1]);
                z = (int) Double.parseDouble(targetParts[2]);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                return ChatColor.RED + "Invalid Exact World Location!";
            }
            return "(Location) " + ChatColor.DARK_AQUA + destinationWorld + ", " + x + ", " + y + ", " + z;
        }
        if (destType.equals("i")) {
            return ChatColor.RED + "Invalid Destination!";
        }
        return ChatColor.DARK_AQUA + portal.getAction();
    }

    public void showPortalPriceInfo(MVPortal portal, CommandSender sender) {
        if (portal.getPrice() > 0D) {
            sender.sendMessage("Price: " + ChatColor.GREEN + economist.formatPrice(portal.getPrice(), portal.getCurrency()));
        } else if (portal.getPrice() < 0D) {
            sender.sendMessage("Price: " + ChatColor.GREEN + economist.formatPrice(-portal.getPrice(), portal.getCurrency()));
        } else {
            sender.sendMessage("Price: " + ChatColor.GREEN + "FREE!");
        }
    }
}
