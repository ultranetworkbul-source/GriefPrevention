package com.griefprevention.protection.temporary;

import io.lumine.mythic.bukkit.events.MythicMobSpawnEvent;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.UUID;

public class MythicMobsProtectionListener implements Listener {

    private final GriefPrevention plugin;

    public MythicMobsProtectionListener(GriefPrevention plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMythicMobSpawn(MythicMobSpawnEvent event) {
        Location location = event.getLocation();
        if (location == null) return;

        Claim claim = plugin.dataStore.getClaimAt(location, false, null);
        if (claim == null) return;

        Claim topClaim = claim.parent != null ? claim.parent : claim;
        UUID ownerUUID = topClaim.ownerID;
        if (ownerUUID == null) return;

        TemporaryProtectionManager manager = TemporaryProtectionManager.getInstance();
        if (manager == null || !manager.hasProtection(ownerUUID, ProtectionType.MOB)) return;

        event.setCancelled(true);
    }
}
