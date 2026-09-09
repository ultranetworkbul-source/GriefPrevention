package com.griefprevention.protection.temporary;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TemporaryProtectionGUI implements Listener {

    private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("#,##0");

    private final GriefPrevention plugin;
    private Economy economy;
    private final Map<UUID, ProtectionType> selectedType = new HashMap<>();
    private final Map<Integer, String> slotToDurationId = new HashMap<>();

    public TemporaryProtectionGUI(GriefPrevention plugin) {
        this.plugin = plugin;
        setupEconomy();
    }

    private boolean setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    public boolean isEconomyAvailable() {
        return economy != null;
    }

    public void openMainMenu(Player player) {
        TemporaryProtectionConfig config = TemporaryProtectionConfig.getInstance();
        if (config == null) return;

        if (!isEconomyAvailable()) {
            player.sendMessage(config.getMessageVaultNotFound());
            return;
        }

        TemporaryProtectionManager manager = TemporaryProtectionManager.getInstance();
        long totalArea = manager.calculateTotalClaimArea(player.getUniqueId());
        if (totalArea == 0) {
            player.sendMessage(config.getMessageNoClaims());
            return;
        }

        selectedType.remove(player.getUniqueId());

        Inventory inventory = Bukkit.createInventory(new MainMenuHolder(), config.getMainMenuSize(), config.getMainMenuTitle());

        if (config.isMainMenuFillerEnabled()) {
            ItemStack filler = createItem(config.getMainMenuFillerMaterial(), config.getMainMenuFillerDisplayName(), null);
            for (int i = 0; i < inventory.getSize(); i++) {
                inventory.setItem(i, filler);
            }
        }

        TemporaryProtection chestProtection = manager.getProtection(player.getUniqueId(), ProtectionType.CHEST);
        TemporaryProtection pvpProtection = manager.getProtection(player.getUniqueId(), ProtectionType.PVP);
        TemporaryProtection mobProtection = manager.getProtection(player.getUniqueId(), ProtectionType.MOB);

        String chestStatus = (chestProtection != null && chestProtection.isActive()) ? "&aActive" : "&cInactive";
        String chestRemaining = (chestProtection != null && chestProtection.isActive()) ? chestProtection.getFormattedRemaining() : "None";

        String pvpStatus = (pvpProtection != null && pvpProtection.isActive()) ? "&aActive" : "&cInactive";
        String pvpRemaining = (pvpProtection != null && pvpProtection.isActive()) ? pvpProtection.getFormattedRemaining() : "None";

        String mobStatus = (mobProtection != null && mobProtection.isActive()) ? "&aActive" : "&cInactive";
        String mobRemaining = (mobProtection != null && mobProtection.isActive()) ? mobProtection.getFormattedRemaining() : "None";

        List<String> chestLore = new ArrayList<>();
        for (String line : config.getChestLore()) {
            chestLore.add(colorize(line.replace("{status}", chestStatus).replace("{remaining}", chestRemaining)));
        }
        inventory.setItem(config.getChestSlot(), createItem(config.getChestMaterial(), config.getChestDisplayName(), chestLore, config.isChestGlow()));

        List<String> pvpLore = new ArrayList<>();
        for (String line : config.getPvpLore()) {
            pvpLore.add(colorize(line.replace("{status}", pvpStatus).replace("{remaining}", pvpRemaining)));
        }
        inventory.setItem(config.getPvpSlot(), createItem(config.getPvpMaterial(), config.getPvpDisplayName(), pvpLore, config.isPvpGlow()));

        List<String> mobLore = new ArrayList<>();
        for (String line : config.getMobLore()) {
            mobLore.add(colorize(line.replace("{status}", mobStatus).replace("{remaining}", mobRemaining)));
        }
        inventory.setItem(config.getMobSlot(), createItem(config.getMobMaterial(), config.getMobDisplayName(), mobLore, config.isMobGlow()));

        player.openInventory(inventory);
    }

    public void openDurationGUI(Player player, ProtectionType type) {
        TemporaryProtectionConfig config = TemporaryProtectionConfig.getInstance();
        if (config == null) return;

        TemporaryProtectionManager manager = TemporaryProtectionManager.getInstance();
        long totalArea = manager.calculateTotalClaimArea(player.getUniqueId());

        TemporaryProtection existing = manager.getProtection(player.getUniqueId(), type);
        String remainingTime = (existing != null && existing.isActive()) ? existing.getFormattedRemaining() : "None";

        String title = config.getDurationGuiTitle()
                .replace("{type}", type.getDisplayName())
                .replace("{remaining}", remainingTime);
        Inventory inventory = Bukkit.createInventory(new DurationMenuHolder(type), config.getDurationGuiSize(), title);

        if (config.isFillerEnabled()) {
            ItemStack filler = createItem(config.getFillerMaterial(), config.getFillerDisplayName(), null);
            for (int i = 0; i < inventory.getSize(); i++) {
                inventory.setItem(i, filler);
            }
        }

        slotToDurationId.clear();
        double multiplier = config.getMultiplier(type);

        for (Map.Entry<String, TemporaryProtectionConfig.DurationOption> entry : config.getDurationOptions().entrySet()) {
            TemporaryProtectionConfig.DurationOption option = entry.getValue();
            double price = calculatePrice(totalArea, option.getHours(), multiplier);

            List<String> lore = new ArrayList<>();
            for (String line : option.getLore()) {
                lore.add(line.replace("{price}", PRICE_FORMAT.format(price)));
            }

            inventory.setItem(option.getSlot(), createItem(option.getMaterial(), option.getDisplayName(), lore, config.isDurationItemsGlow()));
            slotToDurationId.put(option.getSlot(), option.getId());
        }

        inventory.setItem(0, createItem(Material.ARROW, colorize("&cBack"), List.of(colorize("&7Return to main menu"))));
        selectedType.put(player.getUniqueId(), type);
        player.openInventory(inventory);
    }

    private String colorize(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private ItemStack createItem(Material material, String displayName, List<String> lore) {
        return createItem(material, displayName, lore, false);
    }

    private ItemStack createItem(Material material, String displayName, List<String> lore, boolean glow) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            if (lore != null) {
                meta.setLore(lore);
            }
            if (glow) {
                meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public double calculatePrice(long totalBlocks, int hours, double multiplier) {
        return Math.round(totalBlocks * (hours / 24.0) * multiplier);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof MainMenuHolder) {
            event.setCancelled(true);
            handleMainMenuClick(player, event.getRawSlot());
            return;
        }

        if (holder instanceof DurationMenuHolder durationHolder) {
            event.setCancelled(true);
            handleDurationMenuClick(player, event.getRawSlot(), durationHolder.getType());
        }
    }

    private void handleMainMenuClick(Player player, int slot) {
        TemporaryProtectionConfig config = TemporaryProtectionConfig.getInstance();
        if (config == null) return;

        if (slot == config.getChestSlot()) {
            openDurationGUI(player, ProtectionType.CHEST);
        } else if (slot == config.getPvpSlot()) {
            openDurationGUI(player, ProtectionType.PVP);
        } else if (slot == config.getMobSlot()) {
            openDurationGUI(player, ProtectionType.MOB);
        }
    }

    private void handleDurationMenuClick(Player player, int slot, ProtectionType type) {
        TemporaryProtectionConfig config = TemporaryProtectionConfig.getInstance();
        if (config == null) return;

        if (slot == 0) {
            openMainMenu(player);
            return;
        }

        String durationId = slotToDurationId.get(slot);
        if (durationId == null) return;

        TemporaryProtectionConfig.DurationOption option = config.getDurationOptions().get(durationId);
        if (option == null) return;

        TemporaryProtectionManager manager = TemporaryProtectionManager.getInstance();
        long totalArea = manager.calculateTotalClaimArea(player.getUniqueId());
        double price = calculatePrice(totalArea, option.getHours(), config.getMultiplier(type));

        if (!economy.has(player, price)) {
            player.sendMessage(config.getMessageNotEnoughMoney().replace("{price}", PRICE_FORMAT.format(price)));
            return;
        }

        economy.withdrawPlayer(player, price);
        manager.extendProtection(player.getUniqueId(), type, option.getMillis());

        player.sendMessage(config.getMessageProtectionPurchased()
                .replace("{duration}", option.getFormattedDuration())
                .replace("{type}", type.getDisplayName()));

        player.closeInventory();
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                InventoryHolder holder = player.getOpenInventory().getTopInventory().getHolder();
                if (!(holder instanceof MainMenuHolder) && !(holder instanceof DurationMenuHolder)) {
                    selectedType.remove(player.getUniqueId());
                }
            }, 1L);
        }
    }

    private static class MainMenuHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() { return null; }
    }

    private static class DurationMenuHolder implements InventoryHolder {
        private final ProtectionType type;

        public DurationMenuHolder(ProtectionType type) {
            this.type = type;
        }

        public ProtectionType getType() { return type; }

        @Override
        public Inventory getInventory() { return null; }
    }
}
