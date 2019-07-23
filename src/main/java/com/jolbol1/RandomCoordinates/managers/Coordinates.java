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

package com.jolbol1.RandomCoordinates.managers;


import com.jolbol1.RandomCoordinates.RandomCoords;
import com.jolbol1.RandomCoordinates.checks.*;
import com.jolbol1.RandomCoordinates.cooldown.Cooldown;
import com.jolbol1.RandomCoordinates.event.RandomTeleportEvent;
import com.jolbol1.RandomCoordinates.managers.Util.Benchmark;
import com.jolbol1.RandomCoordinates.managers.Util.CoordType;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

/**
 * Created by James on 01/07/2016.
 */
public class Coordinates {

    //Setups the Random Generator. Using SecureRandom for more randomness.
    private final SecureRandom random = new SecureRandom();
    //Creates an instance of the message manager class.
    private final MessageManager messages = new MessageManager();
    //Creates an instance of the Grief Prevention class
    private final GriefPreventionCheck gpc = new GriefPreventionCheck();
    //Creates an instance of the Grief Prevention class.
    private final PlayerRadCheck prc = new PlayerRadCheck();
    //Creates an instance of the Town Checker.
    private final TownyChecker tc = new TownyChecker();
    //Creates an instance of the WorldBorder checker.
    private final WorldBorderChecker wbc = new WorldBorderChecker();
    //Creates an instance of the WorldGuard checker.
    private WorldGuardCheck wgc = null;
    private boolean worldGuardEnabled(Location l) {
        if(RandomCoords.getPlugin().getWorldGuard() != null) {
            if(wgc == null) {
                wgc = new WorldGuardCheck();
            }
            return wgc.WorldguardCheck(l);
        } else {
            return true;
        }
    }

    //Creates an instance of the Nether coordinate generator.
    private final Nether nether = new Nether();
    //Creates an instance of the End Island finder.
    private final End end = new End();
    //Grabs the max attempts value from the config.
    private final int maxAttempts = RandomCoords.getPlugin().config.getInt("MaxAttempts");
    //Load up RedProtect
    private final RedProtect redProtect = new RedProtect();
    //Load up KingdomsClaim
    //private final KingdomsClaim kingdomsClaim = new KingdomsClaim();
    //Grab an instance of the debug manager.
    private final DebugManager debugManager = new DebugManager();

    /**
     * Grabs random coordinates within a world.
     * @param player The player that we're teleporting.
     * @param max The Maximum coordinate they can teleport to.
     * @param min The Miniumum coordinate they can teleport to.
     * @param world The world that we're grabbing the coordinate in.
     * @return Random Location within world.
     */
    private Location getRandomCoordinates(final Player player, final int max, final int min, final World world) {
        //Setup the value for the X coordinate as null, But to be changed later.
        int randomX;
        //Setup the value of the Z coordinate as null, But to be changed later.
        int randomZ;
        //Grab the X coordinate of the center of the Random Area
        final int centerX = getCenterX(world);
        //Grab the Z coordinate of the center of the Random Area
        final int centerZ = getCenterZ(world);
        //Sets the minimum as 0, Usually changed
        int minOne = 0;
        int minTwo = 0;
        //We use two here as one is for the X coordinate and one for Z. This fixed the min cross boundary bug.
        int highestPoint;

        /**
         * This generates a random min value for ONE of either X or Y.
         * This is another step in fixing the cross bug. The math may be understandable but ask Jolbol if you dont understand.
         */
        if (min != 0) {
            if (modulus() == 1) {
                minOne = 0;
                minTwo = min;
            } else {
                minOne = min;
                minTwo = 0;
            }
        }

        /**
         * Should we get the random numbers from Java or Random.org.
         * We then multiply this by the modulus function to allow for Positive and negative coordinates.
         */
        if (RandomCoords.getPlugin().config.getString("RandomOrg").equalsIgnoreCase("true")) {
            randomX = Integer.valueOf(getRandomOrg(minOne, max)) * modulus();
            randomZ = Integer.valueOf(getRandomOrg(minTwo, max)) * modulus();
        } else {
            randomX = getRandomNumberInRange(minOne, max, player) * modulus();
            randomZ = getRandomNumberInRange(minTwo, max, player) * modulus();
        }

        //Just uses a Y = 90 as a default, Is always changed to highest block... I hope.
        final Location preRandom = new Location(world, centerX + (randomX + 0.5), 90, centerZ + (randomZ + 0.5));

        /**
         * If the world is actual the nether, use the Nether class to find a Y coordinate thats safe.
         * This is to avoid .getHighestBlockAt returning the bedrock on top of the nether. (Will take longer to grab coordinate)
         */
        Biome netherBiome;
        if(RandomCoords.getServerVersion() >= 13){
            netherBiome = Biome.valueOf("NETHER");
        } else {
            netherBiome = Biome.valueOf("HELL");
        }

        if (world.getBiome(centerX, centerZ).equals(netherBiome)) {
            highestPoint = nether.getSafeNetherY(preRandom);

        } else {
            highestPoint = world.getHighestBlockYAt(preRandom);
        }

        //Returns a random location they may be able to spawn to.
        return new Location(world, centerX + (randomX + 0.5), highestPoint, centerZ + (randomZ + 0.5));

    }

    /**
     * Gets a random number using the Java function.
     * @param min The minimum this number can be.
     * @param max The maxmimu this number can be.
     * @param sender Who asked for the random number.
     * @return Random number within the range, Min Max.
     */
    public int getRandomNumberInRange(final int min, final int max, final CommandSender sender) {
        /**
         * Checks if the minimum is greater than the max, thus would return an error.
         */
        if (min >= max) {
        }

        //Returns a random number
        return random.nextInt((max - min) + 1) + min;
    }

    //Function to select 1 or -1 to generate positive and negative coordinates, Kinda a reverse modulus but whatever.

    /**
     * Gets a random number thats either -1 or 1.
     * @return Randomly, Either 1 or -1.
     */
    private int modulus() {
        //Grabs a random boolean, then returns -1 or 1 based on the
        return  (random.nextBoolean() ? -1 : 1);
    }



