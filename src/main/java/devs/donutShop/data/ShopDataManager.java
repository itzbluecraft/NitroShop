package devs.donutShop.data;

import devs.donutShop.DonutShop;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;

/** Menyimpan isi Shop (54 slot, sama untuk semua pemain) di data.yml. */
public class ShopDataManager {

    public static final int SLOTS = 54;

    private final DonutShop plugin;
    private final File file;
    private final YamlConfiguration yaml;

    public ShopDataManager(DonutShop plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
        plugin.getDataFolder().mkdirs();
        this.yaml = YamlConfiguration.loadConfiguration(file);
        seedDefaults();
    }

    /** Isi awal Shop (hanya kalau Shop masih belum pernah dibuat). Admin bisa mengubahnya lewat menu. */
    private void seedDefaults() {
        if (yaml.contains("shop")) return;
        Object[][] items = {
                {10, Material.DIAMOND_HELMET, 1}, {11, Material.DIAMOND_CHESTPLATE, 1},
                {12, Material.DIAMOND_LEGGINGS, 1}, {13, Material.DIAMOND_BOOTS, 1},
                {14, Material.DIAMOND_SWORD, 1}, {15, Material.DIAMOND_PICKAXE, 1},
                {16, Material.DIAMOND_AXE, 1},
                {19, Material.OBSIDIAN, 64}, {20, Material.END_CRYSTAL, 64},
                {21, Material.CRYING_OBSIDIAN, 64}, {22, Material.TOTEM_OF_UNDYING, 1},
                {23, Material.ENDER_PEARL, 16},
                {28, Material.COBWEB, 64}, {29, Material.GOLDEN_APPLE, 64},
                {30, Material.EXPERIENCE_BOTTLE, 64}
        };
        for (Object[] it : items) {
            yaml.set("shop." + it[0], new ItemStack((Material) it[1], (Integer) it[2]));
        }
        save();
    }

    /** Isi Shop. Parameter player dipertahankan agar kode lain tetap cocok (Shop sama untuk semua). */
    public ItemStack[] getLayout(Player player) {
        ItemStack[] layout = new ItemStack[SLOTS];
        ConfigurationSection sec = yaml.getConfigurationSection("shop");
        if (sec == null) return layout;
        for (String key : sec.getKeys(false)) {
            int i;
            try {
                i = Integer.parseInt(key);
            } catch (NumberFormatException ex) {
                continue;
            }
            if (i < 0 || i >= SLOTS) continue;
            ItemStack it = sec.getItemStack(key);
            if (it != null && !it.getType().isAir()) layout[i] = it.clone();
        }
        return layout;
    }

    public void setItem(Player player, int slot, ItemStack item) {
        yaml.set("shop." + slot, item);
        save();
    }

    public void removeItem(Player player, int slot) {
        // set kosong lalu pastikan section "shop" tetap ada supaya isi awal tidak muncul lagi
        yaml.set("shop." + slot, null);
        if (!yaml.contains("shop")) yaml.createSection("shop");
        save();
    }

    /** Harga per item dari config, atau null kalau tidak diatur. */
    public Double getPrice(Material material) {
        String path = "prices." + material.name();
        return plugin.getConfig().contains(path) ? plugin.getConfig().getDouble(path) : null;
    }

    /** Harga per 1 item, termasuk tambahan dari enchant. */
    public double getUnitPrice(ItemStack item) {
        String path = "prices." + item.getType().name();
        double base = plugin.getConfig().contains(path)
                ? plugin.getConfig().getDouble(path)
                : plugin.getConfig().getDouble("default-price", 100);
        double perLevel = plugin.getConfig().getDouble("enchant-price-per-level", 500);
        int levels = 0;
        for (int lvl : item.getEnchantments().values()) levels += lvl;
        return base + levels * perLevel;
    }

    /** Harga total untuk seluruh jumlah di slot. */
    public double getTotalPrice(ItemStack item) {
        return getUnitPrice(item) * item.getAmount();
    }

    public void save() {
        try {
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Gagal menyimpan data.yml: " + ex.getMessage());
        }
    }
}
