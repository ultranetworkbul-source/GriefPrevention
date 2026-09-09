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
import org.bukkit.event.entity.EntityDamageEvent;
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

    /**
     * Handles every source of damage to a player, not only entity damage.
     * <p>
     * {@link EntityDamageByEntityEvent} shares a handler list with
     * {@link EntityDamageEvent}, so this one method receives both. That is deliberate:
     * two handlers at the same priority would run in an undefined order, and whichever
     * cancelled first would suppress the other's message.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        UUID ownerUUID = getProtectedClaimOwner(victim.getLocation());
        if (ownerUUID == null) return;

        TemporaryProtectionManager manager = TemporaryProtectionManager.getInstance();
        if (manager == null) return;

        TemporaryProtectionConfig config = TemporaryProtectionConfig.getInstance();
        boolean pvpActive = manager.hasProtection(ownerUUID, ProtectionType.PVP);

        // Damage dealt by another player, directly or by projectile.
        if (event instanceof EntityDamageByEntityEvent entityEvent) {
            Player attacker = getAttacker(entityEvent);
            if (attacker != null) {
                if (attacker.hasPermission("griefprevention.temporaryprotection.bypass")) {
                    return;
                }
                if (pvpActive) {
                    event.setCancelled(true);
                    if (config != null) {
                        attacker.sendMessage(config.getMessagePvpDenied());
                    }
                }
                // A player attacker is never treated as mob damage.
                return;
            }
        }

        // PVP Protection grants immunity to everything else too, unless the cause is exempt.
        if (pvpActive
                && (config == null || config.isPvpFullImmunity())
                && (config == null || !config.isImmunityExempt(event.getCause()))) {
            event.setCancelled(true);
            return;
        }

        // Mob Protection still covers hostile-mob damage on its own.
        if (!(event instanceof EntityDamageByEntityEvent entityEvent)) return;

        Entity damager = entityEvent.getDamager();
        Entity source = (damager instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter)
                ? shooter : damager;
        if (!(source instanceof Enemy)) return;

        if (manager.hasProtection(ownerUUID, ProtectionType.MOB)) {
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

    /**
     * @return the owner of the claim at this location, or null if there is no claim
     *         or it is an admin claim. Subdivisions resolve to their top-level parent.
     */
    private UUID getProtectedClaimOwner(Location location) {
        Claim claim = plugin.dataStore.getClaimAt(location, false, null);
        if (claim == null) return null;

        Claim topClaim = claim.parent != null ? claim.parent : claim;
        return topClaim.ownerID;
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