    /**
     * Generates the final coordinate that a player is ready to teleport to.
     * @param player The player to teleport.
     * @param max The max coordinate they can teleport to.
     * @param min The min coordinate they can teleport to.
     * @param world The world to teleport them in.
     * @param type The method in which they requested the teleport.
     * @param cost The cost of the teleport.
     */
    public void finalCoordinates(final Player player, int max, int min, final World world, final CoordType type, final double cost) {
        //Sets the default search term in the config as "Command". May be changed based on the CoordType.
        String name = "Command";
        //Sets the default search term for the cost name as Default.
        String costName = "default";
        final double thisCost = cost;
        //Sets time before as 0, may be changed later.
        int timeBefore = 0;
        //Sets the cooldown as 0, may be changed later.
        int cooldown = 0;

        /**
         * Changes the search term based on the CoordType provided in the parameters.
         */
        switch (type) {
            case COMMAND:
                name = "Command";
                costName = "CommandCost";
                break;
            case ALL:
                name = "All";
                break;
            case PLAYER:
                name = "Others";
                break;
            case SIGN:
                name = "Signs";
                break;
            case JOIN:
                name = "Join";
                break;
            case PORTAL:
                name = "Portals";
                costName = "PortalCost";
                break;
            case WARPS:
                name = "Warps";
                break;
            case WARPWORLD:
                name = "Warps";
                break;
			case JOINWORLD:
				name = "JoinWorld";
				break;
			default:
				name = "Command";
				costName = "CommandCost";
				break;

        }

        if(!hasMoney(player, costApplys(thisCost, costName))) {
            messages.cost(player, costApplys(thisCost, costName));
            return;
        }
        //Changes the limiter boolean to the one provided by the check.

        /**
         * If the limiter is false, AKA they have reached it. Cancel.
         */
        if (reachedLimit(player, name)) {
            return;
        }



        //Changes the cooldown, and sees if it applys.
        cooldown = cooldownApplys(player, cooldown, name);
        if (inCooldown(player, cooldown)) {
            final int secondsLeft = Cooldown.getTimeLeft(player.getUniqueId(), "Command");
            messages.cooldownMessage(player, secondsLeft);
            return;
        }



        //Sees if timeBefore applys and will change it if so.
        timeBefore = timeBeforeApplys(player, timeBefore, name);
        if (inTimeBefore(player, timeBefore)) {
            return;
        }
        //Changes the cost, and sees if it applys.
        if(!hasPayed(player, costApplys(thisCost, costName))) {
            return;
        }



        if (isWorldBanned(player, world)) {
            return;
        }

        /**
         *If the maxmimum is the secret key, AKA not provided, Then set it to the configured max.
         */
        if (max == 574272099) {
            max = getMax(world);
        }
        /**
         * If the minimum is the secret key, AKA not provided, Then set it to the configured min.
         */
        if (min == 574272099) {
            min = getMin(world);
        }

        exitLoop(player, max, min, world, type, name, thisCost, timeBefore, cooldown);
    }

    
    /**
     * This creates the loop to check for a safe location to teleport users to.
     */
    public void exitLoop(Player player, int max, int min, World world, CoordType type, String name, double thisCost, int timeBefore, int cooldown) {
        new BukkitRunnable() {
        	int attempts = 0;
            Location center;
            Location locationTP;        	
            Benchmark benchmark = new Benchmark("Coordinates.exitLoop.AttemptsPerTick");
            int attemptsPerTick = RandomCoords.getPlugin().config.getInt("AttemptsPerTick");
            boolean fin=false;
            int informPlayerAttempts = RandomCoords.getPlugin().config.getInt("InformPlayerAfterAttempts");
            
            @Override
			public void run() {
            	if(!fin) {
		            /**
		             * If the number of attempts has reached the maximum, Then kill the loop.
		             */	
	            	benchmark.start();
	            	for(int i=0; i<attemptsPerTick;i++) {
	    	            if (attempts >= maxAttempts) {
	    	                messages.couldntFind(player);
	    	                RandomCoords.getPlugin().failedTeleports++;
	    	                benchmark.stop();
	    	                fin=true;
	    	                this.cancel();
	    	                return;
	    	            }
	    	            
	    				if(attempts == informPlayerAttempts) {
	    					messages.takesLonger(player);
	    				}	            
	    	            
	    	            locationTP = getRandomCoordinates(player, max, min, world);
	    	            //Get the coordinate that is going to be tested.
	    	            locationTP = getRandomCoordinates(player, max, min, world);
	    	            //Get the boundary center of the world.
	    	            center = new Location(world, getCenterX(world), locationTP.getY(), getCenterZ(world));
	    	            
	    	            /**
	    	             * If the location is not safe, or isnt in the circular radius, then add to the attempts.
	    	             * Otherwise, Start the proccess of teleporting, Such as cooldowns, time befores and limiter checks.
	    	             */
	    	            if (!isLocSafe(locationTP) || circleRadius(locationTP, max, center)) {
	    	                //Adds to attempts.
	    	                attempts++;
	    	                debugManager.logToFile("\n" + attempts + "LocSafe: " + String.valueOf(isLocSafe(locationTP)) + " Circle: " + String.valueOf(circleRadius(locationTP, max, center)) + "\n");
	    	                //Keeps the loop going.
	    	            }
	    	            else{
	    	                //Sets the location. Mainly used to set it to a warp if that is the coord type.
	    	                locationTP = shouldWarp(player, world, locationTP, name, type);
	    	                //Adds the buffer to the teleport location
	    	                if(!(type == CoordType.WARPWORLD || type == CoordType.WARPS)) {
	    	                    locationTP = addBuffer(locationTP);
	    	                }
	    	                /**
	    	                 * Standard check to see if something has gone wrong. If the location is null, then cancel. Seldom use.
	    	                 */
	    	                if (locationTP == null) {
	    	                	benchmark.stop();
	    	                	fin=true;
	    	                    return;
	    	                }
	    	                /**
	    	                 * Then schedule the final teleport.
	    	                 */
	    	                limiterApplys(player, name);
	    	                scheduleStuff(player, locationTP, thisCost, player.getHealth(), player.getLocation(), timeBefore, cooldown, type);
	    	                //Stops the loop.
	    	                benchmark.stop();
	    	                fin=true;
	    	                this.cancel();
	    	                return;
	    	            }
	            	}
            	}
			}
        }.runTaskTimer(RandomCoords.getPlugin(), 0L, 0L);    	
    }
    

