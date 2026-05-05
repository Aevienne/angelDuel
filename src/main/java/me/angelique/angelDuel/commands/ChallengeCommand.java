package me.angelique.angelDuel.commands;

import me.angelique.angelDuel.AngelDuel;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ChallengeCommand implements CommandExecutor {

    private final AngelDuel plugin;

    public ChallengeCommand(AngelDuel plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (!player.hasPermission("angelduel.challenge")) {
            player.sendMessage(plugin.getMsg("no-permission"));
            return true;
        }
        if (args.length < 1) {
            player.sendMessage(AngelDuel.colorize("&cUsage: /challenge <player>"));
            return true;
        }
        if (plugin.getDuelManager().isInDuel(player.getUniqueId())) {
            player.sendMessage(plugin.getMsg("already-dueling"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(plugin.getMsg("player-not-found"));
            return true;
        }
        if (target.equals(player)) {
            player.sendMessage(plugin.getMsg("cannot-self-challenge"));
            return true;
        }
        if (plugin.getDuelManager().isInDuel(target.getUniqueId())) {
            player.sendMessage(AngelDuel.colorize("&c" + target.getName() + " is already in a duel."));
            return true;
        }
        if (plugin.getDuelManager().hasPendingChallenge(target.getUniqueId())) {
            player.sendMessage(AngelDuel.colorize("&c" + target.getName() + " already has a pending challenge."));
            return true;
        }
        plugin.getDuelManager().sendChallenge(player, target);
        return true;
    }
}
