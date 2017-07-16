package com.jolbol1.RandomCoordinates.managers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import com.jolbol1.RandomCoordinates.RandomCoords;

//TODO: Will maybe replaced with the GitHub API
public class UpdateManager {
	private YamlConfiguration webConfig;
	private boolean hasChecked = false;
	private MessageManager messageManager = new MessageManager();
	
	public boolean doUpdateCheck() {
        //Gets the site URL we're reading from.
        if(isUpdatesEnabled()) {
    		URL site;
            
            try {
            	final String web = RandomCoords.getPlugin().update.getString("updateURL");
            	site = new URL(web);
            	
            	InputStreamReader in = new InputStreamReader(site.openStream());
            	if(in!=null) {
                	webConfig=YamlConfiguration.loadConfiguration(in);
                	hasChecked = true;            		
            	}
            	else{
            		hasChecked = false;
            	}
            }
            catch (IOException e) {
                //Log if we couldnt read the line of the site.
                RandomCoords.logger.severe("Couldn't grab the update.yml from the web.");    
                hasChecked = false;
            }
            return hasChecked;
        }
        return false;
	}
	
	public boolean isNotifyUpdatesEnabled() {
		return RandomCoords.getPlugin().update.getBoolean("notifyUpdates");
	}
	
	public boolean isNotifyExperimentalEnabled() {
		return RandomCoords.getPlugin().update.getBoolean("notifyExperimental");
	}
	
	public boolean isNotifyBuildsAheadEnabled() {
		return RandomCoords.getPlugin().update.getBoolean("notifyBuildsAhead");
	}
	
	public boolean isUpdatesEnabled() {
		return RandomCoords.getPlugin().update.getBoolean("updatesEnabled");
	}
	
	public boolean isPrintToLogEnabled() {
		return RandomCoords.getPlugin().update.getBoolean("printToLog");
	}
	
	public void notifySender(CommandSender recipient) {
		if(isUpdatesEnabled() && hasChecked) {
			if (isNotifyExperimentalEnabled() && isRunningExperimental()) {
				messageManager.notifyExperimentalVersion(recipient);
			}
			if (isNotifyBuildsAheadEnabled()) {
				int buildsAhead=getBuildsAhead();
				if(buildsAhead>0) {
					messageManager.notifyNewBuildsAhead(recipient, buildsAhead);
				}
			}
			if(isNotifyUpdatesEnabled()) {
				if (!isNewestVersion()) {
					messageManager.notifyNewUpdate(recipient, RandomCoords.getPlugin().update.getString("releasePageURL"), getNewestVersion());
				}
				else {
					messageManager.notifyUpdateLatestVersion(recipient);
				}
			}
		}
	}
	
	public boolean isRunningExperimental() {
        if(!RandomCoords.getPlugin().update.getString("branch").equalsIgnoreCase("master")) {
        	return true;
        }
        return false;
	}
	
	public int getBuildsAhead() {
		int buildsAhead = -1;
		if(hasChecked) {
			buildsAhead = webConfig.getInt("build") - RandomCoords.getPlugin().update.getInt("build");		
		}
		return buildsAhead;	
	}
	
	public boolean isNewestVersion() {
		if(hasChecked) {
			return RandomCoords.getPlugin().update.getString("version").equalsIgnoreCase(webConfig.getString("version"));
		}
		return true;
	}
	
	public String getNewestVersion() {
		if(hasChecked) {
			return webConfig.getString("version");
		}
		return "";
	}
}
