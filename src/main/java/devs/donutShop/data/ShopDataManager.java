package devs.donutShop.data;

import devs.donutShop.DonutShop;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;

/** Menyimpan layout Quick Buy per pemain (45 slot) di data.yml. */
public class ShopDataManager {

    public static final int SLOTS = 45;

    private final DonutShop plugin;
    private final File file;
    private final YamlConfiguration yaml;

    public ShopDataManager(DonutShop plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
        plugin.getDataFolder().mkdirs();
        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    public ItemStack[] getLayout(Player player) {
        ItemStack[] layout = new ItemStack[SLOTS];
        ConfigurationSection sec = yaml.getConfigurationSection("players." + player.getUniqueId());
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
        yaml.set("players." + player.getUniqueId() + "." + slot, item);
        save();
    }

    public void removeItem(Player player, int slot) {
        yaml.set("players." + player.getUniqueId() + "." + slot, null);
        save();
    }

    /** Harga per item dari config, atau null kalau tidak diatur. */
    public Double getPrice(Material material) {
        String path = "prices." + material.name();
        return plugin.getConfig().contains(path) ? plugin.getConfig().getDouble(path) : null;
    }

    public void save() {
        try {
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Gagal menyimpan data.yml: " + ex.getMessage());
        }
    }
}
