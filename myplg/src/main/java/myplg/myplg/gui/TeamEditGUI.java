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
import java.util.HashMap;
import java.util.Map;

/**
 * チーム編集用のメインメニューGUI（27スロット）
 */
public class TeamEditGUI {

    public static final String GUI_TITLE_PREFIX = "§e§lチーム編集: ";

    private final PvPGame plugin;

    // チーム名 → 羊毛マテリアル
    private static final Map<String, Material> TEAM_WOOL_MAP = new HashMap<>();

    static {
        TEAM_WOOL_MAP.put("レッド", Material.RED_WOOL);
        TEAM_WOOL_MAP.put("ブルー", Material.BLUE_WOOL);
        TEAM_WOOL_MAP.put("グリーン", Material.GREEN_WOOL);
        TEAM_WOOL_MAP.put("イエロー", Material.YELLOW_WOOL);
        TEAM_WOOL_MAP.put("アクア", Material.CYAN_WOOL);
        TEAM_WOOL_MAP.put("ホワイト", Material.WHITE_WOOL);
        TEAM_WOOL_MAP.put("ピンク", Material.PINK_WOOL);
        TEAM_WOOL_MAP.put("グレー", Material.GRAY_WOOL);
    }

    public TeamEditGUI(PvPGame plugin) {
        this.plugin = plugin;
    }

