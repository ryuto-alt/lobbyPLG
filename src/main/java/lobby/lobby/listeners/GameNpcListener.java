package lobby.lobby.listeners;

import lobby.lobby.Lobby;
import lobby.lobby.commands.BwEggCommand;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Handles Bedwars game NPC interactions
 * - Spawning custom villager from egg
 * - Teleporting players to game world on click
 */
public class GameNpcListener implements Listener {

    private final Lobby plugin;
    private static final String GAME_WORLD_NAME = "world";

    public GameNpcListener(Lobby plugin) {
        this.plugin = plugin;
    }

    /**
     * Handle custom egg placement to spawn Bedwars NPC
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getItem() == null) return;
        if (event.getItem().getType() != Material.VILLAGER_SPAWN_EGG) return;
        if (event.getClickedBlock() == null) return;

        ItemStack item = event.getItem();
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return;

        List<String> lore = meta.getLore();
        if (lore == null) return;

        // Check if this is our custom Bedwars NPC egg
        boolean isBedwarsEgg = lore.stream()
                .anyMatch(line -> line.contains(BwEggCommand.GAME_NPC_IDENTIFIER));

        if (!isBedwarsEgg) return;

        // Cancel default spawn
        event.setCancelled(true);

        // Calculate spawn location and facing direction
        Location spawnLoc = event.getClickedBlock().getLocation().add(0.5, 1, 0.5);

        // Set villager to face the player (opposite of player's direction)
        float playerYaw = event.getPlayer().getLocation().getYaw();
        float villagerYaw = playerYaw + 180; // Face towards player
        spawnLoc.setYaw(villagerYaw);

        Villager villager = (Villager) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.VILLAGER);

        // Configure villager
        villager.setCustomName(BwEggCommand.GAME_NPC_NAME);
        villager.setCustomNameVisible(true);
        villager.setAI(false);
        villager.setInvulnerable(true);
        villager.setSilent(true);
        villager.setProfession(Villager.Profession.NITWIT);
        villager.setVillagerLevel(5);

        // Set rotation to face player
        villager.setRotation(villagerYaw, 0);

        // Add scoreboard tag for identification
        villager.addScoreboardTag(BwEggCommand.GAME_NPC_IDENTIFIER);

        // Save NPC location to data file
        plugin.getGameNpcDataManager().addNpc(spawnLoc);

        // Remove egg from inventory (consume one)
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            event.getPlayer().getInventory().setItemInMainHand(null);
        }

        event.getPlayer().sendMessage(ChatColor.GREEN + "Bedwars NPC spawned!");
    }

    /**
     * Handle clicking on Bedwars NPC villager
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        // Only handle main hand interactions
        if (event.getHand() != EquipmentSlot.HAND) return;

        Entity entity = event.getRightClicked();

        // Check if it's a villager with our tag
        if (entity.getType() != EntityType.VILLAGER) return;
        if (!entity.getScoreboardTags().contains(BwEggCommand.GAME_NPC_IDENTIFIER)) return;

        event.setCancelled(true);

        Player player = event.getPlayer();
        teleportToGame(player);
    }

    /**
     * Prevent villager trading GUI from opening
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onVillagerTrade(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (entity.getType() != EntityType.VILLAGER) return;
        if (entity.getScoreboardTags().contains(BwEggCommand.GAME_NPC_IDENTIFIER)) {
            event.setCancelled(true);
        }
    }

    /**
     * Handle left-click on Bedwars NPC villager
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // Check if damager is a player
        if (!(event.getDamager() instanceof Player)) return;

        Entity entity = event.getEntity();

        // Check if it's a villager with our tag
        if (entity.getType() != EntityType.VILLAGER) return;
        if (!entity.getScoreboardTags().contains(BwEggCommand.GAME_NPC_IDENTIFIER)) return;

        event.setCancelled(true);

        Player player = (Player) event.getDamager();
        teleportToGame(player);
    }

    /**
     * Teleport player to game world
     */
    private void teleportToGame(Player player) {
        World gameWorld = Bukkit.getWorld(GAME_WORLD_NAME);
        if (gameWorld == null) {
            player.sendMessage(ChatColor.RED + "Game world not found!");
            return;
        }

        Location spawnLoc = gameWorld.getSpawnLocation();
        player.teleport(spawnLoc);
        player.sendMessage(ChatColor.GREEN + "Teleported to Bedwars world!");
    }
}
