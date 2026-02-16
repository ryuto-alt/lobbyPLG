package lobby.lobby;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LobbyScoreboardManager {

    private final Lobby plugin;
    private final Map<UUID, String> lastTimeValue = new HashMap<>();

    public LobbyScoreboardManager(Lobby plugin) {
        this.plugin = plugin;
    }

    /**
     * Create and set scoreboard for a player
     */
    public void setScoreboard(Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;

        Scoreboard scoreboard = manager.getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("lobby", "dummy", "§e§lLOBBY");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Empty line at top
        Score emptyLine1 = objective.getScore("§7");
        emptyLine1.setScore(3);

        // Current time (placeholder)
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        String currentTime = timeFormat.format(new Date());
        Score timeScore = objective.getScore("§f時刻: §e" + currentTime);
        timeScore.setScore(2);
        lastTimeValue.put(player.getUniqueId(), currentTime);

        // Empty line at bottom
        Score emptyLine2 = objective.getScore("§6");
        emptyLine2.setScore(1);

        player.setScoreboard(scoreboard);
    }

    /**
     * Update scoreboard for a player
     */
    public void updateScoreboard(Player player) {
        Scoreboard scoreboard = player.getScoreboard();
        if (scoreboard == null) return;

        Objective objective = scoreboard.getObjective("lobby");
        if (objective == null) return;

        // Get current time
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        String currentTime = timeFormat.format(new Date());

        // Get the last time value for this player
        String lastTime = lastTimeValue.get(player.getUniqueId());

        // Only update if the time has changed
        if (lastTime != null && !lastTime.equals(currentTime)) {
            // Remove old time entry
            scoreboard.resetScores("§f時刻: §e" + lastTime);

            // Add new time entry
            Score timeScore = objective.getScore("§f時刻: §e" + currentTime);
            timeScore.setScore(2);

            // Update stored value
            lastTimeValue.put(player.getUniqueId(), currentTime);
        } else if (lastTime == null) {
            // First update, just set the score
            Score timeScore = objective.getScore("§f時刻: §e" + currentTime);
            timeScore.setScore(2);
            lastTimeValue.put(player.getUniqueId(), currentTime);
        }
    }

    /**
     * Remove scoreboard from a player
     */
    public void removeScoreboard(Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            player.setScoreboard(manager.getNewScoreboard());
        }
        // Clean up stored time value
        lastTimeValue.remove(player.getUniqueId());
    }
}
