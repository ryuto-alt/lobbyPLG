package lobby.lobby.listeners;

import lobby.lobby.Lobby;
import lobby.lobby.gui.GameMenuGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class GameMenuListener implements Listener {

    private final Lobby plugin;

    public GameMenuListener(Lobby plugin) {
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
        if (item == null || item.getType() != Material.COMPASS) {
            return;
        }

        // Check if it's the Game menu item
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String displayName = item.getItemMeta().getDisplayName();
            if (displayName.equals("§6§lGame")) {
                event.setCancelled(true);
                GameMenuGUI.openGameMenu(player);
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

        // Check if the inventory is the Game menu
        if (event.getView().getTitle().equals("§6§lGame Menu")) {
            event.setCancelled(true);

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                return;
            }

            // Check if player clicked on Bedwars item
            if (clickedItem.getType() == Material.RED_BED && clickedItem.hasItemMeta()) {
                String displayName = clickedItem.getItemMeta().getDisplayName();
                if (displayName.equals("§c§lBedWars")) {
                    player.closeInventory();

                    // Teleport player to GameWorld
                    org.bukkit.World gameWorld = Bukkit.getWorld("world");
                    if (gameWorld != null) {
                        org.bukkit.Location spawnLoc = gameWorld.getSpawnLocation();
                        player.teleport(spawnLoc);
                        player.sendMessage("§aWelcome to BedWars!");
                    } else {
                        player.sendMessage("§cError: Game world not found!");
                    }
                }
            }
        }
    }
}
