package com.supermartijn642.scarecrowsterritory;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Created 11/30/2020 by SuperMartijn642
 */
public class STConfig {

    static{
        Pair<STConfig,ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(STConfig::new);
        CONFIG_SPEC = pair.getRight();
        INSTANCE = pair.getLeft();
    }

    public static final ForgeConfigSpec CONFIG_SPEC;
    public static final STConfig INSTANCE;

    public final ForgeConfigSpec.BooleanValue loadSpawners;
    public final ForgeConfigSpec.DoubleValue loadSpawnerRange;
    public final ForgeConfigSpec.BooleanValue passiveMobSpawning;
    public final ForgeConfigSpec.DoubleValue passiveMobRange;

    private STConfig(ForgeConfigSpec.Builder builder){
        builder.push("General");
        this.loadSpawners = builder.comment("Should the scarecrows keep spawners in range activated?").define("loadSpawners", true);
        this.loadSpawnerRange = builder.comment("In what range will the scarecrows load spawners?").defineInRange("loadSpawnerRange", 8, 1, 25d);
        this.passiveMobSpawning = builder.comment("Should mobs passively spawn within the scarecrows' range").define("passiveMobSpawning", true);
        this.passiveMobRange = builder.comment("In what range will mobs passively spawn?").defineInRange("passiveMobRange", 8, 1, 25d);
    }

}
