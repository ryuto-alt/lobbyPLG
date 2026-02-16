package myplg.myplg.map;

import org.bukkit.Location;
import org.bukkit.Material;

/**
 * マップ内のジェネレーター情報を保持するクラス
 */
public class MapGenerator {
    private final String id;            // ユニークID
    private String teamName;            // 所属チーム名（"共通"は全員用）
    private Material material;          // 生成するアイテム
    private Location corner1;           // 範囲の角1
    private Location corner2;           // 範囲の角2
    private int spawnInterval;          // 生成間隔（tick）

    public MapGenerator(String id, String teamName, Material material) {
        this.id = id;
        this.teamName = teamName;
        this.material = material;
        this.spawnInterval = getDefaultIntervalForMaterial(material);
    }

    public String getId() {
        return id;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
        // デフォルト間隔を設定
        this.spawnInterval = getDefaultIntervalForMaterial(material);
    }

    public Location getCorner1() {
        return corner1;
    }

    public void setCorner1(Location corner1) {
        this.corner1 = corner1;
    }

    public Location getCorner2() {
        return corner2;
    }

    public void setCorner2(Location corner2) {
        this.corner2 = corner2;
    }

    public int getSpawnInterval() {
        return spawnInterval;
    }

    public void setSpawnInterval(int spawnInterval) {
        this.spawnInterval = spawnInterval;
    }

    /**
     * 素材に応じたデフォルト生成間隔を取得
     */
    private static int getDefaultIntervalForMaterial(Material material) {
        switch (material) {
            case IRON_INGOT:
                return 20;      // 1秒
            case GOLD_INGOT:
                return 300;     // 15秒
            case DIAMOND:
                return 700;     // 35秒
            case EMERALD:
                return 3600;    // 3分
            default:
                return 100;     // 5秒
        }
    }

    /**
     * 素材名を日本語で取得
     */
    public String getMaterialDisplayName() {
        switch (material) {
            case IRON_INGOT: return "鉄インゴット";
            case GOLD_INGOT: return "金インゴット";
            case DIAMOND: return "ダイヤモンド";
            case EMERALD: return "エメラルド";
            default: return material.name();
        }
    }

    /**
     * ジェネレータータイプを文字列で取得（保存用）
     */
    public String getTypeString() {
        switch (material) {
            case IRON_INGOT: return "iron";
            case GOLD_INGOT: return "gold";
            case DIAMOND: return "diamond";
            case EMERALD: return "emerald";
            default: return material.name().toLowerCase();
        }
    }

    /**
     * 文字列からMaterialを取得
     */
    public static Material getMaterialFromType(String type) {
        switch (type.toLowerCase()) {
            case "iron": return Material.IRON_INGOT;
            case "gold": return Material.GOLD_INGOT;
            case "diamond": return Material.DIAMOND;
            case "emerald": return Material.EMERALD;
            default: return Material.valueOf(type.toUpperCase());
        }
    }

    /**
     * セットアップが完了しているかチェック
     */
    public boolean isSetupComplete() {
        return corner1 != null && corner2 != null;
    }

    /**
     * IDを生成するユーティリティメソッド
     */
    public static String generateId(String teamName, Material material) {
        return teamName + "_" + material.name().toLowerCase() + "_" + System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "MapGenerator{" +
                "id='" + id + '\'' +
                ", teamName='" + teamName + '\'' +
                ", material=" + material +
                ", interval=" + spawnInterval +
                '}';
    }
}
