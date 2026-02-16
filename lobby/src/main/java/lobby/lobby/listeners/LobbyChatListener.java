package lobby.lobby.listeners;

import lobby.lobby.Lobby;
import lobby.lobby.data.AdminDataManager;
import lobby.lobby.data.StaffDataManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * Handles chat formatting in the lobby
 * Admin players have their messages displayed in purple
 * Staff players have their messages displayed in yellow
 * Priority: Admin > Staff > Normal
 */
public class LobbyChatListener implements Listener {

    private final Lobby plugin;
    private static final String LOBBY_WORLD_NAME = "lobby";

    public LobbyChatListener(Lobby plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        // Only apply in lobby world
        if (!player.getWorld().getName().equalsIgnoreCase(LOBBY_WORLD_NAME)) {
            return;
        }

        AdminDataManager adminManager = plugin.getAdminDataManager();
        StaffDataManager staffManager = plugin.getStaffDataManager();

        if (adminManager.isAdmin(player)) {
            // Admin player - format message with purple color (highest priority)
            // Format: [Admin] PlayerName: message (all in purple)
            String adminPrefix = AdminDataManager.ADMIN_PREFIX;
            ChatColor chatColor = AdminDataManager.ADMIN_CHAT_COLOR;

            // Set format: [Admin] name > message (all purple)
            event.setFormat(adminPrefix + "%s" + ChatColor.RESET + " > " + chatColor + "%s");
        } else if (staffManager.isStaff(player)) {
            // Staff player - format message with yellow color
            // Format: [Staff] PlayerName: message (all in yellow)
            String staffPrefix = StaffDataManager.STAFF_PREFIX;
            ChatColor chatColor = StaffDataManager.STAFF_CHAT_COLOR;

            // Set format: [Staff] name > message (all yellow)
            event.setFormat(staffPrefix + "%s" + ChatColor.RESET + " > " + chatColor + "%s");
        } else {
            // Normal player - default format
            event.setFormat(ChatColor.WHITE + "%s" + ChatColor.RESET + " > " + ChatColor.WHITE + "%s");
        }
    }
}
