package com.griefprevention.commands;

import com.griefprevention.protection.temporary.ProtectionType;
import com.griefprevention.protection.temporary.TemporaryProtection;
import com.griefprevention.protection.temporary.TemporaryProtectionConfig;
import com.griefprevention.protection.temporary.TemporaryProtectionGUI;
import com.griefprevention.protection.temporary.TemporaryProtectionManager;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ClaimProtectionCommand extends CommandHandler {

    private final TemporaryProtectionGUI gui;

    public ClaimProtectionCommand(@NotNull GriefPrevention plugin, TemporaryProtectionGUI gui) {
        super(plugin, "claimprotection");
        this.gui = gui;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        TemporaryProtectionConfig config = TemporaryProtectionConfig.getInstance();

        if (!(sender instanceof Player player)) {
            if (config != null) {
                sender.sendMessage(config.getMessagePlayersOnly());
            }
            return true;
        }

        if (args.length == 0) {
            gui.openMainMenu(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "status" -> handleStatus(player);
            case "cancel" -> handleCancel(player, args);
            case "reload" -> handleReload(player);
            default -> {
                if (config != null) {
                    player.sendMessage(config.getMessageUnknownSubcommand());
                }
            }
        }

        return true;
    }

    private void handleStatus(Player player) {
        TemporaryProtectionManager manager = TemporaryProtectionManager.getInstance();
        TemporaryProtectionConfig config = TemporaryProtectionConfig.getInstance();

        if (manager == null || config == null) {
            if (config != null) {
                player.sendMessage(config.getMessageSystemUnavailable());
            }
            return;
        }

        Map<ProtectionType, TemporaryProtection> protections = manager.getProtections(player.getUniqueId());

        if (protections.isEmpty()) {
            player.sendMessage(config.getMessageNoActiveProtection());
            player.sendMessage(config.getMessageNoActiveProtectionHint());
            return;
        }

        player.sendMessage(config.getMessageStatusHeader());

        TemporaryProtection chestProtection = protections.get(ProtectionType.CHEST);
        if (chestProtection != null && chestProtection.isActive()) {
            player.sendMessage(config.getMessageProtectionActive()
                    .replace("{type}", ProtectionType.CHEST.getDisplayName())
                    .replace("{remaining}", chestProtection.getFormattedRemaining()));
        } else {
            player.sendMessage(config.getMessageChestInactive());
        }

        TemporaryProtection pvpProtection = protections.get(ProtectionType.PVP);
        if (pvpProtection != null && pvpProtection.isActive()) {
            player.sendMessage(config.getMessageProtectionActive()
                    .replace("{type}", ProtectionType.PVP.getDisplayName())
                    .replace("{remaining}", pvpProtection.getFormattedRemaining()));
        } else {
            player.sendMessage(config.getMessagePvpInactive());
        }

        TemporaryProtection mobProtection = protections.get(ProtectionType.MOB);
        if (mobProtection != null && mobProtection.isActive()) {
            player.sendMessage(config.getMessageProtectionActive()
                    .replace("{type}", ProtectionType.MOB.getDisplayName())
                    .replace("{remaining}", mobProtection.getFormattedRemaining()));
        } else {
            player.sendMessage(config.getMessageMobInactive());
        }

        long totalArea = manager.calculateTotalClaimArea(player.getUniqueId());
        player.sendMessage(config.getMessageProtectedArea()
                .replace("{area}", NumberFormat.getInstance().format(totalArea)));
    }

    private void handleCancel(Player player, String[] args) {
        TemporaryProtectionManager manager = TemporaryProtectionManager.getInstance();
        TemporaryProtectionConfig config = TemporaryProtectionConfig.getInstance();

        if (manager == null || config == null) {
            if (config != null) {
                player.sendMessage(config.getMessageSystemUnavailable());
            }
            return;
        }

        ProtectionType type = null;
        if (args.length >= 2) {
            type = ProtectionType.fromId(args[1]);
            if (type == null) {
                player.sendMessage(config.getMessageInvalidProtectionType());
                return;
            }
        }

        if (type != null) {
            TemporaryProtection protection = manager.getProtection(player.getUniqueId(), type);
            if (protection == null || !protection.isActive()) {
                player.sendMessage(config.getMessageNoProtectionToCancel()
                        .replace("{type}", type.getDisplayName()));
                return;
            }

            manager.removeProtection(player.getUniqueId(), type);
            player.sendMessage(config.getMessageProtectionCancelled()
                    .replace("{type}", type.getDisplayName()));
        } else {
            Map<ProtectionType, TemporaryProtection> protections = manager.getProtections(player.getUniqueId());
            if (protections.isEmpty()) {
                player.sendMessage(config.getMessageNoActiveProtectionToCancel());
                return;
            }

            for (ProtectionType t : ProtectionType.values()) {
                if (manager.hasProtection(player.getUniqueId(), t)) {
                    manager.removeProtection(player.getUniqueId(), t);
                }
            }
            player.sendMessage(config.getMessageAllProtectionsCancelled());
        }
    }

    private void handleReload(Player player) {
        TemporaryProtectionConfig config = TemporaryProtectionConfig.getInstance();

        if (!player.hasPermission("griefprevention.temporaryprotection.admin")) {
            if (config != null) {
                player.sendMessage(config.getMessageNoReloadPermission());
            }
            return;
        }

        TemporaryProtectionManager manager = TemporaryProtectionManager.getInstance();

        if (config != null) config.reload();
        if (manager != null) manager.reload();

        if (config != null) {
            player.sendMessage(config.getMessageConfigReloaded());
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>(Arrays.asList("status", "cancel"));
            if (sender.hasPermission("griefprevention.temporaryprotection.admin")) {
                completions.add("reload");
            }
            String input = args[0].toLowerCase();
            completions.removeIf(s -> !s.startsWith(input));
            return completions;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("cancel")) {
            List<String> completions = new ArrayList<>(Arrays.asList("chest", "pvp", "mob"));
            String input = args[1].toLowerCase();
            completions.removeIf(s -> !s.startsWith(input));
            return completions;
        }

        return List.of();
    }
}
