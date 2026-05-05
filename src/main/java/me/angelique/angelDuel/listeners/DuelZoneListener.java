package me.angelique.angelDuel.listeners;

import me.angelique.angelDuel.AngelDuel;
import me.angelique.angelDuel.managers.DuelManager;
import me.angelique.angelDuel.models.DuelSession;
import me.angelique.angelDuel.models.DuelState;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class DuelZoneListener implements Listener {

    private final AngelDuel plugin;

    public DuelZoneListener(AngelDuel plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        DuelManager dm = plugin.getDuelManager();
        if (!dm.isInDuel(player.getUniqueId())) return;

        DuelSession session = dm.getSession(player.getUniqueId());
        if (session == null || session.getState() != DuelState.ACTIVE) return;

        Location to = event.getTo();
        if (to == null) return;

        // Only check block-level movement
        Location from = event.getFrom();
        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) return;

        if (!dm.isInDuelZone(player.getUniqueId(), to)) {
            // Forfeit on zone exit
            player.sendMessage(plugin.getMsg("zone-boundary"));
            event.setCancelled(true);

            // Teleport back to zone center as a buffer, then forfeit
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!dm.isInDuel(player.getUniqueId())) return;
                DuelSession s2 = dm.getSession(player.getUniqueId());
                if (s2 == null || s2.getState() != DuelState.ACTIVE) return;
                dm.endDuel(s2, s2.getOpponent(player.getUniqueId()), player.getUniqueId(), true);
            }, 40L); // 2 second grace, then forfeit if still outside
        }
    }

    // Block third parties from interacting with duelists (e.g., healing, trading)
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Player target)) return;
        Player interactor = event.getPlayer();
        DuelManager dm = plugin.getDuelManager();

        if (dm.isInDuel(target.getUniqueId())) {
            DuelSession session = dm.getSession(target.getUniqueId());
            if (session != null && session.getState() == DuelState.ACTIVE
                    && !session.involves(interactor.getUniqueId())) {
                event.setCancelled(true);
                interactor.sendMessage(plugin.getMsg("third-party-blocked"));
            }
        }
    }

    // Prevent hunger loss during countdown
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onHunger(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        DuelManager dm = plugin.getDuelManager();
        if (!dm.isInDuel(player.getUniqueId())) return;
        DuelSession session = dm.getSession(player.getUniqueId());
        if (session != null && session.getState() == DuelState.COUNTDOWN) {
            event.setCancelled(true);
        }
    }
}
