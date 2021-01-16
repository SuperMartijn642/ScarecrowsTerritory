package com.supermartijn642.scarecrowsterritory;

import net.minecraftforge.common.config.Configuration;

import java.io.File;

/**
 * Created 11/30/2020 by SuperMartijn642
 */
public class STConfig {

    private static final String FILE_NAME = "scarecrows-territory.cfg";

    public static Configuration instance;

    public static boolean loadSpawners;
    public static float loadSpawnerRange;
    public static boolean passiveMobSpawning;
    public static float passiveMobRange;

    public static void init(File dir){
        instance = new Configuration(new File(dir, FILE_NAME));
        instance.load();

        loadSpawners = instance.getBoolean("loadSpawners", Configuration.CATEGORY_GENERAL, true, "Should the scarecrows keep spawners in range activated?");
        loadSpawnerRange = instance.getFloat("loadSpawnerRange", Configuration.CATEGORY_GENERAL, 8, 1, 25, "In what range will the scarecrows load spawners?");
        passiveMobSpawning = instance.getBoolean("passiveMobSpawning", Configuration.CATEGORY_GENERAL, true, "Should mobs passively spawn within the scarecrows' range");
        passiveMobRange = instance.getFloat("passiveMobRange", Configuration.CATEGORY_GENERAL, 8, 1, 25, "In what range will mobs passively spawn?");

        if(instance.hasChanged())
            instance.save();
    }

}
