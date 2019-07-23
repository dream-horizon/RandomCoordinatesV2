/*
 *     RandomCoords, Provding the best Bukkit Random Teleport Plugin
 *     Copyright (C) 2014  James Shopland
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.jolbol1.RandomCoordinates;

import com.jolbol1.RandomCoordinates.commands.*;
import com.jolbol1.RandomCoordinates.commands.handler.CommandHandler;
import com.jolbol1.RandomCoordinates.listeners.*;
import com.jolbol1.RandomCoordinates.managers.ConstructTabCompleter;
import com.jolbol1.RandomCoordinates.managers.Util.PortalLoaded;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import net.milkbowl.vault.economy.Economy;

import org.bstats.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by James on 01/07/2016.
 */
public class RandomCoords extends JavaPlugin {

	public static Logger logger;
	public static Economy econ;
	private static Plugin plugin;

	/**
	 * Used to put in the current /RC Wand selection, and who selected it.
	 */
	public final Map<Player, Location> wandSelection = new ConcurrentHashMap<>();
	/**
	 * Creates an instance of the messages folder, Which handles most messages
	 * within.
	 */

	// Creates an instance of the config files.
	public FileConfiguration language;
	public FileConfiguration config;
	public FileConfiguration limiter;
	public FileConfiguration portals;
	public FileConfiguration warps;
	public FileConfiguration blacklist;
	public FileConfiguration skyBlockSave;
	public FileConfiguration bonushChestExample;
	public FileConfiguration bonusChest;
	public FileConfiguration versionInfo;

	public HashMap<UUID, Location> skyBlock = new HashMap<>();
	public List<PortalLoaded> loadedPortalList;

	private File languageFile;
	public File bonusChestExample;
	public File bonusChestFile;
	public File configFile;
	public File limiterFile;
	public File portalsFile;
	public File warpFile;
	public File blackFile;
	public File skyBlockSaveFile;

	final String ANSI_RESET = "\u001B[0m";
	final String ANSI_BOLD = "\u001B[1m";
	final String ANSI_BLUE = "\u001B[34m";

	// Sets the number to 0, The counter for successful teleports and failed.
	public int successTeleports;
	public int failedTeleports;

	public int onJoin;
	public int signTeleport;
	public int commandTeleport;
	public int otherTeleport;
	public int allTeleport;
	public int portalTeleport;
	public int warpTeleport;

	/**
	 * Used to grab the plugin instance from this clas
	 * 
	 * @return The plugin instance
	 */
	public static RandomCoords getPlugin() {
		return JavaPlugin.getPlugin(RandomCoords.class);
	}

	/**
	 * Used to get the server version.
	 *
	 * Example: For 1.12.2 it will return 12.
	 * @return The server version number.
	 */
	public static int getServerVersion(){
		return Integer.parseInt(Bukkit.getBukkitVersion().split("\\.")[1]);
	}

