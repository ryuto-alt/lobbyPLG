package myplg.myplg.map;

import myplg.myplg.PvPGame;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * チーム編集ツールの管理クラス
 * /map edit <チーム名> で編集モードを開始し、チームカラーの羊毛と終了用バリアブロックを配布する
 */
public class TeamEditToolManager {

    private final PvPGame plugin;
    private final Map<UUID, TeamEditSession> editSessions = new HashMap<>();
    private final Map<UUID, BukkitTask> particleTasks = new HashMap<>();

    // 英語 → 日本語のチーム名変換
    private static final Map<String, String> TEAM_NAME_EN_TO_JP = new HashMap<>();
    // 日本語 → 英語のチーム名変換
    private static final Map<String, String> TEAM_NAME_JP_TO_EN = new HashMap<>();
    // チーム名 → 羊毛マテリアル
    private static final Map<String, Material> TEAM_WOOL_MAP = new HashMap<>();

    static {
        // 8チーム対応
        TEAM_NAME_EN_TO_JP.put("red", "レッド");
        TEAM_NAME_EN_TO_JP.put("blue", "ブルー");
        TEAM_NAME_EN_TO_JP.put("green", "グリーン");
        TEAM_NAME_EN_TO_JP.put("yellow", "イエロー");
        TEAM_NAME_EN_TO_JP.put("cyan", "アクア");
        TEAM_NAME_EN_TO_JP.put("white", "ホワイト");
        TEAM_NAME_EN_TO_JP.put("pink", "ピンク");
        TEAM_NAME_EN_TO_JP.put("gray", "グレー");

        // 逆方向
        for (Map.Entry<String, String> entry : TEAM_NAME_EN_TO_JP.entrySet()) {
            TEAM_NAME_JP_TO_EN.put(entry.getValue(), entry.getKey());
        }

        // 羊毛マテリアル
        TEAM_WOOL_MAP.put("レッド", Material.RED_WOOL);
        TEAM_WOOL_MAP.put("ブルー", Material.BLUE_WOOL);
        TEAM_WOOL_MAP.put("グリーン", Material.GREEN_WOOL);
        TEAM_WOOL_MAP.put("イエロー", Material.YELLOW_WOOL);
        TEAM_WOOL_MAP.put("アクア", Material.CYAN_WOOL);
        TEAM_WOOL_MAP.put("ホワイト", Material.WHITE_WOOL);
        TEAM_WOOL_MAP.put("ピンク", Material.PINK_WOOL);
        TEAM_WOOL_MAP.put("グレー", Material.GRAY_WOOL);
    }

    public TeamEditToolManager(PvPGame plugin) {
        this.plugin = plugin;
    }

    /**
     * チーム名を正規化（英語→日本語に変換、日本語はそのまま）
     */
    public String normalizeTeamName(String input) {
        String lower = input.toLowerCase();
        if (TEAM_NAME_EN_TO_JP.containsKey(lower)) {
            return TEAM_NAME_EN_TO_JP.get(lower);
        }
        // 日本語チーム名の場合はそのまま返す
        if (TEAM_WOOL_MAP.containsKey(input)) {
            return input;
        }
        return null;
    }

    /**
     * 有効なチーム名かどうか
     */
    public boolean isValidTeamName(String input) {
        return normalizeTeamName(input) != null;
    }

    /**
     * 利用可能なチーム名一覧（英語）
     */
    public List<String> getAvailableTeamNames() {
        return new ArrayList<>(TEAM_NAME_EN_TO_JP.keySet());
    }

    /**
     * 編集セッションを開始
     */
    public boolean startEditSession(Player player, String teamNameInput) {
        String teamName = normalizeTeamName(teamNameInput);
        if (teamName == null) {
            player.sendMessage("§c無効なチーム名です。利用可能: " + String.join(", ", getAvailableTeamNames()));
            return false;
        }

        // 現在のワールドからマップを特定
        String worldName = player.getWorld().getName();
        MapData mapData = plugin.getMapDataManager().getMapByWorldName(worldName);
        if (mapData == null) {
            player.sendMessage("§c現在のワールド「" + worldName + "」に対応するマップが見つかりません。");
            player.sendMessage("§7先に /map create または /map import でマップを作成してください。");
            return false;
        }

        // 既存のセッションがあれば終了
        if (editSessions.containsKey(player.getUniqueId())) {
            endEditSession(player, false);
        }

        // チームがなければ作成
        if (!mapData.hasTeam(teamName)) {
            mapData.addTeam(new MapTeam(teamName));
        }

        // セッション作成
        TeamEditSession session = new TeamEditSession(mapData, teamName);
        editSessions.put(player.getUniqueId(), session);

        // ツールを配布
        giveEditTools(player, teamName);

        // ウェルカムメッセージ
        player.sendMessage("");
        player.sendMessage("§e§l[チーム編集] §fチーム「§6" + teamName + "§f」の編集モードを開始しました");
        player.sendMessage("§7マップ: §f" + mapData.getId());
        player.sendMessage("");
        player.sendMessage("§a▶ §f羊毛を右クリック §7→ §f設定メニューを開く");
        player.sendMessage("§c▶ §fバリアブロックを右クリック §7→ §f編集を終了");
        player.sendMessage("");

        return true;
    }