    /**
     * Checks if the location is safe.
     * @param location The location to check.
     * @return True or False, Is it safe?
     */
    private boolean isLocSafe(final Location location) {
        /**
         *This checks if the nether Y coord is left unchanged, thus bad Y coord. Seldom use.
         */
        Biome netherBione;
        if(RandomCoords.getServerVersion() >= 13){
            netherBione = Biome.valueOf("NETHER");
        } else {
            netherBione = Biome.valueOf("HELL");
        }

        if (location.getWorld().getBiome(location.getBlockX(), location.getBlockZ()).equals(netherBione)) {
            if (location.getY() == 574272099) {
                return false;
            }
        }
        // Get the block and its material thats highest
        final Block block = location.getBlock();
        final Material material = block.getType();
        //Get the material of the top block, e.g Grass layer
        final Material material1 = block.getRelative(BlockFace.DOWN).getType();
        //Get the block thats 1 below the surface
        final Location logLoc = new Location(location.getWorld(), location.getBlockX(), location.getY() - 1, location.getBlockZ());
        final Block log = logLoc.subtract(0, 1, 0).getBlock();
        final Material matt = log.getType();
        //Get the block that is 2 below the surface.
        final Block log2 = logLoc.subtract(0, 1, 0).getBlock();
        final Material matt2 = log2.getType();
        /**
         * Run through the blacklisted list and if the block is on the blacklist, Then return false.
         */
        for(final String materialName : RandomCoords.getPlugin().blacklist.getStringList("Blacklisted")) {
            final Material blacklist = Material.valueOf(materialName);
            if(material1 == blacklist || material == blacklist || matt2 == blacklist || matt == blacklist) {
                debugManager.logToFile("Material1: " + material.toString() + "\nMaterial: " + material.toString() + "\nMatt2: " + matt2.toString() + "\nMatt: " + matt.toString());
                return false;
            }
        }

        /**
         * Otherwise, If the block is safe, Is it in any of the regions?
         */
        if(RandomCoords.getPlugin().config.getString("debug").equalsIgnoreCase("true")) {
            debugManager.logToFile("\nGrief: " + String.valueOf(gpc.griefPrevent(location)) + "\nPlayer: " + String.valueOf(prc.isPlayerNear(location)) + "\nTowny: " + String.valueOf(tc.TownyCheck(location)) + "\nWorldBorder: " + String.valueOf(wbc.WorldBorderCheck(location)) + "\nWorldGuard: " + String.valueOf(worldGuardEnabled(location)) + "\nVanillaBorder" + String.valueOf(!isOutsideBorder(location)));
        }
        //return kingdomsClaim.KingdomsClaim(location) && redProtect.RedProtect(location) && fc.FactionCheck(location) && gpc.griefPrevent(location) && prc.isPlayerNear(location) && tc.TownyCheck(location) && wbc.WorldBorderCheck(location) && worldGuardEnabled(location) && !isOutsideBorder(location);
        return redProtect.RedProtect(location) && gpc.griefPrevent(location) && prc.isPlayerNear(location) && tc.TownyCheck(location) && wbc.WorldBorderCheck(location) && worldGuardEnabled(location) && !isOutsideBorder(location);
    }


    /**
     * Generates a random number from Random.org
     * @param min The minimum this number can be.
     * @param max The maximum this number can be.
     * @return Random number from Random.Org within range, Max Min.
     */
    private String getRandomOrg(final int min, final int max) {
        //Gets the site URL we're reading from.
        URL site;
        //Sets the string as null, until its changed when we read.
        String random = null;

        /**
         * Trys to grab the coorinates from the Random.Org site.
         * Catch if it cant, logs an error in the console.
         */
        try {
            //Website URL.
            final String web = "https://www.random.org/integers/?num=1&min=" + min + "&max=" + max + "&col=1&base=10&format=plain&rnd=new";
            //Sets it as the site to be read from.
            site = new URL(web);

            /**
             * Tries using buffered reader to read the line on the page.
             */
            try (BufferedReader in = new BufferedReader(new InputStreamReader(site.openStream()))) {
                //Set the line
                String line;
                /**
                 * If the line is not null, set Random as the line number.
                 */
                while ((line = in.readLine()) != null) {
                    //Set string random as line.
                    random = line;
                }
            }
        } catch (IOException ignored) {
            //Log if we couldnt read the line of the site.
            RandomCoords.logger.severe("Couldnt grab coordinates from Random.ORG!");
        }
        //Return the number as a string.
        return random;
    }


    /**
     * Generates the chunk that the location is in. Seldom use.
     * @param location The location in which to load.
     * @return True or False, Is the chunk loaded yet?
     */
    private boolean generateChunk(final Location location) {
        //The world that we're loading the chunk in.
        final World world = location.getWorld();
        //The chunk itself that we are loading.
        final Chunk chunk = world.getChunkAt(location);
        /**
         * Checks if the chunk is loaded, if not an attempt is made to load.
         */
        if (chunk.isLoaded()) {
            return true;
        } else {
            chunk.load();
            return false;
        }
    }



    /**
     * This function handles Random Warps.
     * @param p The player we are teleporting
     * @param world The world we're Randomly Warping them in.
     * @param coordType The method in which they requested the warp.
     * @return Returns one of the Warps at random.
     */
    private Location warpTP(final Player p, final World world, final CoordType coordType) {

        /**
         * Checks if the world that player is in is banned.
         */
        for (final String worlds : RandomCoords.getPlugin().config.getStringList("BannedWorlds")) {
            if (world.getName().equals(worlds)) {
                messages.worldBanned(p);
                return null;

            }
        }

        //Initiate the warp list.
        Set<String> list = null;

        /**
         * Checks if there is actually any warps set, If so set it to the list above.
         * Else return no warp message and set location as null.
         */
        if (RandomCoords.getPlugin().warps.get("Warps") != null) {
            //Set the list to the one from the config.
            list = RandomCoords.getPlugin().warps.getConfigurationSection("Warps.").getKeys(false);
        } else if(list == null) {
            messages.noWarps(p);
            return null;
        }

        //Copy the list to be able to edit in for loop.
        final Set<String> listCopy = new HashSet<>(list);

        /**
         * If the list size has been initiated but theres nothing in it, also return no warp message.
         */
        if (listCopy.size() == 0) {
            messages.noWarps(p);
            return null;
        } else if (listCopy.size() != 0) {

            //Starts teh attempt to grab the warps as 0. Adds 1 everytime the random number isnt equal to this.
            int i = 0;
            //Initiates the name of the warp
            String wName;
            //Initiates the X coordinate to teleport to.
            double x;
            //Initiates the Y coordinate to teleport to.
            double y;
            //Initiates the Z coordinate to teleport to.
            double z;
            //Initiates the overall location of it.
            Location location;

            /**
             * Checks is warp cross world is disabled or if a world was specified.
             * If so, remove all the warps from other worlds.
             */
            if (RandomCoords.getPlugin().config.getString("WarpCrossWorld").equalsIgnoreCase("false") || coordType.equals(CoordType.WARPWORLD)) {

                //If not remove all from list that are not in the current players world
                for (final String obj : list) {
                    final World objWorld = Bukkit.getServer().getWorld(RandomCoords.getPlugin().warps.getString("Warps." + obj + ".World"));
                    if (objWorld != world) {
                        //noinspection SuspiciousMethodCalls
                        listCopy.remove(obj);
                    }
                }
                /**
                 * If the list is empty, just return the no warp message.
                 */
                if (listCopy.isEmpty()) {
                    messages.noWarpsWorld(p, world.getName());
                    return null;
                }

            }


            //Whats the size of the list, to know the max min of the random number.
            final int size = listCopy.size();

            //Grabs a random number between 0 and the size of the list
            final int myRandom = random.nextInt(size);

            /**
             * For every object in the list, Check if the number assigned to it is eqaul to the random number generated.
             * If not, Add 1 and repeat in the loop.
             */
            for (final Object obj : listCopy) {
                if (i == myRandom) {
                    //Get the location of the warp that Obj refers to.
                    wName = RandomCoords.getPlugin().warps.getString("Warps." + obj.toString() + ".World");
                    x = RandomCoords.getPlugin().warps.getDouble("Warps." + obj.toString() + ".X");
                    y = RandomCoords.getPlugin().warps.getDouble("Warps." + obj.toString() + ".Y");
                    z = RandomCoords.getPlugin().warps.getDouble("Warps." + obj.toString() + ".Z");
                    location = new Location(Bukkit.getServer().getWorld(wName), x, y, z);
                    return location;


                } else {
                    //If it wasnt, Add 1 to I and re-run.
                    i = i + 1;
                }


            }
        }

        return null;
    }


