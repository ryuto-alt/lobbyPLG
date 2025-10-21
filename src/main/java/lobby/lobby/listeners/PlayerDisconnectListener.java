package lobby.lobby.listeners;

import lobby.lobby.Lobby;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerDisconnectListener implements Listener {

    private final Lobby plugin;

    public PlayerDisconnectListener(Lobby plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Stop playtime tracking if player was in game world
        if (player.getWorld().getName().equalsIgnoreCase("world")) {
            plugin.getPlayTimeManager().stopPlayTime(player.getUniqueId());
            plugin.getPlayTimeManager().savePlayTime();
        }
    }
}
