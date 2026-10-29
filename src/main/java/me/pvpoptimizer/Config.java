package me.pvpoptimizer;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.BooleanValue CRYSTAL;
    public static final ForgeConfigSpec.BooleanValue ANCHOR;
    public static final ForgeConfigSpec.BooleanValue CART;
    public static final ForgeConfigSpec.BooleanValue ENTITY_CULLING;
    public static final ForgeConfigSpec.IntValue CULL_INTERVAL;
    public static final ForgeConfigSpec.IntValue FAR_ENTITY_DISTANCE;
    public static final ForgeConfigSpec.IntValue NAMEPLATE_DISTANCE;
    public static final ForgeConfigSpec.IntValue TILE_ENTITY_DISTANCE;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("pvp");
        ENABLED = b.comment("Master toggle (PVP features + performance features)").define("enabled", true);
        CRYSTAL = b.comment("Remove end crystals client-side instantly when you hit them").define("crystalOptimizer", true);
        ANCHOR = b.comment("Replace exploded respawn anchors with a fern client-side so you can re-place instantly").define("anchorOptimizer", true);
        CART = b.comment("Remove TNT minecarts client-side instantly when you shoot at them with a bow").define("cartPvp", true);
        b.pop();
        b.push("performance");
        ENTITY_CULLING = b.comment("Don't render living entities that are fully hidden behind solid blocks").define("entityCulling", true);
        CULL_INTERVAL = b.comment("Ticks between visibility re-checks per entity (higher = faster, more pop-in)")
                .defineInRange("cullIntervalTicks", 4, 1, 40);
        FAR_ENTITY_DISTANCE = b.comment("Non-player entities farther than this (blocks) are not rendered. 0 = off. Players are never distance-culled.")
                .defineInRange("farEntityDistance", 80, 0, 512);
        NAMEPLATE_DISTANCE = b.comment("Hide nameplates of non-player entities farther than this (blocks). 0 = off.")
                .defineInRange("mobNameplateDistance", 32, 0, 128);
        TILE_ENTITY_DISTANCE = b.comment("[mixin] Don't render block entities (chests, signs, banners...) farther than this many blocks. 0 = off.")
                .defineInRange("blockEntityDistance", 48, 0, 512);
        b.pop();
        SPEC = b.build();
    }
}
