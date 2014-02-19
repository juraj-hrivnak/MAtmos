package eu.ha3.matmos.engine0.game.data;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import eu.ha3.matmos.engine0.conv.MAtmosConvLogger;
import eu.ha3.matmos.engine0.core.implem.GenericSheet;
import eu.ha3.matmos.engine0.core.implem.SelfGeneratingData;
import eu.ha3.matmos.engine0.core.interfaces.Data;
import eu.ha3.matmos.engine0.debug.IDontKnowHowToCode;
import eu.ha3.matmos.engine0.game.data.abstractions.Collector;
import eu.ha3.matmos.engine0.game.data.abstractions.Processor;
import eu.ha3.matmos.engine0.game.data.abstractions.module.Module;
import eu.ha3.matmos.engine0.game.data.abstractions.module.ModuleProcessor;
import eu.ha3.matmos.engine0.game.data.abstractions.module.PassOnceModule;
import eu.ha3.matmos.engine0.game.data.abstractions.processor.ProcessorModel;
import eu.ha3.matmos.engine0.game.data.abstractions.scanner.Progress;
import eu.ha3.matmos.engine0.game.data.abstractions.scanner.ScannerModule;
import eu.ha3.matmos.engine0.game.data.modules.L__legacy;
import eu.ha3.matmos.engine0.game.data.modules.L__legacy_hitscan;
import eu.ha3.matmos.engine0.game.data.modules.L__meta_mod;
import eu.ha3.matmos.engine0.game.data.modules.M__cb_column;
import eu.ha3.matmos.engine0.game.data.modules.M__cb_light;
import eu.ha3.matmos.engine0.game.data.modules.M__cb_pos;
import eu.ha3.matmos.engine0.game.data.modules.M__ply_action;
import eu.ha3.matmos.engine0.game.data.modules.M__ply_general;
import eu.ha3.matmos.engine0.game.data.modules.M__ply_inventory;
import eu.ha3.matmos.engine0.game.data.modules.M__ply_motion;
import eu.ha3.matmos.engine0.game.data.modules.M__ply_stats;
import eu.ha3.matmos.engine0.game.data.modules.M__ride_motion;
import eu.ha3.matmos.engine0.game.data.modules.M__w_biome;
import eu.ha3.matmos.engine0.game.data.modules.M__w_general;
import eu.ha3.matmos.engine0.game.data.modules.S__ench_armor;
import eu.ha3.matmos.engine0.game.data.modules.S__ench_current;
import eu.ha3.matmos.engine0.game.data.modules.S__potion_duration;
import eu.ha3.matmos.engine0.game.data.modules.S__potion_power;
import eu.ha3.matmos.engine0.game.data.modules.S__scan_contact;
import eu.ha3.matmos.engine0.game.system.MAtMod;

/*
--filenotes-placeholder
*/

public class ModularDataGatherer implements Collector, Processor
{
	private final MAtMod mod;
	
	private Data data;
	private int ticksPassed;
	
	public static final String LEGACY_PREFIX = "legacy";
	
	//
	
	private final Map<String, Module> modules;
	private final Map<String, Set<String>> passOnceModules;
	private final Set<String> passOnceSubmodules;
	private final Set<String> requiredModules;
	private final Map<String, Set<String>> moduleStack;
	
	private ScannerModule largeScanner;
	
	//
	
	public ModularDataGatherer(MAtMod mAtmosHaddon)
	{
		this.mod = mAtmosHaddon;
		
		this.modules = new TreeMap<String, Module>();
		this.passOnceModules = new TreeMap<String, Set<String>>();
		this.passOnceSubmodules = new HashSet<String>();
		this.requiredModules = new TreeSet<String>();
		this.moduleStack = new TreeMap<String, Set<String>>();
	}
	
	private void addModule(Module module)
	{
		this.modules.put(module.getModuleName(), module);
		if (module instanceof PassOnceModule)
		{
			this.passOnceModules.put(module.getModuleName(), ((PassOnceModule) module).getSubModules());
			this.passOnceSubmodules.addAll(((PassOnceModule) module).getSubModules());
		}
	}
	
	/**
	 * Adds a module after setting an cycle on it, if it's an instance of a
	 * ProcessorModel. Cycle: Every n ticks. Cycle = 1: Every ticks.
	 * 
	 * @param module
	 * @param interval
	 */
	private void addModule(Module module, int cycle)
	{
		if (module instanceof ProcessorModel)
		{
			((ProcessorModel) module).setInterval(cycle - 1);
		}
		addModule(module);
	}
	
