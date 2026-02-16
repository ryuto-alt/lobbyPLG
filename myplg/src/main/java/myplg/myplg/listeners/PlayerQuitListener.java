package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {
    private final PvPGame plugin;

    public PlayerQuitListener(PvPGame plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Clear invisibility armor storage on quit
        if (plugin.getInvisibilityArmorListener() != null) {
            plugin.getInvisibilityArmorListener().clearStoredArmor(player);
        }
    }
}
