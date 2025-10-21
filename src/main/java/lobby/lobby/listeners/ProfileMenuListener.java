package lobby.lobby.listeners;

import lobby.lobby.Lobby;
import lobby.lobby.gui.ProfileMenuGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class ProfileMenuListener implements Listener {

    private final Lobby plugin;

    public ProfileMenuListener(Lobby plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // Check if player is in lobby world
        if (!player.getWorld().getName().equalsIgnoreCase("lobby")) {
            return;
        }

        // Check if player right-clicked
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.PLAYER_HEAD) {
            return;
        }

        // Check if it's the Profile menu item
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String displayName = item.getItemMeta().getDisplayName();
            if (displayName.equals("§b§lProfile")) {
                event.setCancelled(true);
                ProfileMenuGUI.openProfileMenu(player, plugin);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        // Check if player is in lobby world
        if (!player.getWorld().getName().equalsIgnoreCase("lobby")) {
            return;
        }

        // Check if the inventory is the Profile menu
        if (event.getView().getTitle().equals("§b§lYour Profile")) {
            event.setCancelled(true);
            // No actions needed for profile menu (it's just for viewing stats)
        }
    }
}