    /**
     * エディットツールと終了ツールを配布
     */
    private void giveEditTools(Player player, String teamName) {
        Material woolMaterial = TEAM_WOOL_MAP.getOrDefault(teamName, Material.WHITE_WOOL);

        // エディットツール（羊毛）
        ItemStack editTool = new ItemStack(woolMaterial);
        ItemMeta editMeta = editTool.getItemMeta();
        if (editMeta != null) {
            editMeta.setDisplayName("§e§l" + teamName + " §f§lエディットツール");
            editMeta.setLore(Arrays.asList(
                "§7チーム: §f" + teamName,
                "",
                "§a右クリック §7→ §f設定メニューを開く"
            ));
            editTool.setItemMeta(editMeta);
        }

        // 終了ツール（バリアブロック）
        ItemStack exitTool = new ItemStack(Material.BARRIER);
        ItemMeta exitMeta = exitTool.getItemMeta();
        if (exitMeta != null) {
            exitMeta.setDisplayName("§c§l編集終了");
            exitMeta.setLore(Arrays.asList(
                "§7チーム: §f" + teamName,
                "",
                "§c右クリック §7→ §f編集を終了"
            ));
            exitTool.setItemMeta(exitMeta);
        }

        // インベントリに追加
        player.getInventory().setItem(0, editTool);
        player.getInventory().setItem(8, exitTool);
    }

    /**
     * 編集セッションを終了
     */
    public void endEditSession(Player player, boolean save) {
        UUID uuid = player.getUniqueId();
        TeamEditSession session = editSessions.remove(uuid);

        // パーティクルタスクを停止
        stopParticleTask(player);

        if (session == null) {
            return;
        }

        if (save) {
            // マップデータを保存
            plugin.getMapDataManager().saveMap(session.getMapData());
            player.sendMessage("§a§l[チーム編集] §f設定を保存して編集モードを終了しました");
        } else {
            player.sendMessage("§c§l[チーム編集] §f編集モードを終了しました（保存されていません）");
        }

        // ツールを削除
        removeEditTools(player);
    }

    /**
     * ツールをインベントリから削除
     */
    private void removeEditTools(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && isEditTool(item)) {
                player.getInventory().setItem(i, null);
            }
        }
    }

    /**
     * アイテムがエディットツールかどうか
     */
    public boolean isEditTool(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;
        String name = meta.getDisplayName();
        return name.contains("エディットツール");
    }

    /**
     * アイテムが終了ツールかどうか
     */
    public boolean isExitTool(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;
        return meta.getDisplayName().equals("§c§l編集終了");
    }

    /**
     * プレイヤーが編集中かどうか
     */
    public boolean isEditing(Player player) {
        return editSessions.containsKey(player.getUniqueId());
    }

    /**
     * 編集セッションを取得
     */
    public TeamEditSession getSession(Player player) {
        return editSessions.get(player.getUniqueId());
    }

    /**
     * チーム領域のパーティクル表示を開始
     */
    public void startParticleTask(Player player, Location center, int radius) {
        stopParticleTask(player);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            showTerritoryBoundary(player, center, radius);
        }, 0L, 10L); // 0.5秒ごと

        particleTasks.put(player.getUniqueId(), task);
    }

    /**
     * パーティクル表示を停止
     */
    public void stopParticleTask(Player player) {
        BukkitTask task = particleTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    /**
     * 領域境界をパーティクルで表示
     */
    private void showTerritoryBoundary(Player player, Location center, int radius) {
        if (!player.isOnline()) {
            stopParticleTask(player);
            return;
        }

        double y = center.getY() + 0.5;
        double step = 0.5;

        // 四角形の境界線を描画
        for (double x = -radius; x <= radius; x += step) {
            // 上辺と下辺
            player.spawnParticle(Particle.FLAME,
                center.getX() + x, y, center.getZ() - radius, 1, 0, 0, 0, 0);
            player.spawnParticle(Particle.FLAME,
                center.getX() + x, y, center.getZ() + radius, 1, 0, 0, 0, 0);
        }

        for (double z = -radius; z <= radius; z += step) {
            // 左辺と右辺
            player.spawnParticle(Particle.FLAME,
                center.getX() - radius, y, center.getZ() + z, 1, 0, 0, 0, 0);
            player.spawnParticle(Particle.FLAME,
                center.getX() + radius, y, center.getZ() + z, 1, 0, 0, 0, 0);
        }
    }

    /**
     * 編集セッションクラス
     */
    public static class TeamEditSession {
        private final MapData mapData;
        private final String teamName;
        private EditMode editMode;
        private int territoryRadius = 10;

        public TeamEditSession(MapData mapData, String teamName) {
            this.mapData = mapData;
            this.teamName = teamName;
            this.editMode = EditMode.NONE;
        }

        public MapData getMapData() { return mapData; }
        public String getTeamName() { return teamName; }
        public EditMode getEditMode() { return editMode; }
        public void setEditMode(EditMode mode) { this.editMode = mode; }
        public int getTerritoryRadius() { return territoryRadius; }
        public void setTerritoryRadius(int radius) { this.territoryRadius = radius; }

        public MapTeam getTeam() {
            return mapData.getTeam(teamName);
        }
    }

    /**
     * 編集モード
     */
    public enum EditMode {
        NONE,
        SPAWN,              // スポーン位置設定
        BED,                // ベッド位置設定
        TERRITORY,          // 領域設定（スポーンからの半径）
        ITEM_SHOP,          // アイテムショップ位置設定
        UPGRADE_SHOP,       // アップグレードショップ位置設定
        IRON_GENERATOR,     // アイアンジェネレーター位置設定
        GOLD_GENERATOR      // ゴールドジェネレーター位置設定
    }
}
