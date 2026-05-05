package me.angelique.angelDuel.commands;

import me.angelique.angelDuel.AngelDuel;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class DuelAcceptCommand implements CommandExecutor {

    private final AngelDuel plugin;

    public DuelAcceptCommand(AngelDuel plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (plugin.getDuelManager().isInDuel(player.getUniqueId())) {
            player.sendMessage(plugin.getMsg("already-dueling"));
            return true;
        }
        plugin.getDuelManager().acceptChallenge(player);
        return true;
    }
}
