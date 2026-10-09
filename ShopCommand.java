package devs.donutShop.command;

import devs.donutShop.DonutShop;
import devs.donutShop.data.ShopDataManager;
import devs.donutShop.dialog.ShopDialogManager;
import devs.donutShop.gui.QuickBuyGUI;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * /shop            -> buka Quick Buy
 * /shop __xxx ...  -> sub-command internal yang dipanggil tombol dialog
 */
public class ShopCommand implements CommandExecutor, TabCompleter {

    private final DonutShop plugin;

    public ShopCommand(DonutShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Hanya untuk pemain.");
            return true;
        }
        ShopDialogManager dialogs = plugin.getDialogManager();

        if (args.length == 0) {
            QuickBuyGUI.open(p);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "__chooseitem" -> {
                Integer slot = slot(args, 1);
                if (slot == null) return true;
                String query = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "";
                dialogs.openChooseItemDialog(p, slot, query);
            }
            case "__selectitem" -> {
                Integer slot = slot(args, 1);
                Material m = material(args, 2);
                if (slot == null || m == null) return true;
                dialogs.selectItem(p, slot, m);
            }
            case "__toggleench" -> {
                Integer slot = slot(args, 1);
                Material m = material(args, 2);
                if (slot == null || m == null || args.length < 4) return true;
                dialogs.toggleEnchant(p, slot, m, args[3].toLowerCase());
            }
            case "__skipench" -> {
                Integer slot = slot(args, 1);
                Material m = material(args, 2);
                if (slot == null || m == null) return true;
                dialogs.skipEnchants(p, slot, m);
            }
            case "__continueench" -> {
                Integer slot = slot(args, 1);
                Material m = material(args, 2);
                if (slot == null || m == null) return true;
                dialogs.continueEnchants(p, slot, m);
            }
            case "__setamount" -> {
                Integer slot = slot(args, 1);
                if (slot == null || args.length < 3) return true;
                dialogs.setAmount(p, slot, args[2]);
            }
            case "__edit" -> QuickBuyGUI.open(p, true);
            default -> QuickBuyGUI.open(p);
        }
        return true;
    }

    private Integer slot(String[] args, int idx) {
        if (args.length <= idx) return null;
        try {
            int s = Integer.parseInt(args[idx]);
            return s >= 0 && s < ShopDataManager.SLOTS ? s : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Material material(String[] args, int idx) {
        if (args.length <= idx) return null;
        Material m = Material.matchMaterial(args[idx]);
        return m != null && m.isItem() && !m.isAir() ? m : null;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String alias, String[] args) {
        return Collections.emptyList();
    }
}
