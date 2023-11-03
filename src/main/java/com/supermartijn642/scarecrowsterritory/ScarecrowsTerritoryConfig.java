package com.supermartijn642.scarecrowsterritory;

import com.supermartijn642.configlib.api.ConfigBuilders;
import com.supermartijn642.configlib.api.IConfigBuilder;

import java.util.function.Supplier;

/**
 * Created 11/30/2020 by SuperMartijn642
 */
public class ScarecrowsTerritoryConfig {

    public static final Supplier<Boolean> loadSpawners;
    public static final Supplier<Double> loadSpawnerRange;
    public static final Supplier<Boolean> passiveMobSpawning;
    public static final Supplier<Double> passiveMobRange;
    public static final Supplier<Double> noDespawnBuffer;

    public static final Supplier<Boolean> enableTrophyIntegration;
    public static final Supplier<Integer> trophyCheckRange;
    public static final Supplier<Integer> maxTrophies;

    static{
        IConfigBuilder builder = ConfigBuilders.newTomlConfig("scarecrowsterritory", null, false);

        builder.push("General");

        loadSpawners = builder.comment("Should the scarecrows keep spawners in range activated?").define("loadSpawners", true);
        loadSpawnerRange = builder.comment("In what range will the scarecrows load spawners?").define("loadSpawnerRange", 5, 1, 25d);
        passiveMobSpawning = builder.comment("Should mobs passively spawn within the scarecrows' range").define("passiveMobSpawning", true);
        passiveMobRange = builder.comment("In what range will mobs passively spawn?").define("passiveMobRange", 8, 1, 128d);
        noDespawnBuffer = builder.comment("How many blocks away in addition to passiveMobRange and loadSpawnerRange should mobs be prevented from despawning? I.e. no despawn range = max(passiveMobRange, loadSpawnerRange) + noDespawnBuffer.").define("noDespawnBuffer", 10, 0, 128d);

        builder.pop();

        builder.push("Trophies integration");

        enableTrophyIntegration = builder.comment("Should the integration with OpenBlocks Trophies be enabled? If true, scarecrows will only spawn mobs for which their trophy is placed nearby.").gameRestart().define("enableTrophyIntegration", false);
        trophyCheckRange = builder.comment("What should be the range in blocks in which the scarecrow checks for trophies? A value of 2 would mean the scarecrow checks a 5x5x5 area around itself.").define("trophyCheckRange", 1, 1, 3);
        maxTrophies = builder.comment("What should be the maximum number of trophies for which the scarecrow will spawn mobs?").define("maxTrophies", 8, 1, 1000);

        builder.pop();

        builder.build();
    }

}
