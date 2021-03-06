package eu.ha3.matmos;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.ha3.util.property.simple.ConfigProperty;
import net.minecraft.launchwrapper.Launch;

/**
 * Class for statically initializing and accessing the config. Required to make
 * it possible to access the config from the coremod/tweaker.
 */

public class ConfigManager {

    private static final Logger LOGGER = LogManager.getLogger("matmos");

    private static final ConfigProperty config = new ConfigProperty();
    private static boolean hasInitialized = false;

    private static File configFolder = null;

    private static void initConfig() {
        // Create default configuration

        config.setProperty("world.height", 256);
        config.setProperty("world.maxblockid", 4096,
                "The max block ID. This is 4096 normally, but there are mods that raise it. Getting an ArrayIndexOutOfBoundsException is indication that it needs to be raised.");
        config.setProperty("dump.sheets.enabled", false);
        config.setProperty("start.enabled", true, "If false, MAtmos won't start until the MAtmos key is pressed.");
        config.setProperty("reversed.controls", false);
        config.setProperty("sound.autopreview", true);
        config.setProperty("globalvolume.scale", 1f);
        config.setProperty("key.code", 65);
        config.setProperty("useroptions.altitudes.high", true);
        config.setProperty("useroptions.altitudes.low", true);
        config.setProperty("useroptions.biome.override", -1);
        config.setProperty("debug.mode", 0);
        config.setProperty("minecraftsound.ambient.volume", 1f);
        config.setProperty("coremod.replacesoundsystem", "auto",
                "There's a bug in Minecraft's sound system that causes it to crash after some time if looping streams are played.\n"
                        + "Forge provides a fix for this in 1.12.2, but MAtmos has to provide its own fix on 1.7.10, and on LiteLoader versions.\n"
                        + "Use this option to control when the SoundSystem should be overridden.\n\n"
                        + "Allowed values are: always, never, auto (which only overrides if no other mod is present which also overrides it (like DynamicSurroundings on 1.7.10, or Forge itself on 1.12.2))");
        config.setProperty("dimensions.list", "",
                "Comma-separated list of dimensions. If dimensions.listtype is black, then ambience will NOT be played in these dimensions.\n"
                        + "If it's white, then ambience will ONLY play in these dimensions.\n");
        config.setProperty("dimensions.listtype", "black", "BLACK or WHITE?\n");
        config.setProperty("rain.suppress", "auto",
                "Use this option to control how the conflict should be resolved between MAtmos rain sounds\n" +
                "and rain sounds from vanilla or other mods.\n" +
                "True: rain from other sources is muted\n" +
                "False: rain is muted from MAtmos soundpacks which support this option\n" +
                "Auto: true if there's at least one soundpack which supports this option present, false otherwise\n");
        config.setProperty("rain.soundlist", "weather.rain,weather.rain.above,ambient.weather.rain",
                "Comma-separated list of rain sounds to suppress if rain.suppress is true");
        config.setProperty("rain.strengthThreshold", "-1",
                "Rain strength threshold above which it's considered to be raining by soundpacks\n" +
                "Range: 0~1, or -1 to use the default setting, which is 0.2 in vanilla\n" +
                "Set this to something low like 0 for better compatibility with Weather2\n"
                );
        config.commit();

        config.setGlobalDescription("Tip: restart MAtmos to reload the configs without restarting Minecraft");

        // Load configuration from source
        try {
            config.setSource(new File(getConfigFolder(), "userconfig.cfg").getCanonicalPath());
            config.load();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error caused config not to work: " + e.getMessage());
        }

        hasInitialized = true;
    }

    public static ConfigProperty getConfig() {
        if (!hasInitialized) {
            initConfig();
        }
        return config;
    }

    public static File getConfigFolder() {
        if (configFolder == null) {
            configFolder = new File(Launch.minecraftHome, "config/matmos");
        }
        return configFolder;
    }

    public static void createDefaultConfigFileIfMissing(File configFile) {
        Path configFolderPath = Paths.get(getConfigFolder().getPath());
        Path configFilePath = Paths.get(configFile.getPath());

        Path relPath = configFolderPath.relativize(configFilePath);

        if (configFilePath.startsWith(configFolderPath)) {
            if (!configFile.exists()) {
                try {
                    InputStream defaultFileStream = ConfigManager.class.getClassLoader().getResourceAsStream(
                            Paths.get("assets/matmos/default_config/").resolve(relPath).toString().replace('\\', '/'));
                    // Paths need to have forward slashes in jars

                    if (defaultFileStream != null) {
                        String contents = IOUtils.toString(defaultFileStream);

                        try (FileWriter out = new FileWriter(configFile)) {
                            IOUtils.write(contents, out);
                        }
                    }
                } catch (IOException e) {
                    LOGGER.error("Failed to create default config file for " + relPath.toString());
                }
            }
        } else {
            LOGGER.debug("Invalid argument for creating default config file: " + relPath.toString()
                    + " (file is not in the config directory)");
        }
    }
}
