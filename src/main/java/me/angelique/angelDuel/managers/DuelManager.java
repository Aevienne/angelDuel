package me.angelique.angelDuel.managers;

import me.angelique.angelDuel.AngelDuel;
import me.angelique.angelDuel.models.DuelSession;
import me.angelique.angelDuel.models.DuelState;
import me.angelique.angelNCore.events.DuelCompletedEvent;
import me.angelique.angelNCore.events.EventBus;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.*;

public class DuelManager {

    private final AngelDuel plugin;
    private final Map<UUID, DuelSession> activeDuels = new HashMap<>();
    private final Map<UUID, DuelSession> pendingChallenges = new HashMap<>();
    private final NamespacedKey honorTokenKey;

    private static final String TOKEN_PDC_VALUE = "honor_token";

    public DuelManager(AngelDuel plugin) {
        this.plugin = plugin;
        this.honorTokenKey = new NamespacedKey(plugin, "honor_token");
    }

    // -------------------------------------------------------------------------
    // Challenge lifecycle
    // -------------------------------------------------------------------------

    public boolean hasPendingChallenge(UUID uuid) {
        return pendingChallenges.containsKey(uuid);
    }

    public boolean isInDuel(UUID uuid) {
        return activeDuels.containsKey(uuid);
    }

    public DuelSession getSession(UUID uuid) {
        return activeDuels.get(uuid);
    }

    public DuelSession getPendingChallenge(UUID uuid) {
        return pendingChallenges.get(uuid);
    }

