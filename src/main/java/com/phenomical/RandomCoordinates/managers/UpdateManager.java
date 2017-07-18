package com.phenomical.RandomCoordinates.managers;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import com.jolbol1.RandomCoordinates.RandomCoords;
import com.jolbol1.RandomCoordinates.managers.MessageManager;

//TODO: Will maybe replaced with the GitHub API
public class UpdateManager {
	private YamlConfiguration webConfig;
	private boolean hasChecked = false;
	private MessageManager messageManager = new MessageManager();
	
	public void doUpdateCheck(final CommandSender recipient) {
            Bukkit.getScheduler().runTaskAsynchronously(RandomCoords.getPlugin(), () ->
            {
            	URL site;
            	
                try {
                	final String web = RandomCoords.getPlugin().versionInfo.getString("UpdateURL");
                	site = new URL(web);
                	
                	InputStreamReader in = new InputStreamReader(site.openStream());
                    webConfig=YamlConfiguration.loadConfiguration(in);
                    hasChecked = true;
                    
            		if(hasChecked) {
            			if (isNotifyExperimentalEnabled() && isRunningExperimental()) {
            				messageManager.notifyExperimentalVersion(recipient);
            			}
            			if (isNotifyBuildsAheadEnabled()) {
            				int buildsAhead=getBuildsAhead();
            				if(buildsAhead!=0) {
            					messageManager.notifyNewBuildsAhead(recipient, buildsAhead);
            				}
            			}
            			if (!isNewestVersion()) {
            				messageManager.notifyNewUpdate(recipient, RandomCoords.getPlugin().versionInfo.getString("ReleasePageURL"), getNewestVersion());
            			}
            			else {
            				messageManager.notifyUpdateLatestVersion(recipient);
            			}
            		}                    
                    
                }
                catch (IOException e) {
                    //Log if we couldnt read the line of the site.
                    RandomCoords.logger.severe("Couldn't grab the update.yml from the web.");    
                    hasChecked = false;
                }
            });

	}
	
	public boolean isNotifyExperimentalEnabled() {
		return RandomCoords.getPlugin().config.getBoolean("InformExperimental");
	}
	
	public boolean isNotifyBuildsAheadEnabled() {
		return RandomCoords.getPlugin().config.getBoolean("InformBuildsAhead");
	}
	
	public boolean isUpdatesEnabled() {
		return RandomCoords.getPlugin().config.getBoolean("UpdatesEnabled");
	}
	
	public boolean isPrintToLogEnabled() {
		return RandomCoords.getPlugin().config.getBoolean("PrintToLog");
	}
	
	public boolean isRunningExperimental() {
        if(!RandomCoords.getPlugin().versionInfo.getString("Branch").equalsIgnoreCase("master")) {
        	return true;
        }
        return false;
	}
	
	public int getBuildsAhead() {
		int buildsAhead = 0;
		if(hasChecked) {
			buildsAhead = webConfig.getInt("Build") - RandomCoords.getPlugin().versionInfo.getInt("Build");		
		}
		return buildsAhead;	
	}
	
	public boolean isNewestVersion() {
		if(hasChecked) {
			return RandomCoords.getPlugin().versionInfo.getString("Version").equalsIgnoreCase(webConfig.getString("Version"));
		}
		return true;
	}
	
	public String getNewestVersion() {
		if(hasChecked) {
			return webConfig.getString("Version");
		}
		return "";
	}
}
