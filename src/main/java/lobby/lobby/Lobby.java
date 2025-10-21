package lobby.lobby;

import lobby.lobby.listeners.LobbyJoinListener;
import lobby.lobby.listeners.GameMenuListener;
import lobby.lobby.listeners.ProfileMenuListener;
import lobby.lobby.listeners.PlayerDisconnectListener;
import lobby.lobby.data.PlayTimeManager;
import lobby.lobby.tasks.ScoreboardUpdateTask;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class Lobby extends JavaPlugin {

    private PlayTimeManager playTimeManager;
    private LobbyScoreboardManager scoreboardManager;
    private BukkitTask scoreboardUpdateTask;

    @Override
    public void onEnable() {
        // Initialize managers
        playTimeManager = new PlayTimeManager(this);
        scoreboardManager = new LobbyScoreboardManager(this);

        // Load play time data
        playTimeManager.loadPlayTime();

        // Register listeners
        getServer().getPluginManager().registerEvents(new LobbyJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new GameMenuListener(this), this);
        getServer().getPluginManager().registerEvents(new ProfileMenuListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDisconnectListener(this), this);

        // Start scoreboard update task (update every second = 20 ticks)
        ScoreboardUpdateTask updateTask = new ScoreboardUpdateTask(this, scoreboardManager);
        scoreboardUpdateTask = updateTask.runTaskTimer(this, 0L, 20L);

        getLogger().info("Lobby plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        // Cancel scoreboard update task
        if (scoreboardUpdateTask != null) {
            scoreboardUpdateTask.cancel();
        }

        // Save play time data
        if (playTimeManager != null) {
            playTimeManager.savePlayTime();
        }

        getLogger().info("Lobby plugin has been disabled!");
    }

    public PlayTimeManager getPlayTimeManager() {
        return playTimeManager;
    }

    public LobbyScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }
}
