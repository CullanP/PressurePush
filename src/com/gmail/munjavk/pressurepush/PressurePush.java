package com.gmail.munjavk.pressurepush;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public class PressurePush extends JavaPlugin implements Listener {
  HashSet<String> createactive = new HashSet<String>();
  HashSet<String> disableFall = new HashSet<String>();
  
  public void onDisable() {
	saveDefaultConfig();
    System.out.println("[PressurePush] Disabled");
  }
  
  public void onEnable()
  {
    System.out.println("[PressurePush] Enabled");
    Bukkit.getPluginManager().registerEvents(this, this);
    getConfig().options().copyDefaults(true);
    saveDefaultConfig();
  }
  
  public boolean onCommand(CommandSender sender, Command cmd, String cmdLabel, String[] args) {
    Player player = (Player)sender;
    if (!player.hasPermission("pp.admin")) {
    	player.sendMessage(ChatColor.RED + "You don't have permission to use this command");
    	return true;
    }
    
    if (cmd.getName().equalsIgnoreCase("pressurepush")) {
    	for (String s : getConfig().getStringList("help")) {
    	player.sendMessage(ChatColor.translateAlternateColorCodes('&', s).replace("{version}", getDescription().getVersion()));	
    	}
    	return true;
    }
    
    if (cmd.getName().equalsIgnoreCase("ppload")) {
    	if (!sender.isOp()) return true;
        reloadConfig();
        saveConfig();
        if (sender instanceof Player) {
        createactive.add(player.getName());
        }
        sender.sendMessage(ChatColor.AQUA + "-> PressurePush config has been reloaded <-");
    }
    
    if (cmd.getName().equalsIgnoreCase("ppcreate")) {
      if (args.length == 0) {
      	for (String s : getConfig().getStringList("help")) {
        	player.sendMessage(ChatColor.translateAlternateColorCodes('&', s).replace("{version}", getDescription().getVersion()));	
        }
      } else if (args[0].equalsIgnoreCase("on") || (player.isOp())) {
    	createactive.add(player.getName());
        player.sendMessage(ChatColor.GOLD + "Place the pressure plate somewhere to make it a PressurePush plate, type the command again to disable it");
        if (getConfig().getBoolean("UnlimitedPlates") == true) {
          player.getInventory().addItem(new ItemStack[] { new ItemStack(Material.STONE_PLATE, -1) });
          player.getInventory().addItem(new ItemStack[] { new ItemStack(Material.WOOD_PLATE, -1) });
        }
      } else if (args[0].equalsIgnoreCase("off") || (player.isOp())) {
    	createactive.remove(player.getName());
        player.sendMessage(ChatColor.RED + "You've de-toggled the creation of PressurePush plates!");
        if (getConfig().getBoolean("UnlimitedPlates") == true) {
          player.getInventory().removeItem(new ItemStack[] { new ItemStack(Material.STONE_PLATE, -1) });
          player.getInventory().removeItem(new ItemStack[] { new ItemStack(Material.WOOD_PLATE, -1) });
        }
      } else {
        player.sendMessage(ChatColor.RED + "You don't have permission to use this command");
        return true;
      }
    }
    return true;
  }
  
  @EventHandler(ignoreCancelled = true)
  public void BlockPlaceEvent(BlockPlaceEvent event) throws IOException {
    Player p = event.getPlayer();
    if (!p.hasPermission("pp.create")) {
    	return;
    }
    if (!createactive.contains(p.getName())) {
    	return;
    }
    if (event.getBlock().getType() == Material.STONE_PLATE || event.getBlock().getType() == Material.WOOD_PLATE) {
      Location location = event.getBlock().getLocation();
      String loc = location.getBlockX() + "-" + location.getBlockY() + "-" + location.getBlockZ() + "-" + location.getWorld().getName();
      
      p.sendMessage(ChatColor.GREEN + "You've successfully made a PressurePush Plate");
      
      List<String> locs = getConfig().getStringList("Plates.location");
      if (!locs.contains(loc)) {
        locs.add(loc);
        getConfig().set("Plates.location", locs);
        saveConfig();
      }
    }
  }
  
  @EventHandler(ignoreCancelled = true)
  public void BlockBreakEvent(BlockBreakEvent event) throws IOException {
    Player p = event.getPlayer();
    if (!p.hasPermission("pp.destroy")) {
    	return;
    }
    Location location = event.getBlock().getLocation();
    String loc = location.getBlockX() + "-" + location.getBlockY() + "-" + location.getBlockZ() + "-" + location.getWorld().getName();
    List<String> locs = getConfig().getStringList("Plates.location");
      if (!locs.contains(loc)) {
    	  return;
      }
      if (event.getBlock().getType() == Material.STONE_PLATE || event.getBlock().getType() == Material.WOOD_PLATE) {
      locs.remove(loc);
      getConfig().set("Plates.location", locs);
      saveConfig();

      event.getPlayer().sendMessage(ChatColor.RED + "You have removed a PressurePush plate");
    }
  }
  
  @EventHandler
  public void damageEvent(EntityDamageEvent e) {
    if (e.getCause() == DamageCause.FALL && e.getEntity() instanceof Player) {
      Player p = (Player)e.getEntity();
      if (disableFall.contains(p.getName())) {
        e.setCancelled(true);
        disableFall.remove(p.getName());
      }
    }
  }
  
  @EventHandler(ignoreCancelled = true)
  public void onPressurePlateStep(PlayerInteractEvent e) {
    Player p = e.getPlayer();
    if (!p.hasPermission("pp.use")) {
    	return;
    }
    if (e.getAction().equals(Action.PHYSICAL) && e.getClickedBlock().getType() == Material.STONE_PLATE || e.getClickedBlock().getType() == Material.WOOD_PLATE) {
      double strength = getConfig().getDouble("Strength");
      double up = getConfig().getDouble("Up");
      Location location = e.getClickedBlock().getLocation();
      String loc = location.getBlockX() + "-" + location.getBlockY() + "-" + location.getBlockZ() + "-" + location.getWorld().getName();
      List<String> locs = getConfig().getStringList("Plates.location");
      if (!locs.contains(loc)) {
    	  return;
      }
      if (getConfig().getInt("Sound") == 2) {
        Vector v = p.getLocation().getDirection().multiply(strength).setY(up);
        p.setVelocity(v);
        p.playSound(p.getLocation(), Sound.IRONGOLEM_HIT, 10.0F, 2.0F);
        e.setCancelled(true);
      }
      if (getConfig().getInt("Sound") == 4) {
        Vector v = p.getLocation().getDirection().multiply(strength).setY(up);
        p.setVelocity(v);
        p.playSound(p.getLocation(), Sound.BLAZE_DEATH, 1.0F, 1.0F);
        e.setCancelled(true);
      }
      if (getConfig().getInt("Sound") == 1) {
        Vector v = p.getLocation().getDirection().multiply(strength).setY(up);
        p.setVelocity(v);
        p.playSound(p.getLocation(), Sound.ENDERDRAGON_HIT, 10.0F, 2.0F);
        e.setCancelled(true);
      }
      if (getConfig().getInt("Sound") == 0) {
        Vector v = p.getLocation().getDirection().multiply(strength).setY(up);
        p.setVelocity(v);
        e.setCancelled(true);
      }
      if (getConfig().getInt("Sound") == 3) {
        Vector v = p.getLocation().getDirection().multiply(strength).setY(up);
        p.setVelocity(v);
        p.playSound(p.getLocation(), Sound.ENDERDRAGON_WINGS, 10.0F, 2.0F);
        e.setCancelled(true);
      }
      disableFall.add(p.getName());
    }
  }
}
