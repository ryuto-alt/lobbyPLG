package myplg.myplg.map;

import org.bukkit.Location;

import java.util.*;

/**
 * マップデータを保持するクラス
 * 各マップの情報（チーム、ジェネレーター、ショップ位置など）を管理
 */
public class MapData {
    private final String id;           // マップID（英語: rooftop, glacier等）
    private String displayName;         // 表示名（日本語: 屋上, 氷河等）
    private String worldFolderName;     // ワールドフォルダ名
    private Location lobbySpawn;        // ロビースポーン位置

    // チーム情報（チーム名 -> MapTeam）
    private final Map<String, MapTeam> teams = new LinkedHashMap<>();

    // ジェネレーター情報（ID -> MapGenerator）
    private final Map<String, MapGenerator> generators = new LinkedHashMap<>();

    // ショップ位置（チーム名 -> ショップ位置リスト）
    private final Map<String, List<Location>> shopLocations = new HashMap<>();

    // アップグレードショップ位置（チーム名 -> 位置）
    private final Map<String, Location> upgradeShopLocations = new HashMap<>();

    // マップの状態
    private boolean enabled = true;     // マップが有効かどうか
    private boolean setupComplete = false; // セットアップ完了フラグ

    // EndMode設定
    private double endModeBorderSize = 234.0;  // ENDモード開始時のボーダーサイズ
    private double endModeCenterX = 0.0;       // ボーダー中心X座標
    private double endModeCenterZ = 0.0;       // ボーダー中心Z座標
    private double endModeShrinkSpeed = 1.0;   // ボーダー縮小速度（ブロック/秒）
    private double endModeFinalSize = 10.0;    // 最終ボーダーサイズ

    public MapData(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
        this.worldFolderName = id; // デフォルトはIDと同じ
    }

    // ===== 基本情報 =====

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getWorldFolderName() {
        return worldFolderName;
    }

    public void setWorldFolderName(String worldFolderName) {
        this.worldFolderName = worldFolderName;
    }

    public Location getLobbySpawn() {
        return lobbySpawn;
    }

