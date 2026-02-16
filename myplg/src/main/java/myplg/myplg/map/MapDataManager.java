package myplg.myplg.map;

import myplg.myplg.PvPGame;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * マップデータをYAMLファイルに保存・読み込みするマネージャー
 * 各マップは個別のYAMLファイル（maps/<map_id>.yml）として保存
 */
public class MapDataManager {
    private final PvPGame plugin;
    private final File mapsFolder;
    private final Map<String, MapData> loadedMaps = new LinkedHashMap<>();

    public MapDataManager(PvPGame plugin) {
        this.plugin = plugin;
        this.mapsFolder = new File(plugin.getDataFolder(), "maps");
        if (!mapsFolder.exists()) {
            mapsFolder.mkdirs();
        }
    }

    /**
     * すべてのマップを読み込み
     */
    public void loadAllMaps() {
        loadedMaps.clear();

        File[] files = mapsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            plugin.getLogger().info("マップデータが見つかりません");
            return;
        }

        for (File file : files) {
            String mapId = file.getName().replace(".yml", "");
            try {
                MapData mapData = loadMap(mapId);
                if (mapData != null) {
                    loadedMaps.put(mapId, mapData);
                    plugin.getLogger().info("マップ読み込み完了: " + mapId + " (" + mapData.getDisplayName() + ")");
                }
            } catch (Exception e) {
                plugin.getLogger().warning("マップ読み込み失敗: " + mapId + " - " + e.getMessage());
            }
        }