    /**
     * メニューGUIを開く
     */
    public void openMenu(Player player) {
        TeamEditToolManager.TeamEditSession session = plugin.getTeamEditToolManager().getSession(player);
        if (session == null) {
            player.sendMessage("§c編集セッションが見つかりません。");
            return;
        }

        String teamName = session.getTeamName();
        MapTeam team = session.getTeam();
        Material wool = TEAM_WOOL_MAP.getOrDefault(teamName, Material.WHITE_WOOL);

        Inventory inv = Bukkit.createInventory(null, 27, GUI_TITLE_PREFIX + teamName);

        // 装飾用ガラス
        ItemStack grayPane = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, grayPane);
        }

        // 上部の装飾
        Material glassColor = getTeamGlass(teamName);
        ItemStack colorPane = createItem(glassColor, " ");
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, colorPane);
        }

        // 中央にチーム情報
        ItemStack teamInfo = new ItemStack(wool);
        ItemMeta infoMeta = teamInfo.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName("§6§l" + teamName);
            String spawnStatus = team != null && team.getSpawnLocation() != null ?
                "§a設定済み §7(" + formatLocation(team.getSpawnLocation()) + ")" : "§c未設定";
            String bedStatus = team != null && team.getBedLocation() != null ?
                "§a設定済み §7(" + formatLocation(team.getBedLocation()) + ")" : "§c未設定";
            String territoryStatus = team != null && team.getTerritoryRadius() > 0 ?
                "§a設定済み §7(半径 " + team.getTerritoryRadius() + ")" : "§c未設定";
            String shopStatus = team != null && team.getShopLocation() != null ?
                "§a設定済み" : "§c未設定";
            String upgradeShopStatus = team != null && team.getUpgradeShopLocation() != null ?
                "§a設定済み" : "§c未設定";
            String ironGenStatus = team != null && team.getIronGeneratorLocation() != null ?
                "§a設定済み" : "§c未設定";
            String goldGenStatus = team != null && team.getGoldGeneratorLocation() != null ?
                "§a設定済み" : "§c未設定";

            infoMeta.setLore(Arrays.asList(
                "",
                "§7スポーン: " + spawnStatus,
                "§7ベッド: " + bedStatus,
                "§7領域: " + territoryStatus,
                "§7アイテムショップ: " + shopStatus,
                "§7アップグレードショップ: " + upgradeShopStatus,
                "§7アイアンジェネ: " + ironGenStatus,
                "§7ゴールドジェネ: " + goldGenStatus
            ));
            teamInfo.setItemMeta(infoMeta);
        }
        inv.setItem(4, teamInfo);

        // 設定ボタン（スロット 10, 11, 12, 14, 15）

        // スポーン位置設定（スロット 10）
        ItemStack spawnBtn = new ItemStack(Material.ENDER_PEARL);
        ItemMeta spawnMeta = spawnBtn.getItemMeta();
        if (spawnMeta != null) {
            spawnMeta.setDisplayName("§b§lスポーン位置設定");
            String status = team != null && team.getSpawnLocation() != null ?
                "§a現在: " + formatLocation(team.getSpawnLocation()) : "§c未設定";
            spawnMeta.setLore(Arrays.asList(
                status,
                "",
                "§7チームのリスポーン地点を設定します",
                "",
                "§e左クリック §7→ §f設定モードに入る"
            ));
            spawnBtn.setItemMeta(spawnMeta);
        }
        inv.setItem(10, spawnBtn);

        // ベッド位置設定（スロット 11）
        ItemStack bedBtn = new ItemStack(Material.RED_BED);
        ItemMeta bedMeta = bedBtn.getItemMeta();
        if (bedMeta != null) {
            bedMeta.setDisplayName("§d§lベッド位置設定");
            String status = team != null && team.getBedLocation() != null ?
                "§a現在: " + formatLocation(team.getBedLocation()) : "§c未設定";
            bedMeta.setLore(Arrays.asList(
                status,
                "",
                "§7ベッドの位置を設定します",
                "§7（実際にベッドを置かなくてもOK）",
                "",
                "§e左クリック §7→ §f設定モードに入る"
            ));
            bedBtn.setItemMeta(bedMeta);
        }
        inv.setItem(11, bedBtn);

        // 領域設定（スロット 12）
        ItemStack territoryBtn = new ItemStack(Material.BEACON);
        ItemMeta territoryMeta = territoryBtn.getItemMeta();
        if (territoryMeta != null) {
            territoryMeta.setDisplayName("§a§lチーム領域設定");
            int currentRadius = team != null ? team.getTerritoryRadius() : 0;
            String status = currentRadius > 0 ?
                "§a現在: 半径 " + currentRadius + " ブロック" : "§c未設定";
            territoryMeta.setLore(Arrays.asList(
                status,
                "",
                "§7チームのテリトリー範囲を設定します",
                "§7スポーン地点を中心とした半径で指定",
                "",
                "§e左クリック §7→ §f半径調整GUIを開く"
            ));
            territoryBtn.setItemMeta(territoryMeta);
        }
        inv.setItem(12, territoryBtn);

        // アイテムショップ位置（スロット 14）
        ItemStack shopBtn = new ItemStack(Material.EMERALD);
        ItemMeta shopMeta = shopBtn.getItemMeta();
        if (shopMeta != null) {
            shopMeta.setDisplayName("§2§lアイテムショップ位置");
            String status = team != null && team.getShopLocation() != null ?
                "§a現在: " + formatLocation(team.getShopLocation()) : "§c未設定";
            shopMeta.setLore(Arrays.asList(
                status,
                "",
                "§7アイテムショップの位置を設定します",
                "",
                "§e左クリック §7→ §f設定モードに入る"
            ));
            shopBtn.setItemMeta(shopMeta);
        }
        inv.setItem(14, shopBtn);

        // アップグレードショップ位置（スロット 15）
        ItemStack upgradeBtn = new ItemStack(Material.DIAMOND);
        ItemMeta upgradeMeta = upgradeBtn.getItemMeta();
        if (upgradeMeta != null) {
            upgradeMeta.setDisplayName("§b§lアップグレードショップ位置");
            String status = team != null && team.getUpgradeShopLocation() != null ?
                "§a現在: " + formatLocation(team.getUpgradeShopLocation()) : "§c未設定";
            upgradeMeta.setLore(Arrays.asList(
                status,
                "",
                "§7アップグレードショップの位置を設定します",
                "",
                "§e左クリック §7→ §f設定モードに入る"
            ));
            upgradeBtn.setItemMeta(upgradeMeta);
        }
        inv.setItem(15, upgradeBtn);

        // アイアンジェネレーター位置（スロット 19）
        ItemStack ironGenBtn = new ItemStack(Material.IRON_INGOT);
        ItemMeta ironGenMeta = ironGenBtn.getItemMeta();
        if (ironGenMeta != null) {
            ironGenMeta.setDisplayName("§f§lアイアンジェネレーター位置");
            String status = team != null && team.getIronGeneratorLocation() != null ?
                "§a現在: " + formatLocation(team.getIronGeneratorLocation()) : "§c未設定";
            ironGenMeta.setLore(Arrays.asList(
                status,
                "",
                "§7アイアンジェネレーターの位置を設定します",
                "",
                "§e左クリック §7→ §f設定モードに入る"
            ));
            ironGenBtn.setItemMeta(ironGenMeta);
        }
        inv.setItem(19, ironGenBtn);

        // ゴールドジェネレーター位置（スロット 20）
        ItemStack goldGenBtn = new ItemStack(Material.GOLD_INGOT);
        ItemMeta goldGenMeta = goldGenBtn.getItemMeta();
        if (goldGenMeta != null) {
            goldGenMeta.setDisplayName("§6§lゴールドジェネレーター位置");
            String status = team != null && team.getGoldGeneratorLocation() != null ?
                "§a現在: " + formatLocation(team.getGoldGeneratorLocation()) : "§c未設定";
            goldGenMeta.setLore(Arrays.asList(
                status,
                "",
                "§7ゴールドジェネレーターの位置を設定します",
                "",
                "§e左クリック §7→ §f設定モードに入る"
            ));
            goldGenBtn.setItemMeta(goldGenMeta);
        }
        inv.setItem(20, goldGenBtn);

        // 閉じるボタン（スロット 22）
        ItemStack closeBtn = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeBtn.getItemMeta();
        if (closeMeta != null) {
            closeMeta.setDisplayName("§c§l閉じる");
            closeMeta.setLore(Arrays.asList(
                "",
                "§7メニューを閉じます",
                "§7（編集モードは継続します）"
            ));
            closeBtn.setItemMeta(closeMeta);
        }
        inv.setItem(22, closeBtn);

        player.openInventory(inv);
    }

    /**
     * チーム名に対応したガラスを取得
     */
    private Material getTeamGlass(String teamName) {
        switch (teamName) {
            case "レッド": return Material.RED_STAINED_GLASS_PANE;
            case "ブルー": return Material.BLUE_STAINED_GLASS_PANE;
            case "グリーン": return Material.GREEN_STAINED_GLASS_PANE;
            case "イエロー": return Material.YELLOW_STAINED_GLASS_PANE;
            case "アクア": return Material.CYAN_STAINED_GLASS_PANE;
            case "ホワイト": return Material.WHITE_STAINED_GLASS_PANE;
            case "ピンク": return Material.PINK_STAINED_GLASS_PANE;
            case "グレー": return Material.GRAY_STAINED_GLASS_PANE;
            default: return Material.GRAY_STAINED_GLASS_PANE;
        }
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
