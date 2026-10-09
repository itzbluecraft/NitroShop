package devs.donutShop.dialog;

import devs.donutShop.DonutShop;
import devs.donutShop.gui.QuickBuyGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Dialog native Minecraft (butuh Paper 1.21.6+) untuk memilih item, enchantment dan jumlah.
 * Dikirim lewat console: "dialog show <player> <json>".
 */
public class ShopDialogManager {

    public record SpriteInfo(String atlas, String sprite) {
    }

    /** State pilihan item yang sedang dibuat pemain. */
    private static class Pending {
        int slot;
        Material material;
        final Map<String, Integer> enchants = new LinkedHashMap<>();
        ItemStack item;
    }

    private static final int MAX_RESULTS = 100;

    private static final Set<String> BLOCKED = Set.of(
            "BARRIER", "BEDROCK", "COMMAND_BLOCK", "CHAIN_COMMAND_BLOCK", "REPEATING_COMMAND_BLOCK",
            "COMMAND_BLOCK_MINECART", "STRUCTURE_BLOCK", "STRUCTURE_VOID", "JIGSAW", "DEBUG_STICK",
            "KNOWLEDGE_BOOK", "LIGHT", "SPAWNER", "TRIAL_SPAWNER", "VAULT", "REINFORCED_DEEPSLATE",
            "END_PORTAL_FRAME", "BUDDING_AMETHYST", "FARMLAND", "DIRT_PATH", "CHORUS_PLANT",
            "PETRIFIED_OAK_SLAB", "TEST_BLOCK", "TEST_INSTANCE_BLOCK");

    private static final Map<String, String> NAME_OVERRIDES = Map.of(
            "BAMBOO_CHEST_RAFT", "Bamboo Raft with Chest",
            "AMETHYST_BLOCK", "Block of Amethyst",
            "BAMBOO_BLOCK", "Block of Bamboo",
            "BEEF", "Raw Beef");

    private static final String[] WOODS = {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
            "mangrove", "cherry", "bamboo", "crimson", "warped", "pale_oak"};

    public static final List<Material> ALL_ITEMS = buildAllItems();

    private final DonutShop plugin;
    private final Map<UUID, Pending> pending = new HashMap<>();

