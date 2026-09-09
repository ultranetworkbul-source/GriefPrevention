package com.griefprevention.protection.temporary;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.UUID;

public class TemporaryProtectionListener implements Listener {

    private final GriefPrevention plugin;

    public TemporaryProtectionListener(GriefPrevention plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        if (!isContainer(block.getType())) return;

        Player player = event.getPlayer();

        if (player.hasPermission("griefprevention.temporaryprotection.bypass")) {
            return;
        }

        if (shouldDenyContainerAccess(player, block.getLocation())) {
            event.setCancelled(true);
            TemporaryProtectionConfig config = TemporaryProtectionConfig.getInstance();
            if (config != null) {
                player.sendMessage(config.getMessageChestAccessDenied());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        Player attacker = getAttacker(event);
        if (attacker != null) {
            if (attacker.hasPermission("griefprevention.temporaryprotection.bypass")) {
                return;
            }

            if (shouldDenyPvP(attacker, victim)) {
                event.setCancelled(true);
                TemporaryProtectionConfig config = TemporaryProtectionConfig.getInstance();
                if (config != null) {
                    attacker.sendMessage(config.getMessagePvpDenied());
                }
            }
            return;
        }

        Entity damager = event.getDamager();
        Entity source = (damager instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter)
                ? shooter : damager;
        if (!(source instanceof Enemy)) return;

        if (shouldDenyMobDamage(victim)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();
        if (reason == CreatureSpawnEvent.SpawnReason.CUSTOM
                || reason == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG
                || reason == CreatureSpawnEvent.SpawnReason.BREEDING
                || reason == CreatureSpawnEvent.SpawnReason.DISPENSE_EGG
                || reason == CreatureSpawnEvent.SpawnReason.EGG
                || reason == CreatureSpawnEvent.SpawnReason.BUILD_IRONGOLEM
                || reason == CreatureSpawnEvent.SpawnReason.BUILD_SNOWMAN
                || reason == CreatureSpawnEvent.SpawnReason.BUILD_WITHER) {
            return;
        }

        if (!(event.getEntity() instanceof Enemy)) return;

        if (shouldDenyMobSpawn(event.getLocation())) {
            event.setCancelled(true);
        }
    }

    private Player getAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            return player;
        }

        if (event.getDamager() instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player player) {
                return player;
            }
        }

        return null;
    }

    private boolean isContainer(Material material) {
        return switch (material) {
            case CHEST, TRAPPED_CHEST, BARREL, SHULKER_BOX,
                    WHITE_SHULKER_BOX, ORANGE_SHULKER_BOX, MAGENTA_SHULKER_BOX,
                    LIGHT_BLUE_SHULKER_BOX, YELLOW_SHULKER_BOX, LIME_SHULKER_BOX,
                    PINK_SHULKER_BOX, GRAY_SHULKER_BOX, LIGHT_GRAY_SHULKER_BOX,
                    CYAN_SHULKER_BOX, PURPLE_SHULKER_BOX, BLUE_SHULKER_BOX,
                    BROWN_SHULKER_BOX, GREEN_SHULKER_BOX, RED_SHULKER_BOX,
                    BLACK_SHULKER_BOX, HOPPER, DROPPER, DISPENSER,
                    FURNACE, BLAST_FURNACE, SMOKER, BREWING_STAND -> true;
            default -> false;
        };
    }

    private boolean shouldDenyContainerAccess(Player player, Location location) {
        Claim claim = plugin.dataStore.getClaimAt(location, false, null);
        if (claim == null) return false;

        Claim topClaim = claim.parent != null ? claim.parent : claim;
        UUID ownerUUID = topClaim.ownerID;

        if (ownerUUID == null) return false;

        TemporaryProtectionManager manager = TemporaryProtectionManager.getInstance();
        if (manager == null || !manager.hasProtection(ownerUUID, ProtectionType.CHEST)) {
            return false;
        }

        if (player.getUniqueId().equals(ownerUUID)) return false;

        if (plugin.dataStore.getPlayerData(player.getUniqueId()).ignoreClaims &&
                player.hasPermission("griefprevention.ignoreclaims")) {
            return false;
        }

        return claim.checkPermission(player, ClaimPermission.Container, null) != null;
    }

    private boolean shouldDenyPvP(Player attacker, Player victim) {
        Location victimLocation = victim.getLocation();
        Claim claim = plugin.dataStore.getClaimAt(victimLocation, false, null);
        if (claim == null) return false;

        Claim topClaim = claim.parent != null ? claim.parent : claim;
        UUID ownerUUID = topClaim.ownerID;

        if (ownerUUID == null) return false;

        TemporaryProtectionManager manager = TemporaryProtectionManager.getInstance();
        if (manager == null || !manager.hasProtection(ownerUUID, ProtectionType.PVP)) {
            return false;
        }

        // Block PvP for everyone when protection is active (no exceptions for owner or trusted players)
        return true;
    }

    private boolean shouldDenyMobDamage(Player victim) {
        Claim claim = plugin.dataStore.getClaimAt(victim.getLocation(), false, null);
        if (claim == null) return false;

        Claim topClaim = claim.parent != null ? claim.parent : claim;
        UUID ownerUUID = topClaim.ownerID;
        if (ownerUUID == null) return false;

        TemporaryProtectionManager manager = TemporaryProtectionManager.getInstance();
        return manager != null && manager.hasProtection(ownerUUID, ProtectionType.MOB);
    }

    private boolean shouldDenyMobSpawn(Location location) {
        Claim claim = plugin.dataStore.getClaimAt(location, false, null);
        if (claim == null) return false;

        Claim topClaim = claim.parent != null ? claim.parent : claim;
        UUID ownerUUID = topClaim.ownerID;
        if (ownerUUID == null) return false;

        TemporaryProtectionManager manager = TemporaryProtectionManager.getInstance();
        return manager != null && manager.hasProtection(ownerUUID, ProtectionType.MOB);
    }
}
