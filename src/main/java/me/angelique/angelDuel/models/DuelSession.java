package me.angelique.angelDuel.models;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.Collection;
import java.util.UUID;

public class DuelSession {

    private final UUID challenger;
    private final UUID challenged;
    private DuelState state;

    private Location challengerOrigin;
    private Location challengedOrigin;
    private Location zoneCenter;

    // Saved state for restoration
    private ItemStack[] challengerInventory;
    private ItemStack[] challengedInventory;
    private ItemStack[] challengerArmor;
    private ItemStack[] challengedArmor;
    private double challengerHealth;
    private double challengedHealth;
    private int challengerFood;
    private int challengedFood;
    private Collection<PotionEffect> challengerEffects;
    private Collection<PotionEffect> challengedEffects;

    private int countdownTask = -1;
    private int expiryTask = -1;

    public DuelSession(UUID challenger, UUID challenged) {
        this.challenger = challenger;
        this.challenged = challenged;
        this.state = DuelState.PENDING;
    }

    public UUID getChallenger() { return challenger; }
    public UUID getChallenged() { return challenged; }
    public DuelState getState() { return state; }
    public void setState(DuelState state) { this.state = state; }

    public Location getChallengerOrigin() { return challengerOrigin; }
    public void setChallengerOrigin(Location l) { this.challengerOrigin = l; }
    public Location getChallengedOrigin() { return challengedOrigin; }
    public void setChallengedOrigin(Location l) { this.challengedOrigin = l; }
    public Location getZoneCenter() { return zoneCenter; }
    public void setZoneCenter(Location l) { this.zoneCenter = l; }

    public boolean involves(UUID uuid) {
        return challenger.equals(uuid) || challenged.equals(uuid);
    }

    public UUID getOpponent(UUID uuid) {
        return challenger.equals(uuid) ? challenged : challenger;
    }

    public int getCountdownTask() { return countdownTask; }
    public void setCountdownTask(int t) { this.countdownTask = t; }
    public int getExpiryTask() { return expiryTask; }
    public void setExpiryTask(int t) { this.expiryTask = t; }

    // Inventory snapshots
    public void saveChallengerState(Player p) {
        challengerInventory = p.getInventory().getContents().clone();
        challengerArmor = p.getInventory().getArmorContents().clone();
        challengerHealth = p.getHealth();
        challengerFood = p.getFoodLevel();
        challengerEffects = p.getActivePotionEffects();
    }

    public void saveChallengedState(Player p) {
        challengedInventory = p.getInventory().getContents().clone();
        challengedArmor = p.getInventory().getArmorContents().clone();
        challengedHealth = p.getHealth();
        challengedFood = p.getFoodLevel();
        challengedEffects = p.getActivePotionEffects();
    }

    public void restoreChallengerState(Player p) {
        if (challengerInventory != null) p.getInventory().setContents(challengerInventory);
        if (challengerArmor != null) p.getInventory().setArmorContents(challengerArmor);
        p.setHealth(Math.min(challengerHealth, p.getMaxHealth()));
        p.setFoodLevel(challengerFood);
        p.getActivePotionEffects().forEach(e -> p.removePotionEffect(e.getType()));
        if (challengerEffects != null) challengerEffects.forEach(p::addPotionEffect);
    }

    public void restoreChallengedState(Player p) {
        if (challengedInventory != null) p.getInventory().setContents(challengedInventory);
        if (challengedArmor != null) p.getInventory().setArmorContents(challengedArmor);
        p.setHealth(Math.min(challengedHealth, p.getMaxHealth()));
        p.setFoodLevel(challengedFood);
        p.getActivePotionEffects().forEach(e -> p.removePotionEffect(e.getType()));
        if (challengedEffects != null) challengedEffects.forEach(p::addPotionEffect);
    }
}
