package lobby.lobby.commands;

import lobby.lobby.Lobby;
import lobby.lobby.PermissionUtil;
import lobby.lobby.data.AdminDataManager;
import lobby.lobby.data.StaffDataManager;
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
 * /staff <player> command
 * Toggles staff title for a player
 * Requires OP level 4+
 */
public class StaffCommand implements CommandExecutor, TabCompleter {

    private final Lobby plugin;

    public StaffCommand(Lobby plugin) {
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
            player.sendMessage(ChatColor.RED + "Usage: /staff <player>");
            return true;
        }

        // Find target player
        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);

        if (target == null) {
            player.sendMessage(ChatColor.RED + "Player " + targetName + " not found.");
            return true;
        }

        // Toggle staff status
        StaffDataManager staffManager = plugin.getStaffDataManager();
        boolean isNowStaff = staffManager.toggleStaff(target.getUniqueId());

        if (isNowStaff) {
            // Staff title added
            player.sendMessage(ChatColor.GREEN + target.getName() + " is now a Staff member!");
            target.sendMessage(ChatColor.YELLOW + "You have been granted the Staff title!");

            // Update display name for lobby
            updatePlayerDisplay(target);
        } else {
            // Staff title removed
            player.sendMessage(ChatColor.YELLOW + target.getName() + " is no longer a Staff member.");
            target.sendMessage(ChatColor.YELLOW + "Your Staff title has been removed.");

            // Reset display name
            updatePlayerDisplay(target);
        }

        return true;
    }

    /**
     * Update player's display name based on admin/staff status
     * Priority: Admin > Staff > Normal
     */
    private void updatePlayerDisplay(Player player) {
        AdminDataManager adminManager = plugin.getAdminDataManager();
        StaffDataManager staffManager = plugin.getStaffDataManager();

        // Check if in lobby world
        String lobbyWorldName = "lobby";
        if (player.getWorld().getName().equalsIgnoreCase(lobbyWorldName)) {
            if (adminManager.isAdmin(player)) {
                // Admin takes priority
                player.setPlayerListName(AdminDataManager.ADMIN_PREFIX + player.getName());
            } else if (staffManager.isStaff(player)) {
                // Staff if not admin
                player.setPlayerListName(StaffDataManager.STAFF_PREFIX + player.getName());
            } else {
                // Normal player
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
