package lobby.lobby;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.text.SimpleDateFormat;
import java.util.Date;

public class LobbyScoreboardManager {

    private final Lobby plugin;

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
        emptyLine1.setScore(10);

        // Current time
        Score timeLabel = objective.getScore("§f時刻: §e--:--");
        timeLabel.setScore(9);

        // Empty line
        Score emptyLine2 = objective.getScore("§6");
        emptyLine2.setScore(8);

        // Play time
        Score playTimeLabel = objective.getScore("§fプレイ時間: §b--:--:--");
        playTimeLabel.setScore(7);

        // Empty line at bottom
        Score emptyLine3 = objective.getScore("§5");
        emptyLine3.setScore(6);

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

        // Get play time
        long playTimeSeconds = plugin.getPlayTimeManager().getBedwarsPlayTime(player.getUniqueId());
        String playTime = formatPlayTime(playTimeSeconds);

        // Update scores
        scoreboard.resetScores("§f時刻: §e--:--");
        Score timeScore = objective.getScore("§f時刻: §e" + currentTime);
        timeScore.setScore(9);

        scoreboard.resetScores("§fプレイ時間: §b--:--:--");
        Score playTimeScore = objective.getScore("§fプレイ時間: §b" + playTime);
        playTimeScore.setScore(7);
    }

    /**
     * Format play time as HH:MM:SS
     */
    private String formatPlayTime(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }

    /**
     * Remove scoreboard from a player
     */
    public void removeScoreboard(Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            player.setScoreboard(manager.getNewScoreboard());
        }
    }
}