    //Check to see if the player has reached the limit.

    /**
     * Checks if the player has reached the random teleport limit.
     * @param player The player we're checking.
     * @return True or False, have they reached the limit.
     */
    private boolean isLimiter(final Player player) {

        /**
         * Checks if the player is allowed to bypass the limiter with the permission.
         * If they are, then we will return true.
         * If they are not, we will, provided a Limit is set in the config add to their uses on teleport.
         * If they have reached the limit return 0.
         * If the limit is 0, return true.
         */
        if (player.hasPermission("Random.Limiter.Byapss")) {
            return true;
        } else if (RandomCoords.getPlugin().config.getInt("Limit") != 0) {

            //Save as UUID in order to allow for name changes.
            final String uuid = player.getUniqueId().toString();

            //Grabs the limiter file.
            final FileConfiguration limiter = RandomCoords.getPlugin().limiter;

            /**
             * If the players has no limit set in the limiter file, Create one and return true.
             */
            if (limiter.get(uuid) == null) {
                limiter.set(uuid + ".Uses", 1);
                RandomCoords.getPlugin().saveFile(RandomCoords.getPlugin().limiter, RandomCoords.getPlugin().limiterFile);
                return true;
            }

            //Grab their uses.
            final int used = limiter.getInt(uuid + ".Uses");
            //Grab the limit.
            final int limit = RandomCoords.getPlugin().config.getInt("Limit");

            /**
             * If the amount used is less than the limit, the add 1 and return true.
             * If the amount used however is greater than the limit, send limit reached message and return true.
             */
            if (used < limit) {
                //Add one to their limit.
                limiter.set(uuid + ".Uses", used + 1);
                //Save the chages to the limiter file.
                RandomCoords.getPlugin().saveFile(RandomCoords.getPlugin().limiter, RandomCoords.getPlugin().limiterFile);
                return true;

            } else {
                messages.reachedLimit(player);
                return false;
            }
        } else {
            //If the limit is 0, AKA Off, the return true always.
            return true;
        }
    }

    //Used to shcedule the main basis, to avoid code duplication in the coordinates class.

    /**
     * Schedules the final teleport. Used to handle time before and Cooldowns.
     * @param player The player to teleport.
     * @param locationTP The location to teleport them too.
     * @param thisCost The cost of the teleport.
     * @param health The health of the player before teleport.
     * @param start The start location of the player.
     * @param timeBefore How long to actually wait before teleporting.
     * @param cooldown How long until they can teleport again.
     */
	private void scheduleStuff(final Player player, final Location locationTP, final double thisCost,
			final double health, final Location start, final int timeBefore, final int cooldown,
			final CoordType coordType) {
		// Setup an instance of Bukkit scheduler.
		final BukkitScheduler s = RandomCoords.getPlugin().getServer().getScheduler();
		// Setup an instance of the cooldown labelled command.
		final Cooldown cool = new Cooldown(player.getUniqueId(), "Command", cooldown);

		/**
		 * Starts the scheduler that handles all of the teleportation, and move
		 * check processes. All inside here activates after the TimeBefore
		 * period.
		 */
		s.runTaskLater(RandomCoords.getPlugin(), new Runnable() {
			/**
			 * Checks if we should stop the teleportation on move. If true, we
			 * then check if they have moved. If yes, the proccess is cancelled
			 * and we send them the move message.
			 */

			@Override
			public void run() {
				if (RandomCoords.getPlugin().config.getString("StopOnMove").equalsIgnoreCase("true")) {
					/**
					 * This is where we check if they have moved using the
					 * variables provided in the parameters.
					 */
					if (!coordType.equals(CoordType.JOINWORLD)) {
						if ((start.getWorld() != player.getWorld()) || (start.distance(player.getLocation()) > 1)) {
							messages.youMoved(player);
							return;
						}
					}
				}
				/**
				 * Checks if we should cancel the teleport on damage.
				 */
				if (RandomCoords.getPlugin().config.getString("StopOnCombat").equalsIgnoreCase("true")) {
					/**
					 * This is where we check the health of the player VS. the
					 * health of the player at the start of the scheduler. If
					 * the health is lower, Cancel the teleport as they have
					 * taken damage.
					 */
					if (health > player.getHealth()) {
						messages.tookDamage(player);
						return;
					}
				}
				// Sets the variable for the chunk loader as false so that it
				// continues to loop.
				boolean exit = false;
				/**
				 * The loop that handles the loading of the chunk that they are
				 * teleporting into. If the config options false, Exit the loop.
				 * If the chunks generated, exit the loop.
				 */
				while (!exit) {
					// The boolean thats used to cancel or continue the loop. If
					// chunk loader is off, set as true. If chunk is generated
					// set as trie.
					exit = RandomCoords.getPlugin().config.getString("ChunkLoader").equalsIgnoreCase("false")
							|| generateChunk(locationTP);
					/**
					 * If exit is true, Charge the player, and teleport them.
					 * Then start the cooldown.
					 */
					if (exit) {

						RandomTeleportEvent event = new RandomTeleportEvent(player, locationTP, coordType, cooldown);
						Bukkit.getServer().getPluginManager().callEvent(event);

						if (!event.isCancelled()) {
							// Teleports the player finally to the safe location
							event.getPlayer().teleport(event.location());

							if (event.coordType().equals(CoordType.JOIN)
									|| event.coordType().equals(CoordType.JOINWORLD)) {
								if (RandomCoords.getPlugin().config.get("OnJoinCommand") instanceof String) {
									if (RandomCoords.getPlugin().config.getString("OnJoinCommand")
											.equalsIgnoreCase("none")) {
										return;
									}

									Bukkit.getServer().dispatchCommand(event.getPlayer(),
											RandomCoords.getPlugin().config.getString("OnJoinCommand"));

								} else if (RandomCoords.getPlugin().config.get("OnJoinCommand") instanceof List) {
									if (RandomCoords.getPlugin().config.getStringList("OnJoinCommand")
											.contains("none")) {
										return;
									}

									for (String n : RandomCoords.getPlugin().config.getStringList("OnJoinCommand")) {
										Bukkit.getServer().dispatchCommand(event.getPlayer(), n);

									}
								}
							}

							// Should we put a bonus chest at this location?
							bonusChests(event.getPlayer(), event.location());
							// Adds 1 To successful teleports for metrcis.
							RandomCoords.getPlugin().successTeleports++;
							// Start the cooldown.
							cool.start();
							/**
							 * Checks if we should play a sound. If so try to,
							 * If the config is set wrong log it.
							 */
							if (!RandomCoords.getPlugin().config.getString("Sound").equalsIgnoreCase("false")) {
								// Get the name of the sound to be played.
								final String soundName = RandomCoords.getPlugin().config.getString("Sound");
								/**
								 * Attempts to play the sound at the location
								 * they have teleported to. Catches if the
								 * config is typed incorrectly and will log
								 * this.
								 */
								try {
									// Play the sound.
									player.playSound(locationTP, Sound.valueOf(soundName), 1, 1);
								} catch (IllegalArgumentException e) {
									// Log if the sound is incorrect.
									Bukkit.getServer().getLogger().log(Level.WARNING,
											"Sound: " + soundName + " does not exist!");
								}

							}
							/**
							 * Checks if we should play an effect. If so try it,
							 * If the config is set incorrectly, Log it.
							 */
							if (!RandomCoords.getPlugin().config.getString("Effect").equalsIgnoreCase("false")) {
								// Get the effect name from the config.
								final String effectName = RandomCoords.getPlugin().config.getString("Effect");
								/**
								 * Try to play the effect at the players
								 * location. Catch if the config is set
								 * incorrectly and log that.
								 */
								try {
									// Play the effect at the location
									locationTP.getWorld().playEffect(locationTP, Effect.valueOf(effectName), 1);
								} catch (IllegalArgumentException e) {
									// Log the fact that the config is
									// incorrect.
									Bukkit.getServer().getLogger().log(Level.WARNING,
											"Effect: " + effectName + " does not exist!");
								}

							}
							/**
							 * This starts the cooldown for the suffocation
							 * check. This is what is checked for in the
							 * Suffocation class.
							 */
							final Cooldown c = new Cooldown(player.getUniqueId(), "Invul", 30);
							c.start();
							/**
							 * Start the Invul timer, This is used if the users
							 * would like the players to be invul for a while
							 * after TP.
							 */
							final Cooldown cT = new Cooldown(player.getUniqueId(), "InvulTime",
									RandomCoords.getPlugin().config.getInt("InvulTime"));
							cT.start();
							// Send the message that they have teleported.
							messages.teleportMessage(player, locationTP);
						}
					}
					;
				}
			}

		}, Math.max(timeBefore * 20L, 1));
	}


