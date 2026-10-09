package devs.donutShop.gui;

import devs.donutShop.DonutShop;
import devs.donutShop.data.ShopDataManager;
import devs.donutShop.dialog.ShopDialogManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class QuickBuyGUI implements InventoryHolder {

    public static final int SIZE = 54;
    public static final int SLOT_FILTER = 47;
    public static final int SLOT_REFRESH = 48;
    public static final int SLOT_AUCTION = 49;
    public static final int SLOT_SEARCH = 50;
    public static final int SLOT_YOUR_ITEMS = 51;
    public static final int SLOT_EDIT = 53;

    private final Player player;
    private final Inventory inventory;
    private ShopFilterType filterType;
    private boolean editMode;
    /** slot GUI -> slot layout (-1 kalau kosong) */
    private final int[] slotMap = new int[ShopDataManager.SLOTS];

    public QuickBuyGUI(Player player) {
        this(player, ShopFilterType.DEFAULT);
    }

    public QuickBuyGUI(Player player, ShopFilterType filterType) {
        this.player = player;
        this.filterType = filterType;
        this.inventory = Bukkit.createInventory(this, SIZE, "Quick Buy");
        render();
    }

    public static void open(Player player) {
        open(player, false);
    }

    public static void open(Player player, boolean edit) {
        QuickBuyGUI gui = new QuickBuyGUI(player);
        gui.editMode = edit;
        gui.render();
        player.openInventory(gui.getInventory());
    }

    // ------------------------------------------------------------ render

    public void render() {
        inventory.clear();
        Arrays.fill(slotMap, -1);

        DonutShop plugin = DonutShop.getInstance();
        ItemStack[] layout = plugin.getDataManager().getLayout(player);

        if (editMode) {
            ItemStack emptySlot = createItem(Material.GRAY_STAINED_GLASS_PANE, "§fEmpty",
                    Arrays.asList("§f§oClick to choose", "§f§oan item to buy"));
            for (int i = 0; i < ShopDataManager.SLOTS; i++) {
                ItemStack it = layout[i];
                if (it == null || it.getType().isAir()) {
                    inventory.setItem(i, emptySlot.clone());
                } else {
                    ItemStack display = it.clone();
                    ItemMeta meta = display.getItemMeta();
                    if (meta != null) {
                        List<String> lore = meta.hasLore() && meta.getLore() != null
                                ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                        lore.add("");
                        lore.add("§cClick to remove from Quick Buy");
                        meta.setLore(lore);
                        display.setItemMeta(meta);
                    }
                    inventory.setItem(i, display);
                }
                slotMap[i] = i;
            }
        } else {
            List<Integer> filled = new ArrayList<>();
            for (int i = 0; i < ShopDataManager.SLOTS; i++) {
                if (layout[i] != null && !layout[i].getType().isAir()) filled.add(i);
            }

            switch (filterType) {
                case CHEAPEST -> filled.sort((a, b) -> comparePrice(layout, a, b, true));
                case MOST_EXPENSIVE -> filled.sort((a, b) -> comparePrice(layout, a, b, false));
                case NAME -> filled.sort(Comparator.comparing(i ->
                        ShopDialogManager.formatItemName(layout[i].getType().name()).toLowerCase()));
                default -> { }
            }

            for (int n = 0; n < filled.size(); n++) {
                int layoutSlot = filled.get(n);
                int guiSlot = filterType == ShopFilterType.DEFAULT ? layoutSlot : n;
                ItemStack display = layout[layoutSlot].clone();
                ItemMeta meta = display.getItemMeta();
                if (meta != null) {
                    List<String> lore = meta.hasLore() && meta.getLore() != null
                            ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                    Double price = plugin.getDataManager().getPrice(display.getType());
                    lore.add("");
                    if (price != null) lore.add("§7Price: §a" + formatPrice(price) + " §7each");
                    lore.add("§eClick to buy");
                    meta.setLore(lore);
                    display.setItemMeta(meta);
                }
                inventory.setItem(guiSlot, display);
                slotMap[guiSlot] = layoutSlot;
            }
        }

        buildBottomBar();
    }

    private int comparePrice(ItemStack[] layout, int a, int b, boolean ascending) {
        DonutShop plugin = DonutShop.getInstance();
        Double pa = plugin.getDataManager().getPrice(layout[a].getType());
        Double pb = plugin.getDataManager().getPrice(layout[b].getType());
        if (pa == null && pb == null) return 0;
        if (pa == null) return 1;   // tanpa harga selalu di akhir
        if (pb == null) return -1;
        return ascending ? Double.compare(pa, pb) : Double.compare(pb, pa);
    }

    private String formatPrice(double v) {
        return "$" + (v % 1 == 0 ? String.format("%,.0f", v) : String.format("%,.2f", v));
    }

    private void buildBottomBar() {
        List<String> filterLore = new ArrayList<>();
        filterLore.add("§f§oClick to change");
        for (ShopFilterType type : ShopFilterType.values()) {
            filterLore.add(type == filterType ? "§f▪ " + type.getDisplayName() : "§8▪ " + type.getDisplayName());
        }
        inventory.setItem(SLOT_FILTER, createItem(Material.HOPPER, "§fFilter", filterLore));

        inventory.setItem(SLOT_REFRESH, createItem(Material.ENDER_CHEST, "§fQuick Buy",
                Collections.singletonList("§f§oClick to refresh prices")));
        inventory.setItem(SLOT_AUCTION, createItem(Material.ANVIL, "§fAuction",
                Collections.singletonList("§f§oBuy and sell items")));
        inventory.setItem(SLOT_SEARCH, createItem(Material.OAK_SIGN, "§fSearch",
                Collections.singletonList("§f§oClick to search")));
        inventory.setItem(SLOT_YOUR_ITEMS, createItem(Material.CHEST, "§fYour Items",
                Collections.singletonList("§f§oClick to view")));

        if (editMode) {
            inventory.setItem(SLOT_EDIT, createItem(Material.DEBUG_STICK, "§aDone Editing",
                    Collections.singletonList("§f§oClick to exit edit mode")));
        } else {
            inventory.setItem(SLOT_EDIT, createItem(Material.DEBUG_STICK, "§fEdit",
                    Collections.singletonList("§f§oClick to edit")));
        }
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.values());
        item.setItemMeta(meta);
        return item;
    }

    // ------------------------------------------------------------- state

    /** Slot layout (0-44) untuk slot GUI yang diklik, atau -1. */
    public int layoutSlotAt(int guiSlot) {
        return guiSlot >= 0 && guiSlot < slotMap.length ? slotMap[guiSlot] : -1;
    }

    public void toggleEditMode() {
        editMode = !editMode;
        render();
    }

    public boolean isEditMode() {
        return editMode;
    }

    public void cycleFilter() {
        filterType = filterType.next();
        render();
    }

    public void refresh() {
        render();
    }

    public ShopFilterType getFilterType() {
        return filterType;
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