	public void load()
	{
		this.data = new SelfGeneratingData(GenericSheet.class);
		
		addModule(new L__legacy(this.data));
		addModule(new L__legacy_hitscan(this.data));
		addModule(new L__meta_mod(this.data, this.mod));
		addModule(new M__cb_column(this.data));
		addModule(new M__cb_light(this.data));
		addModule(new M__cb_pos(this.data));
		addModule(new M__ply_action(this.data));
		addModule(new M__ply_general(this.data));
		addModule(new M__ply_inventory(this.data));
		addModule(new M__ply_motion(this.data));
		addModule(new M__ply_stats(this.data));
		addModule(new M__ride_motion(this.data));
		addModule(new M__w_biome(this.data, this.mod), 20);
		addModule(new M__w_general(this.data));
		addModule(new S__ench_armor(this.data, 0));
		addModule(new S__ench_armor(this.data, 1));
		addModule(new S__ench_armor(this.data, 2));
		addModule(new S__ench_armor(this.data, 3));
		addModule(new S__ench_current(this.data));
		addModule(new S__scan_contact(this.data));
		
		this.largeScanner =
			new ScannerModule(
				this.data, "_POM__scan_large", "scan_large", true, 8, 20 /*256*/, 64, 32, 64, 16 * 8 * 16/*64 * 64 * 2*/);
		addModule(this.largeScanner);
		addModule(new ScannerModule(
			this.data, "_POM__scan_small", "scan_small", true, -1, 2 /*64*/, 16, 8, 16, 16 * 8 * 16));
		
		addModule(new S__potion_power(this.data));
		addModule(new S__potion_duration(this.data));
		
		MAtmosConvLogger.info("Modules initialized.");
	}
	
	public Data getData()
	{
		return this.data;
	}
	
	@Override
	public void process()
	{
		for (String requiredModule : this.requiredModules)
		{
			try
			{
				this.modules.get(requiredModule).process();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				IDontKnowHowToCode.whoops__printExceptionToChat(this.mod.getChatter(), e, requiredModule.hashCode());
			}
		}
		
		this.ticksPassed = this.ticksPassed + 1;
		
		/*if (this.mod.util().getClientTick() % 40 == 0)
		{
			System.out.println(DumpData.dumpData(this.data));
		}*/
	}
	
	@Override
	public boolean requires(String moduleName)
	{
		return this.requiredModules.contains(moduleName);
	}
	
	@Override
	public void addModuleStack(String name, Set<String> requiredModules)
	{
		// Recompact required modules to piece deltas.
		Set<String> deltaModules = new HashSet<String>();
		Set<String> actualModules = new HashSet<String>();
		for (String module : requiredModules)
		{
			if (module.endsWith(ModuleProcessor.DELTA_SUFFIX))
			{
				deltaModules.add(module);
				actualModules.add(module.substring(0, module.length() - ModuleProcessor.DELTA_SUFFIX.length()));
			}
		}
		requiredModules.removeAll(deltaModules);
		requiredModules.addAll(actualModules);
		
		// Find missing modules. We don't want to iterate and check through invalid modules.
		Set<String> missingModules = new HashSet<String>();
		for (String module : requiredModules)
		{
			if (!this.modules.containsKey(module) && !this.passOnceSubmodules.contains(module))
			{
				MAtmosConvLogger.severe("Stack " + name + " requires missing module " + module);
				missingModules.add(module);
			}
		}
		
		for (String missingModule : missingModules)
		{
			requiredModules.remove(missingModule);
		}
		
		this.moduleStack.put(name, requiredModules);
		
		recomputeModuleStack();
	}
	
	@Override
	public void removeModuleStack(String name)
	{
		this.moduleStack.remove(name);
		recomputeModuleStack();
	}
	
	private void recomputeModuleStack()
	{
		this.requiredModules.clear();
		for (Set<String> stack : this.moduleStack.values())
		{
			this.requiredModules.addAll(stack);
		}
		
		for (Map.Entry<String, Set<String>> submodules : this.passOnceModules.entrySet())
		{
			// if the submodules have something in common with the required modules
			if (!Collections.disjoint(submodules.getValue(), this.requiredModules))
			{
				this.requiredModules.removeAll(submodules.getValue());
				this.requiredModules.add(submodules.getKey());
			}
		}
	}
	
	public Progress getLargeScanProgress()
	{
		return this.largeScanner;
	}
}