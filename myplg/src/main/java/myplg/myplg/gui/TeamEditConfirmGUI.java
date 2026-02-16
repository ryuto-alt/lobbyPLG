package myplg.myplg.gui;

import myplg.myplg.PvPGame;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/**
 * チーム編集終了確認GUI
 */
public class TeamEditConfirmGUI {

    public static final String GUI_TITLE = "§c§l編集を終了しますか？";

    private final PvPGame plugin;

    public TeamEditConfirmGUI(PvPGame plugin) {
        this.plugin = plugin;
    }

    /**
     * 確認GUIを開く
     */
    public void openConfirmGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, GUI_TITLE);

        // 装飾用ガラス
        ItemStack grayPane = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, grayPane);
        }

        // 上部にメッセージ
        ItemStack infoItem = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = infoItem.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName("§e§l編集を終了しますか？");
            infoMeta.setLore(Arrays.asList(
                "",
                "§7「はい」を押すと設定を保存して終了します",
                "§7「いいえ」を押すとメニューに戻ります"
            ));
            infoItem.setItemMeta(infoMeta);
        }
        inv.setItem(4, infoItem);

        // 「はい」ボタン（スロット 11 - 緑の羊毛）
        ItemStack yesBtn = new ItemStack(Material.LIME_WOOL);
        ItemMeta yesMeta = yesBtn.getItemMeta();
        if (yesMeta != null) {
            yesMeta.setDisplayName("§a§lはい");
            yesMeta.setLore(Arrays.asList(
                "",
                "§7設定を保存して編集モードを終了します",
                "",
                "§a▶ クリックして確定"
            ));
            yesBtn.setItemMeta(yesMeta);
        }
        inv.setItem(11, yesBtn);

        // 「いいえ」ボタン（スロット 15 - 赤の羊毛）
        ItemStack noBtn = new ItemStack(Material.RED_WOOL);
        ItemMeta noMeta = noBtn.getItemMeta();
        if (noMeta != null) {
            noMeta.setDisplayName("§c§lいいえ");
            noMeta.setLore(Arrays.asList(
                "",
                "§7編集を続行します",
                "",
                "§c▶ クリックして戻る"
            ));
            noBtn.setItemMeta(noMeta);
        }
        inv.setItem(15, noBtn);

        player.openInventory(inv);
    }

    /**
     * アイテムを作成
     */
    private ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }
}
