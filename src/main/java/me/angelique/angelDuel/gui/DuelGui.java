package me.angelique.angelDuel.gui;

import me.angelique.angelDuel.AngelDuel;
import me.angelique.angelDuel.managers.DuelManager;
import me.angelique.angelDuel.managers.LeaderboardManager;
import me.angelique.angelDuel.models.DuelSession;
import me.angelique.angelDuel.models.DuelState;
import me.angelique.angelNCore.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public final class DuelGui {

    public static final String TITLE = TextUtil.color("&8Duels &7\u2014 &cHonor Arena");
    static final int SIZE = 45;

    private DuelGui() {}

    public static void open(Player player, AngelDuel plugin) {
        DuelManager dm = plugin.getDuelManager();
        LeaderboardManager lb = plugin.getLeaderboardManager();
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE);
        fillBorder(inv);

        // Header
        inv.setItem(4, item(Material.DIAMOND_SWORD, "&cDuels & Honor",
                "&7Fight for glory and honor tokens",
                "&7Winner receives a trophy item"));

        // Active duels display
        Set<DuelSession> seen = new HashSet<>();
        int activeCount = 0;
        for (DuelSession s : dm.getActiveDuels().values()) {
            if (seen.add(s)) activeCount++;
        }
        inv.setItem(19, item(Material.IRON_SWORD, "&cActive Duels: &f" + activeCount,
                "&7Players currently fighting"));

        // Pending challenges
        int pending = dm.getPendingChallengeCount();
        inv.setItem(21, item(Material.CLOCK, "&ePending Challenges: &f" + pending,
                "&7Challenges waiting for response",
                "",
                "&eClick to view & manage"));

        // Challenge a player
        inv.setItem(23, item(Material.DIAMOND_SWORD, "&aChallenge a Player",
                "&7Send a duel challenge",
                "&7to an online player",
                "",
                "&eClick to select opponent"));

        // My stats
        LeaderboardManager.PlayerStats stats = lb.getStats(player.getUniqueId());
        int totalGames = stats.wins + stats.losses;
        inv.setItem(25, item(Material.PLAYER_HEAD, "&6Your Stats",
                "&aWins: &f" + stats.wins,
                "&cLosses: &f" + stats.losses,
                "&eWin Rate: &f" + (totalGames > 0 ? String.format("%.0f%%", 100.0 * stats.wins / totalGames) : "N/A")));

        // Leaderboard preview
        List<Map.Entry<UUID, LeaderboardManager.PlayerStats>> top = lb.getTopByWins(5);
        List<String> topLines = new ArrayList<>();
        for (Map.Entry<UUID, LeaderboardManager.PlayerStats> e : top) {
            LeaderboardManager.PlayerStats s = e.getValue();
            topLines.add("&7" + s.name + " &8- &f" + s.wins + "W / " + s.losses + "L");
        }
        inv.setItem(31, item(Material.GOLD_INGOT, "&6Top Duelists",
                topLines.toArray(new String[0])));

        // How to play
        inv.setItem(40, item(Material.KNOWLEDGE_BOOK, "&eHow Duels Work",
                "&7/challenge <player> to start",
                "&7Press &f/duelaccept &7to accept",
                "&7Win to earn &6Honor Tokens",
                "&7Climb the leaderboard for prestige"));

        player.openInventory(inv);
    }

    static void fillBorder(Inventory inv) {
        ItemStack glass = pane(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < SIZE; i++) inv.setItem(i, glass);
    }

    static ItemStack item(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(TextUtil.color(name));
            meta.setLore(Arrays.stream(lore).map(TextUtil::color).toList());
            item.setItemMeta(meta);
        }
        return item;
    }

    static ItemStack pane(Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.setDisplayName(" "); item.setItemMeta(meta); }
        return item;
    }
}
