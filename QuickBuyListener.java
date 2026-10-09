package devs.donutShop.gui;

import devs.donutShop.DonutShop;
import devs.donutShop.data.ShopDataManager;
import devs.donutShop.dialog.ShopDialogManager;
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
        if (!(e.getInventory().getHolder() instanceof QuickBuyGUI gui)) return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getInventory()) return;
        if (!(e.getWhoClicked() instanceof Player p)) return;

        int slot = e.getRawSlot();
        ShopDialogManager dialogs = plugin.getDialogManager();

        switch (slot) {
            case QuickBuyGUI.SLOT_FILTER -> gui.cycleFilter();
            case QuickBuyGUI.SLOT_REFRESH -> gui.refresh();
            case QuickBuyGUI.SLOT_AUCTION ->
                    runCommandLater(p, plugin.getConfig().getString("auction-command", "ah"), false);
            case QuickBuyGUI.SLOT_YOUR_ITEMS ->
                    runCommandLater(p, plugin.getConfig().getString("your-items-command", "ah"), false);
            case QuickBuyGUI.SLOT_SEARCH ->
                    Bukkit.getScheduler().runTask(plugin, () -> dialogs.openSearchDialog(p));
            case QuickBuyGUI.SLOT_EDIT -> gui.toggleEditMode();
            default -> {
                if (slot < 0 || slot >= ShopDataManager.SLOTS) return;
                int layoutSlot = gui.layoutSlotAt(slot);
                if (layoutSlot < 0) return;

                ItemStack[] layout = plugin.getDataManager().getLayout(p);
                ItemStack stored = layout[layoutSlot];
                boolean empty = stored == null || stored.getType().isAir();

                if (gui.isEditMode()) {
                    if (empty) {
                        Bukkit.getScheduler().runTask(plugin, () -> dialogs.openChooseItemDialog(p, layoutSlot, ""));
                    } else {
                        plugin.getDataManager().removeItem(p, layoutSlot);
                        gui.refresh();
                    }
                } else if (!empty) {
                    String cmd = plugin.getConfig().getString("buy-command", "ah {item}")
                            .replace("{item}", ShopDialogManager.formatItemName(stored.getType().name()))
                            .replace("{material}", stored.getType().name())
                            .replace("{amount}", String.valueOf(stored.getAmount()))
                            .replace("{player}", p.getName());
                    runCommandLater(p, cmd, plugin.getConfig().getBoolean("buy-command-as-console", false));
                }
            }
        }
    }

    private void runCommandLater(Player p, String command, boolean console) {
        String cmd = command.startsWith("/") ? command.substring(1) : command;
        Bukkit.getScheduler().runTask(plugin, () -> {
            p.closeInventory();
            if (console) Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            else p.performCommand(cmd);
        });
    }
}
