package devs.donutShop.gui;

import devs.donutShop.DonutShop;
import devs.donutShop.data.ShopDataManager;
import devs.donutShop.dialog.ShopDialogManager;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

public class QuickBuyListener implements Listener {

    private final DonutShop plugin;

    public QuickBuyListener(DonutShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof QuickBuyGUI) e.setCancelled(true);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof QuickBuyGUI)) return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getInventory()) return;
        if (!(e.getWhoClicked() instanceof Player p)) return;

        int slot = e.getRawSlot();
        if (slot < 0 || slot >= ShopDataManager.SLOTS) return;

        ShopDialogManager dialogs = plugin.getDialogManager();
        ItemStack stored = plugin.getDataManager().getLayout(p)[slot];
        boolean empty = stored == null || stored.getType().isAir();

        boolean admin = p.hasPermission("donutshop.admin");

        // Empty slot: admins can add a new item
        if (empty) {
            if (!admin) return;
            Bukkit.getScheduler().runTask(plugin, () -> dialogs.openChooseItemDialog(p, slot, ""));
            return;
        }

        // Right-click (admin): edit item (enchantments, amount, or delete)
        if (admin && e.isRightClick()) {
            Bukkit.getScheduler().runTask(plugin, () -> dialogs.editItem(p, slot, stored));
            return;
        }

        // Buy item
        ItemStack give = stored.clone();
        if (!canFit(p, give)) {
            p.sendMessage("§cYour inventory is full.");
            return;
        }

        double price = plugin.getDataManager().getTotalPrice(stored);
        Economy eco = plugin.getEconomy();
        if (eco != null && price > 0) {
            if (!eco.has(p, price)) {
                p.sendMessage("§cNot enough money. You need §f" + plugin.formatPrice(price) + "§c.");
                return;
            }
            EconomyResponse r = eco.withdrawPlayer(p, price);
            if (!r.transactionSuccess()) {
                p.sendMessage("§cPayment failed.");
                return;
            }
        }

        p.getInventory().addItem(give);
        String name = ShopDialogManager.formatItemName(give.getType().name());
        if (eco != null && price > 0) {
            p.sendMessage("§aYou bought §f" + give.getAmount() + "x " + name
                    + " §afor §f" + plugin.formatPrice(price) + "§a.");
        } else {
            p.sendMessage("§aYou received §f" + give.getAmount() + "x " + name + "§a.");
        }
    }

    private boolean canFit(Player p, ItemStack item) {
        int need = item.getAmount();
        int max = item.getMaxStackSize();
        int space = 0;
        for (ItemStack c : p.getInventory().getStorageContents()) {
            if (c == null || c.getType().isAir()) space += max;
            else if (c.isSimilar(item)) space += Math.max(0, max - c.getAmount());
            if (space >= need) return true;
        }
        return false;
    }
}
