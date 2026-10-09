package devs.donutShop;

import devs.donutShop.command.ShopCommand;
import devs.donutShop.data.ShopDataManager;
import devs.donutShop.dialog.ShopDialogManager;
import devs.donutShop.gui.QuickBuyListener;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class DonutShop extends JavaPlugin {

    private static DonutShop instance;
    private ShopDataManager dataManager;
    private ShopDialogManager dialogManager;

    public static DonutShop getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        dataManager = new ShopDataManager(this);
        dialogManager = new ShopDialogManager(this);

        getServer().getPluginManager().registerEvents(new QuickBuyListener(this), this);

        PluginCommand cmd = getCommand("shop");
        if (cmd != null) {
            ShopCommand executor = new ShopCommand(this);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }
    }

    @Override
    public void onDisable() {
        if (dataManager != null) dataManager.save();
    }

    public ShopDataManager getDataManager() {
        return dataManager;
    }

    public ShopDialogManager getDialogManager() {
        return dialogManager;
    }
}
