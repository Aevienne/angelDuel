package me.angelique.angelDuel.commands;

import me.angelique.angelDuel.AngelDuel;
import me.angelique.angelDuel.managers.LeaderboardManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DuelLeaderboardCommand implements CommandExecutor {

    private final AngelDuel plugin;

    public DuelLeaderboardCommand(AngelDuel plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("angelduel.leaderboard")) {
            sender.sendMessage(plugin.getMsg("no-permission"));
            return true;
        }
        List<Map.Entry<UUID, LeaderboardManager.PlayerStats>> top =
                plugin.getLeaderboardManager().getTopByWins(10);

        sender.sendMessage(AngelDuel.colorize("&8&m----&r &6&l⚔ Duelist Leaderboard &r&8&m----"));
        if (top.isEmpty()) {
            sender.sendMessage(AngelDuel.colorize("&7No duels have been recorded yet."));
        } else {
            int rank = 1;
            for (Map.Entry<UUID, LeaderboardManager.PlayerStats> entry : top) {
                LeaderboardManager.PlayerStats s = entry.getValue();
                String color = rank == 1 ? "&6" : rank == 2 ? "&f" : rank == 3 ? "&e" : "&7";
                sender.sendMessage(AngelDuel.colorize(
                        color + "#" + rank + " &f" + s.name +
                        " &8| &aW: &f" + s.wins + " &8| &cL: &f" + s.losses
                ));
                rank++;
            }
        }
        sender.sendMessage(AngelDuel.colorize("&8&m--------------------------"));
        return true;
    }
}