    /**
     * Manages the bonus chest feature of the plugin. Whether or not we should spawn one for them.
     * @param player The player thats been teleported.
     * @param locationTP The location they were teleported to.
     */
    public void bonusChests(final Player player, final Location locationTP) {
        /**
         * Checks if the bonus chest feature is active. If not it does nothing.
         */
        if (RandomCoords.getPlugin().config.getString("BonusChest").equalsIgnoreCase("true")) {
            /**
             * If the player hasnt already recieved a chest, Spawn one in for them.
             * Else do nothing.
             */
            if (RandomCoords.getPlugin().limiter.getString(player.getUniqueId() + ".Chest") == null) {
                //Gets the location for the chest. Takes the teleport location and adds 1 to The X, -2 to add it to the ground and 1 to the Z.
                final Location chestLoc = new Location(locationTP.getWorld(), locationTP.getBlockX() + 1.0, locationTP.getBlockY() - 2, locationTP.getBlockZ() + 1.0);
                //This uses the chestloc location but adds 1 to the Y in order to allow opening of the chest wherever.
                final Location airLoc = new Location(chestLoc.getWorld(), chestLoc.getBlockX(), chestLoc.getBlockY() + 1, chestLoc.getBlockZ());
                //Gets the acual block at the location of the chest.
                final Block chestBlock = chestLoc.getBlock();
                //Gets the actual block at the location of the air block.
                final Block airBlock = airLoc.getBlock();
                //Sets the airblock to AIR.
                airBlock.setType(Material.AIR);
                //Sets the chest block to a chest.
                chestBlock.setType(Material.CHEST);
                //Get the chest at the location, Create an instance of it.
                final Chest chest = (Chest) chestBlock.getState();
                //Get the inventory of this chest.
                final Inventory chestInv = chest.getInventory();
                /**
                 * For all the items in the config list: BonusChestItems, Create an itemstack and add it to the Chest.
                 * This will also check for Essentials kits that may want to be put in the chest.
                 */
                for (final String material : RandomCoords.getPlugin().config.getStringList("BonusChestItems")) {
                    /**
                     * If the string in the list is an essentials kit, Check if essentials is enabled, and add it to the chest.
                     */
                    if (material.contains("Essentials,")) {
                        /**
                         * This checks that they actually are using essentials.
                         */
                        if (Bukkit.getPluginManager().getPlugin("Essentials") != null) {
                            //This grabs an instance of the KitManager class, Where most essentials kit stuff is handled.
                            final KitManager kitManager = new KitManager();
                            //Seperate the Essentials bit from the kit name.
                            final String[] kits = material.split(",");
                            /**
                             * For the kit names, Not the essentials bit, Get the kit from the kit manager class.
                             */
                            for (final String kit : kits) {
                                /**
                                 * If the Kit isnt the Essentials keyword, Add it to the chest.
                                 */
                                if (!kit.equals("Essentials")) {
                                    //Get the kit from the kit manager class.
                                    kitManager.getKit(player, chest, kit);
                                    //Tell the limiter file that they have had a bonus chest.
                                    RandomCoords.getPlugin().limiter.set(player.getUniqueId() + ".Chest", "true");
                                    //Save these changes to the limiter file.
                                    RandomCoords.getPlugin().saveFile(RandomCoords.getPlugin().limiter, RandomCoords.getPlugin().limiterFile);


                                }
                            }
                        }
                    } else {
                        /**
                         * If the material is not null, Create an itemstack and add it to the chest.
                         * If not, log that the item isnt configured properly.
                         */
                        if (Material.getMaterial(material) != null) {
                            //Create the itemstack for the listed item.
                            final ItemStack itemStack = new ItemStack(Material.getMaterial(material), 1);
                            //Add the item to the chest.
                            chestInv.addItem(itemStack);
                            //Tell the limiter file they have recieved a chest.
                            RandomCoords.getPlugin().limiter.set(player.getUniqueId() + ".Chest", "true");
                            //Save these changes.
                            RandomCoords.getPlugin().saveFile(RandomCoords.getPlugin().limiter, RandomCoords.getPlugin().limiterFile);
                        } else {
                            //Log that it is not a material.
                            Bukkit.getLogger().log(Level.WARNING, material + "  is not a material.");
                        }
                    }

                }
            }


        }
    }