    public ShopDialogManager(DonutShop plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------ helpers

    private String cmd(String rest) {
        return plugin.getConfig().getString("dialog-command-prefix", "/") + rest;
    }

    private void show(Player p, String json) {
        p.closeInventory();
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dialog show " + p.getName() + " " + json);
    }

    private static List<Material> buildAllItems() {
        List<Material> list = new ArrayList<>();
        for (Material m : Material.values()) {
            if (m.name().startsWith("LEGACY_")) continue;
            if (!m.isItem() || m.isAir()) continue;
            if (BLOCKED.contains(m.name()) || m.name().endsWith("_SPAWN_EGG")) continue;
            list.add(m);
        }
        list.sort(Comparator.comparing(m -> formatItemName(m.name())));
        return list;
    }

    public static String formatItemName(String materialName) {
        String up = materialName.toUpperCase(Locale.ROOT);
        if (NAME_OVERRIDES.containsKey(up)) return NAME_OVERRIDES.get(up);
        if (up.endsWith("_CHEST_BOAT")) return formatItemName(up.replace("_CHEST_BOAT", "_BOAT")) + " with Chest";
        StringBuilder sb = new StringBuilder();
        for (String part : up.toLowerCase(Locale.ROOT).split("_")) {
            if (part.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String toRoman(int n) {
        String[] r = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return n >= 0 && n < r.length ? r[n] : String.valueOf(n);
    }

    // ----------------------------------------------------------- sprites

    /** Menebak sprite atlas untuk ikon item di label dialog (tebakan, bukan jaminan 100%). */
    public static SpriteInfo resolveSprite(Material m) {
        String n = m.name().toLowerCase(Locale.ROOT);
        if (!m.isBlock() || isItemTexture(n)) return new SpriteInfo("minecraft:items", "item/" + n);

        for (String w : WOODS) {
            if (n.startsWith(w + "_") && (n.endsWith("_slab") || n.endsWith("_stairs") || n.endsWith("_fence")
                    || n.endsWith("_fence_gate") || n.endsWith("_button") || n.endsWith("_pressure_plate"))) {
                return new SpriteInfo("minecraft:blocks", "block/" + w + "_planks");
            }
        }

        String base = n;
        for (String suf : new String[]{"_stairs", "_slab", "_wall", "_button", "_pressure_plate"}) {
            if (base.endsWith(suf)) {
                base = base.substring(0, base.length() - suf.length());
                if (base.endsWith("brick")) base = base + "s";
                break;
            }
        }
        if (base.equals("cut_sandstone") || base.equals("smooth_sandstone")) base = "sandstone";
        if (base.endsWith("_wood")) base = base.replace("_wood", "_log");
        if (base.endsWith("_hyphae")) base = base.replace("_hyphae", "_stem");

        Map<String, String> special = Map.ofEntries(
                Map.entry("grass_block", "grass_block_side"), Map.entry("podzol", "podzol_side"),
                Map.entry("mycelium", "mycelium_side"), Map.entry("furnace", "furnace_front"),
                Map.entry("blast_furnace", "blast_furnace_front"), Map.entry("smoker", "smoker_front"),
                Map.entry("crafting_table", "crafting_table_front"), Map.entry("barrel", "barrel_side"),
                Map.entry("dispenser", "dispenser_front"), Map.entry("dropper", "dropper_front"),
                Map.entry("observer", "observer_front"), Map.entry("quartz_block", "quartz_block_side"));
        base = special.getOrDefault(base, base);
        return new SpriteInfo("minecraft:blocks", "block/" + base);
    }

    private static boolean isItemTexture(String n) {
        if (n.endsWith("_carpet") || n.endsWith("_wool")) return false;
        if (n.contains("glass_pane") && !n.equals("glass_pane")) return false;
        return n.endsWith("_banner") || n.endsWith("_bed") || n.endsWith("_bundle") || n.endsWith("_candle")
                || n.endsWith("_sign") || n.endsWith("_door") || n.endsWith("boat") || n.contains("minecart")
                || n.endsWith("torch") || n.endsWith("lantern") || n.endsWith("campfire") || n.equals("cauldron")
                || n.equals("brewing_stand") || n.equals("flower_pot") || n.equals("repeater")
                || n.equals("comparator") || n.equals("chain") || n.equals("bell") || n.equals("hopper")
                || n.equals("grindstone") || n.equals("glass_pane") || n.equals("iron_bars")
                || n.equals("ladder") || n.equals("lever") || n.equals("redstone") || n.equals("rail");
    }

    // ----------------------------------------------------------- dialogs

    public void openSearchDialog(Player p) {
        String template = plugin.getConfig().getString("search-command-template", "ah $(search_term)");
        String json = "{\"type\":\"minecraft:multi_action\",\"title\":\"Search Auction\","
                + "\"body\":[{\"type\":\"minecraft:item\",\"item\":{\"id\":\"minecraft:oak_sign\",\"count\":1}}],"
                + "\"inputs\":[{\"type\":\"minecraft:text\",\"key\":\"search_term\",\"label\":\"Search\"}],"
                + "\"actions\":["
                + "{\"label\":\"§cCancel\",\"action\":{\"type\":\"minecraft:run_command\",\"command\":\"" + cmd("shop") + "\"}},"
                + "{\"label\":\"§aSearch\",\"action\":{\"type\":\"minecraft:dynamic/run_command\",\"template\":\""
                + escapeJson(cmd(template)) + "\"}}]}";
        show(p, json);
    }

    public void openChooseItemDialog(Player p, int targetSlot, String query) {
        String q = query == null ? "" : query.toLowerCase(Locale.ROOT).trim();
        List<Material> filtered = new ArrayList<>();
        for (Material m : ALL_ITEMS) {
            if (q.isEmpty() || formatItemName(m.name()).toLowerCase(Locale.ROOT).contains(q)) {
                filtered.add(m);
                if (filtered.size() >= MAX_RESULTS) break;
            }
        }

        StringBuilder actions = new StringBuilder();
        actions.append("{\"label\":\"Search\",\"action\":{\"type\":\"minecraft:dynamic/run_command\",\"template\":\"")
                .append(cmd("shop __chooseitem ")).append(targetSlot).append(" $(search_item)\"}}");
        for (Material m : filtered) {
            SpriteInfo s = resolveSprite(m);
            actions.append(",{\"label\":[{\"type\":\"object\",\"object\":\"atlas\",\"atlas\":\"")
                    .append(s.atlas()).append("\",\"sprite\":\"").append(s.sprite()).append("\"},\" ")
                    .append(escapeJson(formatItemName(m.name()))).append("\"],")
                    .append("\"action\":{\"type\":\"minecraft:run_command\",\"command\":\"")
                    .append(cmd("shop __selectitem ")).append(targetSlot).append(' ').append(m.name()).append("\"}}");
        }

        String json = "{\"type\":\"minecraft:multi_action\",\"title\":\"Choose Item\",\"columns\":4,"
                + "\"inputs\":[{\"type\":\"minecraft:text\",\"key\":\"search_item\",\"label\":\"Search\"}],"
                + "\"actions\":[" + actions + "],"
                + "\"exit_action\":{\"label\":\"§cCancel\",\"action\":{\"type\":\"minecraft:run_command\",\"command\":\""
                + cmd("shop __edit") + "\"}}}";
        show(p, json);
    }

    public void openChooseEnchantmentsDialog(Player p, int slot, Material material, Map<String, Integer> selected) {
        List<Enchantment> applicable = getOrderedEnchantments(material);
        StringBuilder actions = new StringBuilder();
        for (Enchantment ench : applicable) {
            String key = ench.getKey().getKey();
            int max = ench.getMaxLevel();
            int lvl = selected.getOrDefault(key, 0);
            String name = formatItemName(key);
            String label = lvl > 0
                    ? "§a✔ " + name + (max > 1 ? " " + toRoman(lvl) : "")
                    : (key.endsWith("_curse") ? "§c" : "§7") + name;
            actions.append("{\"label\":\"").append(escapeJson(label))
                    .append("\",\"action\":{\"type\":\"minecraft:run_command\",\"command\":\"")
                    .append(cmd("shop __toggleench ")).append(slot).append(' ')
                    .append(material.name()).append(' ').append(key).append("\"}},");
        }

        String footer = "{\"label\":\"Back to Items\",\"action\":{\"type\":\"minecraft:run_command\",\"command\":\""
                + cmd("shop __chooseitem ") + slot + "\"}},"
                + "{\"label\":\"§eSkip Enchantments\",\"action\":{\"type\":\"minecraft:run_command\",\"command\":\""
                + cmd("shop __skipench ") + slot + " " + material.name() + "\"}},"
                + "{\"label\":\"§aContinue\",\"action\":{\"type\":\"minecraft:run_command\",\"command\":\""
                + cmd("shop __continueench ") + slot + " " + material.name() + "\"}}";

        String json = "{\"type\":\"minecraft:multi_action\",\"title\":\"Choose Enchantments\",\"columns\":2,"
                + "\"body\":[{\"type\":\"minecraft:item\",\"item\":{\"id\":\"" + material.getKey() + "\",\"count\":1}}],"
                + "\"actions\":[" + actions + footer + "]}";
        show(p, json);
    }

    public void openAmountDialog(Player p, int slot, ItemStack item) {
        int maxStack = item.getMaxStackSize();
        String json = "{\"type\":\"minecraft:multi_action\",\"title\":\"Choose Amount\","
                + "\"body\":[{\"type\":\"minecraft:item\",\"item\":{\"id\":\"" + item.getType().getKey()
                + "\",\"count\":1}},{\"type\":\"minecraft:plain_message\",\"contents\":\"Max per purchase: " + maxStack + "\"}],"
                + "\"inputs\":[{\"type\":\"minecraft:text\",\"key\":\"buy_amount\",\"label\":\"Amount\",\"initial\":\"1\"}],"
                + "\"actions\":["
                + "{\"label\":\"§cCancel\",\"action\":{\"type\":\"minecraft:run_command\",\"command\":\"" + cmd("shop __edit") + "\"}},"
                + "{\"label\":\"Add to Quick Buy\",\"action\":{\"type\":\"minecraft:dynamic/run_command\",\"template\":\""
                + cmd("shop __setamount ") + slot + " $(buy_amount)\"}}]}";
        show(p, json);
    }

    // ----------------------------------------------------- flow handlers

    public void selectItem(Player p, int slot, Material material) {
        Pending pd = new Pending();
        pd.slot = slot;
        pd.material = material;
        pending.put(p.getUniqueId(), pd);

        if (getOrderedEnchantments(material).isEmpty()) {
            pd.item = new ItemStack(material);
            openAmountDialog(p, slot, pd.item);
        } else {
            openChooseEnchantmentsDialog(p, slot, material, pd.enchants);
        }
    }

    public void toggleEnchant(Player p, int slot, Material material, String key) {
        Pending pd = pending.get(p.getUniqueId());
        if (pd == null || pd.material != material) {
            pd = new Pending();
            pd.slot = slot;
            pd.material = material;
            pending.put(p.getUniqueId(), pd);
        }
        Enchantment ench = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(key));
        if (ench == null) return;
        int next = pd.enchants.getOrDefault(key, 0) + 1;
        if (next > ench.getMaxLevel()) pd.enchants.remove(key);
        else pd.enchants.put(key, next);
        openChooseEnchantmentsDialog(p, slot, material, pd.enchants);
    }

    public void skipEnchants(Player p, int slot, Material material) {
        Pending pd = pending.computeIfAbsent(p.getUniqueId(), k -> new Pending());
        pd.slot = slot;
        pd.material = material;
        pd.enchants.clear();
        pd.item = new ItemStack(material);
        openAmountDialog(p, slot, pd.item);
    }

    public void continueEnchants(Player p, int slot, Material material) {
        Pending pd = pending.computeIfAbsent(p.getUniqueId(), k -> new Pending());
        pd.slot = slot;
        pd.material = material;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            for (Map.Entry<String, Integer> en : pd.enchants.entrySet()) {
                Enchantment ench = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(en.getKey()));
                if (ench != null) meta.addEnchant(ench, en.getValue(), true);
            }
            item.setItemMeta(meta);
        }
        pd.item = item;
        openAmountDialog(p, slot, item);
    }

    public void setAmount(Player p, int slot, String rawAmount) {
        Pending pd = pending.get(p.getUniqueId());
        if (pd == null || pd.item == null) {
            QuickBuyGUI.open(p, true);
            return;
        }
        int amount;
        try {
            amount = Integer.parseInt(rawAmount.trim());
        } catch (NumberFormatException ex) {
            p.sendMessage("§cJumlah tidak valid.");
            openAmountDialog(p, slot, pd.item);
            return;
        }
        amount = Math.max(1, Math.min(amount, pd.item.getMaxStackSize()));
        ItemStack item = pd.item.clone();
        item.setAmount(amount);
        plugin.getDataManager().setItem(p, slot, item);
        pending.remove(p.getUniqueId());
        QuickBuyGUI.open(p, true);
    }

    // ------------------------------------------------------- enchantments

    public List<Enchantment> getOrderedEnchantments(Material material) {
        ItemStack test = new ItemStack(material);
        List<Enchantment> normals = new ArrayList<>();
        List<Enchantment> curses = new ArrayList<>();
        for (Enchantment e : Registry.ENCHANTMENT) {
            if (!e.canEnchantItem(test)) continue;
            if (e.getKey().getKey().endsWith("_curse")) curses.add(e);
            else normals.add(e);
        }
        Comparator<Enchantment> byKey = Comparator.comparing(e -> e.getKey().getKey());
        normals.sort(byKey);
        curses.sort(byKey);
        List<Enchantment> combined = new ArrayList<>(normals);
        combined.addAll(curses);
        return combined;
    }
}