        plugin.getLogger().info("合計 " + loadedMaps.size() + " マップを読み込みました");
    }

    /**
     * 単一マップを読み込み
     */
    public MapData loadMap(String mapId) {
        File file = new File(mapsFolder, mapId + ".yml");
        if (!file.exists()) {
            return null;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        String displayName = config.getString("displayName", mapId);
        MapData mapData = new MapData(mapId, displayName);

        mapData.setWorldFolderName(config.getString("worldFolder", mapId));
        mapData.setEnabled(config.getBoolean("enabled", true));
        mapData.setSetupComplete(config.getBoolean("setupComplete", false));

        // EndMode設定読み込み
        if (config.contains("endMode")) {
            ConfigurationSection endModeSection = config.getConfigurationSection("endMode");
            if (endModeSection != null) {
                mapData.setEndModeBorderSize(endModeSection.getDouble("borderSize", 234.0));
                mapData.setEndModeCenterX(endModeSection.getDouble("centerX", 0.0));
                mapData.setEndModeCenterZ(endModeSection.getDouble("centerZ", 0.0));
                mapData.setEndModeShrinkSpeed(endModeSection.getDouble("shrinkSpeed", 1.0));
                mapData.setEndModeFinalSize(endModeSection.getDouble("finalSize", 10.0));
            }
        }

        // ロビースポーン読み込み
        if (config.contains("lobbySpawn")) {
            mapData.setLobbySpawn(loadLocation(config.getConfigurationSection("lobbySpawn")));
        }

        // チーム読み込み
        ConfigurationSection teamsSection = config.getConfigurationSection("teams");
        if (teamsSection != null) {
            for (String teamName : teamsSection.getKeys(false)) {
                ConfigurationSection teamSection = teamsSection.getConfigurationSection(teamName);
                if (teamSection != null) {
                    MapTeam team = loadTeam(teamName, teamSection);
                    mapData.addTeam(team);
                }
            }
        }

        // ジェネレーター読み込み
        ConfigurationSection gensSection = config.getConfigurationSection("generators");
        if (gensSection != null) {
            for (String genId : gensSection.getKeys(false)) {
                ConfigurationSection genSection = gensSection.getConfigurationSection(genId);
                if (genSection != null) {
                    MapGenerator generator = loadGenerator(genId, genSection);
                    mapData.addGenerator(generator);
                }
            }
        }

        // ショップ位置読み込み
        ConfigurationSection shopsSection = config.getConfigurationSection("shops");
        if (shopsSection != null) {
            for (String teamName : shopsSection.getKeys(false)) {
                List<Map<?, ?>> shopList = shopsSection.getMapList(teamName);
                for (Map<?, ?> shopMap : shopList) {
                    Location loc = loadLocationFromMap(shopMap);
                    if (loc != null) {
                        mapData.addShopLocation(teamName, loc);
                    }
                }
            }
        }

        // アップグレードショップ位置読み込み
        ConfigurationSection upgradeShopsSection = config.getConfigurationSection("upgradeShops");
        if (upgradeShopsSection != null) {
            for (String teamName : upgradeShopsSection.getKeys(false)) {
                ConfigurationSection locSection = upgradeShopsSection.getConfigurationSection(teamName);
                if (locSection != null) {
                    Location loc = loadLocation(locSection);
                    if (loc != null) {
                        mapData.setUpgradeShopLocation(teamName, loc);
                    }
                }
            }
        }

        return mapData;
    }

    /**
     * マップを保存
     */
    public void saveMap(MapData mapData) {
        File file = new File(mapsFolder, mapData.getId() + ".yml");
        YamlConfiguration config = new YamlConfiguration();

        config.set("displayName", mapData.getDisplayName());
        config.set("worldFolder", mapData.getWorldFolderName());
        config.set("enabled", mapData.isEnabled());
        config.set("setupComplete", mapData.isSetupComplete());

        // EndMode設定保存
        ConfigurationSection endModeSection = config.createSection("endMode");
        endModeSection.set("borderSize", mapData.getEndModeBorderSize());
        endModeSection.set("centerX", mapData.getEndModeCenterX());
        endModeSection.set("centerZ", mapData.getEndModeCenterZ());
        endModeSection.set("shrinkSpeed", mapData.getEndModeShrinkSpeed());
        endModeSection.set("finalSize", mapData.getEndModeFinalSize());

        // ロビースポーン保存
        if (mapData.getLobbySpawn() != null) {
            saveLocation(config.createSection("lobbySpawn"), mapData.getLobbySpawn());
        }

        // チーム保存
        ConfigurationSection teamsSection = config.createSection("teams");
        for (MapTeam team : mapData.getTeams().values()) {
            ConfigurationSection teamSection = teamsSection.createSection(team.getName());
            saveTeam(teamSection, team);
        }

        // ジェネレーター保存
        ConfigurationSection gensSection = config.createSection("generators");
        for (MapGenerator generator : mapData.getGenerators().values()) {
            ConfigurationSection genSection = gensSection.createSection(generator.getId());
            saveGenerator(genSection, generator);
        }

        // ショップ位置保存
        ConfigurationSection shopsSection = config.createSection("shops");
        for (Map.Entry<String, List<Location>> entry : mapData.getAllShopLocations().entrySet()) {
            List<Map<String, Object>> shopList = new ArrayList<>();
            for (Location loc : entry.getValue()) {
                shopList.add(locationToMap(loc));
            }
            shopsSection.set(entry.getKey(), shopList);
        }

        // アップグレードショップ位置保存
        ConfigurationSection upgradeShopsSection = config.createSection("upgradeShops");
        for (Map.Entry<String, Location> entry : mapData.getAllUpgradeShopLocations().entrySet()) {
            saveLocation(upgradeShopsSection.createSection(entry.getKey()), entry.getValue());
        }

        try {
            config.save(file);
            loadedMaps.put(mapData.getId(), mapData);
            plugin.getLogger().info("マップ保存完了: " + mapData.getId());
        } catch (IOException e) {
            plugin.getLogger().severe("マップ保存失敗: " + mapData.getId() + " - " + e.getMessage());
        }
    }

    /**
     * マップを削除
     */
    public boolean deleteMap(String mapId) {
        File file = new File(mapsFolder, mapId + ".yml");
        if (file.exists() && file.delete()) {
            loadedMaps.remove(mapId);
            plugin.getLogger().info("マップ削除完了: " + mapId);
            return true;
        }
        return false;
    }

    // ===== ヘルパーメソッド =====

    private MapTeam loadTeam(String name, ConfigurationSection section) {
        MapTeam team = new MapTeam(name);

        if (section.contains("bed")) {
            team.setBedLocation(loadLocation(section.getConfigurationSection("bed")));
        }
        if (section.contains("spawn")) {
            team.setSpawnLocation(loadLocation(section.getConfigurationSection("spawn")));
        }
        if (section.contains("shop")) {
            team.setShopLocation(loadLocation(section.getConfigurationSection("shop")));
        }
        if (section.contains("upgradeShop")) {
            team.setUpgradeShopLocation(loadLocation(section.getConfigurationSection("upgradeShop")));
        }
        if (section.contains("territoryRadius")) {
            team.setTerritoryRadius(section.getInt("territoryRadius"));
        }
        if (section.contains("ironGenerator")) {
            team.setIronGeneratorLocation(loadLocation(section.getConfigurationSection("ironGenerator")));
        }
        if (section.contains("goldGenerator")) {
            team.setGoldGeneratorLocation(loadLocation(section.getConfigurationSection("goldGenerator")));
        }

        return team;
    }

    private void saveTeam(ConfigurationSection section, MapTeam team) {
        if (team.getBedLocation() != null) {
            saveLocation(section.createSection("bed"), team.getBedLocation());
        }
        if (team.getSpawnLocation() != null) {
            saveLocation(section.createSection("spawn"), team.getSpawnLocation());
        }
        if (team.getShopLocation() != null) {
            saveLocation(section.createSection("shop"), team.getShopLocation());
        }
        if (team.getUpgradeShopLocation() != null) {
            saveLocation(section.createSection("upgradeShop"), team.getUpgradeShopLocation());
        }
        if (team.getTerritoryRadius() > 0) {
            section.set("territoryRadius", team.getTerritoryRadius());
        }
        if (team.getIronGeneratorLocation() != null) {
            saveLocation(section.createSection("ironGenerator"), team.getIronGeneratorLocation());
        }
        if (team.getGoldGeneratorLocation() != null) {
            saveLocation(section.createSection("goldGenerator"), team.getGoldGeneratorLocation());
        }
    }

    private MapGenerator loadGenerator(String id, ConfigurationSection section) {
        String teamName = section.getString("teamName", "共通");
        String materialType = section.getString("material", "IRON_INGOT");
        Material material = MapGenerator.getMaterialFromType(materialType);

        MapGenerator generator = new MapGenerator(id, teamName, material);
        generator.setSpawnInterval(section.getInt("spawnInterval", generator.getSpawnInterval()));

        if (section.contains("corner1")) {
            generator.setCorner1(loadLocation(section.getConfigurationSection("corner1")));
        }
        if (section.contains("corner2")) {
            generator.setCorner2(loadLocation(section.getConfigurationSection("corner2")));
        }

        return generator;
    }

    private void saveGenerator(ConfigurationSection section, MapGenerator generator) {
        section.set("teamName", generator.getTeamName());
        section.set("material", generator.getTypeString());
        section.set("spawnInterval", generator.getSpawnInterval());

        if (generator.getCorner1() != null) {
            saveLocation(section.createSection("corner1"), generator.getCorner1());
        }
        if (generator.getCorner2() != null) {
            saveLocation(section.createSection("corner2"), generator.getCorner2());
        }
    }

    private Location loadLocation(ConfigurationSection section) {
        if (section == null) return null;

        String worldName = section.getString("world", "world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            // ワールドがロードされていない場合、後で解決するためにnullでは返さない
            world = Bukkit.getWorlds().get(0); // フォールバック
        }

        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        float yaw = (float) section.getDouble("yaw", 0);
        float pitch = (float) section.getDouble("pitch", 0);

        return new Location(world, x, y, z, yaw, pitch);
    }

    private Location loadLocationFromMap(Map<?, ?> map) {
        if (map == null) return null;

        Object worldObj = map.get("world");
        String worldName = worldObj != null ? (String) worldObj : "world";
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            world = Bukkit.getWorlds().get(0);
        }

        double x = getNumberValue(map.get("x"), 0.0);
        double y = getNumberValue(map.get("y"), 0.0);
        double z = getNumberValue(map.get("z"), 0.0);
        float yaw = (float) getNumberValue(map.get("yaw"), 0.0);
        float pitch = (float) getNumberValue(map.get("pitch"), 0.0);

        return new Location(world, x, y, z, yaw, pitch);
    }

    private double getNumberValue(Object obj, double defaultValue) {
        if (obj instanceof Number) {
            return ((Number) obj).doubleValue();
        }
        return defaultValue;
    }

    private void saveLocation(ConfigurationSection section, Location loc) {
        section.set("world", loc.getWorld().getName());
        section.set("x", loc.getX());
        section.set("y", loc.getY());
        section.set("z", loc.getZ());
        section.set("yaw", loc.getYaw());
        section.set("pitch", loc.getPitch());
    }

    private Map<String, Object> locationToMap(Location loc) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("world", loc.getWorld().getName());
        map.put("x", loc.getX());
        map.put("y", loc.getY());
        map.put("z", loc.getZ());
        map.put("yaw", loc.getYaw());
        map.put("pitch", loc.getPitch());
        return map;
    }

    // ===== マップ取得メソッド =====

    public MapData getMap(String mapId) {
        return loadedMaps.get(mapId);
    }

    public Collection<MapData> getAllMaps() {
        return Collections.unmodifiableCollection(loadedMaps.values());
    }

    public List<MapData> getEnabledMaps() {
        List<MapData> enabled = new ArrayList<>();
        for (MapData map : loadedMaps.values()) {
            if (map.isEnabled() && map.isSetupComplete()) {
                enabled.add(map);
            }
        }
        return enabled;
    }

    public boolean hasMap(String mapId) {
        return loadedMaps.containsKey(mapId);
    }


    /**
     * ワールド名からマップを取得
     */
    public MapData getMapByWorldName(String worldName) {
        for (MapData map : loadedMaps.values()) {
            // マップのワールドフォルダ名と一致するか確認
            String mapWorldName = map.getWorldFolderName();
            if (mapWorldName != null && mapWorldName.equals(worldName)) {
                return map;
            }
            // マップIDとワールド名が一致するか確認（map_XXX形式）
            if (worldName.equals("map_" + map.getId())) {
                return map;
            }
            // マップIDとワールド名が直接一致するか確認
            if (worldName.equals(map.getId())) {
                return map;
            }
        }
        return null;
    }

    public int getMapCount() {
        return loadedMaps.size();
    }

    /**
     * 新規マップを作成
     */
    public MapData createMap(String mapId, String displayName) {
        if (hasMap(mapId)) {
            return null; // 既に存在
        }

        MapData mapData = new MapData(mapId, displayName);
        loadedMaps.put(mapId, mapData);
        saveMap(mapData);
        return mapData;
    }

    public File getMapsFolder() {
        return mapsFolder;
    }
}
