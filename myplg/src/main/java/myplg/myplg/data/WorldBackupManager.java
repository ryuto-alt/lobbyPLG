package myplg.myplg.data;

import myplg.myplg.PvPGame;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.CompletableFuture;

/**
 * ワールドスワップ方式によるワールド管理
 *
 * 仕組み:
 * - world: ゲームで使用するメインワールド
 * - world_backup: 常時ロードされたバックアップワールド（プレイヤーは入れない）
 * - world_master: 初期バックアップの永続コピー（plugins/myplg/world_master）
 *
 * ゲーム終了時:
 * 1. プレイヤーをlobbyへ移動
 * 2. worldをアンロード → フォルダ削除
 * 3. world_backupをアンロード → worldにリネーム
 * 4. worldをロード（瞬時に復元完了）
 * 5. 非同期でworld_masterからworld_backupを再作成
 */
public class WorldBackupManager {
    private final PvPGame plugin;
    private final File masterFolder;  // 永続マスターバックアップ

    private static final String GAME_WORLD = "world";
    private static final String BACKUP_WORLD = "world_backup";

    private boolean backupReady = false;
    private boolean preparingBackup = false;

    public WorldBackupManager(PvPGame plugin) {
        this.plugin = plugin;
        this.masterFolder = new File(plugin.getDataFolder(), "world_master");
    }

    /**
     * プラグイン起動時の初期化
     * マスターバックアップとworld_backupを準備
     */
    public void initialize() {
        plugin.getLogger().info("===== ワールドスワップシステム初期化 =====");

        // マスターバックアップが存在しない場合は作成
        if (!masterFolder.exists()) {
            plugin.getLogger().info("マスターバックアップが存在しません。初回セットアップを行います。");
            plugin.getLogger().info("※ /save コマンドでマスターバックアップを作成してください。");
            return;
        }

        // world_backupを準備
        prepareBackupWorldSync();

        plugin.getLogger().info("===== ワールドスワップシステム初期化完了 =====");
    }

    /**
     * 現在のワールドをマスターバックアップとして保存
     */
    public boolean saveMasterBackup(World world) {
        plugin.getLogger().info("マスターバックアップを作成中: " + world.getName());

        // ワールドを保存
        world.save();

        File worldFolder = world.getWorldFolder();

        // 古いマスターバックアップを削除
        if (masterFolder.exists()) {
            deleteDirectorySync(masterFolder);
        }
        masterFolder.mkdirs();

        try {
            copyDirectorySync(worldFolder.toPath(), masterFolder.toPath());
            plugin.getLogger().info("マスターバックアップ作成完了");

            // バックアップワールドも準備
            prepareBackupWorldSync();

            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("マスターバックアップ作成失敗: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * マスターバックアップが存在するか
     */
    public boolean hasBackup(String worldName) {
        return masterFolder.exists() && masterFolder.isDirectory();
    }

    /**
     * バックアップワールドが準備完了しているか
     */
    public boolean isBackupReady() {
        return backupReady;
    }

    /**
     * ワールドスワップを実行（ゲーム終了時）
     * これがメインの復元メソッド
     */
    public CompletableFuture<Boolean> swapWorlds() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        plugin.getLogger().info("===== ワールドスワップ開始 =====");

        // バックアップの準備確認
        if (!backupReady) {
            plugin.getLogger().severe("バックアップワールドが準備されていません！");
            future.complete(false);
            return future;
        }

        World gameWorld = Bukkit.getWorld(GAME_WORLD);
        World backupWorld = Bukkit.getWorld(BACKUP_WORLD);

        if (backupWorld == null) {
            plugin.getLogger().severe("バックアップワールドがロードされていません！");
            future.complete(false);
            return future;
        }

        // Step 1: プレイヤーをlobbyへ移動
        World lobbyWorld = Bukkit.getWorld("lobby");
        if (lobbyWorld == null) {
            plugin.getLogger().severe("Lobbyワールドが見つかりません");
            future.complete(false);
            return future;
        }

        org.bukkit.Location lobbySpawn = new org.bukkit.Location(lobbyWorld, -210, 7, 15);

        if (gameWorld != null) {
            for (Player player : gameWorld.getPlayers()) {
                player.teleport(lobbySpawn);
            }
        }

        // backupWorldからもプレイヤーを退避（念のため）
        for (Player player : backupWorld.getPlayers()) {
            player.teleport(lobbySpawn);
        }

        plugin.getLogger().info("プレイヤー退避完了");

        // Step 2: 少し待ってからスワップ実行
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                performSwap(future);
            } catch (Exception e) {
                plugin.getLogger().severe("ワールドスワップ中にエラー: " + e.getMessage());
                e.printStackTrace();
                future.complete(false);
            }
        }, 10L); // 0.5秒待機

