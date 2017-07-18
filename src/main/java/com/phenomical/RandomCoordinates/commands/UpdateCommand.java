package com.phenomical.RandomCoordinates.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.jolbol1.RandomCoordinates.RandomCoords;
import com.jolbol1.RandomCoordinates.commands.handler.CommandInterface;

public class UpdateCommand implements CommandInterface {

	@Override
	public void onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (args.length == 1) {
            if (RandomCoords.getPlugin().hasPermission(sender, "Random.Admin.Update") || RandomCoords.getPlugin().hasPermission(sender, "Random.Admin.*") || RandomCoords.getPlugin().hasPermission(sender, "Random.*"))
            	if (args[0].equalsIgnoreCase("update")) {
            		RandomCoords.getPlugin().updateManager.doUpdateCheck(sender);
            	}
        }
	}

}