    public void setLobbySpawn(Location lobbySpawn) {
        this.lobbySpawn = lobbySpawn;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isSetupComplete() {
        return setupComplete;
    }

    public void setSetupComplete(boolean setupComplete) {
        this.setupComplete = setupComplete;
    }

    // ===== EndMode設定 =====

    public double getEndModeBorderSize() {
        return endModeBorderSize;
    }

    public void setEndModeBorderSize(double endModeBorderSize) {
        this.endModeBorderSize = endModeBorderSize;
    }

    public double getEndModeCenterX() {
        return endModeCenterX;
    }

    public void setEndModeCenterX(double endModeCenterX) {
        this.endModeCenterX = endModeCenterX;
    }

    public double getEndModeCenterZ() {
        return endModeCenterZ;
    }

    public void setEndModeCenterZ(double endModeCenterZ) {
        this.endModeCenterZ = endModeCenterZ;
    }

    public double getEndModeShrinkSpeed() {
        return endModeShrinkSpeed;
    }

    public void setEndModeShrinkSpeed(double endModeShrinkSpeed) {
        this.endModeShrinkSpeed = endModeShrinkSpeed;
    }

    public double getEndModeFinalSize() {
        return endModeFinalSize;
    }

    public void setEndModeFinalSize(double endModeFinalSize) {
        this.endModeFinalSize = endModeFinalSize;
    }

    /**
     * EndModeの設定を一括で設定
     */
    public void setEndModeSettings(double borderSize, double centerX, double centerZ, double shrinkSpeed, double finalSize) {
        this.endModeBorderSize = borderSize;
        this.endModeCenterX = centerX;
        this.endModeCenterZ = centerZ;
        this.endModeShrinkSpeed = shrinkSpeed;
        this.endModeFinalSize = finalSize;
    }

    // ===== チーム管理 =====

    public void addTeam(MapTeam team) {
        teams.put(team.getName(), team);
    }

    public void removeTeam(String teamName) {
        teams.remove(teamName);
    }

    public MapTeam getTeam(String teamName) {
        return teams.get(teamName);
    }

    public Map<String, MapTeam> getTeams() {
        return Collections.unmodifiableMap(teams);
    }

    public int getTeamCount() {
        return teams.size();
    }

    public boolean hasTeam(String teamName) {
        return teams.containsKey(teamName);
    }

    // ===== ジェネレーター管理 =====

    public void addGenerator(MapGenerator generator) {
        generators.put(generator.getId(), generator);
    }

    public void removeGenerator(String generatorId) {
        generators.remove(generatorId);
    }

    public MapGenerator getGenerator(String generatorId) {
        return generators.get(generatorId);
    }

    public Map<String, MapGenerator> getGenerators() {
        return Collections.unmodifiableMap(generators);
    }

    public List<MapGenerator> getGeneratorsByTeam(String teamName) {
        List<MapGenerator> result = new ArrayList<>();
        for (MapGenerator gen : generators.values()) {
            if (teamName.equals(gen.getTeamName())) {
                result.add(gen);
            }
        }
        return result;
    }

    public List<MapGenerator> getCommonGenerators() {
        return getGeneratorsByTeam("共通");
    }

    // ===== ショップ管理 =====

    public void addShopLocation(String teamName, Location location) {
        shopLocations.computeIfAbsent(teamName, k -> new ArrayList<>()).add(location);
    }

    public void clearShopLocations(String teamName) {
        shopLocations.remove(teamName);
    }

    public List<Location> getShopLocations(String teamName) {
        return shopLocations.getOrDefault(teamName, Collections.emptyList());
    }

    public Map<String, List<Location>> getAllShopLocations() {
        return Collections.unmodifiableMap(shopLocations);
    }

    public void setUpgradeShopLocation(String teamName, Location location) {
        upgradeShopLocations.put(teamName, location);
    }

    public Location getUpgradeShopLocation(String teamName) {
        return upgradeShopLocations.get(teamName);
    }

    public Map<String, Location> getAllUpgradeShopLocations() {
        return Collections.unmodifiableMap(upgradeShopLocations);
    }

    // ===== セットアップ検証 =====

    /**
     * マップのセットアップが完了しているか検証
     * @return 不足している項目のリスト（空ならセットアップ完了）
     */
    public List<String> validateSetup() {
        List<String> missingItems = new ArrayList<>();

        // チームが最低1つ必要
        if (teams.isEmpty()) {
            missingItems.add("チームが設定されていません");
        } else {
            // 各チームの検証
            for (MapTeam team : teams.values()) {
                if (team.getBedLocation() == null) {
                    missingItems.add("チーム「" + team.getName() + "」のベッド位置が未設定");
                }
                if (team.getSpawnLocation() == null) {
                    missingItems.add("チーム「" + team.getName() + "」のスポーン位置が未設定");
                }
            }
        }

        // ジェネレーターが最低1つ必要
        if (generators.isEmpty()) {
            missingItems.add("ジェネレーターが設定されていません");
        }

        // ロビースポーンは任意

        return missingItems;
    }

    /**
     * セットアップ進捗を取得（パーセント）
     */
    public int getSetupProgress() {
        int total = 0;
        int completed = 0;

        // チーム（各チームでベッド+スポーン=2項目）
        if (!teams.isEmpty()) {
            for (MapTeam team : teams.values()) {
                total += 2;
                if (team.getBedLocation() != null) completed++;
                if (team.getSpawnLocation() != null) completed++;
            }
        } else {
            total += 2; // 最低1チーム必要
        }

        // ジェネレーター
        total += 1;
        if (!generators.isEmpty()) completed++;

        // ショップ（任意だが進捗に含める）
        if (!teams.isEmpty()) {
            total += teams.size();
            for (String teamName : teams.keySet()) {
                if (shopLocations.containsKey(teamName) && !shopLocations.get(teamName).isEmpty()) {
                    completed++;
                }
            }
        }

        return total > 0 ? (completed * 100 / total) : 0;
    }

    @Override
    public String toString() {
        return "MapData{" +
                "id='" + id + '\'' +
                ", displayName='" + displayName + '\'' +
                ", teams=" + teams.size() +
                ", generators=" + generators.size() +
                ", enabled=" + enabled +
                ", setupComplete=" + setupComplete +
                '}';
    }
}
