package lobby.lobby.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class GameMenuGUI {

    public static void openGameMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, "§6§lGame Menu");

        // Create Bedwars item
        ItemStack bedwars = new ItemStack(Material.RED_BED);
        ItemMeta bedwarsMeta = bedwars.getItemMeta();
        if (bedwarsMeta != null) {
            bedwarsMeta.setDisplayName("§c§lBedWars");
            bedwarsMeta.setLore(Arrays.asList(
                    "§7Protect your bed and destroy",
                    "§7the enemy beds!",
                    "",
                    "§eClick to join BedWars"
            ));
            bedwars.setItemMeta(bedwarsMeta);
        }

        // Place Bedwars item in center slot (slot 13)
        gui.setItem(13, bedwars);

        // Fill empty slots with glass pane
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
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
