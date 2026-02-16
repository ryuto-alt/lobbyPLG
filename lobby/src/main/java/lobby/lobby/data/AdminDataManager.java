package lobby.lobby.data;

import lobby.lobby.Lobby;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Manages admin title data
 * Admin players get [Admin] prefix displayed before their name
 */
public class AdminDataManager {

    private final Lobby plugin;
    private File adminFile;
    private FileConfiguration adminConfig;
    private Set<UUID> admins;

    // Admin prefix with purple color (name also purple)
    public static final String ADMIN_PREFIX = ChatColor.LIGHT_PURPLE + "[Admin] ";
    public static final ChatColor ADMIN_CHAT_COLOR = ChatColor.LIGHT_PURPLE;

    public AdminDataManager(Lobby plugin) {
        this.plugin = plugin;
        this.admins = new HashSet<>();
        setupAdminFile();
    }

    private void setupAdminFile() {
        adminFile = new File(plugin.getDataFolder(), "admins.yml");

        if (!adminFile.exists()) {
            adminFile.getParentFile().mkdirs();
            try {
                adminFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create admins.yml file!");
                e.printStackTrace();
            }
        }

        adminConfig = YamlConfiguration.loadConfiguration(adminFile);
    }

    public void loadAdmins() {
        admins.clear();
        if (adminConfig.contains("admins")) {
            for (String uuidString : adminConfig.getStringList("admins")) {
                try {
                    UUID uuid = UUID.fromString(uuidString);
                    admins.add(uuid);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in admins.yml: " + uuidString);
                }
            }
        }
        plugin.getLogger().info("Loaded " + admins.size() + " admin(s)");
    }

    public void saveAdmins() {
        adminConfig.set("admins", admins.stream().map(UUID::toString).toList());

        try {
            adminConfig.save(adminFile);
            plugin.getLogger().info("Saved admin data");
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save admins.yml!");
            e.printStackTrace();
        }
    }

    /**
     * Check if a player has admin title
     */
    public boolean isAdmin(UUID playerUUID) {
        return admins.contains(playerUUID);
    }

    /**
     * Check if a player has admin title
     */
    public boolean isAdmin(Player player) {
        return isAdmin(player.getUniqueId());
    }

    /**
     * Toggle admin title for a player
     * @return true if admin was added, false if removed
     */
    public boolean toggleAdmin(UUID playerUUID) {
        if (admins.contains(playerUUID)) {
            admins.remove(playerUUID);
            saveAdmins();
            return false;
        } else {
            admins.add(playerUUID);
            saveAdmins();
            return true;
        }
    }

    /**
     * Add admin title to a player
     */
    public void addAdmin(UUID playerUUID) {
        if (!admins.contains(playerUUID)) {
            admins.add(playerUUID);
            saveAdmins();
        }
    }

    /**
     * Remove admin title from a player
     */
    public void removeAdmin(UUID playerUUID) {
        if (admins.contains(playerUUID)) {
            admins.remove(playerUUID);
            saveAdmins();
        }
    }

    /**
     * Get all admin UUIDs
     */
    public Set<UUID> getAdmins() {
        return new HashSet<>(admins);
    }

    /**
     * Get display name with admin prefix if applicable
     */
    public String getDisplayName(Player player) {
        if (isAdmin(player)) {
            return ADMIN_PREFIX + player.getName();
        }
        return player.getName();
    }

    /**
     * Get the admin data file path for external access
     */
    public static File getAdminFilePath() {
        // Return the standard location in lobby plugin data folder
        return new File(Bukkit.getPluginsFolder(), "lobby/admins.yml");
    }
}
