/*
 * Copyright (c) 2012, tuxed
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 	* Redistributions of source code must retain the above copyright
 * 	notice, this list of conditions and the following disclaimer.
 * 	* Redistributions in binary form must reproduce the above copyright
 * 	notice, this list of conditions and the following disclaimer in the
 * 	documentation and/or other materials provided with the distribution.
 * 	* Neither the name of tuxed nor the
 *	names of its contributors may be used to endorse or promote products
 * 	derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL TUXED BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.tropicalwikis.tuxcraft.plugins.simpleecosigns;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class SimpleEcoSigns extends JavaPlugin implements Listener {
	private static Economy econ;
	private static HashMap<String, String> namesToIds = new HashMap<String, String>();
	@Override
	public void onEnable() {
		if (getServer().getPluginManager().getPlugin("Vault") != null) {
			RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
			if (rsp != null) {
				econ = rsp.getProvider();
			}
		} else {
			getLogger().severe("Vault not found! Disabling plugin.");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		
		getServer().getPluginManager().registerEvents(new SignListeners(), this);
		try {
			File pluginFolder = this.getDataFolder();
			if (!pluginFolder.exists()) {
				pluginFolder.mkdir();
			}
			
			Scanner scan = new Scanner(new File(pluginFolder, "items.csv"));
			while (scan.hasNextLine()) {
				String line = scan.nextLine();
				String[] values = line.split(",");
				if (!line.startsWith("#") && values.length == 3) {
					namesToIds.put(values[0].toLowerCase(), values[1] + ":" + values[2]);
				}
			}
			getLogger().info("Successfully loaded items.csv!");
			scan.close();
		} catch (IOException e) {
			getLogger().info("Could not load items.csv; this is okay. We will use Bukkit names for materials.");
		}
	}
	
	@Override
	public void onDisable() {
		
	}
	
    //some methods to access econ
	public static double getEconBalance(String playerName) {
		return econ.getBalance(playerName);
	}
	public static void withdrawFromPlayer(String playerName, double amount) {
		econ.withdrawPlayer(playerName, amount);
	}
	public static void depositToPlayer(String playerName, double amount) {
		econ.depositPlayer(playerName, amount);
	}
	
	//access item namesToIds
	public static boolean hasItem(String name) {
		if (namesToIds.containsKey(name.toLowerCase())){
			return true;
		}
		//check if the player's item name is an abbreviation
		//we also must check that the abbreviation only matches one key
		int numMatches = 0;
		for (String key : namesToIds.keySet()) {
			
			if (key.startsWith(name.toLowerCase())) {
				numMatches++;
			}
		}
		if (numMatches == 1) {
			return true;
		}
		return false;
	}
	
    public static ItemStack nameToItemStack(String name, Integer amt) {
    	// Let's make life a bit easier on us:
    	name = name.toLowerCase();
    	if(name.contains(":")) {
    		try {
    			ItemStack tmp = new ItemStack(Integer.valueOf(name.split(":")[0]), amt, Short.valueOf(name.split(":")[1]));
    			return tmp;
    		} catch (NumberFormatException e) {
	    		if(!namesToIds.containsKey(name.split(":")[0])) {
	    			//first check if the player's item name is an abbreviation (this may be problematic)
	    			for (String key : namesToIds.keySet()) {
	    				if (key.startsWith(name)) {
	    					return new ItemStack(Integer.valueOf(namesToIds.get(key).split(":")[0]), amt, Short.valueOf(namesToIds.get(key).split(":")[1]));
	    				}
	    			}
	    			
	    			return new ItemStack(Material.matchMaterial(name.split(":")[0]), amt, Short.valueOf(name.split(":")[1]));
	    		}
	    		else {
	    			String s2 = name.split(":")[0];
	    			String s3 = name.split(":")[1];
	    			return new ItemStack(Integer.valueOf(namesToIds.get(s2).split(":")[0]), amt, Short.valueOf(s3));
	    		}
    		}
    	} else {
    		try {
    			ItemStack tmp = new ItemStack(Integer.valueOf(name), amt);
    			return tmp;
    		} catch (NumberFormatException e) {
    			if(!namesToIds.containsKey(name.toLowerCase())) {
    				for (String key : namesToIds.keySet()) {
	    				if (key.startsWith(name)) {
	    					return new ItemStack(Integer.valueOf(namesToIds.get(key).split(":")[0]), amt, Short.valueOf(namesToIds.get(key).split(":")[1]));
	    				}
	    			}
    				
    				return new ItemStack(Material.matchMaterial(name), amt);
    			}
    			else {
    				return new ItemStack(Integer.valueOf(namesToIds.get(name).split(":")[0]), amt, Short.valueOf(namesToIds.get(name).split(":")[1]));
    			}
    		}
    	}
    }
    
    public static boolean blockIsValidEcoSign(Sign s) {
    	try {
    		Integer.valueOf(s.getLine(1));
    		String s2 = s.getLine(2);
    		String s3 = s2;
    		if(s2.contains(":")) {
    			s3 = s2.split(":")[0];
    		}
    		try {
    			Integer.valueOf(s3);
    		} catch (NumberFormatException e) {
    			if(!namesToIds.containsKey(s3.toLowerCase())) {
    				boolean b = false;
    				for (String key : namesToIds.keySet()) {
	    				if (key.startsWith(s3)) {
	    					b = true;
	    				}
	    			}
    				
    				if(Material.matchMaterial(s3) == null && !b) {
    					return false;
    				}
    			}
    		}
    		Double.valueOf(s.getLine(3).replace("$", ""));
    	} catch (Exception e) {
    		return false;
    	}
    	return s.getLine(0).matches(ChatColor.DARK_BLUE + "(?i)\\[Buy\\]") || s.getLine(0).matches(ChatColor.DARK_BLUE + "(?i)\\[Sell\\]");
    }
}