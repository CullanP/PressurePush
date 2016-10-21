package com.gmail.munjavk.pressurepush;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;

import org.apache.commons.lang3.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
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
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class PressurePush extends JavaPlugin implements Listener {
    private final HashMap<Location, PushPlate> plates = Maps.newHashMap();
    private final HashSet<String> createactive = Sets.newHashSet();
    private final HashSet<String> disableFall = Sets.newHashSet();

    public void onDisable() {
        saveDefaultConfig();
        createactive.clear();
        disableFall.clear();
        plates.clear();
        System.out.println("[PressurePush] Disabled");
    }

    public void onEnable() {
        System.out.println("[PressurePush] Enabled, Revisioned by CullanP");
        Bukkit.getPluginManager().registerEvents(this, this);
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();
        loadPlates();
    }

    public void loadPlates() {
        plates.clear();
        for (String location : getConfig().getStringList("Plates.location")) {
            String[] locSplit = location.split(" ")[0].replaceAll("--", "-;").split("-");

            World world = Bukkit.getWorld(locSplit[locSplit.length - 1]);
            if (world == null) {
                getLogger().log(Level.WARNING, "Unable to load plate " + location + " - Does the world exist?");
                continue;
            }

            if (locSplit[0].isEmpty()) {
                locSplit[0] = "-" + locSplit[1];
                locSplit[1] = locSplit[2];
                locSplit[2] = locSplit[3];
            }
            double x = Double.valueOf(locSplit[0].replace(";", "-"));
            double y = Double.valueOf(locSplit[1].replace(";", "-"));
            double z = Double.valueOf(locSplit[2].replace(";", "-"));
            plates.put(new Location(world, x, y, z), new PushPlate(location.contains(";") ? location.split(" ")[1] : location));
        }
    }

    public boolean onCommand(CommandSender sender, Command cmd, String cmdLabel, String[] args) {
        Player player = (Player) sender;
        if (!player.hasPermission("pp.admin")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command");
            return true;
        } else if (cmd.getName().equalsIgnoreCase("pressurepush")) {
            for (String s : getConfig().getStringList("help")) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', s).replace("{version}", getDescription().getVersion()));
            }
            return true;
        } else if (cmd.getName().equalsIgnoreCase("ppload")) {
            if (!sender.isOp()) {
                return true;
            }

            reloadConfig();
            if (sender instanceof Player) {
                createactive.add(player.getName());
            }
            loadPlates();
            sender.sendMessage(ChatColor.AQUA + "-> PressurePush config has been reloaded <-");
        } else if (cmd.getName().equalsIgnoreCase("ppcreate")) {
            if (!player.hasPermission("pp.create")) {
                player.sendMessage(ChatColor.RED + "You don't have permission to use this command");
                return true;
            } else if (args.length == 0) {
                for (String s : getConfig().getStringList("help")) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', s).replace("{version}", getDescription().getVersion()));
                }
            } else if (args[0].equalsIgnoreCase("on")) {
                createactive.add(player.getName());

                player.sendMessage(ChatColor.GOLD + "Place the pressure plate somewhere to make it a push plate, use /ppcreate off to disable");
                if (getConfig().getBoolean("UnlimitedPlates") == true) {
                    player.getInventory().addItem(new ItemStack[] { new ItemStack(Material.STONE_PLATE, -1) });
                    player.getInventory().addItem(new ItemStack[] { new ItemStack(Material.WOOD_PLATE, -1) });
                }
            } else if (args[0].equalsIgnoreCase("off")) {
                createactive.remove(player.getName());

                player.sendMessage(ChatColor.RED + "You've de-toggled the creation of PressurePush plates!");
                if (getConfig().getBoolean("UnlimitedPlates") == true) {
                    player.getInventory().removeItem(new ItemStack[] { new ItemStack(Material.STONE_PLATE, -1) });
                    player.getInventory().removeItem(new ItemStack[] { new ItemStack(Material.WOOD_PLATE, -1) });
                }
            }
        } else if (cmd.getName().equalsIgnoreCase("ppset")) {
            if (!player.hasPermission("pp.set")) {
                player.sendMessage(ChatColor.RED + "You don't have permission to use this command");
                return true;
            } else if (args.length == 0 || args.length != 2) {
                player.sendMessage(ChatColor.RED + "Usage: /ppset up|strength #");
                return true;
            } else if (!NumberUtils.isNumber(args[1])) {
                player.sendMessage(ChatColor.RED + "Invalid number specified, please use a number.");
                return true;
            }

            Block targetBlock = player.getTargetBlock((HashSet<Material>) null, 3);
            if (targetBlock != null && targetBlock.getType().name().contains("PLATE")) {
                Location location = targetBlock.getLocation();
                String loc = location.getBlockX() + "-" + location.getBlockY() + "-" + location.getBlockZ() + "-" + location.getWorld().getName();
                if (!plates.containsKey(location)) {
                    player.sendMessage(ChatColor.RED + "The plate you're looking at isn't a push plate!");
                    return true;
                }

                List<String> locs = getConfig().getStringList("Plates.location");
                PushPlate plate = plates.get(location);
                double value = Double.valueOf(args[1]);
                if (args[0].equalsIgnoreCase("up")) {
                    plate.setUp(value);
                    locs.remove(locs);
                    locs.add(loc + " " + value + ";" + (!plate.isUpdated() ? getConfig().getDouble("Strength", 2.0) : plate.getStrength()));
                    getConfig().set("Plates.location", locs);
                    Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                        saveConfig();
                    });
                    player.sendMessage(ChatColor.GREEN + "Successfully set the plate Up to " + value);
                } else if (args[0].equalsIgnoreCase("strength")) {
                    plate.setStrength(value);
                    locs.remove(locs);
                    locs.add(loc + " " + (!plate.isUpdated() ? getConfig().getDouble("Up", 1.1) : plate.getUp()) + ";" + value);
                    getConfig().set("Plates.location", locs);
                    Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                        saveConfig();
                    });
                    player.sendMessage(ChatColor.GREEN + "Successfully set the plate strength to " + value);
                } else {
                    player.sendMessage(ChatColor.RED + "Usage: /ppset up|strength #");
                    return true;
                }
            } else {
                player.sendMessage(ChatColor.RED + "The block you're looking at isn't a push plate!");
                return true;
            }
        }
        return true;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlatePlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("pp.create") || !createactive.contains(player.getName())) {
            return;
        } else if (event.getBlock().getType().name().contains("PLATE")) {
            Location location = event.getBlock().getLocation();
            String loc = location.getBlockX() + "-" + location.getBlockY() + "-" + location.getBlockZ() + "-" + location.getWorld().getName();
            List<String> locs = getConfig().getStringList("Plates.location");
            if (!locs.contains(loc)) {
                locs.add(loc);
                getConfig().set("Plates.location", locs);
                plates.put(location, new PushPlate(loc));
                player.sendMessage(ChatColor.GREEN + "You've successfully made a Pressure-Push Plate");
                Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                    saveConfig();
                });
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlateBreak(BlockBreakEvent event) throws IOException {
        Player player = event.getPlayer();
        if (!player.hasPermission("pp.destroy")) {
            return;
        }
        Location location = event.getBlock().getLocation();
        String loc = location.getBlockX() + "-" + location.getBlockY() + "-" + location.getBlockZ() + "-" + location.getWorld().getName();
        List<String> locs = getConfig().getStringList("Plates.location");
        if (!locs.contains(loc)) {
            return;
        } else if (event.getBlock().getType().name().contains("PLATE")) {
            locs.remove(loc);
            getConfig().set("Plates.location", locs);
            plates.remove(location);
            player.sendMessage(ChatColor.RED + "You removed a Pressure-Push plate");
            Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                saveConfig();
            });
        }
    }

    @EventHandler
    public void handleFall(EntityDamageEvent e) {
        if (e.getCause() == DamageCause.FALL && e.getEntity() instanceof Player) {
            Player p = (Player) e.getEntity();
            if (disableFall.contains(p.getName())) {
                p.setFallDistance(0);
                e.setCancelled(true);
                disableFall.remove(p.getName());
            }
        }
    }

    @EventHandler
    public void onLogout(PlayerQuitEvent e) {
        createactive.remove(e.getPlayer().getName());
        disableFall.remove(e.getPlayer().getName());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPressurePlateStep(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!p.hasPermission("pp.use")) {
            return;
        } else if (e.getAction().equals(Action.PHYSICAL)
                && (p.hasPermission("pp.use") && e.getClickedBlock().getType().name().contains("PLATE"))) {
            Location loc = e.getClickedBlock().getLocation();
            if (!plates.containsKey(loc)) {
                return;
            }

            PushPlate plate = plates.get(loc);
            try {
                Sound sound = Sound.valueOf(getConfig().getString("Sound.Name"));
                float volume = getConfig().getFloat("Sound.Volume", 10);
                float pitch = getConfig().getFloat("Sound.Pitch", 2);
                p.playSound(p.getLocation(), sound, volume, pitch);
            } catch (Exception ex) {
                getLogger().log(Level.WARNING, "The specified sound in the config could not be found.");
            }

            Vector v = p.getLocation().getDirection().multiply(plate.getStrength()).setY(plate.getUp());
            p.setVelocity(v);
            e.setCancelled(true);
            disableFall.add(p.getName());
        }
    }

    public class PushPlate {
        final boolean updated;
        double strength;
        double up;

        public PushPlate(String location) {
            if (location.contains(";")) {
                String[] split = location.split(";");

                updated = true;
                strength = Double.valueOf(split[1]);
                up = Double.valueOf(split[0]);
            } else {
                updated = false;
                strength = getConfig().getDouble("Strength", 1.1);
                up = getConfig().getDouble("Up", 2.0);
            }
        }

        public boolean isUpdated() {
            return updated;
        }

        public double getStrength() {
            return strength;
        }

        public double getUp() {
            return up;
        }

        public void setStrength(double strength) {
            this.strength = strength;
        }

        public void setUp(double up) {
            this.up = up;
        }
    }
}
