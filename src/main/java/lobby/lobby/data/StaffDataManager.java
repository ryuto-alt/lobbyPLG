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
 * Manages staff title data
 * Staff players get [Staff] prefix displayed before their name in yellow
 */
public class StaffDataManager {

    private final Lobby plugin;
    private File staffFile;
    private FileConfiguration staffConfig;
    private Set<UUID> staffMembers;

    // Staff prefix with yellow color (name also yellow)
    public static final String STAFF_PREFIX = ChatColor.YELLOW + "[Staff] ";
    public static final ChatColor STAFF_CHAT_COLOR = ChatColor.YELLOW;

    public StaffDataManager(Lobby plugin) {
        this.plugin = plugin;
        this.staffMembers = new HashSet<>();
        setupStaffFile();
    }

    private void setupStaffFile() {
        staffFile = new File(plugin.getDataFolder(), "staff.yml");

        if (!staffFile.exists()) {
            staffFile.getParentFile().mkdirs();
            try {
                staffFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create staff.yml file!");
                e.printStackTrace();
            }
        }

        staffConfig = YamlConfiguration.loadConfiguration(staffFile);
    }

    public void loadStaff() {
        staffMembers.clear();
        if (staffConfig.contains("staff")) {
            for (String uuidString : staffConfig.getStringList("staff")) {
                try {
                    UUID uuid = UUID.fromString(uuidString);
                    staffMembers.add(uuid);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in staff.yml: " + uuidString);
                }
            }
        }
        plugin.getLogger().info("Loaded " + staffMembers.size() + " staff member(s)");
    }

    public void saveStaff() {
        staffConfig.set("staff", staffMembers.stream().map(UUID::toString).toList());

        try {
            staffConfig.save(staffFile);
            plugin.getLogger().info("Saved staff data");
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save staff.yml!");
            e.printStackTrace();
        }
    }

    /**
     * Check if a player has staff title
     */
    public boolean isStaff(UUID playerUUID) {
        return staffMembers.contains(playerUUID);
    }

    /**
     * Check if a player has staff title
     */
    public boolean isStaff(Player player) {
        return isStaff(player.getUniqueId());
    }

    /**
     * Toggle staff title for a player
     * @return true if staff was added, false if removed
     */
    public boolean toggleStaff(UUID playerUUID) {
        if (staffMembers.contains(playerUUID)) {
            staffMembers.remove(playerUUID);
            saveStaff();
            return false;
        } else {
            staffMembers.add(playerUUID);
            saveStaff();
            return true;
        }
    }

    /**
     * Add staff title to a player
     */
    public void addStaff(UUID playerUUID) {
        if (!staffMembers.contains(playerUUID)) {
            staffMembers.add(playerUUID);
            saveStaff();
        }
    }

    /**
     * Remove staff title from a player
     */
    public void removeStaff(UUID playerUUID) {
        if (staffMembers.contains(playerUUID)) {
            staffMembers.remove(playerUUID);
            saveStaff();
        }
    }

    /**
     * Get all staff UUIDs
     */
    public Set<UUID> getStaffMembers() {
        return new HashSet<>(staffMembers);
    }

    /**
     * Get display name with staff prefix if applicable
     */
    public String getDisplayName(Player player) {
        if (isStaff(player)) {
            return STAFF_PREFIX + player.getName();
        }
        return player.getName();
    }

    /**
     * Get the staff data file path for external access
     */
    public static File getStaffFilePath() {
        // Return the standard location in lobby plugin data folder
        return new File(Bukkit.getPluginsFolder(), "lobby/staff.yml");
    }
}
