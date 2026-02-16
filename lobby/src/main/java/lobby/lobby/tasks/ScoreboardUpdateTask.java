package lobby.lobby.tasks;

import lobby.lobby.Lobby;
import lobby.lobby.LobbyScoreboardManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class ScoreboardUpdateTask extends BukkitRunnable {

    private final Lobby plugin;
    private final LobbyScoreboardManager scoreboardManager;

    public ScoreboardUpdateTask(Lobby plugin, LobbyScoreboardManager scoreboardManager) {
        this.plugin = plugin;
        this.scoreboardManager = scoreboardManager;
    }

    @Override
    public void run() {
        // Update scoreboard for all players in lobby world
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().getName().equalsIgnoreCase("lobby")) {
                scoreboardManager.updateScoreboard(player);
            }
        }
    }
}
