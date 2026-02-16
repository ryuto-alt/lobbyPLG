package lobby.lobby;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Utility class to check player permissions
 */
public class PermissionUtil {

    /**
     * Check if player has OP level 4 or higher
     * @param player Player to check
     * @return true if OP level 4 or higher
     */
    public static boolean isOpLevel4(Player player) {
        if (!player.isOp()) {
            return false;
        }

        // Read ops.json file
        File opsFile = new File(Bukkit.getServer().getWorldContainer(), "ops.json");
        if (!opsFile.exists()) {
            // If ops.json doesn't exist, return true for OPs (default behavior)
            return true;
        }

        try (FileReader reader = new FileReader(opsFile)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (!element.isJsonArray()) {
                return true; // Return true if format is invalid
            }

            JsonArray opsArray = element.getAsJsonArray();
            String playerUuid = player.getUniqueId().toString();
            String playerName = player.getName();

            for (JsonElement opElement : opsArray) {
                if (!opElement.isJsonObject()) {
                    continue;
                }

                JsonObject opObject = opElement.getAsJsonObject();

                // Check if UUID or name matches
                boolean isMatch = false;
                if (opObject.has("uuid") && opObject.get("uuid").getAsString().equals(playerUuid)) {
                    isMatch = true;
                } else if (opObject.has("name") && opObject.get("name").getAsString().equalsIgnoreCase(playerName)) {
                    isMatch = true;
                }

                if (isMatch) {
                    // Check level field
                    if (opObject.has("level")) {
                        int level = opObject.get("level").getAsInt();
                        return level >= 4;
                    } else {
                        // If level is not specified, assume level 4
                        return true;
                    }
                }
            }

            // If not in ops.json but is OP, return true
            return true;

        } catch (IOException e) {
            Bukkit.getLogger().warning("Failed to read ops.json: " + e.getMessage());
            return true;
        } catch (Exception e) {
            Bukkit.getLogger().warning("Failed to parse ops.json: " + e.getMessage());
            return true;
        }
    }
}