    /**
     * Checks to see if location is within a circular range of the center.
     * @param loc The location we're checking.
     * @param radius The radius of the circle to check.
     * @param center The center of the circle we're checking.
     * @return True or False, Is it within the circle.
     */
    private boolean circleRadius(final Location loc, final double radius, final Location center) {
        /**
         * Returns a boolean if the location is outside of the radius.
         * AND checks if the CircleRadius is default overall, or for the world.
         */
        return (RandomCoords.getPlugin().config.getString("CircleRadiusDefault").equalsIgnoreCase("true") || RandomCoords.getPlugin().config.getStringList("CircleRadiusWorlds").contains(loc.getWorld().getName())) && center.distanceSquared(loc) >= (radius * radius);
    }

    /**
     * Checks if the locations outside of the world border.
     * @param loc The location we're checking
     * @return True or False, Is it outside of the location.
     */
    private boolean isOutsideBorder(final Location loc) {
        /**
         * Checks if we should check for VanillaBorders. Else return false.
         */
        if (RandomCoords.getPlugin().config.getString("VanillaBorder").equalsIgnoreCase("true")) {
            //Get the worlds world border.
            final WorldBorder wb = loc.getWorld().getWorldBorder();
            //Get the center of that border.
            final Location center = wb.getCenter();
            //Get the size of that border.
            final double size = wb.getSize();
            //Divide that size in half to get the radius of the border.
            final double radius = size / 2;
            //Get the X coord of the center.
            final int centerX = center.getBlockX();
            //Get the Z coord of the center.
            final int centerZ = center.getBlockZ();
            //Get the X coord of the player.
            final int playerX = loc.getBlockX();
            //Get the Z coord of the Player.
            final int playerZ = loc.getBlockZ();

            /**
             * If the player is outside the border, then return true.
             */
            if (playerX > (centerX + radius) || playerX < (centerX - radius) || playerZ > (centerZ + radius) || playerZ < (centerZ - radius)) {
                return true;
            }
        }
        return false;


    }

    /**
     * Gets the maximum coordinate that we can teleport them to for the world.
     * @param world The world we want the max of.
     * @return The maximum coordinate.
     */
    public int getMax(final World world) {
        //Initiate the X coordinate of the spawnpoint
        int spawnX;
        //Initiate the Z coordinate of the spawnpoint
        int spawnZ;
        //Initiate the world max.
        int max;
        /**
         * If a custom center for the world has been set, Set spawn X as the X coord provided.
         * Else, SpawnX is the spawn of the world.
         */
        if (RandomCoords.getPlugin().config.get(world.getName() + ".Center.X") != null) {
            //Set the spawnX as the coordinate in the config.
            spawnX = RandomCoords.getPlugin().config.getInt(world.getName() + ".Center.X");
        } else {
            //Set the spawnX as the spawn location.
            spawnX = world.getSpawnLocation().getBlockX();
        }
        /**
         * If a custom center for the world has been set, set spawnZ as the Z coord provided.
         * Else, Set spawnZ as the world spawn Z coordinate.
         */
        if (RandomCoords.getPlugin().config.get(world.getName() + ".Center.Z") != null) {
            //Set the spawnZ as the coordinate in the config.
            spawnZ = RandomCoords.getPlugin().config.getInt(world.getName() + ".Center.Z");
        } else {
            //Set the spawnZ as the coordinate of the spawn.
            spawnZ = world.getSpawnLocation().getBlockZ();
        }

        /**
         * If a custom max for the world has been set, Grab it.
         * Else use the default one provided.
         */
        if (RandomCoords.getPlugin().config.get(world.getName() + ".Max") != null) {
            //Set the max as the one provided for the world in the config.
            max = RandomCoords.getPlugin().config.getInt(world.getName() + ".Max");
        } else {
            //Set it as the default max in the coordinate.
            max = RandomCoords.getPlugin().config.getInt("MaxCoordinate");
        }


        /**
         * This manages what happens to the max if the center and world border center are the same, And the Max coord is greater than the border.
         * If they want the border to be checked in the config.
         */
        if (RandomCoords.getPlugin().config.getString("VanillaBorder").equalsIgnoreCase("true")) {
            //Get the worlds WorldBorder.
            final WorldBorder border = world.getWorldBorder();
            //Get the center of the world border.
            final Location center = border.getCenter();
            //Get the X coordinate of the center.
            final int borderX = center.getBlockX();
            //Get the Z coordinate of the center.
            final int borderZ = center.getBlockZ();
            //Get the size of the world border.
            final double size = border.getSize();
            //Get the radius.
            final double radius = size / 2;
            /**
             * If the border center is equal to the center point, Then change the max if need be.
             */
            if (spawnX == borderX && spawnZ == borderZ) {
                /**
                 * If the radius of the border is less than the max coordinate, Change it to the radius.
                 */
                if (radius < max) {
                    //Set the max as the radius.
                    max = (int) radius;
                }
            }

        }
        //Return a suitable value for the max.
        return max;


    }

    /**
     * Gets the minimum coordinate that we can teleport them to for the world.
     * @param world The world we want the min of.
     * @return The minimum coordinate.
     */
    public int getMin(final World world) {
        //Initiate a number for the min coordinate.
        int min;
        /**
         * If a Min coordinate is provided, Set it. If not set it as the default one.
         */
        if (RandomCoords.getPlugin().config.get(world.getName() + ".Min") != null) {
            //Set the min to the world value if provided.
            min = RandomCoords.getPlugin().config.getInt(world.getName() + ".Min");
        } else {
            //Else set the min coordinate to the default one in the config.
            min = RandomCoords.getPlugin().config.getInt("MinCoordinate");
        }
        //Return the minimum coordinate we gathered.
        return min;
    }

    /**
     * Gets the center X coordinate of the world.
     * @param world The world we want the center of.
     * @return The X coordinate.
     */
    private int getCenterX(final World world) {
        //Initiate a value for the center X of the world.
        int spawnX;
        /**
         * If a center is provided, Set the X to that. If not, Set the X to the spawn point.
         */
        if (RandomCoords.getPlugin().config.get(world.getName() + ".Center.X") != null) {
            //Set spawnX as the value for the world provided in the config.
            spawnX = RandomCoords.getPlugin().config.getInt(world.getName() + ".Center.X");
        } else {
            //Set the value of the spawnX to the spawn location of the world.
            spawnX = world.getSpawnLocation().getBlockX();
        }
        //Return the spawnX value gathered from above.
        return spawnX;
    }