        return future;
    }

    private void performSwap(CompletableFuture<Boolean> future) {
        World gameWorld = Bukkit.getWorld(GAME_WORLD);
        World backupWorld = Bukkit.getWorld(BACKUP_WORLD);

        // Step 3: ゲームワールドをアンロード
        if (gameWorld != null) {
            plugin.getLogger().info("ゲームワールドをアンロード中...");
            boolean unloaded = Bukkit.unloadWorld(gameWorld, false);
            if (!unloaded) {
                plugin.getLogger().severe("ゲームワールドのアンロードに失敗");
                future.complete(false);
                return;
            }
        }

        // Step 4: バックアップワールドをアンロード
        plugin.getLogger().info("バックアップワールドをアンロード中...");
        boolean backupUnloaded = Bukkit.unloadWorld(backupWorld, false);
        if (!backupUnloaded) {
            plugin.getLogger().severe("バックアップワールドのアンロードに失敗");
            // ゲームワールドを再ロードして復旧
            Bukkit.createWorld(new WorldCreator(GAME_WORLD));
            future.complete(false);
            return;
        }

        backupReady = false;

        // Step 5: フォルダ操作（同期で高速実行）
        File worldFolder = new File(Bukkit.getWorldContainer(), GAME_WORLD);
        File backupFolder = new File(Bukkit.getWorldContainer(), BACKUP_WORLD);

        // 古いゲームワールドフォルダを削除
        plugin.getLogger().info("古いワールドフォルダを削除中...");
        deleteDirectorySync(worldFolder);

        // バックアップフォルダをゲームワールドにリネーム
        plugin.getLogger().info("バックアップをゲームワールドにリネーム中...");
        boolean renamed = backupFolder.renameTo(worldFolder);

        if (!renamed) {
            plugin.getLogger().severe("フォルダリネームに失敗！コピーを試行...");
            try {
                copyDirectorySync(backupFolder.toPath(), worldFolder.toPath());
                deleteDirectorySync(backupFolder);
            } catch (IOException e) {
                plugin.getLogger().severe("コピーにも失敗: " + e.getMessage());
                future.complete(false);
                return;
            }
        }

        // Step 6: 新しいゲームワールドをロード
        plugin.getLogger().info("新しいゲームワールドをロード中...");
        World newGameWorld = Bukkit.createWorld(new WorldCreator(GAME_WORLD));

        if (newGameWorld == null) {
            plugin.getLogger().severe("ワールドのロードに失敗");
            future.complete(false);
            return;
        }

        // Step 7: エンティティをクリーンアップ（アイテム、Mob等を削除）
        plugin.getLogger().info("ワールド内のエンティティをクリーンアップ中...");
        cleanupWorldEntities(newGameWorld);

        plugin.getLogger().info("===== ワールドスワップ完了！ =====");

        // Step 8: 非同期で次のバックアップを準備
        prepareBackupWorldAsync();

        future.complete(true);
    }

    /**
     * バックアップワールドを同期で準備（起動時用）
     */
    private void prepareBackupWorldSync() {
        if (!masterFolder.exists()) {
            plugin.getLogger().warning("マスターバックアップが存在しません");
            return;
        }

        plugin.getLogger().info("バックアップワールドを準備中...");

        File backupFolder = new File(Bukkit.getWorldContainer(), BACKUP_WORLD);

        // 既存のバックアップワールドをアンロード
        World existingBackup = Bukkit.getWorld(BACKUP_WORLD);
        if (existingBackup != null) {
            // プレイヤーを退避
            World lobbyWorld = Bukkit.getWorld("lobby");
            if (lobbyWorld != null) {
                org.bukkit.Location lobbySpawn = new org.bukkit.Location(lobbyWorld, -210, 7, 15);
                for (Player player : existingBackup.getPlayers()) {
                    player.teleport(lobbySpawn);
                }
            }
            Bukkit.unloadWorld(existingBackup, false);
        }

        // フォルダを削除して再作成
        if (backupFolder.exists()) {
            deleteDirectorySync(backupFolder);
        }

        try {
            copyDirectorySync(masterFolder.toPath(), backupFolder.toPath());

            // バックアップワールドをロード
            World backup = Bukkit.createWorld(new WorldCreator(BACKUP_WORLD));
            if (backup != null) {
                // バックアップワールドに入れないようにする
                backup.setAutoSave(false);
                backupReady = true;
                plugin.getLogger().info("バックアップワールド準備完了");
            }
        } catch (IOException e) {
            plugin.getLogger().severe("バックアップワールドの準備に失敗: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * バックアップワールドを非同期で準備（ゲーム終了後用）
     */
    public void prepareBackupWorldAsync() {
        if (preparingBackup) {
            plugin.getLogger().info("バックアップ準備が既に進行中です");
            return;
        }

        if (!masterFolder.exists()) {
            plugin.getLogger().warning("マスターバックアップが存在しません");
            return;
        }

        preparingBackup = true;
        plugin.getLogger().info("次のバックアップを非同期で準備開始...");

        File backupFolder = new File(Bukkit.getWorldContainer(), BACKUP_WORLD);

        // 非同期でファイルコピー
        CompletableFuture.runAsync(() -> {
            try {
                // フォルダを削除して再作成
                if (backupFolder.exists()) {
                    deleteDirectorySync(backupFolder);
                }

                copyDirectorySync(masterFolder.toPath(), backupFolder.toPath());

                plugin.getLogger().info("バックアップファイルコピー完了");

                // メインスレッドでワールドをロード
                Bukkit.getScheduler().runTask(plugin, () -> {
                    World backup = Bukkit.createWorld(new WorldCreator(BACKUP_WORLD));
                    if (backup != null) {
                        backup.setAutoSave(false);
                        backupReady = true;
                        preparingBackup = false;
                        plugin.getLogger().info("バックアップワールド準備完了（非同期）");
                    } else {
                        preparingBackup = false;
                        plugin.getLogger().severe("バックアップワールドのロードに失敗");
                    }
                });
            } catch (IOException e) {
                preparingBackup = false;
                plugin.getLogger().severe("バックアップ準備中にエラー: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * 旧APIとの互換性のため（restoreWorldRealtimeの代替）
     * @deprecated このメソッドはメインスレッドをブロックします。代わりに swapWorlds() を使用してください。
     */
    @Deprecated
    public boolean restoreWorldRealtime(String worldName) {
        plugin.getLogger().warning("restoreWorldRealtime()は非推奨です。メインスレッドをブロックするため、サーバーがフリーズする可能性があります。swapWorlds()を使用してください。");
        try {
            return swapWorlds().get();
        } catch (Exception e) {
            plugin.getLogger().severe("ワールド復元エラー: " + e.getMessage());
            return false;
        }
    }

    /**
     * 旧APIとの互換性のため（saveWorldの代替）
     */
    public boolean saveWorld(World world) {
        return saveMasterBackup(world);
    }

    /**
     * 旧APIとの互換性のため（restoreWorldの代替）
     * @deprecated このメソッドはメインスレッドをブロックします。代わりに swapWorlds() を使用してください。
     */
    @Deprecated
    public boolean restoreWorld(String worldName) {
        return restoreWorldRealtime(worldName);
    }

    // ===== ユーティリティメソッド =====

    private void copyDirectorySync(Path source, Path destination) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                String fileName = dir.getFileName().toString();
                // session.lockとuid.datはスキップ
                if (fileName.equals("session.lock") || fileName.equals("uid.dat")) {
                    return FileVisitResult.SKIP_SUBTREE;
                }

                Path targetDir = destination.resolve(source.relativize(dir));
                Files.createDirectories(targetDir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String fileName = file.getFileName().toString();
                // session.lockとuid.datはスキップ
                if (fileName.equals("session.lock") || fileName.equals("uid.dat")) {
                    return FileVisitResult.CONTINUE;
                }

                Path targetFile = destination.resolve(source.relativize(file));
                Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void deleteDirectorySync(File directory) {
        if (!directory.exists()) {
            return;
        }

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                // session.lockはスキップ（削除できない場合があるため）
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
     * マップワールドをマスターコピーとして保存
     * @param worldName ワールド名（例：Airshow, Glacier）
     * @return 保存成功時はtrue
     */
    public boolean saveMapWorld(String worldName) {
        plugin.getLogger().info("===== マップワールド保存開始: " + worldName + " =====");

        World mapWorld = Bukkit.getWorld(worldName);
        if (mapWorld == null) {
            plugin.getLogger().severe("マップワールドが見つかりません: " + worldName);
            return false;
        }

        // ワールドを保存
        mapWorld.save();

        // server直下の一時ワールドフォルダ（ゲーム中のみ存在）
        File serverWorldFolder = new File(Bukkit.getWorldContainer(), worldName);
        if (!serverWorldFolder.exists()) {
            plugin.getLogger().severe("ワールドフォルダが見つかりません: " + serverWorldFolder.getAbsolutePath());
            plugin.getLogger().warning("マップワールドをロードしてから保存してください");
            return false;
        }

        // マスターコピー保存先（server/maps_master）
        File masterBackupRoot = new File(Bukkit.getWorldContainer(), "maps_master");
        File mapMasterFolder = new File(masterBackupRoot, worldName);

        // 古いマスターフォルダを削除
        if (mapMasterFolder.exists()) {
            deleteDirectorySync(mapMasterFolder);
        }
        mapMasterFolder.mkdirs();

        try {
            copyDirectorySync(serverWorldFolder.toPath(), mapMasterFolder.toPath());
            plugin.getLogger().info("===== マップワールド保存完了（マスターコピー作成）: " + worldName + " =====");
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("マップワールド保存失敗: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * ワールド名からマップIDを抽出
     * map_xxx 形式の場合は xxx を返す、それ以外はnull
     */
    public String extractMapId(String worldName) {
        if (worldName != null && worldName.startsWith("map_")) {
            return worldName.substring(4); // "map_" の後の部分を返す
        }
        return null;
    }

    /**
     * マップワールドをリセット（マスターコピーから復元）
     * @param worldName ワールド名（例：Airshow, Glacier）
     * @return リセット成功時はtrue
     */
    public CompletableFuture<Boolean> resetMapWorld(String worldName) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        plugin.getLogger().info("===== マップワールドリセット開始: " + worldName + " =====");

        // マスターコピー（server/maps_master）
        File masterBackupRoot = new File(Bukkit.getWorldContainer(), "maps_master");
        File mapMasterFolder = new File(masterBackupRoot, worldName);

        if (!mapMasterFolder.exists() || !new File(mapMasterFolder, "level.dat").exists()) {
            plugin.getLogger().severe("マスターコピーが見つかりません: " + mapMasterFolder.getAbsolutePath());
            plugin.getLogger().warning("先に /mapsave コマンドでマスターコピーを作成してください");
            future.complete(false);
            return future;
        }

        // 現在のマップワールド
        World mapWorld = Bukkit.getWorld(worldName);

        // プレイヤーをlobbyに退避
        World lobbyWorld = Bukkit.getWorld("lobby");
        if (lobbyWorld == null) {
            plugin.getLogger().severe("Lobbyワールドが見つかりません");
            future.complete(false);
            return future;
        }

        org.bukkit.Location lobbySpawn = new org.bukkit.Location(lobbyWorld, -210, 7, 15);

        if (mapWorld != null) {
            for (Player player : mapWorld.getPlayers()) {
                player.teleport(lobbySpawn);
            }
        }

        plugin.getLogger().info("プレイヤー退避完了");

        // 少し待ってからリセット実行
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                performMapReset(worldName, mapMasterFolder, future);
            } catch (Exception e) {
                plugin.getLogger().severe("マップリセット中にエラー: " + e.getMessage());
                e.printStackTrace();
                future.complete(false);
            }
        }, 10L);

        return future;
    }

    private void performMapReset(String worldName, File mapMasterFolder, CompletableFuture<Boolean> future) {
        World mapWorld = Bukkit.getWorld(worldName);

        // ワールドをアンロード
        if (mapWorld != null) {
            plugin.getLogger().info("マップワールドをアンロード中: " + worldName);
            boolean unloaded = Bukkit.unloadWorld(mapWorld, false);
            if (!unloaded) {
                plugin.getLogger().severe("マップワールドのアンロードに失敗");
                future.complete(false);
                return;
            }
        }

        // server直下の一時ワールドフォルダを削除
        File serverWorldFolder = new File(Bukkit.getWorldContainer(), worldName);
        plugin.getLogger().info("古いマップワールドフォルダを削除中...");
        deleteDirectorySync(serverWorldFolder);

        // マスターコピーからserver直下にコピー（一時的なゲーム実行用）
        plugin.getLogger().info("マスターコピーからワールドを復元中...");
        try {
            copyDirectorySync(mapMasterFolder.toPath(), serverWorldFolder.toPath());
        } catch (IOException e) {
            plugin.getLogger().severe("ワールドコピーに失敗: " + e.getMessage());
            future.complete(false);
            return;
        }

        // ワールドを再ロード
        plugin.getLogger().info("マップワールドを再ロード中...");
        World newMapWorld = Bukkit.createWorld(new WorldCreator(worldName));

        if (newMapWorld == null) {
            plugin.getLogger().severe("マップワールドのロードに失敗");
            future.complete(false);
            return;
        }

        newMapWorld.setAutoSave(false);

        // エンティティをクリーンアップ
        plugin.getLogger().info("マップワールド内のエンティティをクリーンアップ中...");
        cleanupWorldEntities(newMapWorld);

        plugin.getLogger().info("===== マップワールドリセット完了: " + worldName + " =====");
        future.complete(true);
    }

    /**
     * ワールド内の不要なエンティティをクリーンアップ
     * ドロップアイテム、矢、経験値オーブ、スポーンしたMobなどを削除
     */
    private void cleanupWorldEntities(World world) {
        int removedCount = 0;
        for (Entity entity : world.getEntities()) {
            EntityType type = entity.getType();

            // プレイヤーはスキップ
            if (type == EntityType.PLAYER) {
                continue;
            }

            // 以下のエンティティタイプを削除
            // - ドロップアイテム
            // - 矢
            // - 経験値オーブ
            // - TNT
            // - 落下中のブロック
            // - プレイヤーがスポーンしたMob（アイアンゴーレム等）
            // - ファイヤーボール
            if (type == EntityType.ITEM ||
                type == EntityType.ARROW ||
                type == EntityType.SPECTRAL_ARROW ||
                type == EntityType.EXPERIENCE_ORB ||
                type == EntityType.TNT ||
                type == EntityType.FALLING_BLOCK ||
                type == EntityType.IRON_GOLEM ||
                type == EntityType.FIREBALL ||
                type == EntityType.SMALL_FIREBALL ||
                type == EntityType.DRAGON_FIREBALL ||
                type == EntityType.WITHER_SKULL ||
                type == EntityType.EGG ||
                type == EntityType.SNOWBALL ||
                type == EntityType.ENDER_PEARL ||
                type == EntityType.TRIDENT ||
                type == EntityType.SPLASH_POTION ||
                type == EntityType.LINGERING_POTION ||
                type == EntityType.EXPERIENCE_BOTTLE ||
                type == EntityType.AREA_EFFECT_CLOUD) {
                entity.remove();
                removedCount++;
            }
        }

        if (removedCount > 0) {
            plugin.getLogger().info("クリーンアップ: " + removedCount + " エンティティを削除しました");
        }
    }
}
