package myplg.myplg.map;

import myplg.myplg.PvPGame;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * マップ選択を管理するクラス
 * ランダム選択と管理者選択の両方をサポート
 */
public class MapSelector {
    private final PvPGame plugin;
    private final Random random = new Random();

    // 現在選択されているマップ
    private MapData selectedMap;

    // 最近プレイしたマップ（重複回避用）
    private final List<String> recentlyPlayed = new LinkedList<>();
    private static final int RECENT_MAP_HISTORY = 3;

    public MapSelector(PvPGame plugin) {
        this.plugin = plugin;
    }

    /**
     * ランダムにマップを選択
     * 最近プレイしたマップは除外
     */
    public MapData selectRandomMap() {
        List<MapData> availableMaps = plugin.getMapDataManager().getEnabledMaps();

        if (availableMaps.isEmpty()) {
            plugin.getLogger().warning("利用可能なマップがありません");
            return null;
        }

        // 最近プレイしたマップを除外
        List<MapData> candidates = new ArrayList<>();
        for (MapData map : availableMaps) {
            if (!recentlyPlayed.contains(map.getId())) {
                candidates.add(map);
            }
        }

        // 候補がなければ全マップから選択
        if (candidates.isEmpty()) {
            candidates = new ArrayList<>(availableMaps);
        }

        // ランダム選択
        MapData selected = candidates.get(random.nextInt(candidates.size()));
        setSelectedMap(selected);

        return selected;
    }

    /**
     * 管理者がマップを選択
     */
    public boolean selectMap(String mapId) {
        MapData map = plugin.getMapDataManager().getMap(mapId);
        if (map == null) {
            return false;
        }

        if (!map.isEnabled()) {
            return false;
        }

        if (!map.isSetupComplete()) {
            return false;
        }

        setSelectedMap(map);
        return true;
    }

    /**
     * 選択されたマップを設定
     */
    private void setSelectedMap(MapData map) {
        this.selectedMap = map;

        // 最近プレイしたマップに追加
        recentlyPlayed.remove(map.getId()); // 重複削除
        recentlyPlayed.add(0, map.getId());

        // 履歴を制限
        while (recentlyPlayed.size() > RECENT_MAP_HISTORY) {
            recentlyPlayed.remove(recentlyPlayed.size() - 1);
        }

        plugin.getLogger().info("マップ選択: " + map.getDisplayName() + " (" + map.getId() + ")");
        Bukkit.broadcastMessage("§e§l次のマップ: §f" + map.getDisplayName());
    }

    /**
     * 現在選択されているマップを取得
     */
    public MapData getSelectedMap() {
        return selectedMap;
    }

    /**
     * マップが選択されているか
     */
    public boolean hasSelectedMap() {
        return selectedMap != null;
    }

    /**
     * 選択をクリア
     */
    public void clearSelection() {
        this.selectedMap = null;
    }

    /**
     * プレイヤーを選択されたマップのロビースポーンにテレポート
     * @return テレポート成功した場合true
     */
    public boolean teleportToSelectedMap(Player player) {
        if (selectedMap == null) {
            player.sendMessage("§cマップが選択されていません。");
            return false;
        }

        Location lobbySpawn = selectedMap.getLobbySpawn();
        if (lobbySpawn == null) {
            player.sendMessage("§cマップの待機場所が設定されていません。");
            player.sendMessage("§7ゲームワールドで /setlobby を実行してください。");
            return false;
        }

        // ワールド名を取得（例：Airshow, Glacier）
        String worldName = selectedMap.getWorldFolderName();
        World correctWorld = Bukkit.getWorld(worldName);

        // ワールドがロードされていない場合はMapImporterを使用してロード
        if (correctWorld == null) {
            player.sendMessage("§eワールド「" + worldName + "」をロード中...");
            correctWorld = plugin.getMapImporter().loadMapWorld(worldName);

            if (correctWorld == null) {
                player.sendMessage("§cワールド「" + worldName + "」のロードに失敗しました。");
                player.sendMessage("§7BedWarsMap内にワールドが存在するか確認してください。");
                return false;
            }

            player.sendMessage("§aワールドのロードが完了しました！");
        }

        // 正しいワールドで新しいLocationを作成
        Location teleportLocation = new Location(
            correctWorld,
            lobbySpawn.getX(),
            lobbySpawn.getY(),
            lobbySpawn.getZ(),
            lobbySpawn.getYaw(),
            lobbySpawn.getPitch()
        );

        player.teleport(teleportLocation);
        player.sendMessage("§aマップ「" + selectedMap.getDisplayName() + "」に移動しました！");

        // ゲーム管理本を配布（OP権限がある場合）
        if (player.isOp()) {
            plugin.getGameManagerBook().giveBook(player);
        }

        return true;
    }

    /**
     * 全オンラインプレイヤーを選択されたマップにテレポート
     */
    public void teleportAllToSelectedMap() {
        if (selectedMap == null) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            teleportToSelectedMap(player);
        }
    }

    /**
     * 最近プレイしたマップリストをクリア
     */
    public void clearRecentHistory() {
        recentlyPlayed.clear();
    }

    /**
     * 利用可能なマップ数を取得
     */
    public int getAvailableMapCount() {
        return plugin.getMapDataManager().getEnabledMaps().size();
    }

    /**
     * 特定のチーム数のマップをフィルタリング
     */
    public List<MapData> getMapsByTeamCount(int teamCount) {
        List<MapData> result = new ArrayList<>();
        for (MapData map : plugin.getMapDataManager().getEnabledMaps()) {
            if (map.getTeamCount() == teamCount) {
                result.add(map);
            }
        }
        return result;
    }

    /**
     * 特定のチーム数でランダムマップを選択
     */
    public MapData selectRandomMapWithTeamCount(int teamCount) {
        List<MapData> maps = getMapsByTeamCount(teamCount);

        if (maps.isEmpty()) {
            plugin.getLogger().warning("チーム数 " + teamCount + " のマップがありません");
            return null;
        }

        // 最近プレイしたマップを除外
        List<MapData> candidates = new ArrayList<>();
        for (MapData map : maps) {
            if (!recentlyPlayed.contains(map.getId())) {
                candidates.add(map);
            }
        }

        if (candidates.isEmpty()) {
            candidates = maps;
        }

        MapData selected = candidates.get(random.nextInt(candidates.size()));
        setSelectedMap(selected);

        return selected;
    }

    /**
     * マップ情報を表示用に整形
     */
    public List<String> getMapListForDisplay() {
        List<String> lines = new ArrayList<>();
        Collection<MapData> allMaps = plugin.getMapDataManager().getAllMaps();

        if (allMaps.isEmpty()) {
            lines.add("§7マップがありません");
            return lines;
        }

        for (MapData map : allMaps) {
            StringBuilder sb = new StringBuilder();

            // ステータスアイコン
            if (!map.isEnabled()) {
                sb.append("§8[無効] ");
            } else if (!map.isSetupComplete()) {
                sb.append("§e[未完了] ");
            } else {
                sb.append("§a[有効] ");
            }

            // マップ情報
            sb.append("§f").append(map.getDisplayName());
            sb.append(" §7(").append(map.getId()).append(")");
            sb.append(" §7チーム: §e").append(map.getTeamCount());

            // 現在選択中
            if (selectedMap != null && selectedMap.getId().equals(map.getId())) {
                sb.append(" §6★選択中");
            }

            lines.add(sb.toString());
        }

        return lines;
    }
}
