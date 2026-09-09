package com.griefprevention.protection.temporary;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.entity.EntityDamageEvent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class TemporaryProtectionConfig {

    private static final int CURRENT_CONFIG_VERSION = 3;

    /**
     * Damage causes that stay lethal even while PVP protection grants full immunity.
     * VOID and WORLD_BORDER are here because cancelling them leaves a player falling
     * forever instead of respawning; KILL and SUICIDE are here so staff commands work.
     */
    private static final List<String> DEFAULT_IMMUNITY_EXEMPT_CAUSES =
            List.of("VOID", "WORLD_BORDER", "KILL", "SUICIDE");

    private static TemporaryProtectionConfig instance;

    private final GriefPrevention plugin;
    private File configFile;
    private FileConfiguration config;

    private double chestMultiplier = 2.0;
    private double pvpMultiplier = 4.0;
    private double mobMultiplier = 3.0;

    private boolean pvpFullImmunity = true;
    private Set<EntityDamageEvent.DamageCause> pvpImmunityExemptCauses =
            EnumSet.noneOf(EntityDamageEvent.DamageCause.class);

    private final Map<String, DurationOption> durationOptions = new HashMap<>();

    private String mainMenuTitle = "&8Protection Shop";
    private int mainMenuSize = 27;
    private boolean mainMenuFillerEnabled = false;
    private Material mainMenuFillerMaterial = Material.BLACK_STAINED_GLASS_PANE;
    private String mainMenuFillerDisplayName = " ";

    private int chestSlot = 11;
    private Material chestMaterial = Material.CHEST;
    private String chestDisplayName = "&aChest Protection";
    private List<String> chestLore = new ArrayList<>();
    private boolean chestGlow = false;

    private int pvpSlot = 15;
    private Material pvpMaterial = Material.DIAMOND_SWORD;
    private String pvpDisplayName = "&aPVP Protection";
    private List<String> pvpLore = new ArrayList<>();
    private boolean pvpGlow = false;

    private int mobSlot = 13;
    private Material mobMaterial = Material.ZOMBIE_HEAD;
    private String mobDisplayName = "&aMob Protection";
    private List<String> mobLore = new ArrayList<>();
    private boolean mobGlow = false;

    private boolean durationItemsGlow = false;

    private String durationGuiTitle = "&8{type}: {remaining}";
    private int durationGuiSize = 27;
    private boolean fillerEnabled = false;
    private Material fillerMaterial = Material.BLACK_STAINED_GLASS_PANE;
    private String fillerDisplayName = " ";

    private double knockbackStrength = 1.5;
    private double knockbackVertical = 0.5;
    private String titleMain = "&c&lProtected Claim!";
    private String titleSubtitle = "&7You cannot enter this area";
    private int titleFadeIn = 5;
    private int titleStay = 30;
    private int titleFadeOut = 10;
    private Sound sound = Sound.ENTITY_GENERIC_EXPLODE;
    private float soundVolume = 0.5f;
    private float soundPitch = 1.2f;

    private String messageProtectionPurchased = "&aYou purchased {duration} of {type} for all your claims!";
    private String messageProtectionExpired = "&cYour {type} has expired.";
    private String messageProtectionActive = "&aYour {type} is active for {remaining}.";
    private String messageNoClaims = "&cYou don't have any claims!";
    private String messageNotEnoughMoney = "&cYou need {price} to purchase this protection.";
    private String messageVaultNotFound = "&cEconomy system not found! Contact an administrator.";
    private String messageChestAccessDenied = "&cThis chest is protected!";
    private String messagePvpDenied = "&cPVP is disabled in this claim!";
    private String messageMobDenied = "&cMobs can't damage players in this claim!";

    // Command messages
    private String messagePlayersOnly = "&cThis command can only be used by players.";
    private String messageUnknownSubcommand = "&cUnknown sub-command. Use: /claimprotection [status|cancel|reload]";
    private String messageSystemUnavailable = "&cProtection system is not available.";
    private String messageNoActiveProtection = "&eYou don't have any active claim protection.";
    private String messageNoActiveProtectionHint = "&7Use /claimprotection to purchase protection.";
    private String messageStatusHeader = "&a=== Protection Status ===";
    private String messageChestInactive = "&7Chest Protection: &cInactive";
    private String messagePvpInactive = "&7PVP Protection: &cInactive";
    private String messageMobInactive = "&7Mob Protection: &cInactive";
    private String messageProtectedArea = "&7Protected area: &f{area} blocks";
    private String messageInvalidProtectionType = "&cInvalid protection type. Use: chest, pvp or mob";
    private String messageNoProtectionToCancel = "&cYou don't have active {type} to cancel.";
    private String messageProtectionCancelled = "&eYour {type} has been cancelled. No refund has been issued.";
    private String messageNoActiveProtectionToCancel = "&cYou don't have active protection to cancel.";
    private String messageAllProtectionsCancelled = "&eAll your claim protections have been cancelled. No refund has been issued.";
    private String messageNoReloadPermission = "&cYou don't have permission to reload the configuration.";
    private String messageConfigReloaded = "&aTemporary protection configuration reloaded.";

    public TemporaryProtectionConfig(GriefPrevention plugin) {
        this.plugin = plugin;
        instance = this;
    }

    public static TemporaryProtectionConfig getInstance() {
        return instance;
    }

    public void load() {
        configFile = new File(plugin.getDataFolder(), "temporary-protection.yml");

        if (!configFile.exists()) {
            createDefaultConfig();
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        migrateConfig();
        loadSettings();
    }

    private void migrateConfig() {
        int version = config.getInt("config-version", 1);
        if (version >= CURRENT_CONFIG_VERSION) return;

        boolean changed = false;

        if (!config.isSet("pricing.mob.per-day-multiplier")) {
            config.set("pricing.mob.per-day-multiplier", 3.0);
            changed = true;
        }

        if (!config.isSet("main-menu.mob.slot")) {
            config.set("main-menu.mob.slot", 13);
            changed = true;
        }
        if (!config.isSet("main-menu.mob.material")) {
            config.set("main-menu.mob.material", "ZOMBIE_HEAD");
            changed = true;
        }
        if (!config.isSet("main-menu.mob.display-name")) {
            config.set("main-menu.mob.display-name", "&aMob Protection");
            changed = true;
        }
        if (!config.isSet("main-menu.mob.lore")) {
            config.set("main-menu.mob.lore", List.of(
                    "&7Disables mob spawning and",
                    "&7mob damage inside your claims.",
                    "",
                    "&7Status: {status}",
                    "&7Remaining: &f{remaining}",
                    "",
                    "&eClick to purchase!"
            ));
            changed = true;
        }
        if (!config.isSet("main-menu.mob.glow")) {
            config.set("main-menu.mob.glow", false);
            changed = true;
        }

        if (!config.isSet("messages.mob-denied")) {
            config.set("messages.mob-denied", "&cMobs can't damage players in this claim!");
            changed = true;
        }
        if (!config.isSet("messages.mob-inactive")) {
            config.set("messages.mob-inactive", "&7Mob Protection: &cInactive");
            changed = true;
        }

        // v3: PVP Protection became full damage immunity.
        if (!config.isSet("protection.pvp.full-immunity")) {
            config.set("protection.pvp.full-immunity", true);
            changed = true;
        }
        if (!config.isSet("protection.pvp.immunity-exempt-causes")) {
            config.set("protection.pvp.immunity-exempt-causes", DEFAULT_IMMUNITY_EXEMPT_CAUSES);
            changed = true;
        }

        config.set("config-version", CURRENT_CONFIG_VERSION);

        if (!changed) {
            try {
                config.save(configFile);
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "[TemporaryProtection] Failed to stamp config version!", e);
            }
            return;
        }

        try {
            config.save(configFile);
            plugin.getLogger().info("[TemporaryProtection] Migrated config to v" + CURRENT_CONFIG_VERSION + " with mob protection defaults.");
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "[TemporaryProtection] Failed to save migrated config!", e);
        }
    }

    private void createDefaultConfig() {
        try {
            if (!configFile.getParentFile().exists()) {
                configFile.getParentFile().mkdirs();
            }
            configFile.createNewFile();
            FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(configFile);

            defaultConfig.set("config-version", CURRENT_CONFIG_VERSION);

            defaultConfig.set("pricing.chest.per-day-multiplier", 2.0);
            defaultConfig.set("pricing.pvp.per-day-multiplier", 4.0);
            defaultConfig.set("pricing.mob.per-day-multiplier", 3.0);

            // true  = PVP Protection makes players inside the claim immune to every damage
            //         source, not only other players.
            // false = PVP Protection blocks player-versus-player damage only (the old behaviour).
            defaultConfig.set("protection.pvp.full-immunity", true);
            // Damage causes that still apply even with full immunity on. Names come from
            // Bukkit's EntityDamageEvent.DamageCause. Set to [] for literally no damage at all.
            defaultConfig.set("protection.pvp.immunity-exempt-causes", DEFAULT_IMMUNITY_EXEMPT_CAUSES);

            defaultConfig.set("durations.12h.hours", 12);
            defaultConfig.set("durations.12h.slot", 10);
            defaultConfig.set("durations.12h.material", "CLOCK");
            defaultConfig.set("durations.12h.display-name", "&a12 Hours");
            defaultConfig.set("durations.12h.lore", List.of("&7Cost: &e{price}", "&7Duration: &f12 hours", "", "&eClick to purchase!"));

            defaultConfig.set("durations.24h.hours", 24);
            defaultConfig.set("durations.24h.slot", 12);
            defaultConfig.set("durations.24h.material", "CLOCK");
            defaultConfig.set("durations.24h.display-name", "&a24 Hours");
            defaultConfig.set("durations.24h.lore", List.of("&7Cost: &e{price}", "&7Duration: &f24 hours", "", "&eClick to purchase!"));

            defaultConfig.set("durations.3d.hours", 72);
            defaultConfig.set("durations.3d.slot", 14);
            defaultConfig.set("durations.3d.material", "CLOCK");
            defaultConfig.set("durations.3d.display-name", "&a3 Days");
            defaultConfig.set("durations.3d.lore", List.of("&7Cost: &e{price}", "&7Duration: &f3 days", "", "&eClick to purchase!"));

            defaultConfig.set("durations.7d.hours", 168);
            defaultConfig.set("durations.7d.slot", 16);
            defaultConfig.set("durations.7d.material", "CLOCK");
            defaultConfig.set("durations.7d.display-name", "&a7 Days");
            defaultConfig.set("durations.7d.lore", List.of("&7Cost: &e{price}", "&7Duration: &f7 days", "", "&eClick to purchase!"));

            defaultConfig.set("main-menu.title", "&8Protection Shop");
            defaultConfig.set("main-menu.size", 27);
            defaultConfig.set("main-menu.filler.enabled", false);
            defaultConfig.set("main-menu.filler.material", "BLACK_STAINED_GLASS_PANE");
            defaultConfig.set("main-menu.filler.display-name", " ");

            defaultConfig.set("main-menu.chest.slot", 11);
            defaultConfig.set("main-menu.chest.material", "CHEST");
            defaultConfig.set("main-menu.chest.display-name", "&aChest Protection");
            defaultConfig.set("main-menu.chest.lore", List.of(
                    "&7Prevents others from opening",
                    "&7containers in your claims.",
                    "",
                    "&7Status: {status}",
                    "&7Remaining: &f{remaining}",
                    "",
                    "&eClick to purchase!"
            ));
            defaultConfig.set("main-menu.chest.glow", false);

            defaultConfig.set("main-menu.pvp.slot", 15);
            defaultConfig.set("main-menu.pvp.material", "DIAMOND_SWORD");
            defaultConfig.set("main-menu.pvp.display-name", "&aPVP Protection");
            defaultConfig.set("main-menu.pvp.lore", List.of(
                    "&7Makes players inside your claims",
                    "&7immune to all damage.",
                    "",
                    "&7Status: {status}",
                    "&7Remaining: &f{remaining}",
                    "",
                    "&eClick to purchase!"
            ));
            defaultConfig.set("main-menu.pvp.glow", false);

            defaultConfig.set("main-menu.mob.slot", 13);
            defaultConfig.set("main-menu.mob.material", "ZOMBIE_HEAD");
            defaultConfig.set("main-menu.mob.display-name", "&aMob Protection");
            defaultConfig.set("main-menu.mob.lore", List.of(
                    "&7Disables mob spawning and",
                    "&7mob damage inside your claims.",
                    "",
                    "&7Status: {status}",
                    "&7Remaining: &f{remaining}",
                    "",
                    "&eClick to purchase!"
            ));
            defaultConfig.set("main-menu.mob.glow", false);

            defaultConfig.set("duration-gui.title", "&8{type}: {remaining}");
            defaultConfig.set("duration-gui.size", 27);
            defaultConfig.set("duration-gui.filler.enabled", false);
            defaultConfig.set("duration-gui.filler.material", "BLACK_STAINED_GLASS_PANE");
            defaultConfig.set("duration-gui.filler.display-name", " ");
            defaultConfig.set("duration-gui.items-glow", false);

            defaultConfig.set("effects.knockback.strength", 1.5);
            defaultConfig.set("effects.knockback.vertical", 0.5);
            defaultConfig.set("effects.title.main", "&c&lProtected Claim!");
            defaultConfig.set("effects.title.subtitle", "&7You cannot enter this area");
            defaultConfig.set("effects.title.fade-in", 5);
            defaultConfig.set("effects.title.stay", 30);
            defaultConfig.set("effects.title.fade-out", 10);
            defaultConfig.set("effects.sound", "ENTITY_GENERIC_EXPLODE");
            defaultConfig.set("effects.sound-volume", 0.5);
            defaultConfig.set("effects.sound-pitch", 1.2);

            defaultConfig.set("messages.protection-purchased", "&aYou purchased {duration} of {type} for all your claims!");
            defaultConfig.set("messages.protection-expired", "&cYour {type} has expired.");
            defaultConfig.set("messages.protection-active", "&aYour {type} is active for {remaining}.");
            defaultConfig.set("messages.no-claims", "&cYou don't have any claims!");
            defaultConfig.set("messages.not-enough-money", "&cYou need {price} to purchase this protection.");
            defaultConfig.set("messages.vault-not-found", "&cEconomy system not found! Contact an administrator.");
            defaultConfig.set("messages.chest-access-denied", "&cThis chest is protected!");
            defaultConfig.set("messages.pvp-denied", "&cPVP is disabled in this claim!");
            defaultConfig.set("messages.mob-denied", "&cMobs can't damage players in this claim!");

            // Command messages
            defaultConfig.set("messages.players-only", "&cThis command can only be used by players.");
            defaultConfig.set("messages.unknown-subcommand", "&cUnknown sub-command. Use: /claimprotection [status|cancel|reload]");
            defaultConfig.set("messages.system-unavailable", "&cProtection system is not available.");
            defaultConfig.set("messages.no-active-protection", "&eYou don't have any active claim protection.");
            defaultConfig.set("messages.no-active-protection-hint", "&7Use /claimprotection to purchase protection.");
            defaultConfig.set("messages.status-header", "&a=== Protection Status ===");
            defaultConfig.set("messages.chest-inactive", "&7Chest Protection: &cInactive");
            defaultConfig.set("messages.pvp-inactive", "&7PVP Protection: &cInactive");
            defaultConfig.set("messages.mob-inactive", "&7Mob Protection: &cInactive");
            defaultConfig.set("messages.protected-area", "&7Protected area: &f{area} blocks");
            defaultConfig.set("messages.invalid-protection-type", "&cInvalid protection type. Use: chest, pvp or mob");
            defaultConfig.set("messages.no-protection-to-cancel", "&cYou don't have active {type} to cancel.");
            defaultConfig.set("messages.protection-cancelled", "&eYour {type} has been cancelled. No refund has been issued.");
            defaultConfig.set("messages.no-active-protection-to-cancel", "&cYou don't have active protection to cancel.");
            defaultConfig.set("messages.all-protections-cancelled", "&eAll your claim protections have been cancelled. No refund has been issued.");
            defaultConfig.set("messages.no-reload-permission", "&cYou don't have permission to reload the configuration.");
            defaultConfig.set("messages.config-reloaded", "&aTemporary protection configuration reloaded.");

            defaultConfig.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "[TemporaryProtection] Failed to create default config!", e);
        }
    }

    private void loadSettings() {
        chestMultiplier = config.getDouble("pricing.chest.per-day-multiplier", 2.0);
        pvpMultiplier = config.getDouble("pricing.pvp.per-day-multiplier", 4.0);
        mobMultiplier = config.getDouble("pricing.mob.per-day-multiplier", 3.0);

        pvpFullImmunity = config.getBoolean("protection.pvp.full-immunity", true);

        List<String> exemptNames = config.isSet("protection.pvp.immunity-exempt-causes")
                ? config.getStringList("protection.pvp.immunity-exempt-causes")
                : DEFAULT_IMMUNITY_EXEMPT_CAUSES;
        Set<EntityDamageEvent.DamageCause> exemptCauses =
                EnumSet.noneOf(EntityDamageEvent.DamageCause.class);
        for (String name : exemptNames) {
            if (name == null || name.isBlank()) continue;
            try {
                exemptCauses.add(EntityDamageEvent.DamageCause
                        .valueOf(name.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("[TemporaryProtection] Unknown damage cause '" + name
                        + "' in protection.pvp.immunity-exempt-causes - ignoring it.");
            }
        }
        pvpImmunityExemptCauses = exemptCauses;

        durationOptions.clear();
        ConfigurationSection durationsSection = config.getConfigurationSection("durations");
        if (durationsSection != null) {
            for (String key : durationsSection.getKeys(false)) {
                ConfigurationSection durationSection = durationsSection.getConfigurationSection(key);
                if (durationSection != null) {
                    int hours = durationSection.getInt("hours", 24);
                    int slot = durationSection.getInt("slot", 0);
                    Material material = Material.matchMaterial(durationSection.getString("material", "CLOCK"));
                    if (material == null) material = Material.CLOCK;

                    String displayName = colorize(durationSection.getString("display-name", "&aProtection"));
                    List<String> lore = new ArrayList<>();
                    for (String line : durationSection.getStringList("lore")) {
                        lore.add(colorize(line));
                    }

                    durationOptions.put(key, new DurationOption(key, hours, slot, material, displayName, lore));
                }
            }
        }

        mainMenuTitle = colorize(config.getString("main-menu.title", "&8Protection Shop"));
        mainMenuSize = config.getInt("main-menu.size", 27);
        mainMenuFillerEnabled = config.getBoolean("main-menu.filler.enabled", false);
        mainMenuFillerMaterial = Material.matchMaterial(config.getString("main-menu.filler.material", "BLACK_STAINED_GLASS_PANE"));
        if (mainMenuFillerMaterial == null) mainMenuFillerMaterial = Material.BLACK_STAINED_GLASS_PANE;
        mainMenuFillerDisplayName = colorize(config.getString("main-menu.filler.display-name", " "));

        chestSlot = config.getInt("main-menu.chest.slot", 11);
        chestMaterial = Material.matchMaterial(config.getString("main-menu.chest.material", "CHEST"));
        if (chestMaterial == null) chestMaterial = Material.CHEST;
        chestDisplayName = colorize(config.getString("main-menu.chest.display-name", "&aChest Protection"));
        chestLore = new ArrayList<>();
        for (String line : config.getStringList("main-menu.chest.lore")) {
            chestLore.add(colorize(line));
        }
        chestGlow = config.getBoolean("main-menu.chest.glow", false);

        pvpSlot = config.getInt("main-menu.pvp.slot", 15);
        pvpMaterial = Material.matchMaterial(config.getString("main-menu.pvp.material", "DIAMOND_SWORD"));
        if (pvpMaterial == null) pvpMaterial = Material.DIAMOND_SWORD;
        pvpDisplayName = colorize(config.getString("main-menu.pvp.display-name", "&aPVP Protection"));
        pvpLore = new ArrayList<>();
        for (String line : config.getStringList("main-menu.pvp.lore")) {
            pvpLore.add(colorize(line));
        }
        pvpGlow = config.getBoolean("main-menu.pvp.glow", false);

        mobSlot = config.getInt("main-menu.mob.slot", 13);
        mobMaterial = Material.matchMaterial(config.getString("main-menu.mob.material", "ZOMBIE_HEAD"));
        if (mobMaterial == null) mobMaterial = Material.ZOMBIE_HEAD;
        mobDisplayName = colorize(config.getString("main-menu.mob.display-name", "&aMob Protection"));
        mobLore = new ArrayList<>();
        for (String line : config.getStringList("main-menu.mob.lore")) {
            mobLore.add(colorize(line));
        }
        mobGlow = config.getBoolean("main-menu.mob.glow", false);

        durationGuiTitle = colorize(config.getString("duration-gui.title", "&8{type}: {remaining}"));
        durationGuiSize = config.getInt("duration-gui.size", 27);
        fillerEnabled = config.getBoolean("duration-gui.filler.enabled", false);
        fillerMaterial = Material.matchMaterial(config.getString("duration-gui.filler.material", "BLACK_STAINED_GLASS_PANE"));
        if (fillerMaterial == null) fillerMaterial = Material.BLACK_STAINED_GLASS_PANE;
        fillerDisplayName = colorize(config.getString("duration-gui.filler.display-name", " "));
        durationItemsGlow = config.getBoolean("duration-gui.items-glow", false);

        knockbackStrength = config.getDouble("effects.knockback.strength", 1.5);
        knockbackVertical = config.getDouble("effects.knockback.vertical", 0.5);
        titleMain = colorize(config.getString("effects.title.main", "&c&lProtected Claim!"));
        titleSubtitle = colorize(config.getString("effects.title.subtitle", "&7You cannot enter this area"));
        titleFadeIn = config.getInt("effects.title.fade-in", 5);
        titleStay = config.getInt("effects.title.stay", 30);
        titleFadeOut = config.getInt("effects.title.fade-out", 10);

        String soundName = config.getString("effects.sound", "ENTITY_GENERIC_EXPLODE");
        try {
            sound = Sound.valueOf(soundName.toUpperCase());
        } catch (IllegalArgumentException e) {
            sound = Sound.ENTITY_GENERIC_EXPLODE;
        }
        soundVolume = (float) config.getDouble("effects.sound-volume", 0.5);
        soundPitch = (float) config.getDouble("effects.sound-pitch", 1.2);

        messageProtectionPurchased = colorize(config.getString("messages.protection-purchased", messageProtectionPurchased));
        messageProtectionExpired = colorize(config.getString("messages.protection-expired", messageProtectionExpired));
        messageProtectionActive = colorize(config.getString("messages.protection-active", messageProtectionActive));
        messageNoClaims = colorize(config.getString("messages.no-claims", messageNoClaims));
        messageNotEnoughMoney = colorize(config.getString("messages.not-enough-money", messageNotEnoughMoney));
        messageVaultNotFound = colorize(config.getString("messages.vault-not-found", messageVaultNotFound));
        messageChestAccessDenied = colorize(config.getString("messages.chest-access-denied", messageChestAccessDenied));
        messagePvpDenied = colorize(config.getString("messages.pvp-denied", messagePvpDenied));
        messageMobDenied = colorize(config.getString("messages.mob-denied", messageMobDenied));

        // Command messages
        messagePlayersOnly = colorize(config.getString("messages.players-only", messagePlayersOnly));
        messageUnknownSubcommand = colorize(config.getString("messages.unknown-subcommand", messageUnknownSubcommand));
        messageSystemUnavailable = colorize(config.getString("messages.system-unavailable", messageSystemUnavailable));
        messageNoActiveProtection = colorize(config.getString("messages.no-active-protection", messageNoActiveProtection));
        messageNoActiveProtectionHint = colorize(config.getString("messages.no-active-protection-hint", messageNoActiveProtectionHint));
        messageStatusHeader = colorize(config.getString("messages.status-header", messageStatusHeader));
        messageChestInactive = colorize(config.getString("messages.chest-inactive", messageChestInactive));
        messagePvpInactive = colorize(config.getString("messages.pvp-inactive", messagePvpInactive));
        messageMobInactive = colorize(config.getString("messages.mob-inactive", messageMobInactive));
        messageProtectedArea = colorize(config.getString("messages.protected-area", messageProtectedArea));
        messageInvalidProtectionType = colorize(config.getString("messages.invalid-protection-type", messageInvalidProtectionType));
        messageNoProtectionToCancel = colorize(config.getString("messages.no-protection-to-cancel", messageNoProtectionToCancel));
        messageProtectionCancelled = colorize(config.getString("messages.protection-cancelled", messageProtectionCancelled));
        messageNoActiveProtectionToCancel = colorize(config.getString("messages.no-active-protection-to-cancel", messageNoActiveProtectionToCancel));
        messageAllProtectionsCancelled = colorize(config.getString("messages.all-protections-cancelled", messageAllProtectionsCancelled));
        messageNoReloadPermission = colorize(config.getString("messages.no-reload-permission", messageNoReloadPermission));
        messageConfigReloaded = colorize(config.getString("messages.config-reloaded", messageConfigReloaded));

        plugin.getLogger().info("[TemporaryProtection] Loaded " + durationOptions.size() + " duration options.");
    }

    private String colorize(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public void reload() {
        config = YamlConfiguration.loadConfiguration(configFile);
        migrateConfig();
        loadSettings();
    }

    public double getMultiplier(ProtectionType type) {
        return switch (type) {
            case CHEST -> chestMultiplier;
            case PVP -> pvpMultiplier;
            case MOB -> mobMultiplier;
        };
    }

    public double getChestMultiplier() { return chestMultiplier; }
    public double getPvpMultiplier() { return pvpMultiplier; }

    public boolean isPvpFullImmunity() { return pvpFullImmunity; }

    public boolean isImmunityExempt(EntityDamageEvent.DamageCause cause) {
        return cause != null && pvpImmunityExemptCauses.contains(cause);
    }
    public double getMobMultiplier() { return mobMultiplier; }
    public Map<String, DurationOption> getDurationOptions() { return durationOptions; }

    public String getMainMenuTitle() { return mainMenuTitle; }
    public int getMainMenuSize() { return mainMenuSize; }
    public boolean isMainMenuFillerEnabled() { return mainMenuFillerEnabled; }
    public Material getMainMenuFillerMaterial() { return mainMenuFillerMaterial; }
    public String getMainMenuFillerDisplayName() { return mainMenuFillerDisplayName; }

    public int getChestSlot() { return chestSlot; }
    public Material getChestMaterial() { return chestMaterial; }
    public String getChestDisplayName() { return chestDisplayName; }
    public List<String> getChestLore() { return chestLore; }
    public boolean isChestGlow() { return chestGlow; }

    public int getPvpSlot() { return pvpSlot; }
    public Material getPvpMaterial() { return pvpMaterial; }
    public String getPvpDisplayName() { return pvpDisplayName; }
    public List<String> getPvpLore() { return pvpLore; }
    public boolean isPvpGlow() { return pvpGlow; }

    public int getMobSlot() { return mobSlot; }
    public Material getMobMaterial() { return mobMaterial; }
    public String getMobDisplayName() { return mobDisplayName; }
    public List<String> getMobLore() { return mobLore; }
    public boolean isMobGlow() { return mobGlow; }

    public String getDurationGuiTitle() { return durationGuiTitle; }
    public int getDurationGuiSize() { return durationGuiSize; }
    public boolean isFillerEnabled() { return fillerEnabled; }
    public Material getFillerMaterial() { return fillerMaterial; }
    public String getFillerDisplayName() { return fillerDisplayName; }
    public boolean isDurationItemsGlow() { return durationItemsGlow; }

    public double getKnockbackStrength() { return knockbackStrength; }
    public double getKnockbackVertical() { return knockbackVertical; }
    public String getTitleMain() { return titleMain; }
    public String getTitleSubtitle() { return titleSubtitle; }
    public int getTitleFadeIn() { return titleFadeIn; }
    public int getTitleStay() { return titleStay; }
    public int getTitleFadeOut() { return titleFadeOut; }
    public Sound getSound() { return sound; }
    public float getSoundVolume() { return soundVolume; }
    public float getSoundPitch() { return soundPitch; }

    public String getMessageProtectionPurchased() { return messageProtectionPurchased; }
    public String getMessageProtectionExpired() { return messageProtectionExpired; }
    public String getMessageProtectionActive() { return messageProtectionActive; }
    public String getMessageNoClaims() { return messageNoClaims; }
    public String getMessageNotEnoughMoney() { return messageNotEnoughMoney; }
    public String getMessageVaultNotFound() { return messageVaultNotFound; }
    public String getMessageChestAccessDenied() { return messageChestAccessDenied; }
    public String getMessagePvpDenied() { return messagePvpDenied; }
    public String getMessageMobDenied() { return messageMobDenied; }

    // Command message getters
    public String getMessagePlayersOnly() { return messagePlayersOnly; }
    public String getMessageUnknownSubcommand() { return messageUnknownSubcommand; }
    public String getMessageSystemUnavailable() { return messageSystemUnavailable; }
    public String getMessageNoActiveProtection() { return messageNoActiveProtection; }
    public String getMessageNoActiveProtectionHint() { return messageNoActiveProtectionHint; }
    public String getMessageStatusHeader() { return messageStatusHeader; }
    public String getMessageChestInactive() { return messageChestInactive; }
    public String getMessagePvpInactive() { return messagePvpInactive; }
    public String getMessageMobInactive() { return messageMobInactive; }
    public String getMessageProtectedArea() { return messageProtectedArea; }
    public String getMessageInvalidProtectionType() { return messageInvalidProtectionType; }
    public String getMessageNoProtectionToCancel() { return messageNoProtectionToCancel; }
    public String getMessageProtectionCancelled() { return messageProtectionCancelled; }
    public String getMessageNoActiveProtectionToCancel() { return messageNoActiveProtectionToCancel; }
    public String getMessageAllProtectionsCancelled() { return messageAllProtectionsCancelled; }
    public String getMessageNoReloadPermission() { return messageNoReloadPermission; }
    public String getMessageConfigReloaded() { return messageConfigReloaded; }

    public static class DurationOption {
        private final String id;
        private final int hours;
        private final int slot;
        private final Material material;
        private final String displayName;
        private final List<String> lore;

        public DurationOption(String id, int hours, int slot, Material material, String displayName, List<String> lore) {
            this.id = id;
            this.hours = hours;
            this.slot = slot;
            this.material = material;
            this.displayName = displayName;
            this.lore = lore;
        }

        public String getId() { return id; }
        public int getHours() { return hours; }
        public long getMillis() { return hours * 60L * 60L * 1000L; }
        public int getSlot() { return slot; }
        public Material getMaterial() { return material; }
        public String getDisplayName() { return displayName; }
        public List<String> getLore() { return lore; }

        public String getFormattedDuration() {
            if (hours >= 24) {
                int days = hours / 24;
                return days + " day" + (days > 1 ? "s" : "");
            }
            return hours + " hour" + (hours > 1 ? "s" : "");
        }
    }
}
