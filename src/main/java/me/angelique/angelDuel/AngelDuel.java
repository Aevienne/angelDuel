package me.angelique.angelDuel;

import me.angelique.angelDuel.commands.ChallengeCommand;
import me.angelique.angelDuel.commands.DuelAcceptCommand;
import me.angelique.angelDuel.commands.DuelDeclineCommand;
import me.angelique.angelDuel.commands.DuelLeaderboardCommand;
import me.angelique.angelDuel.gui.DuelGui;
import me.angelique.angelDuel.gui.DuelListener;
import me.angelique.angelDuel.listeners.DuelCombatListener;
import me.angelique.angelDuel.listeners.DuelZoneListener;
import me.angelique.angelDuel.managers.DuelManager;
import me.angelique.angelDuel.managers.LeaderboardManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class AngelDuel extends JavaPlugin {

    private static AngelDuel instance;
    private DuelManager duelManager;
    private LeaderboardManager leaderboardManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        leaderboardManager = new LeaderboardManager(this);
        duelManager = new DuelManager(this);

        registerCommands();
        registerListeners();

        getLogger().info("AngelDuel enabled. Duelist's Code is in effect.");
    }

    @Override
    public void onDisable() {
        if (duelManager != null) duelManager.forceEndAllDuels();
        if (leaderboardManager != null) leaderboardManager.save();
        getLogger().info("AngelDuel disabled.");
    }

    private void registerCommands() {
        getCommand("challenge").setExecutor(new ChallengeCommand(this));
        getCommand("duelaccept").setExecutor(new DuelAcceptCommand(this));
        getCommand("dueldecline").setExecutor(new DuelDeclineCommand(this));
        getCommand("duelleaderboard").setExecutor(new DuelLeaderboardCommand(this));
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new DuelCombatListener(this), this);
        getServer().getPluginManager().registerEvents(new DuelZoneListener(this), this);
        getServer().getPluginManager().registerEvents(new DuelListener(this), this);
    }

    public static AngelDuel getInstance() {
        return instance;
    }

    public DuelManager getDuelManager() {
        return duelManager;
    }

    public LeaderboardManager getLeaderboardManager() {
        return leaderboardManager;
    }

    public String getMsg(String key) {
        String prefix = colorize(getConfig().getString("messages.prefix", "&8[&6⚔ AngelDuel&8] &r"));
        String msg = getConfig().getString("messages." + key, "&cMissing message: " + key);
        return prefix + colorize(msg);
    }

    public String getMsgRaw(String key) {
        return colorize(getConfig().getString("messages." + key, "&cMissing message: " + key));
    }

    public static String colorize(String s) {
        return s == null ? "" : s.replace('&', '§');
    }
}