    /**
     * Gets the center Z coordinate of the world.
     * @param world The world we want the center of.
     * @return The Z coordinate.
     */
    private int getCenterZ(final World world) {
        //Initiate a value for the center Z of the world.
        int spawnZ;
        /**
         * If a center is provided, Set the Z to that. If not, Set the Z to the spawn point.
         */
        if (RandomCoords.getPlugin().config.get(world.getName() + ".Center.Z") != null) {
            //Set spawnZ to the one provided for the world in the config.
            spawnZ = RandomCoords.getPlugin().config.getInt(world.getName() + ".Center.Z");
        } else {
            //Set the spawnZ to the one thats the worlds spawn location.
            spawnZ = world.getSpawnLocation().getBlockZ();
        }
        //Return a suitable spawnZ value.
        return spawnZ;
    }

    /**
     * Adds a buffer to the location.
     * @param locationTP The location we want to add the buffer too.
     * @return The new location, add the buffer.
     */
    private Location addBuffer(Location locationTP) {
        /**
         * If the biome is hell, Only add 1 to the location.
         * Else if the biome is the sky, Get the highest block for the end coord. (May seem odd to do this here, but why not)
         * Else add the default 2.5 amount.
         */
        Biome netherBiome;
        Biome endBiome;
        if(RandomCoords.getServerVersion() >= 13){
            netherBiome = Biome.valueOf("NETHER");
            endBiome = Biome.valueOf("THE_END");
        } else {
            netherBiome = Biome.valueOf("HELL");
            endBiome = Biome.valueOf("SKY");
        }

        if (locationTP.getWorld().getBiome(locationTP.getBlockX(), locationTP.getBlockZ()).equals(netherBiome)) {
            //Adds 1 to the Y value if the biome is hell.
            locationTP = locationTP.add(0, 1, 0);

        } else if (locationTP.getWorld().getBiome(locationTP.getBlockX(), locationTP.getBlockZ()).equals(endBiome) && !RandomCoords.getPlugin().skyBlockSave.getStringList("SkyBlockWorlds").contains(locationTP.getWorld().getName())) {
            //Gets the end coord from the end class.
            locationTP = end.endCoord(locationTP);
            //Gets the highest value at this point.
            final int highest = locationTP.getWorld().getHighestBlockYAt(locationTP.getBlockX(), locationTP.getBlockZ());
            //Sets the locationTp as the new location made from above.
            locationTP = new Location(locationTP.getWorld(), locationTP.getBlockX(), highest, locationTP.getBlockZ());


        } else {
            //Add 2.5 to the location. The max amount before fall damage.
            if(locationTP.getY() <= 0) {
                locationTP.add(0, 60, 0);
            }
            locationTP = locationTP.add(0, 2.5, 0);
        }

        //Return the locationa after the buffer is added.
        return locationTP;

    }

    /**
     * Should we warp the player, or randomly teleport them?.
     * @param player The player.
     * @param world The world we're teleporting them in.
     * @param locationTP The location we have at the moment.
     * @param name The name of the CoordType.
     * @param coordType The CoordType enum. The method of teleporting.
     * @return The final location.
     */
    private Location shouldWarp(final Player player, final World world, Location locationTP, final String name, final CoordType coordType) {

        /**
         * If the name of the teleport type provide (See FinalCoordinates) is Warps, Set the locationTP as the warp location.
         */
        if (name.equalsIgnoreCase("Warps")) {
            //Sets the location as the warp location.
            locationTP = warpTP(player, world, coordType);
            //Returns this location.
            return locationTP;
        }

        /**
         * If the config setting for this specific name type is set to warps, Get the location from warpTP.
         */
        if (RandomCoords.getPlugin().config.getString(name).equalsIgnoreCase("warps")) {
            //Set the location as the warp location
            locationTP = warpTP(player, world, coordType);
            //Return this location
            return locationTP;
        } else {
            //Otherwise, Just return the given location, indicating no change.
            return locationTP;
        }


    }


    private boolean reachedLimit(final Player player, final String name) {
        if(!RandomCoords.getPlugin().config.getStringList("LimiterApplys").contains(name)) {
            return false;
        }

        if(RandomCoords.getPlugin().config.getInt("Limit") == 0) {
            return false;
        }
        final FileConfiguration limiterFile = RandomCoords.getPlugin().limiter;

        final String uuid = player.getUniqueId().toString();
        //Grab their uses.
        final int used = limiterFile.getInt(uuid + ".Uses");
        //Grab the limit.
        final int limit = RandomCoords.getPlugin().config.getInt("Limit");
        if (used == limit) {
            messages.reachedLimit(player);
            return true;

        }

        return false;
    }

    /**
     * Does the limiter apply?
     * @param player The player we're checking for.
     * @param name The name of the CoordType.
     * @return True or False, Should we use the limiter.
     */
    private boolean limiterApplys(final Player player, final String name) {
        /**
         * If the LimiterApplys list contains the teleport type name, set limiter as isLimiter boolean.
         */
        if (RandomCoords.getPlugin().config.getStringList("LimiterApplys").contains(name)) {
            //Sets the limiter boolean as the one provided in the isLimiter boolean function.
            return isLimiter(player);
        }
        //Returns the original given limiter value.
        return true;

    }

    /**
     * Does the cost of teleporting apply?
     * @param thisCost The cost of the teleport.
     * @param name The name of the CoordType.
     * @return The cost of the teleport.
     */
    private double costApplys(double thisCost, final String name) {
        /**
         * If the cost name (See FinalCoordinates) is default, just return thisCost value provided.
         */
        if (name.equals("default")) {
            //Return the cost.
            return thisCost;
        }
        /**
         * If the config has a cost set, thats not 0 then set thisCost to the cost of "costName".
         */
        if (RandomCoords.getPlugin().config.getDouble(name) != 0) {
            //Sets this cost to the one provided in the config.
            thisCost = RandomCoords.getPlugin().config.getDouble(name);
        }
        //Return the cost given.
        return thisCost;

    }

    /**
     * Does the timeBefore apply?
     * @param player The player.
     * @param timeBefore The timeBefore int.
     * @param name The name of the CoordType.
     * @return The timeBefore int.
     */
    private int timeBeforeApplys(final Player player, int timeBefore, final String name) {

        /**
         * If the teleport type name is Join, Return the given timeBefore.
         */
        if (name.equals("Join")) {
            //Return the timebefore value.
            return timeBefore;
        }

        /**
         * If the timeBeforeTeleport in the config is not set to 0.
         * Check if they could bypass it, and if not set timebefore as the config value.
         * If so, Set timebefore to 0.
         */
        if (RandomCoords.getPlugin().config.getInt("TimeBeforeTeleport") != 0) {
            /**
             * Does the timeBefore apply to this teleport type?
             */
            if (RandomCoords.getPlugin().config.getStringList("TimeBeforeApplys").contains(name)) {
                /**
                 * Do they have the bypass permission?
                 */
                if (player.hasPermission("Random.TBT.Bypass") || player.hasPermission("Random.*") || player.isOp()) {
                    //they can bypass, thus TB = 0.
                    timeBefore = 0;
                } else {
                    //They cant bypass thus the TB is the on in the config.
                    timeBefore = RandomCoords.getPlugin().config.getInt("TimeBeforeTeleport");
                }
            }
        }
         //Return the timeBefore value.
        return timeBefore;

    }

