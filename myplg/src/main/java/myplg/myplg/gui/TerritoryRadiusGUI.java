package myplg.myplg.gui;

import myplg.myplg.PvPGame;
import myplg.myplg.map.MapTeam;
import myplg.myplg.map.TeamEditToolManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/**
 * チーム領域の半径調整GUI
 * +/- ボタンで半径を増減し、パーティクルで可視化
 */
public class TerritoryRadiusGUI {

    public static final String GUI_TITLE = "§a§l領域半径設定";

    private final PvPGame plugin;

    public TerritoryRadiusGUI(PvPGame plugin) {
        this.plugin = plugin;
    }

    /**
     * 半径調整GUIを開く
     */
    public void openRadiusGUI(Player player) {
        TeamEditToolManager manager = plugin.getTeamEditToolManager();
        TeamEditToolManager.TeamEditSession session = manager.getSession(player);
        if (session == null) {
            player.sendMessage("§c編集セッションが見つかりません。");
            return;
        }

        MapTeam team = session.getTeam();
        int currentRadius = session.getTerritoryRadius();
        if (team != null && team.getTerritoryRadius() > 0) {
            currentRadius = team.getTerritoryRadius();
            session.setTerritoryRadius(currentRadius);
        }

        Inventory inv = Bukkit.createInventory(null, 27, GUI_TITLE);

        // 装飾用ガラス
        ItemStack grayPane = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, grayPane);
        }

        // 上部の説明
        ItemStack infoItem = new ItemStack(Material.BEACON);
        ItemMeta infoMeta = infoItem.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName("§a§l領域半径設定");
            infoMeta.setLore(Arrays.asList(
                "",
                "§7スポーン地点を中心とした正方形の範囲を設定します",
                "§7パーティクルで境界線が表示されます",
                "",
                "§e先にスポーン位置を設定してください"
            ));
            infoItem.setItemMeta(infoMeta);
        }
        inv.setItem(4, infoItem);

        // -5 ボタン（スロット 10）
        ItemStack minus5 = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta minus5Meta = minus5.getItemMeta();
        if (minus5Meta != null) {
            minus5Meta.setDisplayName("§c§l-5");
            minus5Meta.setLore(Arrays.asList("§7半径を5ブロック減らす"));
            minus5.setItemMeta(minus5Meta);
        }
        inv.setItem(10, minus5);

        // -1 ボタン（スロット 11）
        ItemStack minus1 = new ItemStack(Material.PINK_STAINED_GLASS_PANE);
        ItemMeta minus1Meta = minus1.getItemMeta();
        if (minus1Meta != null) {
            minus1Meta.setDisplayName("§c§l-1");
            minus1Meta.setLore(Arrays.asList("§7半径を1ブロック減らす"));
            minus1.setItemMeta(minus1Meta);
        }
        inv.setItem(11, minus1);

        // 現在値表示（スロット 13）
        ItemStack currentValue = new ItemStack(Material.PAPER);
        ItemMeta currentMeta = currentValue.getItemMeta();
        if (currentMeta != null) {
            currentMeta.setDisplayName("§e§l現在の半径: §f" + currentRadius);
            Location spawnLoc = team != null ? team.getSpawnLocation() : null;
            if (spawnLoc != null) {
                currentMeta.setLore(Arrays.asList(
                    "",
                    "§7中心: §f" + formatLocation(spawnLoc),
                    "§7範囲: §f" + (currentRadius * 2) + " x " + (currentRadius * 2) + " ブロック"
                ));
            } else {
                currentMeta.setLore(Arrays.asList(
                    "",
                    "§c※ スポーン位置が未設定です",
                    "§c先にスポーン位置を設定してください"
                ));
            }
            currentValue.setItemMeta(currentMeta);
        }
        inv.setItem(13, currentValue);

        // +1 ボタン（スロット 15）
        ItemStack plus1 = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta plus1Meta = plus1.getItemMeta();
        if (plus1Meta != null) {
            plus1Meta.setDisplayName("§a§l+1");
            plus1Meta.setLore(Arrays.asList("§7半径を1ブロック増やす"));
            plus1.setItemMeta(plus1Meta);
        }
        inv.setItem(15, plus1);

        // +5 ボタン（スロット 16）
        ItemStack plus5 = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta plus5Meta = plus5.getItemMeta();
        if (plus5Meta != null) {
            plus5Meta.setDisplayName("§a§l+5");
            plus5Meta.setLore(Arrays.asList("§7半径を5ブロック増やす"));
            plus5.setItemMeta(plus5Meta);
        }
        inv.setItem(16, plus5);

        // 確定ボタン（スロット 22）
        ItemStack confirmBtn = new ItemStack(Material.LIME_WOOL);
        ItemMeta confirmMeta = confirmBtn.getItemMeta();
        if (confirmMeta != null) {
            confirmMeta.setDisplayName("§a§l確定");
            confirmMeta.setLore(Arrays.asList(
                "",
                "§7この半径で領域を設定します",
                "",
                "§a▶ クリックして確定"
            ));
            confirmBtn.setItemMeta(confirmMeta);
        }
        inv.setItem(21, confirmBtn);

        // 戻るボタン（スロット 23）
        ItemStack backBtn = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = backBtn.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§c§l戻る");
            backMeta.setLore(Arrays.asList(
                "",
                "§7メニューに戻ります"
            ));
            backBtn.setItemMeta(backMeta);
        }
        inv.setItem(23, backBtn);

        player.openInventory(inv);

        // パーティクル表示を開始（スポーン位置があれば）
        if (team != null && team.getSpawnLocation() != null) {
            manager.startParticleTask(player, team.getSpawnLocation(), currentRadius);
        }
    }

    /**
     * 半径を変更してGUIを更新
     */
    public void updateRadius(Player player, int delta) {
        TeamEditToolManager manager = plugin.getTeamEditToolManager();
        TeamEditToolManager.TeamEditSession session = manager.getSession(player);
        if (session == null) return;

        int newRadius = session.getTerritoryRadius() + delta;
        if (newRadius < 1) newRadius = 1;
        if (newRadius > 100) newRadius = 100;

        session.setTerritoryRadius(newRadius);

        // パーティクル表示を更新
        MapTeam team = session.getTeam();
        if (team != null && team.getSpawnLocation() != null) {
            manager.startParticleTask(player, team.getSpawnLocation(), newRadius);
        }

        // GUIを更新
        openRadiusGUI(player);
    }

    /**
     * 半径を確定して保存
     */
    public void confirmRadius(Player player) {
        TeamEditToolManager manager = plugin.getTeamEditToolManager();
        TeamEditToolManager.TeamEditSession session = manager.getSession(player);
        if (session == null) return;

        MapTeam team = session.getTeam();
        if (team == null) return;

        if (team.getSpawnLocation() == null) {
            player.sendMessage("§c先にスポーン位置を設定してください。");
            return;
        }

        int radius = session.getTerritoryRadius();
        team.setTerritoryRadius(radius);

        // パーティクル表示を停止
        manager.stopParticleTask(player);

        player.closeInventory();
        player.sendMessage("§a§l[チーム編集] §fチーム領域を設定しました（半径: " + radius + " ブロック）");
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

    /**
     * 座標をフォーマット
     */
    private String formatLocation(Location loc) {
        if (loc == null) return "なし";
        return String.format("%.0f, %.0f, %.0f", loc.getX(), loc.getY(), loc.getZ());
    }
}
