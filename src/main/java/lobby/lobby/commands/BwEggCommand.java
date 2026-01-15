package lobby.lobby.commands;

import lobby.lobby.Lobby;
import lobby.lobby.PermissionUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/**
 * /bwegg command
 * Gives admin a villager spawn egg for game teleport NPC
 * Requires OP level 4+
 */
public class BwEggCommand implements CommandExecutor {

    private final Lobby plugin;

    // Custom name for the game NPC villager
    public static final String GAME_NPC_NAME = ChatColor.GOLD + "" + ChatColor.BOLD + "Bedwars";
    public static final String GAME_NPC_IDENTIFIER = "BedwarsGameNPC";

    public BwEggCommand(Lobby plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Permission check - must be a player with OP level 4
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;
        if (!PermissionUtil.isOpLevel4(player)) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        // Create villager spawn egg with custom name
        ItemStack egg = new ItemStack(Material.VILLAGER_SPAWN_EGG);
        ItemMeta meta = egg.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(GAME_NPC_NAME);
            meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Place to spawn a Bedwars NPC",
                ChatColor.GRAY + "Players can click to join game",
                ChatColor.DARK_GRAY + GAME_NPC_IDENTIFIER
            ));
            egg.setItemMeta(meta);
        }

        // Give egg to player
        player.getInventory().addItem(egg);
        player.sendMessage(ChatColor.GREEN + "Bedwars NPC egg received!");

        return true;
    }
}