	/**
	 * What to do on start up.
	 */
	public void onEnable() {
		// Setup bStats Metrics
		logger = Bukkit.getServer().getLogger();
		plugin = this;
		final PluginManager pm = getServer().getPluginManager();

		final PluginDescriptionFile pdf = getDescription();

		logger.log(Level.INFO,
				ANSI_BLUE + ANSI_BOLD + "[RandomCoords]" + ANSI_BLUE + ANSI_BOLD + pdf.getName() + ANSI_BLUE + ANSI_BOLD
						+ " Version: " + ANSI_BLUE + ANSI_BOLD + pdf.getVersion() + ANSI_BLUE + ANSI_BOLD + " enabled."
						+ ANSI_RESET);

		File bonusFolder;
		try {
			bonusFolder = new File(getDataFolder() + File.separator + "BonusChests");
			if (!bonusFolder.exists()) {
				bonusFolder.mkdirs();
			}
		} catch (SecurityException e) {
			// do something...
			return;
		}
		this.getDataFolder().mkdirs();

		// Setup Language File
		languageFile = new File(this.getDataFolder(), "language.yml");
		language = setupFile(languageFile);
		matchFile(language, languageFile, "language");

		configFile = new File(this.getDataFolder(), "config.yml");
		config = setupFile(configFile);
		config.options()
				.header("RandomCoords: Need Help Setting Up? See the Wiki! https://github.com/jolbol1/RandomCoordinatesV2/wiki \n "
						+ "This will explain everything you need to know about all the options. \n "
						+ "Developed by Jolbol1");
		matchFile(config, configFile, "config");

		bonusChestFile = new File(this.getDataFolder(), "BonusChestConfig.yml");
		bonusChest = setupFile(bonusChestFile);
		bonusChest.options()
				.header("RandomCoords: Need Help Setting Up? See the Wiki! https://github.com/jolbol1/RandomCoordinatesV2/wiki \n "
						+ "This will explain everything you need to know about all the options. \n "
						+ "Developed by Jolbol1");
		// matchFile(bonusChest, bonusChestFile, "BonusChestFile");

		File bonusChestFolder = new File(this.getDataFolder() + File.separator + "BonusChests");
		if (!bonusChestFolder.exists()) {
			bonusChestFolder.mkdirs();
		}

		bonusChestExample = new File(this.getDataFolder() + File.separator + "BonusChests", "BonusChestExample.yml");
		bonushChestExample = setupFile(bonusChestExample);
		matchFile(bonushChestExample, bonusChestExample, "BonusChestExample");

		limiterFile = new File(this.getDataFolder(), "limiter.yml");
		limiter = setupFile(limiterFile);
		matchFile(limiter, limiterFile, "limiter");

		portalsFile = new File(this.getDataFolder(), "portals.yml");
		portals = setupFile(portalsFile);
		
		warpFile = new File(this.getDataFolder(), "warps.yml");
		warps = setupFile(warpFile);
		matchFile(warps, warpFile, "warps");

		skyBlockSaveFile = new File(this.getDataFolder(), "SkyBlockSave.yml");
		skyBlockSave = setupFile(skyBlockSaveFile);
		matchFile(skyBlockSave, skyBlockSaveFile, "SkyBlockSave");

		blackFile = new File(this.getDataFolder(), "blacklist.yml");
		blacklist = setupFile(blackFile);
		if (blacklist.getStringList("Blacklisted") != null) {
			matchFile(blacklist, blackFile, "blacklist");
		}
		
		versionInfo = YamlConfiguration.loadConfiguration(new InputStreamReader(this.getResource("versionInfo.yml")));
		
		pm.registerEvents(new Suffocation(), this);
		pm.registerEvents(new Invulnerable(), this);
		pm.registerEvents(new onJoin(), this);
		pm.registerEvents(new Signs(), this);
		pm.registerEvents(new SignClick(), this);
		pm.registerEvents(new BlockPortal(), this);
		pm.registerEvents(new InNetherPortal(), this);
		pm.registerEvents(new Wand(), this);
		pm.registerEvents(new PlayerSwitchWorld(), this);
		pm.registerEvents(new RandomTeleportListener(), this);
		pm.registerEvents(new SkyBlockBreak(), this);

		final CommandHandler handler = new CommandHandler();
		handler.register("rc", new RandomCommand());
		handler.register("wand", new WandGive());
		handler.register("reload", new Reload());
		handler.register("all", new All());
		handler.register("help", new HelpCommand());
		handler.register("player", new Others());
		handler.register("portal", new Portals());
		handler.register("set", new WorldSettings());
		handler.register("region", new RegionCommand());
		handler.register("warp", new WarpsNew());
		handler.register("radius", new RadiusCommand());
		handler.register("chest", new BonusCommand());

		getCommand("rc").setExecutor(handler);
		getCommand("rc").setTabCompleter(new ConstructTabCompleter());

		if (RandomCoords.getPlugin().config.getString("Metrics").equalsIgnoreCase("true")) {
			final Metrics metrics = new Metrics(this);
			setupCharts(metrics);
		}

		final PortalEnter pe = new PortalEnter();
		loadedPortalList = loadedPortals();
		pe.runTaskTimer(this, 0L, 20L);

		for (String str : skyBlockSave.getKeys(true)) {
			if (str.equalsIgnoreCase("SkyBlock") || str.equalsIgnoreCase("AutoRemove")
					|| str.equalsIgnoreCase("SkyBlockWorlds") || str.equalsIgnoreCase("DefaultY")) {
				// For some reason this wouldnt work when reversed... Not to
				// sure why... I have had 4hrs sleep.
			} else {

				skyBlock.put(UUID.fromString(str), (Location) skyBlockSave.get(str));
				skyBlockSave.set(str, null);
				saveFile(skyBlockSave, skyBlockSaveFile);
			}
		}
	}

