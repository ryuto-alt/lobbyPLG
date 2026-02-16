package myplg.myplg.map;

import myplg.myplg.PvPGame;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * FastServerからワールドをインポートするクラス
 */
public class MapImporter {
    private final PvPGame plugin;

    // BedWarsMapフォルダパス（server/BedWarsMap）
    private static final String BEDWARS_MAP_PATH = "BedWarsMap";

    public MapImporter(PvPGame plugin) {
        this.plugin = plugin;
    }

    /**
     * 利用可能なマップ一覧を取得（BedWarsMapフォルダから）
     */
    public List<String> getAvailableStages() {
        List<String> stages = new ArrayList<>();
        File bedwarsMapFolder = new File(Bukkit.getWorldContainer(), BEDWARS_MAP_PATH);

        if (!bedwarsMapFolder.exists() || !bedwarsMapFolder.isDirectory()) {
            plugin.getLogger().warning("BedWarsMapフォルダが見つかりません: " + bedwarsMapFolder.getAbsolutePath());
            return stages;
        }

        File[] folders = bedwarsMapFolder.listFiles(File::isDirectory);
        if (folders != null) {
            for (File folder : folders) {
                // level.datが存在するフォルダのみを有効なワールドとして追加
                if (new File(folder, "level.dat").exists()) {
                    stages.add(folder.getName());
                }
            }
        }

        return stages;
    }

    /**
     * BedWarsMap内のワールドをマップとして登録
     * @param worldName BedWarsMap内のワールド名（例：Airshow, Glacier）
     * @param mapId 新しいマップID
     * @param displayName 表示名
     * @return インポート結果のFuture
     */
    public CompletableFuture<ImportResult> importStage(String worldName, String mapId, String displayName) {
        CompletableFuture<ImportResult> future = new CompletableFuture<>();

        // 既存チェック
        if (plugin.getMapDataManager().hasMap(mapId)) {
            future.complete(new ImportResult(false, "マップID「" + mapId + "」は既に存在します"));
            return future;
        }

        // BedWarsMap内のワールドフォルダを確認
        File bedwarsMapFolder = new File(Bukkit.getWorldContainer(), BEDWARS_MAP_PATH);
        File worldFolder = new File(bedwarsMapFolder, worldName);

        if (!worldFolder.exists() || !new File(worldFolder, "level.dat").exists()) {
            future.complete(new ImportResult(false, "BedWarsMap内にワールド「" + worldName + "」が見つかりません"));
            return future;
        }

        // メインスレッドでマップデータ作成
        Bukkit.getScheduler().runTask(plugin, () -> {
            // マップデータ作成
            MapData mapData = plugin.getMapDataManager().createMap(mapId, displayName);
            if (mapData != null) {
                mapData.setWorldFolderName(worldName);
                plugin.getMapDataManager().saveMap(mapData);
                future.complete(new ImportResult(true, "マップ「" + displayName + "」の登録が完了しました"));
            } else {
                future.complete(new ImportResult(false, "マップデータの作成に失敗しました"));
            }
        });

        return future;
    }


    /**
     * ディレクトリを再帰的にコピー
     */
    private void copyDirectory(Path source, Path destination) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                String fileName = dir.getFileName().toString();
                // session.lock、uid.dat、playerdata、statsはスキップ
                if (fileName.equals("session.lock") || fileName.equals("uid.dat") ||
                        fileName.equals("playerdata") || fileName.equals("stats")) {
                    return FileVisitResult.SKIP_SUBTREE;
                }

