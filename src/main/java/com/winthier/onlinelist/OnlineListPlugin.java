package com.winthier.onlinelist;

import com.winthier.connect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Value;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
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
    private Permission permission = null;
    
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

    private Permission getPermission() {
        if (permission == null) {
            RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(Permission.class);
            if (permissionProvider != null) {
                permission = permissionProvider.getProvider();
            }
        }
        return permission;
    }

    private boolean isStaff(UUID uuid) {
        Player bukkitPlayer = getServer().getPlayer(uuid);
        if (bukkitPlayer != null) {
            return bukkitPlayer.hasPermission("onlinelist.staff");
        } else {
            Permission permission = getPermission();
            if (permission == null) return false;
            OfflinePlayer off = getServer().getOfflinePlayer(uuid);
            return permission.playerHas((String)null, off, "onlinelist.staff");
        }
    }

    void showOnlineList(CommandSender sender)
    {
        Player playerSender = sender instanceof Player ? (Player)sender : null;
        Map<String, List<OnlinePlayer>> serverList = new HashMap<>();
        int totalCount = 0;
        for (ServerConnection con: new ArrayList<>(Connect.getInstance().getServer().getConnections())) {
            List<OnlinePlayer> conList = new ArrayList<>(con.getOnlinePlayers());
            String displayName = con.getName();
            Client client = Connect.getInstance().getClient(displayName);
            if (client != null) displayName = client.getDisplayName();
            List<OnlinePlayer> playerList = serverList.get(displayName);
            if (playerList == null) {
                playerList = new ArrayList<>();
                serverList.put(displayName, playerList);
            }
            playerList.addAll(conList);
            totalCount += conList.size();
        }
        String[] serverNames = serverList.keySet().toArray(new String[0]);
        Arrays.sort(serverNames);
        Msg.send(sender, "&3&l%s Player List&r &3(&r%d&3)", Connect.getInstance().getServer().getDisplayName(), totalCount);
        for (String serverName: serverNames) {
            OnlinePlayer[] playerArray = serverList.get(serverName).toArray(new OnlinePlayer[0]);
            if (playerArray.length == 0) continue;
            Arrays.sort(playerArray, new Comparator<OnlinePlayer>() {
                @Override public int compare(OnlinePlayer a, OnlinePlayer b) { return String.CASE_INSENSITIVE_ORDER.compare(a.getName(), b.getName()); }
                @Override public boolean equals(Object o) { return this == o; }
            });
            List<Object> json = new ArrayList<>();
            json.add(Msg.button(
                         ChatColor.DARK_AQUA,
                         Msg.format(" &3%s(&r%d&3)", serverName, playerArray.length),
                         null, null));
            for (OnlinePlayer player: playerArray) {
                json.add(" ");
                ChatColor color;
                if (isStaff(player.getUuid())) {
                    color = ChatColor.AQUA;
                } else {
                    color = ChatColor.WHITE;
                }
                json.add(Msg.button(
                             color,
                             player.getName(),
                             player.getName(),
                             "/msg " + player.getName() + " "));
            }
            if (playerSender != null) {
                Msg.raw(playerSender, json);
            } else {
                sender.sendMessage(Msg.jsonToString(json));
            }
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