    /**
     * Does a cooldown apply?
     * @param player The player.
     * @param cooldown The cooldown int.
     * @param name The name of the CoordType.
     * @return The cooldown integer.
     */
    private int cooldownApplys(final Player player, int cooldown, final String name) {
        /**
         * If name is equal to teleport type Join. Return the cooldown provide. (Usually 0)
         */
        if (name.equals("Join")) {
            //Returns the cooldown value.
            return cooldown;
        }

        /**
         * If the cooldown time is not equal to 0.
         * Check that they can bypass it, If so set cooldown to 0.
         * If not, set the cooldown to the one provided in the config.
         */
        if (RandomCoords.getPlugin().config.getInt("CooldownTime") != 0) {
            /**
             * Checks the cooldown applys list to see if a cooldown should be used for this teleport type.
             */
            if (RandomCoords.getPlugin().config.getStringList("CooldownApplys").contains(name)) {
                /**
                 * If the player has permission to bypass it, Then just set the cooldown to 0.
                 */
                if (player.hasPermission("Random.Cooldown.Bypass") || player.hasPermission("Random.*")) {
                    //They have perms to bypass, thus cooldown is 0.
                    cooldown = 0;
                } else {
                    //Set the cooldown to the one provided in the config.
                    cooldown = RandomCoords.getPlugin().config.getInt("CooldownTime");
                }
            }
        }
        //Returns the cooldown value.
        return cooldown;

    }

    /**
     * Is the world banned from randomly teleporting in?
     * @param player The player thats teleporting.
     * @param world The world we're checking.
     * @return True or False, Is the world banned?
     */
    private boolean isWorldBanned(final Player player, final World world) {
        /**
         * If the world is banned, Return true, and a message.
         */
        if (RandomCoords.getPlugin().config.getStringList("BannedWorlds").contains(world.getName())) {
            //Message the player.
            messages.worldBanned(player);
            //Return true, the worlds banned
            return true;
        }
        //Return false, the world is not banned.
        return false;
    }

    /**
     * Is the player already in the teleproting time before process?
     * @param player The player thats teleporting.
     * @param timeBefore The timeBefore teleporting int.
     * @return True or False, Are they in the time before?
     */
    private boolean inTimeBefore(final Player player, final int timeBefore) {
        /**
         * If the player is in the TimeBefore cooldown, Then tell them they are about to teleport.
         * Also return true.
         * Otherwise, Start the cooldown and message them so.
         */
        if (Cooldown.isInCooldown(player.getUniqueId(), "TimeBefore")) {
            //Message them that they are about to teleport to the location.
            messages.aboutTo(player, Cooldown.getTimeLeft(player.getUniqueId(), "TimeBefore"));
            //Return true, they are in the cooldown.
            return true;
        } else {
            //Create the cooldown if they are not in it.
            final Cooldown cTb = new Cooldown(player.getUniqueId(), "TimeBefore", timeBefore);

            /**
             * If the time before is not 0, Start the cooldown and message the player.
             */
            if (timeBefore != 0) {
                cTb.start();
                messages.TeleportingIn(player, timeBefore);
            }
        }
        //Otherwise just return false, they are not in the timeBefore.
        return false;

    }

    /**
     * Is the player in the cooldown.
     * @param player The player thats trying to teleport.
     * @param cooldown The cooldown int.
     * @return True or False, Are they in the cooldown?
     */
    private boolean inCooldown(final Player player, final int cooldown) {
        /**
         * If the cooldown is equal to 0, Return false, they are not in the cooldown.
         */
        if (cooldown == 0) {
            //Return that they are not in the cooldown.
            return false;
        }
        /**
         * If the player is not in the Command cooldown, return false.
         * Otherwise, return a message that tells them how long they have left in the cooldown.
         * (Misleading cooldown name, Woops)
         */
        return Cooldown.isInCooldown(player.getUniqueId(), "Command");
    }

    /**
     * Check if the player has paid the price of teleporting.
     * @param p The initiater of the teleport, and who we're charging
     * @param cost The cost
     * @return True or False, Have they paid.
     */
    public boolean hasPayed(final Player p, final double cost) {
        if (!RandomCoords.getPlugin().setupEconomy() || cost == 0) {
            return true;
        } else {
            final EconomyResponse r = RandomCoords.econ.withdrawPlayer(p, cost);
            if (r.transactionSuccess()) {
                messages.charged(p, cost);
                return true;

            } else {
                messages.cost(p, cost);

                return false;
            }
        }
    }

    /**
     * Checks if they have the correct amount of money to pay for the teleport.
     * @param p The player that we are cheking.
     * @param cost the cost of teleport
     * @return True or False, do they have the money?
     */
    public boolean hasMoney(final Player p, final double cost) {
        return !RandomCoords.getPlugin().setupEconomy() || cost == 0 || cost <= RandomCoords.econ.getBalance(p);
    }

    //1000 max, 5444min
    public boolean maxGreaterThanMin(CommandSender sender, int max, int min, World world) {
     if(max == 574272099 && min != 574272099) {
         if(getMax(world) <= min) {

             messages.minTooLarge(sender);
             return false;
         }
     } else if(max != 574272099 && min == 574272099) {
            if(getMin(world) >= max) {
                messages.minTooLarge(sender);
                return false;
            }
        } else if(max == 574272099 && min == 574272099) {
            if(getMin(world) >= getMax(world)) {
                messages.minTooLarge(sender);
                return false;
            }
        } else if(min >= max)  {

            messages.minTooLarge(sender);
         return false;
     }
     return true;
    }


    public boolean isMinTooBig(int min, int max, int key, String toWorldName, CommandSender sender) {
        if((min > max && max != key && min != key)) {
            messages.minTooLarge(sender);
            return true;
        } else if(max != key &&  min == key) {
            int minimum = getMin(Bukkit.getWorld(toWorldName));
            if(max > minimum) {
                messages.minTooLarge(sender);
                return true;
            }
        } else if(max == key &&  min != key) {
            int maximum = getMax(Bukkit.getWorld(toWorldName));
            if(min > maximum) {
                messages.minTooLarge(sender);
                return true;
            }
        }
        return false;
    }




}
