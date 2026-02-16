package lobby.lobby.listeners;

import lobby.lobby.Lobby;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class LobbyDeathListener implements Listener {

    private final Lobby plugin;

    public LobbyDeathListener(Lobby plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        // Check if player died in lobby world
        World lobbyWorld = Bukkit.getWorld("world");
        if (lobbyWorld != null && event.getEntity().getWorld().equals(lobbyWorld)) {
            // Clear drops - don't drop items in lobby
            event.getDrops().clear();
            event.setDroppedExp(0);

            // Clear death message in lobby
            event.setDeathMessage(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        // Check if player died in lobby world
        World lobbyWorld = Bukkit.getWorld("world");
        if (lobbyWorld != null && event.getPlayer().getWorld().equals(lobbyWorld)) {
            // Respawn at lobby spawn location
            Location spawnLocation = lobbyWorld.getSpawnLocation();
            event.setRespawnLocation(spawnLocation);
        }
    }
}
