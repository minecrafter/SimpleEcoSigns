package com.tropicalwikis.tuxcraft.plugins.simpleecosigns;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class SignListeners implements Listener {
	
	@EventHandler
    public void signChange(SignChangeEvent event) {
        if (event.getLine(0).equalsIgnoreCase("[Buy]") || 
    		event.getLine(0).equalsIgnoreCase("[Sell]")) {
            // Verify the sign:
        	// [Buy]
        	// 1
        	// Diamond
        	// $5000
        	try {
        		Integer.valueOf(event.getLine(1));
        		String s2 = event.getLine(2);
        		String s3 = s2;
        		if(s2.contains(":")) {
        			s3 = s2.split(":")[0];
        		}
        		try {
        			Integer.valueOf(s3);
        		} catch (NumberFormatException e) {
        			if(!SimpleEcoSigns.hasItem(s3.toLowerCase()))
        				if(Material.matchMaterial(s3) == null)
        					throw new Exception();
        		}
        		Double.valueOf(event.getLine(3).replace("$", ""));
        	} catch (Exception e) {
        		event.setLine(0, ChatColor.DARK_RED + event.getLine(0));
        		event.getPlayer().sendMessage(ChatColor.RED + "Sign format invalid");
        		return;
        	}
            if (event.getPlayer().hasPermission("simpleecosigns.create")) {
                event.setLine(0, ChatColor.DARK_BLUE + event.getLine(0));
            } else {
                event.setLine(0, ChatColor.DARK_RED + event.getLine(0));
            }
        }
    }
    
    @SuppressWarnings("deprecation")
	@EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        if (event.hasBlock() && event.getClickedBlock().getState() instanceof Sign && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Sign s = (Sign) event.getClickedBlock().getState();
            if(SimpleEcoSigns.blockIsValidEcoSign(s)) {
            	if(s.getLine(0).matches(ChatColor.DARK_BLUE + "(?i)\\[Buy\\]")) {
            		if(SimpleEcoSigns.getEconBalance(p.getName()) >= Double.valueOf(s.getLine(3).replace("$", ""))) {
            			SimpleEcoSigns.withdrawFromPlayer(p.getName(), Double.valueOf(s.getLine(3).replace("$", "")));
            			if(p.getInventory().firstEmpty() == -1) {
            				p.sendMessage(ChatColor.RED + "You do not have enough inventory space. The items will be dropped on the ground.");
            				p.getWorld().dropItemNaturally(p.getLocation(), SimpleEcoSigns.nameToItemStack(s.getLine(2), Integer.valueOf(s.getLine(1))));
            			} else {
            				p.getInventory().addItem(SimpleEcoSigns.nameToItemStack(s.getLine(2), Integer.valueOf(s.getLine(1))));
            			}
            			p.sendMessage(ChatColor.GREEN + "Item purchased.");
            		} else {
            			p.sendMessage(ChatColor.RED + "You do not have enough funds.");
            		}
            		p.updateInventory();
            	}
            	if(s.getLine(0).matches(ChatColor.DARK_BLUE + "(?i)\\[Sell\\]")) {
            		// I have to do this backwards-ass way because Bukkit sucks.
            		Integer amt = Integer.valueOf(s.getLine(1));
            		Integer s2 = 0;
            		ItemStack i = SimpleEcoSigns.nameToItemStack(s.getLine(2), amt);
            		if(!p.getInventory().containsAtLeast(i, amt)) {
            			p.sendMessage(ChatColor.RED + "You don't have enough items in your inventory.");
            			return;
            		}
            		for(ItemStack item : p.getInventory().getContents()) {
            			if(item != null && item.isSimilar(i) && s2 < amt) {
            				p.getInventory().removeItem(item);
            				s2 = s2 + item.getAmount();
            			}
            		}
            		p.updateInventory();
            		if(s2 > amt) {
            			s2 = s2 - amt;
            			p.getInventory().addItem(new ItemStack(i.getTypeId(), s2, i.getDurability()));
            			p.updateInventory();
            		}
        			SimpleEcoSigns.depositToPlayer(p.getName(), Double.valueOf(s.getLine(3).replace("$", "")));
        			p.sendMessage(ChatColor.GREEN + "Item sold.");
        			p.updateInventory();
        			return;
            	}
            }
        }
    }
	
	@EventHandler
    public void blockPlace(BlockPlaceEvent event) {
        if (event.getBlock() instanceof Sign) {
            Sign sign = (Sign) event.getBlock().getState();
            // Don't let the user make this an auth'd sign
            if (sign.getLine(0).matches(ChatColor.DARK_BLUE + "(?i)\\[Buy\\]") || sign.getLine(0).matches(ChatColor.DARK_BLUE + "(?i)\\[Sell\\]")) {
                event.setCancelled(true);
                return;
            }
        }
        if (event.getBlockAgainst().getState() instanceof Sign && SimpleEcoSigns.blockIsValidEcoSign((Sign)event.getBlockAgainst().getState())) {
        	event.setCancelled(true);
        }
    }

    @EventHandler
    public void blockBreak(BlockBreakEvent event) {
        if (!(event.getBlock().getState() instanceof Sign)) {
            return;
        }
        Sign s = (Sign) event.getBlock().getState();
        if (SimpleEcoSigns.blockIsValidEcoSign(s)) {
            if (!event.getPlayer().hasPermission("simpleecosigns.destroy")) {
                event.setCancelled(true);
            }
        }
    }
}