    public void sendChallenge(Player challenger, Player challenged) {
        DuelSession session = new DuelSession(challenger.getUniqueId(), challenged.getUniqueId());

        int timeoutTicks = plugin.getConfig().getInt("duel.challenge-timeout", 30) * 20;
        int expiryTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (pendingChallenges.get(challenged.getUniqueId()) == session) {
                pendingChallenges.remove(challenged.getUniqueId());
                challenger.sendMessage(plugin.getMsg("challenge-expired"));
                challenged.sendMessage(plugin.getMsg("challenge-expired"));
            }
        }, timeoutTicks).getTaskId();

        session.setExpiryTask(expiryTask);
        pendingChallenges.put(challenged.getUniqueId(), session);

        int timeout = plugin.getConfig().getInt("duel.challenge-timeout", 30);
        String msgSent = plugin.getMsg("challenge-sent")
                .replace("%player%", challenged.getName())
                .replace("%timeout%", String.valueOf(timeout));
        String msgRecv = plugin.getMsg("challenge-received")
                .replace("%challenger%", challenger.getName());

        challenger.sendMessage(msgSent);
        challenged.sendMessage(msgRecv);
    }

    public void acceptChallenge(Player challenged) {
        DuelSession session = pendingChallenges.remove(challenged.getUniqueId());
        if (session == null) {
            challenged.sendMessage(plugin.getMsg("no-challenge"));
            return;
        }

        Bukkit.getScheduler().cancelTask(session.getExpiryTask());

        Player challenger = Bukkit.getPlayer(session.getChallenger());
        if (challenger == null || !challenger.isOnline()) {
            challenged.sendMessage(plugin.getMsg("player-not-found"));
            return;
        }

        activeDuels.put(challenger.getUniqueId(), session);
        activeDuels.put(challenged.getUniqueId(), session);
        session.setState(DuelState.COUNTDOWN);

        // Save origins
        session.setChallengerOrigin(challenger.getLocation().clone());
        session.setChallengedOrigin(challenged.getLocation().clone());

        // Compute zone center midpoint
        Location mid = challenger.getLocation().clone().add(challenged.getLocation()).multiply(0.5);
        mid.setY(challenger.getLocation().getY());
        session.setZoneCenter(mid);

        // Save states if configured
        if (plugin.getConfig().getBoolean("duel.restore-state", true)) {
            session.saveChallengerState(challenger);
            session.saveChallengedState(challenged);
        }

        int countdownSeconds = plugin.getConfig().getInt("duel.countdown", 5);
        String acceptMsg = plugin.getMsg("duel-accepted")
                .replace("%countdown%", String.valueOf(countdownSeconds));
        challenger.sendMessage(acceptMsg);
        challenged.sendMessage(acceptMsg);

        startCountdown(session, challenger, challenged, countdownSeconds);
    }

    public void declineChallenge(Player challenged) {
        DuelSession session = pendingChallenges.remove(challenged.getUniqueId());
        if (session == null) {
            challenged.sendMessage(plugin.getMsg("no-challenge"));
            return;
        }
        Bukkit.getScheduler().cancelTask(session.getExpiryTask());
        Player challenger = Bukkit.getPlayer(session.getChallenger());
        if (challenger != null) {
            challenger.sendMessage(AngelDuel.colorize("&c" + challenged.getName() + " declined your duel challenge."));
        }
        challenged.sendMessage(AngelDuel.colorize("&cYou declined the duel challenge."));
    }

    // -------------------------------------------------------------------------
    // Countdown + start
    // -------------------------------------------------------------------------

    private void startCountdown(DuelSession session, Player challenger, Player challenged, int seconds) {
        final int[] remaining = {seconds};
        int taskId = new BukkitRunnable() {
            @Override
            public void run() {
                if (remaining[0] <= 0) {
                    cancel();
                    startDuel(session);
                    return;
                }
                Player c1 = Bukkit.getPlayer(session.getChallenger());
                Player c2 = Bukkit.getPlayer(session.getChallenged());
                String countMsg = AngelDuel.colorize("&eDuel starts in &c" + remaining[0] + "&e...");
                if (c1 != null) c1.sendMessage(countMsg);
                if (c2 != null) c2.sendMessage(countMsg);
                remaining[0]--;
            }
        }.runTaskTimer(plugin, 0L, 20L).getTaskId();
        session.setCountdownTask(taskId);
    }

    private void startDuel(DuelSession session) {
        session.setState(DuelState.ACTIVE);
        Player c1 = Bukkit.getPlayer(session.getChallenger());
        Player c2 = Bukkit.getPlayer(session.getChallenged());

        Component startTitle = Component.text("⚔ DUEL START ⚔").color(net.kyori.adventure.text.format.NamedTextColor.RED);
        Component startSub = Component.text("Fight with honor!").color(net.kyori.adventure.text.format.NamedTextColor.GOLD);
        Title title = Title.title(startTitle, startSub, Title.Times.times(
                Duration.ofMillis(200), Duration.ofMillis(1500), Duration.ofMillis(500)));

        if (c1 != null) {
            c1.showTitle(title);
            c1.sendMessage(plugin.getMsg("duel-start"));
            c1.playSound(c1.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1f);
        }
        if (c2 != null) {
            c2.showTitle(title);
            c2.sendMessage(plugin.getMsg("duel-start"));
            c2.playSound(c2.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1f);
        }
    }

    // -------------------------------------------------------------------------
    // End duel
    // -------------------------------------------------------------------------

    public void endDuel(DuelSession session, UUID winnerUUID, UUID loserUUID, boolean fled) {
        if (session.getState() == DuelState.ENDED) return;
        session.setState(DuelState.ENDED);

        Bukkit.getScheduler().cancelTask(session.getCountdownTask());

        activeDuels.remove(winnerUUID);
        activeDuels.remove(loserUUID);

        Player winner = Bukkit.getPlayer(winnerUUID);
        Player loser = Bukkit.getPlayer(loserUUID);

        if (fled && loser != null) {
            String fledMsg = plugin.getMsg("duel-fled").replace("%player%", loser.getName());
            broadcastToDuelants(session, fledMsg);
        }

        // Restore states
        if (plugin.getConfig().getBoolean("duel.restore-state", true)) {
            if (winner != null) {
                if (winnerUUID.equals(session.getChallenger())) session.restoreChallengerState(winner);
                else session.restoreChallengedState(winner);
            }
            if (loser != null) {
                if (loserUUID.equals(session.getChallenger())) session.restoreChallengerState(loser);
                else session.restoreChallengedState(loser);
            }
        }

        // Teleport back
        if (winner != null && session.getZoneCenter() != null) {
            Location origin = winnerUUID.equals(session.getChallenger()) ? session.getChallengerOrigin() : session.getChallengedOrigin();
            if (origin != null) winner.teleport(origin);
        }
        if (loser != null && session.getZoneCenter() != null) {
            Location origin = loserUUID.equals(session.getChallenger()) ? session.getChallengerOrigin() : session.getChallengedOrigin();
            if (origin != null) loser.teleport(origin);
        }

        // Give honor token to winner
        if (winner != null && loser != null) {
            giveHonorToken(winner, loser.getName());
            String winMsg = plugin.getMsg("duel-winner").replace("%winner%", winner.getName());
            Bukkit.getServer().broadcast(Component.text(winMsg));
            plugin.getLeaderboardManager().recordWin(winnerUUID, winner.getName());
            plugin.getLeaderboardManager().recordLoss(loserUUID, loser.getName());
            String duelId = session.getChallenger() + ":" + session.getChallenged();
            EventBus.publish(new DuelCompletedEvent(duelId, winner.getName(), loser.getName()));
        }
    }

    private void giveHonorToken(Player winner, String loserName) {
        ItemStack token = new ItemStack(Material.GOLD_NUGGET);
        ItemMeta meta = token.getItemMeta();
        meta.displayName(Component.text("§6§lHonor Token")
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7Taken from §e" + loserName).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        lore.add(Component.text("§7A trophy of victory.").decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("§8[AngelDuel Honor Trophy]").decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        meta.lore(lore);
        meta.getPersistentDataContainer().set(honorTokenKey, PersistentDataType.STRING, TOKEN_PDC_VALUE);
        token.setItemMeta(meta);

        boolean dropToken = plugin.getConfig().getBoolean("duel.drop-token", true);
        if (!dropToken || winner.getInventory().firstEmpty() != -1) {
            winner.getInventory().addItem(token);
        } else {
            winner.getWorld().dropItemNaturally(winner.getLocation(), token);
        }

        String tokenMsg = plugin.getMsg("duel-honor-token")
                .replace("%loser%", loserName);
        winner.sendMessage(tokenMsg);
        winner.playSound(winner.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
    }

    public boolean isHonorToken(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
                .has(honorTokenKey, PersistentDataType.STRING);
    }

    // -------------------------------------------------------------------------
    // Zone enforcement
    // -------------------------------------------------------------------------

    public boolean isInDuelZone(UUID uuid, Location loc) {
        DuelSession session = activeDuels.get(uuid);
        if (session == null || session.getZoneCenter() == null) return true;
        double radius = plugin.getConfig().getDouble("duel.zone-radius", 15);
        return session.getZoneCenter().getWorld().equals(loc.getWorld())
                && session.getZoneCenter().distanceSquared(loc) <= radius * radius;
    }

    public boolean isThirdPartyInZone(UUID outsiderUUID, Location loc) {
        for (DuelSession session : activeDuels.values()) {
            if (session.getState() != DuelState.ACTIVE) continue;
            if (session.involves(outsiderUUID)) continue;
            if (session.getZoneCenter() == null) continue;
            double radius = plugin.getConfig().getDouble("duel.zone-radius", 15);
            if (session.getZoneCenter().getWorld().equals(loc.getWorld())
                    && session.getZoneCenter().distanceSquared(loc) <= radius * radius) {
                return true;
            }
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

    private void broadcastToDuelants(DuelSession session, String msg) {
        Player c1 = Bukkit.getPlayer(session.getChallenger());
        Player c2 = Bukkit.getPlayer(session.getChallenged());
        if (c1 != null) c1.sendMessage(msg);
        if (c2 != null) c2.sendMessage(msg);
    }

    public void forceEndAllDuels() {
        Set<DuelSession> handled = new HashSet<>();
        for (DuelSession session : new ArrayList<>(activeDuels.values())) {
            if (handled.contains(session)) continue;
            handled.add(session);
            session.setState(DuelState.ENDED);
            Bukkit.getScheduler().cancelTask(session.getCountdownTask());
            // Restore states on shutdown
            if (plugin.getConfig().getBoolean("duel.restore-state", true)) {
                Player c1 = Bukkit.getPlayer(session.getChallenger());
                Player c2 = Bukkit.getPlayer(session.getChallenged());
                if (c1 != null) {
                    session.restoreChallengerState(c1);
                    if (session.getChallengerOrigin() != null) c1.teleport(session.getChallengerOrigin());
                }
                if (c2 != null) {
                    session.restoreChallengedState(c2);
                    if (session.getChallengedOrigin() != null) c2.teleport(session.getChallengedOrigin());
                }
            }
        }
        activeDuels.clear();
        pendingChallenges.clear();
    }
}