                Path targetDir = destination.resolve(source.relativize(dir));
                Files.createDirectories(targetDir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String fileName = file.getFileName().toString();
                // 不要なファイルをスキップ
                if (fileName.equals("session.lock") || fileName.equals("uid.dat")) {
                    return FileVisitResult.CONTINUE;
                }

                Path targetFile = destination.resolve(source.relativize(file));
                Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * BedWarsMapからワールドをロード（一時コピー方式）
     * ゲーム開始時: BedWarsMap/Airshow → server/Airshow にコピーしてロード
     * @param worldName ワールド名（例：Airshow, Glacier）
     */
    public World loadMapWorld(String worldName) {
        // 既にロードされている場合はそのまま返す
        World existing = Bukkit.getWorld(worldName);
        if (existing != null) {
            return existing;
        }

        // BedWarsMapフォルダ内のマスターワールド
        File bedwarsMapFolder = new File(Bukkit.getWorldContainer(), BEDWARS_MAP_PATH);
        File masterWorldFolder = new File(bedwarsMapFolder, worldName);

        if (!masterWorldFolder.exists() || !new File(masterWorldFolder, "level.dat").exists()) {
            plugin.getLogger().warning("BedWarsMap内にワールドが見つかりません: " + masterWorldFolder.getAbsolutePath());
            return null;
        }

        // server直下にコピー（一時的なゲーム実行用）
        File serverWorldFolder = new File(Bukkit.getWorldContainer(), worldName);
        if (!serverWorldFolder.exists()) {
            try {
                plugin.getLogger().info("BedWarsMapからワールドをコピー中: " + worldName);
                copyDirectory(masterWorldFolder.toPath(), serverWorldFolder.toPath());
                plugin.getLogger().info("ワールドコピー完了: " + serverWorldFolder.getAbsolutePath());
            } catch (IOException e) {
                plugin.getLogger().severe("ワールドコピー失敗: " + e.getMessage());
                return null;
            }
        }

        // ワールドをロード
        plugin.getLogger().info("ワールドをロード中: " + worldName);
        WorldCreator creator = new WorldCreator(worldName);
        World world = Bukkit.createWorld(creator);

        if (world != null) {
            world.setAutoSave(false);
            plugin.getLogger().info("ワールドロード完了: " + world.getName());
        } else {
            plugin.getLogger().severe("ワールドのロードに失敗しました: " + worldName);
        }

        return world;
    }

    /**
     * マップワールドをアンロードして削除（一時コピー方式）
     * ゲーム終了時: server/Airshow をアンロード → 即削除
     * @param worldName ワールド名（例：Airshow, Glacier）
     * @param deleteFolder server直下のフォルダを削除するか（通常true）
     */
    public boolean unloadMapWorld(String worldName, boolean deleteFolder) {
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            plugin.getLogger().info("ワールドをアンロード中: " + worldName);
            boolean unloaded = Bukkit.unloadWorld(world, false);
            if (!unloaded) {
                plugin.getLogger().warning("ワールドのアンロードに失敗: " + worldName);
                return false;
            }
            plugin.getLogger().info("ワールドアンロード完了: " + worldName);
        }

        // server直下の一時ワールドフォルダを削除（フォルダ構成をスッキリ保つ）
        if (deleteFolder) {
            File serverWorldFolder = new File(Bukkit.getWorldContainer(), worldName);
            if (serverWorldFolder.exists()) {
                plugin.getLogger().info("server直下の一時ワールドフォルダを削除中: " + worldName);
                deleteDirectorySync(serverWorldFolder);
                plugin.getLogger().info("一時ワールドフォルダ削除完了（server直下はlobbyのみでスッキリ）");
            }
        }

        return true;
    }

    /**
     * マップワールドをアンロード（デフォルトで削除）
     * ゲーム終了時にserver直下をスッキリ保つため、デフォルトで削除します
     */
    public boolean unloadMapWorld(String worldName) {
        return unloadMapWorld(worldName, true);
    }

    private void deleteDirectorySync(File directory) {
        if (!directory.exists()) {
            return;
        }

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().equals("session.lock")) {
                    continue;
                }
                if (file.isDirectory()) {
                    deleteDirectorySync(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }

    /**
     * マップ情報を取得
     */
    public StageInfo getStageInfo(String worldName) {
        File bedwarsMapFolder = new File(Bukkit.getWorldContainer(), BEDWARS_MAP_PATH);
        File worldFolder = new File(bedwarsMapFolder, worldName);

        if (!worldFolder.exists() || !new File(worldFolder, "level.dat").exists()) {
            return null;
        }

        // フォルダサイズを計算
        long size = calculateFolderSize(worldFolder);

        return new StageInfo(worldName, worldName, size);
    }

    private long calculateFolderSize(File folder) {
        long size = 0;
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    size += calculateFolderSize(file);
                } else {
                    size += file.length();
                }
            }
        }
        return size;
    }

    /**
     * インポート結果
     */
    public static class ImportResult {
        private final boolean success;
        private final String message;

        public ImportResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * ステージ情報
     */
    public static class StageInfo {
        private final String name;
        private final String worldFolderName;
        private final long sizeBytes;

        public StageInfo(String name, String worldFolderName, long sizeBytes) {
            this.name = name;
            this.worldFolderName = worldFolderName;
            this.sizeBytes = sizeBytes;
        }

        public String getName() {
            return name;
        }

        public String getWorldFolderName() {
            return worldFolderName;
        }

        public long getSizeBytes() {
            return sizeBytes;
        }

        public String getSizeFormatted() {
            if (sizeBytes < 1024) return sizeBytes + " B";
            if (sizeBytes < 1024 * 1024) return String.format("%.1f KB", sizeBytes / 1024.0);
            return String.format("%.1f MB", sizeBytes / (1024.0 * 1024.0));
        }
    }
}
