package eu.ha3.matmos.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

import eu.ha3.matmos.ConfigManager;
import eu.ha3.matmos.Matmos;
import net.minecraft.block.Block;
import net.minecraft.util.ResourceLocation;

public class BlockDealiaser {

    private int[] dealiasMap;

    public BlockDealiaser(File aliasFile) {
        loadAliasFile(aliasFile);

        compile();
    }

    private void loadAliasFile(File aliasFile) {
        ConfigManager.createDefaultConfigFileIfMissing(aliasFile);

        Properties props = new Properties();

        try (FileReader reader = new FileReader(aliasFile)) {
            props.load(reader);
        } catch (FileNotFoundException e) {
            Matmos.LOGGER.warn("Alias file (" + aliasFile.getPath() + ") is missing");
        } catch (IOException e) {
            Matmos.LOGGER.error("Error loading alias file (" + aliasFile.getPath() + "): " + e);
        }

        dealiasMap = new int[Matmos.MAX_ID];
        for (int i = 0; i < dealiasMap.length; i++)
            dealiasMap[i] = i;

        props.stringPropertyNames().forEach(ks -> Arrays.stream(props.getProperty(ks).split(",")).forEach(vs -> {
            ResourceLocation k = new ResourceLocation(ks);
            ResourceLocation v = new ResourceLocation(vs);
            if (Block.REGISTRY.containsKey(k) && Block.REGISTRY.containsKey(v)) {
                Object keyObj = Block.REGISTRY.getObject(k);
                Object valueObj = Block.REGISTRY.getObject(v);
                if (keyObj instanceof Block && valueObj instanceof Block) {
                    dealiasMap[Block.getIdFromBlock((Block) valueObj)] = Block.getIdFromBlock((Block) keyObj);
                }
            }
        }));
    }

    private void compile() {
        for (int i = 0; i < dealiasMap.length; i++) {
            if (dealiasMap[i] != i) {
                int id = dealiasMap[i];
                do {
                    id = dealiasMap[id];

                } while (dealiasMap[id] != dealiasMap[id] && id != i);

                if (id == i) {
                    Matmos.LOGGER.warn("Circular dependency detected in alias file when dealiasing "
                            + Block.REGISTRY.getNameForObject(Block.getBlockById(i)) + ". Alias will be ignored.");
                    dealiasMap[i] = i;
                } else { // OK
                    dealiasMap[i] = id;
                }
            }
        }
    }

    public int dealiasID(int alias) {
        return dealiasMap[alias];
    }

}
