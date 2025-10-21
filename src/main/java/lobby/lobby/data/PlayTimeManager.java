package lobby.lobby.data;

import lobby.lobby.Lobby;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayTimeManager {

    private final Lobby plugin;
    private File playTimeFile;
    private FileConfiguration playTimeConfig;
    private Map<UUID, Long> bedwarsPlayTime; // Store playtime in seconds
    private Map<UUID, Long> sessionStartTime; // Track when player entered the game

    public PlayTimeManager(Lobby plugin) {
        this.plugin = plugin;
        this.bedwarsPlayTime = new HashMap<>();
        this.sessionStartTime = new HashMap<>();
        setupPlayTimeFile();
    }

    private void setupPlayTimeFile() {
        playTimeFile = new File(plugin.getDataFolder(), "playtime.yml");

        if (!playTimeFile.exists()) {
            playTimeFile.getParentFile().mkdirs();
            try {
                playTimeFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create playtime.yml file!");
                e.printStackTrace();
            }
        }

        playTimeConfig = YamlConfiguration.loadConfiguration(playTimeFile);
    }

    public void loadPlayTime() {
        if (playTimeConfig.contains("bedwars")) {
            for (String uuidString : playTimeConfig.getConfigurationSection("bedwars").getKeys(false)) {
                UUID uuid = UUID.fromString(uuidString);
                long playtime = playTimeConfig.getLong("bedwars." + uuidString);
                bedwarsPlayTime.put(uuid, playtime);
            }
        }

        plugin.getLogger().info("Loaded playtime data for " + bedwarsPlayTime.size() + " players");
    }

    public void savePlayTime() {
        // Save all current playtimes
        for (Map.Entry<UUID, Long> entry : bedwarsPlayTime.entrySet()) {
            playTimeConfig.set("bedwars." + entry.getKey().toString(), entry.getValue());
        }

        try {
            playTimeConfig.save(playTimeFile);
            plugin.getLogger().info("Saved playtime data");
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save playtime.yml!");
            e.printStackTrace();
        }
    }

    /**
     * Start tracking playtime for a player who entered the game world
     */
    public void startPlayTime(UUID playerUUID) {
        sessionStartTime.put(playerUUID, System.currentTimeMillis());
    }

    /**
     * Stop tracking playtime for a player who left the game world
     */
    public void stopPlayTime(UUID playerUUID) {
        if (sessionStartTime.containsKey(playerUUID)) {
            long startTime = sessionStartTime.get(playerUUID);
            long currentTime = System.currentTimeMillis();
            long sessionDuration = (currentTime - startTime) / 1000; // Convert to seconds

            // Add session duration to total playtime
            long currentPlayTime = bedwarsPlayTime.getOrDefault(playerUUID, 0L);
            bedwarsPlayTime.put(playerUUID, currentPlayTime + sessionDuration);

            sessionStartTime.remove(playerUUID);
        }
    }

    /**
     * Get total Bedwars playtime for a player in seconds
     */
    public long getBedwarsPlayTime(UUID playerUUID) {
        long totalTime = bedwarsPlayTime.getOrDefault(playerUUID, 0L);

        // If player is currently playing, add current session time
        if (sessionStartTime.containsKey(playerUUID)) {
            long startTime = sessionStartTime.get(playerUUID);
            long currentTime = System.currentTimeMillis();
            long sessionDuration = (currentTime - startTime) / 1000;
            totalTime += sessionDuration;
        }

        return totalTime;
    }

    /**
     * Format playtime in hours, minutes, and seconds
     */
    public String formatPlayTime(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, secs);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, secs);
        } else {
            return String.format("%ds", secs);
        }
    }
}
