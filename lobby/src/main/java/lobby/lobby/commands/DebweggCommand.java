package lobby.lobby.commands;

import lobby.lobby.Lobby;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;

import java.util.Collection;

/**
 * /debwegg command
 * Deletes the nearest Bedwars NPC villager
 * Requires admin permission (AdminDataManager)
 */
public class DebweggCommand implements CommandExecutor {

    private final Lobby plugin;
    private static final double SEARCH_RADIUS = 5.0;

    public DebweggCommand(Lobby plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        // Check if player is admin
        if (!plugin.getAdminDataManager().isAdmin(player)) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        // Find nearest Bedwars NPC
        Collection<Entity> nearbyEntities = player.getWorld().getNearbyEntities(
                player.getLocation(), SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS
        );

        Villager nearestNpc = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Entity entity : nearbyEntities) {
            if (entity.getType() != EntityType.VILLAGER) continue;
            if (!entity.getScoreboardTags().contains(BwEggCommand.GAME_NPC_IDENTIFIER)) continue;

            double distance = entity.getLocation().distance(player.getLocation());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestNpc = (Villager) entity;
            }
        }

        if (nearestNpc == null) {
            player.sendMessage(ChatColor.RED + "No Bedwars NPC found within " + (int) SEARCH_RADIUS + " blocks.");
            return true;
        }

        // Remove from data file
        plugin.getGameNpcDataManager().removeNpc(nearestNpc.getLocation());

        // Remove the villager
        nearestNpc.remove();

        player.sendMessage(ChatColor.GREEN + "Bedwars NPC deleted!");

        return true;
    }
}