	public void onDisable() {
		if (skyBlock.size() != 0 || skyBlock != null) {
			for (Map.Entry<UUID, Location> map : skyBlock.entrySet()) {
				skyBlockSave.set(map.getKey().toString(), map.getValue());
				saveFile(skyBlockSave, skyBlockSaveFile);
				loadedPortalList = null;
			}
		}
	}

	/**
	 * Used to update files that contain new default settings
	 */
	private void matchFile(final FileConfiguration fileConfig, final File file, final String name) {

		final InputStream is = getResource(name + "-defaults.yml");
		if (is != null) {

			final YamlConfiguration defConfig = YamlConfiguration
					.loadConfiguration(new InputStreamReader(this.getResource(name + "-defaults.yml")));
			final File updateConfig = new File(getDataFolder(), name + ".yml");

			for (final String key : defConfig.getConfigurationSection("").getKeys(false)) {
				final YamlConfiguration newConfig = YamlConfiguration.loadConfiguration(updateConfig);
				if (!newConfig.contains(key)) {
					fileConfig.set(key, defConfig.get(key));
					try {
						fileConfig.save(file);
					} catch (IOException e) {
						logger.log(Level.SEVERE, " There was a problem matching the" + name + ".yml!");
					}

				}
			}
		}
	}

	/**
	 * Another method to grab an instance of plugin.
	 * 
	 * @return the plugin instance.
	 */
	public Plugin getInstance() {
		return plugin;
	}

