package devs.donutShop.gui;

public enum ShopFilterType {
    DEFAULT("Default"),
    CHEAPEST("Cheapest"),
    MOST_EXPENSIVE("Most Expensive"),
    NAME("Name");

    private final String displayName;

    ShopFilterType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ShopFilterType next() {
        ShopFilterType[] v = values();
        return v[(ordinal() + 1) % v.length];
    }
}
