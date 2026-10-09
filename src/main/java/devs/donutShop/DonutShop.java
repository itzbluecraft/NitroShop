package devs.donutShop;

import devs.donutShop.command.ShopCommand;
import devs.donutShop.data.ShopDataManager;
import devs.donutShop.dialog.ShopDialogManager;
import devs.donutShop.gui.QuickBuyListener;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.RegisteredServiceProvider;
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

        // Cek economy setelah semua plugin selesai dimuat
        getServer().getScheduler().runTask(this, () -> {
            if (getEconomy() == null) {
                getLogger().warning("Vault/economy tidak ditemukan. Item di Shop akan GRATIS sampai economy terpasang.");
            }
        });
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

    /** Economy Vault, atau null kalau tidak ada. Dicari ulang tiap dipanggil. */
    public Economy getEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return null;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        return rsp == null ? null : rsp.getProvider();
    }

    public String formatPrice(double v) {
        String sym = getConfig().getString("currency-symbol", "$");
        return sym + (v % 1 == 0 ? String.format("%,.0f", v) : String.format("%,.2f", v));
    }
    }
