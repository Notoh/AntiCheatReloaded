/*
 * AntiCheat for Bukkit.
 * Copyright (C) 2012 H31IX http://h31ix.net
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package net.h31ix.anticheat;

import java.util.ArrayList;
import java.util.List;
import net.h31ix.anticheat.manage.AnticheatManager;
import net.h31ix.anticheat.manage.PlayerManager;
import net.h31ix.anticheat.xray.XRayTracker;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandHandler implements CommandExecutor {
    
    private Configuration config = AnticheatManager.CONFIGURATION;
    private PlayerManager playerManager = AnticheatManager.PLAYER_MANAGER;
    private XRayTracker xtracker = AnticheatManager.XRAY_TRACKER;
    private static final ChatColor RED = ChatColor.RED;
    private static final ChatColor YELLOW = ChatColor.YELLOW;
    private static final ChatColor GREEN = ChatColor.GREEN;
    private static final ChatColor WHITE = ChatColor.WHITE;
    private List<Player> high = new ArrayList<Player>();
    private List<Player> med = new ArrayList<Player>();
    private List<Player> low = new ArrayList<Player>();  
    private static final Server SERVER = Bukkit.getServer();
    private static final int MED_THRESHOLD = 20;
    private static final int HIGH_THRESHOLD = 50; 
    
    public void handleLog(CommandSender cs, String [] args)
    {
        if(hasPermission(cs))
        {
            if(args[1].equalsIgnoreCase("enable"))
            {
                if(!config.logConsole())
                {
                    config.setLog(true);
                    cs.sendMessage(GREEN+"Console logging enabled.");                            
                }
                else
                {
                    cs.sendMessage(GREEN+"Console logging is already enabled!");
                }
            }
            else if(args[1].equalsIgnoreCase("disable"))
            {
                if(config.logConsole())
                {
                    config.setLog(false);
                    cs.sendMessage(GREEN+"Console logging disabled.");                            
                }
                else
                {
                    cs.sendMessage(GREEN+"Console logging is already disabled!");
                }
            }   
            else
            {
                cs.sendMessage(RED+"Usage: /anticheat log [enable/disable]");
            }
        }        
    }
    
    public void handleXRay(CommandSender cs, String [] args)
    {
        if(hasPermission(cs))
        {
            if(config.logXRay())
            {
                List<Player> list = SERVER.matchPlayer(args[1]);
                if(list.size() == 1)
                {
                    Player player = list.get(0);
                    if(xtracker.sufficientData(player.getName()))
                    {
                        xtracker.sendStats(cs, player.getName());
                    }
                    else
                    {
                        cs.sendMessage(RED+"Insufficient data collected from "+WHITE+args[1]+RED+".");
                        cs.sendMessage(RED+"Please wait until more info is collected before predictions are calculated.");
                    }
                }
                else if(list.size() > 1)
                {
                    cs.sendMessage(RED+"Multiple players found by name: "+WHITE+args[1]+RED+".");
                }
                else if(xtracker.sufficientData(args[1]))
                {
                        xtracker.sendStats(cs, args[1]);
                }
                else
                {
                    cs.sendMessage(RED+"Insufficient data collected from "+WHITE+args[1]+RED+".");
                    cs.sendMessage(RED+"Please wait until more info is collected before predictions are calculated.");
                }
            }
            else
            {
                cs.sendMessage(RED+"XRay logging is off in the config.");
            } 
        }
    }
    
    public void handleReset(CommandSender cs, String [] args)
    {
        if(hasPermission(cs))
        {
            List<Player> list = SERVER.matchPlayer(args[1]);
            if(list.size() == 1)
            {
                Player player = list.get(0);
                getPlayers();
                if(low.contains(player))
                {
                    cs.sendMessage(player.getName()+RED+" is already in Low Level!");
                }
                else if (med.contains(player) || high.contains(player))
                {
                    playerManager.reset(player);
                    cs.sendMessage(player.getName()+GREEN+" has been reset to Low Level.");
                }
                xtracker.reset(player.getName());
                cs.sendMessage(player.getName()+GREEN+"'s XRay stats have been reset.");
            }
            else if(list.size() > 1)
            {
                cs.sendMessage(RED+"Multiple players found by name: "+WHITE+args[1]+RED+".");
            }
            else
            {
                cs.sendMessage(RED+"Player: "+WHITE+args[1]+RED+" not found.");
            }
        }        
    }
    
    public void handleHelp(CommandSender cs)
    {
        if(hasPermission(cs))
        {
            String base = "/AntiCheat ";
            cs.sendMessage("----------------------["+GREEN+"AntiCheat"+WHITE+"]----------------------");
            cs.sendMessage(base+GREEN+"log [Enable/Disable]"+WHITE+" - toggle logging");
            cs.sendMessage(base+GREEN+"report"+WHITE+" - get a detailed cheat report");
            cs.sendMessage(base+GREEN+"reload"+WHITE+" - reload AntiCheat configuration");
            cs.sendMessage(base+GREEN+"reset [user]"+WHITE+" - reset user's hack level");
            cs.sendMessage(base+GREEN+"xray [user]"+WHITE+" - check user's xray levels");
            cs.sendMessage(base+GREEN+"help"+WHITE+" - access this page");     
            cs.sendMessage(base+GREEN+"update"+WHITE+" - check update status");   
            cs.sendMessage("-----------------------------------------------------");
        }        
    }   
    
    public void handleUpdate(CommandSender cs)
    {
        if(hasPermission(cs))
        {
            cs.sendMessage("Running "+GREEN+"AntiCheat "+WHITE+"v"+GREEN+Anticheat.getVersion());
            cs.sendMessage("-----------------------------------------------------");
            if(!Anticheat.isUpdated())
            {
                cs.sendMessage("There "+GREEN+"IS"+WHITE+" a newer version avaliable.");
                if(config.autoUpdate())
                {
                    cs.sendMessage("It will be installed automatically for you on next launch.");
                }
                else
                {
                    cs.sendMessage("Due to your config settings, we "+RED+"can not"+WHITE+" auto update.");
                    cs.sendMessage("Please visit http://dev.bukkit.org/server-mods/anticheat/");
                }
            }
            else
            {
                cs.sendMessage("AntiCheat is "+GREEN+"UP TO DATE!");
            }
        }        
    }    
    
    public void handleReport(CommandSender cs)
    {
        if(hasPermission(cs))
        {
            getPlayers();
            if(!low.isEmpty())
            {
                cs.sendMessage(GREEN+"----Level: Low (Not likely hacking)----");
                for(Player player : low)
                {     
                    cs.sendMessage(GREEN+player.getName());
                } 
            }
            if(!med.isEmpty())
            {                    
                cs.sendMessage(YELLOW+"----Level: Medium (Possibly hacking/lagging)----");
                for(Player player : med)
                {     
                    cs.sendMessage(YELLOW+player.getName());
                }  
            }
            if(!high.isEmpty())
            {                    
                cs.sendMessage(RED+"----Level: High (Probably hacking or bad connection)----");
                for(Player player : high)
                {     
                    cs.sendMessage(RED+player.getName());
                }  
            }
        }        
    }
    
    public void handleReload(CommandSender cs)
    {
        if(hasPermission(cs))
        {
            config.load();
            cs.sendMessage(GREEN+"AntiCheat configuration reloaded.");
        }        
    }
    
    @Override
    public boolean onCommand(CommandSender cs, Command cmd, String alias, String[] args) 
    {
        if(args.length == 2)
        {
            if(args[0].equalsIgnoreCase("log"))
            {
                handleLog(cs,args);
            }
            else if(args[0].equalsIgnoreCase("xray"))
            {   
                handleXRay(cs,args);
            }
            else if(args[0].equalsIgnoreCase("reset"))
            {   
                handleReset(cs,args);
            }                 
            else
            {
                cs.sendMessage(RED+"Unrecognized command.");
            }            
        }
        else if (args.length == 1)
        {
            if(args[0].equalsIgnoreCase("help"))
            {   
                handleHelp(cs);
            }       
            else if(args[0].equalsIgnoreCase("report"))
            {
                handleReport(cs);
            }
            else if(args[0].equalsIgnoreCase("reload"))
            {
                handleReload(cs);
            }            
            else if(args[0].equalsIgnoreCase("update"))
            {
                handleUpdate(cs);
            }            
            else
            {
                cs.sendMessage(RED+"Unrecognized command.");
            }              
        }
        else
        {
            cs.sendMessage(RED+"Unrecognized command. Try "+ChatColor.WHITE+"/anticheat help");
        }          
        return true;
    }
    
    public boolean hasPermission(CommandSender cs)
    {
        if(cs instanceof Player)
        {
            if(((Player)cs).hasPermission("anticheat.admin"))
            {
                return true;
            }
            else
            {
                cs.sendMessage("Insufficient permissions.");
                return false;
            }
        }
        else
        {
            return true;
        }
    }
    public void getPlayers()
    {
        high.clear();
        med.clear();
        low.clear();
        for(Player player : SERVER.getOnlinePlayers())
        {
            int level = playerManager.getLevel(player);
            if(level <= MED_THRESHOLD)
            {
                low.add(player);
            }
            else if(level <= HIGH_THRESHOLD)
            {
                med.add(player);
            }
            else
            {
                high.add(player);
            }
        }        
    }    
}
