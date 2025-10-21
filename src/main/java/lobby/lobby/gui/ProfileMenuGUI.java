package lobby.lobby.gui;

import lobby.lobby.Lobby;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class ProfileMenuGUI {

    public static void openProfileMenu(Player player, Lobby plugin) {
        Inventory gui = Bukkit.createInventory(null, 27, "§b§lYour Profile");

        // Get player's playtime
        long bedwarsTime = plugin.getPlayTimeManager().getBedwarsPlayTime(player.getUniqueId());
        String formattedTime = plugin.getPlayTimeManager().formatPlayTime(bedwarsTime);

        // Create Bedwars stats item
        ItemStack bedwarsStats = new ItemStack(Material.RED_BED);
        ItemMeta bedwarsMeta = bedwarsStats.getItemMeta();
        if (bedwarsMeta != null) {
            bedwarsMeta.setDisplayName("§c§lBedWars Stats");
            bedwarsMeta.setLore(Arrays.asList(
                    "§7Playtime: §e" + formattedTime,
                    "",
                    "§7Keep playing to increase",
                    "§7your playtime!"
            ));
            bedwarsStats.setItemMeta(bedwarsMeta);
        }

        // Place Bedwars stats in center slot (slot 13)
        gui.setItem(13, bedwarsStats);

        // Create player info item
        ItemStack playerInfo = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta infoMeta = playerInfo.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName("§e§l" + player.getName());
            infoMeta.setLore(Arrays.asList(
                    "§7UUID: §f" + player.getUniqueId().toString().substring(0, 8) + "...",
                    "",
                    "§7Total Games Played: §e1",
                    "§7(BedWars)"
            ));

            // Set player head texture
            if (infoMeta instanceof org.bukkit.inventory.meta.SkullMeta) {
                ((org.bukkit.inventory.meta.SkullMeta) infoMeta).setOwningPlayer(player);
            }

            playerInfo.setItemMeta(infoMeta);
        }

        // Place player info in slot 11
        gui.setItem(11, playerInfo);

        // Fill empty slots with glass pane
        ItemStack filler = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(" ");
            filler.setItemMeta(fillerMeta);
        }

        for (int i = 0; i < 27; i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, filler);
            }
        }

        player.openInventory(gui);
    }
}
