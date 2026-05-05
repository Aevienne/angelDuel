package me.angelique.angelDuel.managers;

import me.angelique.angelDuel.AngelDuel;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class LeaderboardManager {

    private final AngelDuel plugin;
    private final File lbFile;
    private FileConfiguration lbConfig;

    // In-memory map: UUID -> {wins, losses, name}
    private final Map<UUID, PlayerStats> stats = new HashMap<>();

    public LeaderboardManager(AngelDuel plugin) {
        this.plugin = plugin;
        this.lbFile = new File(plugin.getDataFolder(), "leaderboard.yml");
        if (!lbFile.exists()) plugin.saveResource("leaderboard.yml", false);
        lbConfig = YamlConfiguration.loadConfiguration(lbFile);
        load();
    }

    private void load() {
        if (!lbConfig.isConfigurationSection("leaderboard")) return;
        for (String key : lbConfig.getConfigurationSection("leaderboard").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String name = lbConfig.getString("leaderboard." + key + ".name", "Unknown");
                int wins = lbConfig.getInt("leaderboard." + key + ".wins", 0);
                int losses = lbConfig.getInt("leaderboard." + key + ".losses", 0);
                stats.put(uuid, new PlayerStats(name, wins, losses));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public void save() {
        for (Map.Entry<UUID, PlayerStats> entry : stats.entrySet()) {
            String path = "leaderboard." + entry.getKey();
            lbConfig.set(path + ".name", entry.getValue().name);
            lbConfig.set(path + ".wins", entry.getValue().wins);
            lbConfig.set(path + ".losses", entry.getValue().losses);
        }
        try {
            lbConfig.save(lbFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save leaderboard: " + e.getMessage());
        }
    }

    public void recordWin(UUID uuid, String name) {
        stats.computeIfAbsent(uuid, k -> new PlayerStats(name, 0, 0)).wins++;
        stats.get(uuid).name = name;
        save();
    }

    public void recordLoss(UUID uuid, String name) {
        stats.computeIfAbsent(uuid, k -> new PlayerStats(name, 0, 0)).losses++;
        stats.get(uuid).name = name;
        save();
    }

    public List<Map.Entry<UUID, PlayerStats>> getTopByWins(int limit) {
        return stats.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().wins, a.getValue().wins))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public PlayerStats getStats(UUID uuid) {
        return stats.getOrDefault(uuid, new PlayerStats("Unknown", 0, 0));
    }

    public static class PlayerStats {
        public String name;
        public int wins;
        public int losses;

        public PlayerStats(String name, int wins, int losses) {
            this.name = name;
            this.wins = wins;
            this.losses = losses;
        }
    }
}
