package eu.ha3.matmos.data.modules;

import java.util.Map;

/*
 * --filenotes-placeholder
 */

public interface EntryBasedModule extends Module {
    public Map<String, EI> getModuleEntries();
}