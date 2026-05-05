package me.angelique.angelDuel.listeners;

import me.angelique.angelDuel.AngelDuel;
import me.angelique.angelDuel.managers.DuelManager;
import me.angelique.angelDuel.models.DuelSession;
import me.angelique.angelDuel.models.DuelState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import java.util.UUID;

public class DuelCombatListener implements Listener {

    private final AngelDuel plugin;

    public DuelCombatListener(AngelDuel plugin) { this.plugin = plugin; }

    // Block damage to duelists from third parties and non-duel damage during duel
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        DuelManager dm = plugin.getDuelManager();

        // If victim is in active duel
        if (dm.isInDuel(victim.getUniqueId())) {
            DuelSession session = dm.getSession(victim.getUniqueId());
            if (session == null || session.getState() != DuelState.ACTIVE) {
                event.setCancelled(true);
                return;
            }
            // Only allow damage from the duel opponent
            if (event.getDamager() instanceof Player attacker) {
                if (!attacker.getUniqueId().equals(session.getOpponent(victim.getUniqueId()))) {
                    event.setCancelled(true);
                    attacker.sendMessage(plugin.getMsg("third-party-blocked"));
                }
            }
            return;
        }

        // If attacker is trying to hit a duelist from outside
        if (event.getDamager() instanceof Player attacker && dm.isInDuel(victim.getUniqueId())) {
            event.setCancelled(true);
            attacker.sendMessage(plugin.getMsg("third-party-blocked"));
        }
    }

    // Block environmental damage to duelists during countdown
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEnvironmentalDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event instanceof EntityDamageByEntityEvent) return; // handled above
        DuelManager dm = plugin.getDuelManager();
        if (!dm.isInDuel(player.getUniqueId())) return;
        DuelSession session = dm.getSession(player.getUniqueId());
        if (session != null && session.getState() == DuelState.COUNTDOWN) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player loser = event.getEntity();
        DuelManager dm = plugin.getDuelManager();
        if (!dm.isInDuel(loser.getUniqueId())) return;

        DuelSession session = dm.getSession(loser.getUniqueId());
        if (session == null || session.getState() != DuelState.ACTIVE) return;

        // Cancel normal death drops in duel
        event.getDrops().clear();
        event.setDroppedExp(0);
        event.setDeathMessage(null);

        // Keep inventory since we restore state
        event.setKeepInventory(true);

        UUID winnerUUID = session.getOpponent(loser.getUniqueId());
        // Schedule end on next tick (let death process first)
        plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                dm.endDuel(session, winnerUUID, loser.getUniqueId(), false), 1L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        DuelManager dm = plugin.getDuelManager();
        if (!dm.isInDuel(player.getUniqueId())) return;

        DuelSession session = dm.getSession(player.getUniqueId());
        if (session == null || session.getState() == DuelState.ENDED) return;

        UUID opponentUUID = session.getOpponent(player.getUniqueId());
        dm.endDuel(session, opponentUUID, player.getUniqueId(), false);
    }
}
