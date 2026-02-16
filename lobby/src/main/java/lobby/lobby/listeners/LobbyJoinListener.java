package lobby.lobby.listeners;

import lobby.lobby.Lobby;
import lobby.lobby.data.AdminDataManager;
import lobby.lobby.data.StaffDataManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

public class LobbyJoinListener implements Listener {

    private final Lobby plugin;

    public LobbyJoinListener(Lobby plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Check if player is in lobby world
        if (player.getWorld().getName().equalsIgnoreCase("lobby")) {
            giveLobbyItems(player);
            // Set scoreboard for player
            plugin.getScoreboardManager().setScoreboard(player);
            // Set admin prefix if applicable
            updateAdminDisplayName(player);
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        org.bukkit.World fromWorld = event.getFrom();
        org.bukkit.World toWorld = player.getWorld();

        // Check if player entered lobby world
        if (toWorld.getName().equalsIgnoreCase("lobby")) {
            giveLobbyItems(player);
            // Set scoreboard for player
            plugin.getScoreboardManager().setScoreboard(player);
            // Set admin prefix if applicable
            updateAdminDisplayName(player);

            // Stop playtime tracking if coming from game world
            if (fromWorld.getName().equalsIgnoreCase("world")) {
                plugin.getPlayTimeManager().stopPlayTime(player.getUniqueId());
                plugin.getPlayTimeManager().savePlayTime();
            }
        } else if (toWorld.getName().equalsIgnoreCase("world")) {
            // Clear inventory when entering game world
            player.getInventory().clear();
            // Remove scoreboard when leaving lobby
            plugin.getScoreboardManager().removeScoreboard(player);

            // Start playtime tracking
            plugin.getPlayTimeManager().startPlayTime(player.getUniqueId());
        } else {
            // Clear inventory when leaving lobby to other worlds
            player.getInventory().clear();
            // Remove scoreboard when leaving lobby
            plugin.getScoreboardManager().removeScoreboard(player);
        }
    }

    private void giveLobbyItems(Player player) {
        // Clear inventory first
        player.getInventory().clear();

        // Game menu item (Compass) - Slot 0
        ItemStack gameItem = new ItemStack(Material.COMPASS);
        org.bukkit.inventory.meta.ItemMeta gameMeta = gameItem.getItemMeta();
        if (gameMeta != null) {
            gameMeta.setDisplayName("§6§lGame");
            gameMeta.setLore(java.util.Arrays.asList("§7Right click to open game menu"));
            gameItem.setItemMeta(gameMeta);
        }
        player.getInventory().setItem(0, gameItem);

        // Profile menu item (Player Head) - Slot 1
        ItemStack profileItem = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta profileMeta = (SkullMeta) profileItem.getItemMeta();
        if (profileMeta != null) {
            profileMeta.setDisplayName("§b§lProfile");
            profileMeta.setLore(java.util.Arrays.asList("§7Right click to view your stats"));
            profileMeta.setOwningPlayer(player);
            profileItem.setItemMeta(profileMeta);
        }
        player.getInventory().setItem(1, profileItem);

        player.sendMessage("§aWelcome to the lobby!");
    }

    /**
     * Update player's display name (TAB list) based on admin/staff status
     * Priority: Admin > Staff > Normal
     */
    private void updateAdminDisplayName(Player player) {
        AdminDataManager adminManager = plugin.getAdminDataManager();
        StaffDataManager staffManager = plugin.getStaffDataManager();

        if (adminManager.isAdmin(player)) {
            // Set [Admin] prefix in TAB list (highest priority)
            player.setPlayerListName(AdminDataManager.ADMIN_PREFIX + player.getName());
        } else if (staffManager.isStaff(player)) {
            // Set [Staff] prefix in TAB list
            player.setPlayerListName(StaffDataManager.STAFF_PREFIX + player.getName());
        } else {
            // Reset to normal name
            player.setPlayerListName(player.getName());
        }
    }
}