	public void reloadFile(final FileConfiguration fileConfig, final String fileName) {
		final File file = new File(this.getDataFolder(), fileName);
		try {
			fileConfig.load(file);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Saves the config file, to add changes.
	 */
	public void saveFile(final FileConfiguration fileConfig, final File file) {
		if (fileConfig == null || file == null) {
			return;
		}
		try {
			fileConfig.save(file);
		} catch (IOException ex) {
			plugin.getLogger().log(Level.SEVERE, "Could not save to " + fileConfig.getName(), ex);
		}
	}

	/**
	 * Grabs an instance of the WorldGuard plugin
	 * 
	 * @return WorldGuard plugin
	 */
	public WorldGuardPlugin getWorldGuard() {
		final Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("WorldGuard");

		// WorldGuard may not be loaded
		if (!(plugin instanceof WorldGuardPlugin) || plugin == null) {
			return null; // Maybe you want throw an exception instead
		}
		return (WorldGuardPlugin) plugin;
	}

	/**
	 * Checks if the player has the specified permission.
	 * 
	 * @param sender
	 *            Who are we checking
	 * @param permission
	 *            What permission are we checking
	 * @return True or False, Do they have the permission?
	 */
	public boolean hasPermission(final CommandSender sender, final String permission) {
		return sender.hasPermission(permission);
	}

	/**
	 * Creates the economy envoronment for this plugin/
	 * 
	 * @return the instance of the economy, Using vault.
	 */
	public boolean setupEconomy() {
		if (getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}
		final RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return false;
		}
		econ = rsp.getProvider();
		return econ != null;
	}

	/**
	 * Generates the charts to send to metric.
	 * 
	 * @param metrics
	 *            returns charts to send.
	 */
	public void setupCharts(final Metrics metrics) {
		metrics.addCustomChart(new Metrics.SingleLineChart("success") {
			@Override
			public int getValue() {
				// (This is useless as there is already a player chart by
				// default.)
				return successTeleports;
			}
		});
		metrics.addCustomChart(new Metrics.SingleLineChart("failed") {
			@Override
			public int getValue() {
				// (This is useless as there is already a player chart by
				// default.)
				return failedTeleports;
			}
		});
		metrics.addCustomChart(new Metrics.AdvancedPie("teleport_method") {
			@Override
			public HashMap<String, Integer> getValues(HashMap<String, Integer> valueMap) {
				valueMap.put("All", allTeleport);
				valueMap.put("Others", otherTeleport);
				valueMap.put("Portal", portalTeleport);
				valueMap.put("Sign", signTeleport);
				valueMap.put("Join", onJoin);
				valueMap.put("Command", commandTeleport);
				valueMap.put("Warps", warpTeleport);

				return valueMap;
			}

		});
	}

	public FileConfiguration setupFile(final File file) {
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				logger.severe(file.getName() + " may not have been created.");
			}
		}
		return YamlConfiguration.loadConfiguration(file);
	}

	public List<PortalLoaded> loadedPortals() {
		if (RandomCoords.getPlugin().portals.get("Portal") == null) {
			return null;
		}

		// Gets a list of the portals now that we know its not null.
		final Set<String> portals = RandomCoords.getPlugin().portals.getConfigurationSection("Portal").getKeys(false);

		List<PortalLoaded> portalLoadedList = new ArrayList<PortalLoaded>();

		/**
		 * For all of these portals, Check if anyone online is within the
		 * portal.
		 */
		for (final String name : portals) {

			// Gets the world that the portal will go to.
			final String world = RandomCoords.getPlugin().portals.getString("Portal." + name + ".world");
			/**
			 * If this is null then return and log.
			 */
			if (Bukkit.getServer().getWorld(world) == null) {
				// Log the fact that the world is non existent.
				Bukkit.getServer().getLogger().severe(world + " is an invalid world, Change this portal!");
				return null;

			}
			// Get the world that the portal is in.
			final String portalWorld = RandomCoords.getPlugin().portals.getString("Portal." + name + ".PortalWorld");
			/**
			 * Checks if the world the portal is in is null, If so return and
			 * log.
			 */
			if (Bukkit.getServer().getWorld(portalWorld) == null) {
				// Log the fact that the world is null.
				Bukkit.getServer().getLogger().severe(portalWorld + "no longer exists");
				return null;

			}
			// Get the world that the portal is in.
			final World w = Bukkit.getServer().getWorld(portalWorld);
			/**
			 * Get one corner of the portal.
			 */
			final int p1y = RandomCoords.getPlugin().portals.getInt("Portal." + name + ".p1y");
			final int p1z = RandomCoords.getPlugin().portals.getInt("Portal." + name + ".p1z");
			final int p1x = RandomCoords.getPlugin().portals.getInt("Portal." + name + ".p1x");

			/**
			 * Get another corner of the portal.
			 */
			final int p2y = RandomCoords.getPlugin().portals.getInt("Portal." + name + ".p2y");
			final int p2z = RandomCoords.getPlugin().portals.getInt("Portal." + name + ".p2z");
			final int p2x = RandomCoords.getPlugin().portals.getInt("Portal." + name + ".p2x");

			int max = 574272099;
			int min = 574272099;

			if (RandomCoords.getPlugin().portals.get("Portal." + name + ".max") != null) {
				max = RandomCoords.getPlugin().portals.getInt("Portal." + name + ".max");
			}

			if (RandomCoords.getPlugin().portals.get("Portal." + name + ".min") != null) {
				min = RandomCoords.getPlugin().portals.getInt("Portal." + name + ".min");
			}

			final World worldW = Bukkit.getServer().getWorld(world);

			/**
			 * Get the locations of these corners.
			 */
			final Location l1 = new Location(w, p1x, p1y, p1z);// NOPMD
			final Location l2 = new Location(w, p2x, p2y, p2z);// NOPMD

			portalLoadedList.add(new PortalLoaded(name, l1, l2, max, min, worldW.getName(), w.getName()));
		}
		return portalLoadedList;
	}

	public void reloadPortals() {
		loadedPortalList = loadedPortals();
	}
}
