package lobby.lobby;

import lobby.lobby.commands.AdminCommand;
import lobby.lobby.commands.BwEggCommand;
import lobby.lobby.commands.DebweggCommand;
import lobby.lobby.commands.StaffCommand;
import lobby.lobby.data.AdminDataManager;
import lobby.lobby.data.GameNpcDataManager;
import lobby.lobby.data.StaffDataManager;
import lobby.lobby.listeners.LobbyJoinListener;
import lobby.lobby.listeners.GameMenuListener;
import lobby.lobby.listeners.ProfileMenuListener;
import lobby.lobby.listeners.PlayerDisconnectListener;
import lobby.lobby.listeners.LobbyChatListener;
import lobby.lobby.listeners.GameNpcListener;
import lobby.lobby.listeners.LobbyDeathListener;
import lobby.lobby.listeners.BedEffectSelectListener;
import lobby.lobby.gui.BedEffectSelectGUI;
import lobby.lobby.data.PlayTimeManager;
import lobby.lobby.tasks.ScoreboardUpdateTask;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class Lobby extends JavaPlugin {

    private PlayTimeManager playTimeManager;
    private AdminDataManager adminDataManager;
    private StaffDataManager staffDataManager;
    private GameNpcDataManager gameNpcDataManager;
    private LobbyScoreboardManager scoreboardManager;
    private BukkitTask scoreboardUpdateTask;
    private BedEffectSelectGUI bedEffectSelectGUI;

    @Override
    public void onEnable() {
        // Initialize managers
        playTimeManager = new PlayTimeManager(this);
        adminDataManager = new AdminDataManager(this);
        staffDataManager = new StaffDataManager(this);
        gameNpcDataManager = new GameNpcDataManager(this);
        scoreboardManager = new LobbyScoreboardManager(this);
        bedEffectSelectGUI = new BedEffectSelectGUI(this);

        // Load data
        playTimeManager.loadPlayTime();
        adminDataManager.loadAdmins();
        staffDataManager.loadStaff();

        // Spawn saved NPCs (delayed to ensure world is loaded)
        getServer().getScheduler().runTaskLater(this, () -> {
            gameNpcDataManager.spawnAllNpcs();
        }, 20L);

        // Register commands
        getCommand("admin").setExecutor(new AdminCommand(this));
        getCommand("admin").setTabCompleter(new AdminCommand(this));
        getCommand("staff").setExecutor(new StaffCommand(this));
        getCommand("staff").setTabCompleter(new StaffCommand(this));
        getCommand("bwegg").setExecutor(new BwEggCommand(this));
        getCommand("debwegg").setExecutor(new DebweggCommand(this));

        // Register listeners
        getServer().getPluginManager().registerEvents(new LobbyJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new GameMenuListener(this), this);
        getServer().getPluginManager().registerEvents(new ProfileMenuListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDisconnectListener(this), this);
        getServer().getPluginManager().registerEvents(new LobbyChatListener(this), this);
        getServer().getPluginManager().registerEvents(new GameNpcListener(this), this);
        getServer().getPluginManager().registerEvents(new LobbyDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new BedEffectSelectListener(this, bedEffectSelectGUI), this);

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

        // Save admin data
        if (adminDataManager != null) {
            adminDataManager.saveAdmins();
        }

        // Save staff data
        if (staffDataManager != null) {
            staffDataManager.saveStaff();
        }

        getLogger().info("Lobby plugin has been disabled!");
    }

    public PlayTimeManager getPlayTimeManager() {
        return playTimeManager;
    }

    public AdminDataManager getAdminDataManager() {
        return adminDataManager;
    }

    public StaffDataManager getStaffDataManager() {
        return staffDataManager;
    }

    public LobbyScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public GameNpcDataManager getGameNpcDataManager() {
        return gameNpcDataManager;
    }

    public BedEffectSelectGUI getBedEffectSelectGUI() {
        return bedEffectSelectGUI;
    }
}
