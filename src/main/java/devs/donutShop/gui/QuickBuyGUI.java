package devs.donutShop.gui;

import devs.donutShop.DonutShop;
import devs.donutShop.data.ShopDataManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/** Menu "Shop" sederhana: 54 slot berisi item milik pemain, tanpa tombol. */
public class QuickBuyGUI implements InventoryHolder {

    public static final int SIZE = ShopDataManager.SLOTS; // 54

    private final Player player;
    private final Inventory inventory;

    public QuickBuyGUI(Player player) {
        this.player = player;
        this.inventory = Bukkit.createInventory(this, SIZE, "Shop");
        render();
    }

    public static void open(Player player) {
        QuickBuyGUI gui = new QuickBuyGUI(player);
        player.openInventory(gui.getInventory());
    }

    /** Dipertahankan agar kode lain tetap cocok; parameter mode edit sudah tidak dipakai. */
    public static void open(Player player, boolean ignored) {
        open(player);
    }

    public void render() {
        inventory.clear();
        ItemStack[] layout = DonutShop.getInstance().getDataManager().getLayout(player);
        for (int i = 0; i < SIZE; i++) {
            if (layout[i] != null && !layout[i].getType().isAir()) {
                ItemStack display = layout[i].clone();
                ItemMeta meta = display.getItemMeta();
                if (meta != null) {
                    List<String> lore = meta.hasLore() && meta.getLore() != null
                            ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                    double price = DonutShop.getInstance().getDataManager().getTotalPrice(layout[i]);
                    lore.add("");
                    lore.add("§7Price: §a" + DonutShop.getInstance().formatPrice(price));
                    lore.add("§eLeft-click: buy");
                    lore.add("§7Right-click: edit");
                    meta.setLore(lore);
                    display.setItemMeta(meta);
                }
                inventory.setItem(i, display);
            }
        }
    }

    public void refresh() {
        render();
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
