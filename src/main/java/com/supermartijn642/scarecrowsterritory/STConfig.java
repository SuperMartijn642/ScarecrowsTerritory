package com.supermartijn642.scarecrowsterritory;

import com.supermartijn642.configlib.ModConfig;
import com.supermartijn642.configlib.ModConfigBuilder;

import java.util.function.Supplier;

/**
 * Created 11/30/2020 by SuperMartijn642
 */
public class STConfig {

    public static final Supplier<Boolean> loadSpawners;
    public static final Supplier<Double> loadSpawnerRange;
    public static final Supplier<Boolean> passiveMobSpawning;
    public static final Supplier<Double> passiveMobRange;

    static{
        ModConfigBuilder builder = new ModConfigBuilder("scarecrowsterritory", ModConfig.Type.COMMON);

        builder.push("General");

        loadSpawners = builder.comment("Should the scarecrows keep spawners in range activated?").define("loadSpawners", true);
        loadSpawnerRange = builder.comment("In what range will the scarecrows load spawners?").define("loadSpawnerRange", 8, 1, 25d);
        passiveMobSpawning = builder.comment("Should mobs passively spawn within the scarecrows' range").define("passiveMobSpawning", true);
        passiveMobRange = builder.comment("In what range will mobs passively spawn?").define("passiveMobRange", 8, 1, 128d);

        builder.pop();

        builder.build();
    }

}
