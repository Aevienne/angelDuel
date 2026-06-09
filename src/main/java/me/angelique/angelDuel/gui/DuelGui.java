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

import java.util.*;

public final class DuelGui {

    public static final String TITLE = TextUtil.color("&cHonor Arena");
    public static final String CHALLENGE_TITLE = TextUtil.color("&aSelect Opponent");
    static final int SIZE = 45;

    private DuelGui() {}

    public static void open(Player player, AngelDuel plugin) {
        DuelManager dm = plugin.getDuelManager();
        LeaderboardManager lb = plugin.getLeaderboardManager();
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE);
        fillBorder(inv);

        inv.setItem(4, item(Material.DIAMOND_SWORD, "&cDuels & Honor",
                "&7Fight for glory and honor tokens",
                "&7Winner receives a trophy item"));

        // Active duels
        Set<DuelSession> seen = new HashSet<>();
        int activeCount = 0;
        for (DuelSession s : dm.getActiveDuels().values()) {
            if (seen.add(s)) activeCount++;
        }
        inv.setItem(19, item(Material.IRON_SWORD, "&cActive Duels: &f" + activeCount,
                "&7Players currently fighting"));

        // Pending challenge (for this player)
        DuelSession myPending = dm.getPendingChallenge(player.getUniqueId());
        if (myPending != null) {
            OfflinePlayer challenger = Bukkit.getOfflinePlayer(myPending.getChallenger());
            inv.setItem(21, item(Material.CLOCK, "&eChallenge from &f" + (challenger.getName() != null ? challenger.getName() : "Unknown"),
                    "&7They want to duel you!",
                    "&7Expires in ~30 seconds",
                    "",
                    "&aLeft-click to ACCEPT",
                    "&cRight-click to DECLINE"));
        } else {
            inv.setItem(21, item(Material.CLOCK, "&eNo pending challenges",
                    "&7Challenges you've sent or received"));
        }

        // Pending sent
        long sent = dm.getActiveDuels().values().stream().filter(s -> s.getChallenger().equals(player.getUniqueId()) && s.getState() == DuelState.PENDING).count();
        if (sent > 0) {
            inv.setItem(22, item(Material.CLOCK, "&eSent: &f" + sent + " pending",
                    "&7Waiting for opponent to accept"));
        }

        // Challenge a player
        inv.setItem(23, item(Material.DIAMOND_SWORD, "&aChallenge a Player",
                "&7Select an online opponent",
                "&7to send a duel challenge",
                "",
                "&eClick to open player list"));

        // My stats
        LeaderboardManager.PlayerStats stats = lb.getStats(player.getUniqueId());
        int totalGames = stats.wins + stats.losses;
        inv.setItem(25, item(Material.PLAYER_HEAD, "&6Your Stats",
                "&aWins: &f" + stats.wins,
                "&cLosses: &f" + stats.losses,
                "&eWin Rate: &f" + (totalGames > 0 ? String.format("%.0f%%", 100.0 * stats.wins / totalGames) : "N/A")));

        // Leaderboard
        List<Map.Entry<UUID, LeaderboardManager.PlayerStats>> top = lb.getTopByWins(5);
        List<String> topLines = new ArrayList<>();
        for (Map.Entry<UUID, LeaderboardManager.PlayerStats> e : top) {
            LeaderboardManager.PlayerStats s = e.getValue();
            topLines.add("&7" + s.name + " &8- &f" + s.wins + "W / " + s.losses + "L");
        }
        if (topLines.isEmpty()) topLines.add("&7No duelists yet");
        inv.setItem(31, item(Material.GOLD_INGOT, "&6Top Duelists", topLines.toArray(new String[0])));

        inv.setItem(40, item(Material.KNOWLEDGE_BOOK, "&eHow Duels Work",
                "&7Challenge a player from the menu",
                "&7Accept or decline challenges here",
                "&7Win to earn &6Honor Tokens",
                "&7Climb the leaderboard for prestige"));

        player.openInventory(inv);
    }

    public static void openChallenge(Player player, AngelDuel plugin) {
        Inventory inv = Bukkit.createInventory(null, 54, CHALLENGE_TITLE);
        fillBorder54(inv);

        inv.setItem(4, item(Material.DIAMOND_SWORD, "&aSelect an opponent",
                "&7Choose an online player to challenge"));

        int slot = 10;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getUniqueId().equals(player.getUniqueId())) continue;
            if (slot > 43) break;
            inv.setItem(slot++, item(Material.PLAYER_HEAD, "&f" + online.getName(),
                    "&7Send a duel challenge",
                    "",
                    "&eClick to challenge"));
        }
        if (slot == 10) {
            inv.setItem(22, item(Material.BARRIER, "&cNo other players online"));
        }

        inv.setItem(49, item(Material.OAK_DOOR, "&cBack", "&7Return to duel menu"));
        player.openInventory(inv);
    }

    static void fillBorder(Inventory inv) {
        ItemStack glass = pane(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, glass);
    }

    static void fillBorder54(Inventory inv) {
        ItemStack glass = pane(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < 54; i++) inv.setItem(i, glass);
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
