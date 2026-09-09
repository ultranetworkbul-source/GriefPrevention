package com.griefprevention.protection.temporary;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.sql.*;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class TemporaryProtectionManager {

    private static TemporaryProtectionManager instance;

    private final GriefPrevention plugin;
    private final Map<UUID, Map<ProtectionType, TemporaryProtection>> cache = new ConcurrentHashMap<>();
    private Connection connection;
    private BukkitTask cleanupTask;

    private static final String SQL_CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS gp_temporary_protection (" +
                    "owner_uuid VARCHAR(36) NOT NULL, " +
                    "protection_type VARCHAR(20) NOT NULL, " +
                    "start_time BIGINT NOT NULL, " +
                    "duration_millis BIGINT NOT NULL, " +
                    "PRIMARY KEY (owner_uuid, protection_type))";

    private static final String SQL_INSERT =
            "INSERT OR REPLACE INTO gp_temporary_protection (owner_uuid, protection_type, start_time, duration_millis) VALUES (?, ?, ?, ?)";

    private static final String SQL_DELETE =
            "DELETE FROM gp_temporary_protection WHERE owner_uuid = ? AND protection_type = ?";

    private static final String SQL_SELECT_ALL =
            "SELECT * FROM gp_temporary_protection";

    private static final String SQL_DELETE_EXPIRED =
            "DELETE FROM gp_temporary_protection WHERE (start_time + duration_millis) < ?";

    public TemporaryProtectionManager(GriefPrevention plugin) {
        this.plugin = plugin;
        instance = this;
    }

    public static TemporaryProtectionManager getInstance() {
        return instance;
    }

    public void initialize() {
        initializeDatabase();
        loadAllProtections();
        startCleanupTask();
    }

    private void initializeDatabase() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            String dbPath = new File(dataFolder, "temporary_protection.db").getAbsolutePath();
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);

            try (Statement stmt = connection.createStatement()) {
                stmt.execute(SQL_CREATE_TABLE);
            }

            plugin.getLogger().info("[TemporaryProtection] Database initialized.");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[TemporaryProtection] Failed to initialize database!", e);
        }
    }

    private void loadAllProtections() {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(SQL_SELECT_ALL)) {

            int loaded = 0;
            while (rs.next()) {
                UUID ownerUUID = UUID.fromString(rs.getString("owner_uuid"));
                ProtectionType type = ProtectionType.fromId(rs.getString("protection_type"));
                long startTime = rs.getLong("start_time");
                long durationMillis = rs.getLong("duration_millis");

                if (type == null) continue;

                TemporaryProtection protection = new TemporaryProtection(ownerUUID, type, startTime, durationMillis);

                if (protection.isActive()) {
                    cache.computeIfAbsent(ownerUUID, k -> new EnumMap<>(ProtectionType.class))
                            .put(type, protection);
                    loaded++;
                }
            }

            plugin.getLogger().info("[TemporaryProtection] Loaded " + loaded + " active protections.");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[TemporaryProtection] Failed to load protections!", e);
        }
    }

    private void startCleanupTask() {
        cleanupTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::cleanupExpiredProtections, 6000L, 6000L);
    }

    private void cleanupExpiredProtections() {
        long now = System.currentTimeMillis();

        for (Map.Entry<UUID, Map<ProtectionType, TemporaryProtection>> entry : cache.entrySet()) {
            UUID ownerUUID = entry.getKey();
            Map<ProtectionType, TemporaryProtection> protections = entry.getValue();

            protections.entrySet().removeIf(typeEntry -> {
                if (typeEntry.getValue().isExpired()) {
                    ProtectionType type = typeEntry.getKey();

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        Player player = Bukkit.getPlayer(ownerUUID);
                        if (player != null && player.isOnline()) {
                            TemporaryProtectionConfig config = TemporaryProtectionConfig.getInstance();
                            if (config != null) {
                                String msg = config.getMessageProtectionExpired()
                                        .replace("{type}", type.getDisplayName());
                                player.sendMessage(msg);
                            }
                        }
                    });

                    return true;
                }
                return false;
            });

            if (protections.isEmpty()) {
                cache.remove(ownerUUID);
            }
        }

        try (PreparedStatement stmt = connection.prepareStatement(SQL_DELETE_EXPIRED)) {
            stmt.setLong(1, now);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "[TemporaryProtection] Failed to cleanup expired protections!", e);
        }
    }

    public void addProtection(UUID ownerUUID, ProtectionType type, long durationMillis) {
        long startTime = System.currentTimeMillis();
        TemporaryProtection protection = new TemporaryProtection(ownerUUID, type, startTime, durationMillis);

        cache.computeIfAbsent(ownerUUID, k -> new EnumMap<>(ProtectionType.class))
                .put(type, protection);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> saveProtectionToDatabase(protection));
    }

    public void extendProtection(UUID ownerUUID, ProtectionType type, long additionalMillis) {
        TemporaryProtection existing = getProtection(ownerUUID, type);

        if (existing != null && existing.isActive()) {
            long newDuration = existing.getRemainingMillis() + additionalMillis;
            long startTime = System.currentTimeMillis();
            TemporaryProtection extended = new TemporaryProtection(ownerUUID, type, startTime, newDuration);

            cache.computeIfAbsent(ownerUUID, k -> new EnumMap<>(ProtectionType.class))
                    .put(type, extended);

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> saveProtectionToDatabase(extended));
        } else {
            addProtection(ownerUUID, type, additionalMillis);
        }
    }

    private void saveProtectionToDatabase(TemporaryProtection protection) {
        try (PreparedStatement stmt = connection.prepareStatement(SQL_INSERT)) {
            stmt.setString(1, protection.getOwnerUUID().toString());
            stmt.setString(2, protection.getType().getId());
            stmt.setLong(3, protection.getStartTime());
            stmt.setLong(4, protection.getDurationMillis());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "[TemporaryProtection] Failed to save protection!", e);
        }
    }

    public void removeProtection(UUID ownerUUID, ProtectionType type) {
        Map<ProtectionType, TemporaryProtection> protections = cache.get(ownerUUID);
        if (protections != null) {
            protections.remove(type);
            if (protections.isEmpty()) {
                cache.remove(ownerUUID);
            }
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (PreparedStatement stmt = connection.prepareStatement(SQL_DELETE)) {
                stmt.setString(1, ownerUUID.toString());
                stmt.setString(2, type.getId());
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "[TemporaryProtection] Failed to remove protection!", e);
            }
        });
    }

    public boolean hasProtection(UUID ownerUUID, ProtectionType type) {
        TemporaryProtection protection = getProtection(ownerUUID, type);
        return protection != null && protection.isActive();
    }

    public TemporaryProtection getProtection(UUID ownerUUID, ProtectionType type) {
        Map<ProtectionType, TemporaryProtection> protections = cache.get(ownerUUID);
        if (protections == null) return null;

        TemporaryProtection protection = protections.get(type);
        if (protection != null && protection.isActive()) {
            return protection;
        }
        return null;
    }

    public Map<ProtectionType, TemporaryProtection> getProtections(UUID ownerUUID) {
        return cache.getOrDefault(ownerUUID, new EnumMap<>(ProtectionType.class));
    }

    public boolean isClaimProtected(Claim claim, ProtectionType type) {
        if (claim == null || claim.ownerID == null) return false;
        return hasProtection(claim.ownerID, type);
    }

    public long calculateTotalClaimArea(UUID ownerUUID) {
        long totalArea = 0;
        for (Claim claim : plugin.dataStore.getClaims()) {
            if (ownerUUID.equals(claim.ownerID)) {
                totalArea += claim.getArea();
            }
        }
        return totalArea;
    }

    public void shutdown() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }

        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "[TemporaryProtection] Failed to close database connection!", e);
            }
            connection = null;
        }

        cache.clear();
        instance = null;
    }

    public void reload() {
        cache.clear();
        loadAllProtections();
    }
}
