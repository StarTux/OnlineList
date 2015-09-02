package com.winthier.onlinelist;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Value;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class OnlineListPlugin extends JavaPlugin implements Listener {
    @Value class Group { String displayName; String permission; }
    List<Group> groups = null;
    
    @Override
    public void onEnable()
    {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable()
    {
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (args.length == 0) {
            showOnlineList(sender);
        } else if (args.length == 1 && "reload".equalsIgnoreCase(args[0]) && sender.hasPermission("onlinelist.reload")) {
            reloadConfig();
            groups = null;
            sender.sendMessage(ChatColor.YELLOW + "Configuration reloaded");
        } else {
            return false;
        }
        return true;
    }

    List<Group> getGroups()
    {
        if (groups == null) {
            List<Group> groups = new ArrayList<>();
            for (Map<?,?> map : getConfig().getMapList("groups")) {
                String displayName = (String)map.get("DisplayName");
                String permission = (String)map.get("Permission");
                groups.add(new Group(displayName, permission));
            }
            this.groups = groups;
        }
        return groups;
    }

    String format(String string)
    {
        return ChatColor.translateAlternateColorCodes('&', string);
    }

    void showOnlineList(CommandSender sender)
    {
        String title = format(getConfig().getString("format.Title"));
        Collection<Player> players = new LinkedList<Player>(getServer().getOnlinePlayers());
        title = title.replace("%PlayerCount%", "" + players.size());
        sender.sendMessage(title);
        String delim = format(getConfig().getString("format.Delim", " "));
        for (Group group : getGroups()) {
            List<String> list = playerList(group, players);
            StringBuilder sb = new StringBuilder(list.isEmpty() ? "" : list.get(0));
            for (int i = 1; i < list.size(); ++i) sb.append(delim).append(list.get(i));
            String line = format(getConfig().getString("format.Group"));
            line = line.replace("%DisplayName%", group.displayName);
            line = line.replace("%PlayerList%", sb.toString());
            line = line.replace("%PlayerCount%", "" + list.size());
            sender.sendMessage(line);
        }
    }

    List<String> playerList(Group group, Collection<Player> players)
    {
        List<String> result = new ArrayList<>();
        Iterator<Player> iter = players.iterator();
        while (iter.hasNext()) {
            Player player = iter.next();
            if (group.permission == null || player.hasPermission(group.permission)) {
                result.add(player.getName());
                iter.remove();
            }
        }
        Collections.sort(result, String.CASE_INSENSITIVE_ORDER);
        return result;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        if (!getConfig().getBoolean("ShowOnLogin")) return;
        if (!event.getPlayer().hasPermission("onlinelist.list")) return;
        showOnlineList(event.getPlayer());
    }
}
