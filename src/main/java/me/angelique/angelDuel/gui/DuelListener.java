package me.angelique.angelDuel.gui;

import me.angelique.angelDuel.AngelDuel;
import me.angelique.angelDuel.managers.DuelManager;
import me.angelique.angelDuel.models.DuelSession;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class DuelListener implements Listener {

    private final AngelDuel plugin;

    public DuelListener(AngelDuel plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();

        if (title.equals(DuelGui.TITLE)) {
            event.setCancelled(true);
            handleMainClick(player, event);
        } else if (title.equals(DuelGui.CHALLENGE_TITLE)) {
            event.setCancelled(true);
            handleChallengeClick(player, event);
        }
    }

    private void handleMainClick(Player player, InventoryClickEvent event) {
        DuelManager dm = plugin.getDuelManager();
        int slot = event.getSlot();

        switch (slot) {
            case 21 -> {
                DuelSession pending = dm.getPendingChallenge(player.getUniqueId());
                if (pending != null) {
                    if (event.isRightClick()) {
                        dm.declineChallenge(player);
                        player.sendMessage(AngelDuel.colorize("&cChallenge declined."));
                    } else {
                        dm.acceptChallenge(player);
                    }
                }
            }
            case 23 -> DuelGui.openChallenge(player, plugin);
            case 40 -> player.closeInventory();
        }
    }

    private void handleChallengeClick(Player player, InventoryClickEvent event) {
        int slot = event.getSlot();
        if (slot == 49) { DuelGui.open(player, plugin); return; }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName()) return;

        String name = org.bukkit.ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        if (name == null || name.isEmpty()) return;

        Player target = Bukkit.getPlayer(name);
        if (target == null) {
            player.sendMessage(AngelDuel.colorize("&cThat player went offline."));
            DuelGui.openChallenge(player, plugin);
            return;
        }
        plugin.getDuelManager().sendChallenge(player, target);
        player.closeInventory();
    }
}
