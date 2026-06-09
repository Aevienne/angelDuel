package me.angelique.angelDuel.gui;

import me.angelique.angelDuel.AngelDuel;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class DuelListener implements Listener {

    private final AngelDuel plugin;

    public DuelListener(AngelDuel plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (!title.equals(DuelGui.TITLE)) return;

        event.setCancelled(true);

        switch (event.getSlot()) {
            case 21 -> { // Pending challenges
                player.closeInventory();
                player.chat("/duelaccept");
            }
            case 23 -> { // Challenge a player
                player.closeInventory();
                player.sendMessage(me.angelique.angelDuel.AngelDuel.colorize("&eUse &f/challenge <player> &eto start a duel."));
            }
            case 31 -> { // Leaderboard
                player.closeInventory();
                player.chat("/duelleaderboard");
            }
        }
    }
}
