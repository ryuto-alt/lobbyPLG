package myplg.myplg.map;

import org.bukkit.Color;
import org.bukkit.Location;

/**
 * マップ内のチーム情報を保持するクラス
 */
public class MapTeam {
    private final String name;          // チーム名（レッド、ブルー等）
    private Location bedLocation;       // ベッド位置（頭の部分）
    private Location spawnLocation;     // スポーン位置（nullの場合はベッドから計算）
    private Color armorColor;           // 防具の色
    private Location shopLocation;      // アイテムショップ位置
    private Location upgradeShopLocation; // アップグレードショップ位置
    private int territoryRadius = 0;    // テリトリー半径（スポーンからの距離）
    private Location ironGeneratorLocation;  // アイアンジェネレーター位置
    private Location goldGeneratorLocation;  // ゴールドジェネレーター位置

    public MapTeam(String name) {
        this.name = name;
        this.armorColor = getDefaultColorForTeam(name);
    }

    public MapTeam(String name, Location bedLocation, Location spawnLocation) {
        this.name = name;
        this.bedLocation = bedLocation;
        this.spawnLocation = spawnLocation;
        this.armorColor = getDefaultColorForTeam(name);
    }

    public String getName() {
        return name;
    }

    public Location getBedLocation() {
        return bedLocation;
    }

    public void setBedLocation(Location bedLocation) {
        this.bedLocation = bedLocation;
    }

    public Location getSpawnLocation() {
        return spawnLocation;
    }

    public void setSpawnLocation(Location spawnLocation) {
        this.spawnLocation = spawnLocation;
    }

    public Color getArmorColor() {
        return armorColor;
    }

    public void setArmorColor(Color armorColor) {
        this.armorColor = armorColor;
    }

    public Location getShopLocation() {
        return shopLocation;
    }

    public void setShopLocation(Location shopLocation) {
        this.shopLocation = shopLocation;
    }

    public Location getUpgradeShopLocation() {
        return upgradeShopLocation;
    }

    public void setUpgradeShopLocation(Location upgradeShopLocation) {
        this.upgradeShopLocation = upgradeShopLocation;
    }

    public int getTerritoryRadius() {
        return territoryRadius;
    }

    public void setTerritoryRadius(int territoryRadius) {
        this.territoryRadius = territoryRadius;
    }

    public Location getIronGeneratorLocation() {
        return ironGeneratorLocation;
    }

    public void setIronGeneratorLocation(Location ironGeneratorLocation) {
        this.ironGeneratorLocation = ironGeneratorLocation;
    }

    public Location getGoldGeneratorLocation() {
        return goldGeneratorLocation;
    }

    public void setGoldGeneratorLocation(Location goldGeneratorLocation) {
        this.goldGeneratorLocation = goldGeneratorLocation;
    }

    /**
     * チーム名に対応するデフォルトカラーを取得
     */
    private static Color getDefaultColorForTeam(String teamName) {
        switch (teamName) {
            case "レッド": return Color.RED;
            case "ブルー": return Color.BLUE;
            case "グリーン": return Color.GREEN;
            case "イエロー": return Color.YELLOW;
            case "アクア": return Color.AQUA;
            case "ホワイト": return Color.WHITE;
            case "ピンク": return Color.FUCHSIA;
            case "グレー": return Color.GRAY;
            case "オレンジ": return Color.ORANGE;
            case "パープル": return Color.PURPLE;
            default: return Color.WHITE;
        }
    }

    /**
     * チーム名に対応するチャットカラーコードを取得
     */
    public String getChatColorCode() {
        switch (name) {
            case "レッド": return "§c";
            case "ブルー": return "§9";
            case "グリーン": return "§a";
            case "イエロー": return "§e";
            case "アクア": return "§b";
            case "ホワイト": return "§f";
            case "ピンク": return "§d";
            case "グレー": return "§7";
            case "オレンジ": return "§6";
            case "パープル": return "§5";
            default: return "§f";
        }
    }

    /**
     * セットアップが完了しているかチェック
     */
    public boolean isSetupComplete() {
        return bedLocation != null && spawnLocation != null;
    }

    @Override
    public String toString() {
        return "MapTeam{" +
                "name='" + name + '\'' +
                ", bedLocation=" + (bedLocation != null ? "set" : "null") +
                ", spawnLocation=" + (spawnLocation != null ? "set" : "null") +
                '}';
    }
}
