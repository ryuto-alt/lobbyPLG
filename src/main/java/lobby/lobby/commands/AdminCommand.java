package lobby.lobby.commands;

import lobby.lobby.Lobby;
import lobby.lobby.PermissionUtil;
import lobby.lobby.data.AdminDataManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * /admin <player> command
 * Toggles admin title for a player
 * Requires OP level 4+
 */
public class AdminCommand implements CommandExecutor, TabCompleter {

    private final Lobby plugin;

    public AdminCommand(Lobby plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Permission check - must be a player with OP level 4
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;
        if (!PermissionUtil.isOpLevel4(player)) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        // Usage check
        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "Usage: /admin <player>");
            return true;
        }

        // Find target player
        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);

        if (target == null) {
            player.sendMessage(ChatColor.RED + "Player " + targetName + " not found.");
            return true;
        }

        // Toggle admin status
        AdminDataManager adminManager = plugin.getAdminDataManager();
        boolean isNowAdmin = adminManager.toggleAdmin(target.getUniqueId());

        if (isNowAdmin) {
            // Admin title added
            player.sendMessage(ChatColor.GREEN + target.getName() + " is now an Admin!");
            target.sendMessage(ChatColor.LIGHT_PURPLE + "You have been granted the Admin title!");

            // Update display name for lobby
            updatePlayerDisplay(target);
        } else {
            // Admin title removed
            player.sendMessage(ChatColor.YELLOW + target.getName() + " is no longer an Admin.");
            target.sendMessage(ChatColor.YELLOW + "Your Admin title has been removed.");

            // Reset display name
            updatePlayerDisplay(target);
        }

        return true;
    }

    /**
     * Update player's display name based on admin status
     */
    private void updatePlayerDisplay(Player player) {
        AdminDataManager adminManager = plugin.getAdminDataManager();

        // Check if in lobby world
        String lobbyWorldName = "lobby";
        if (player.getWorld().getName().equalsIgnoreCase(lobbyWorldName)) {
            if (adminManager.isAdmin(player)) {
                player.setPlayerListName(AdminDataManager.ADMIN_PREFIX + player.getName());
            } else {
                player.setPlayerListName(player.getName());
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
