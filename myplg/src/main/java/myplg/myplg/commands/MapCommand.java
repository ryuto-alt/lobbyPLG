package myplg.myplg.commands;

import myplg.myplg.PvPGame;
import myplg.myplg.map.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * /map コマンド
 * マップの作成、編集、選択を管理
 */
public class MapCommand implements CommandExecutor, TabCompleter {
    private final PvPGame plugin;

    // チーム名リスト
    private static final List<String> TEAM_NAMES = Arrays.asList(
            "レッド", "ブルー", "グリーン", "イエロー",
            "アクア", "ホワイト", "ピンク", "グレー"
    );

    // ジェネレータータイプ
    private static final List<String> GENERATOR_TYPES = Arrays.asList(
            "iron", "gold", "diamond", "emerald"
    );

    public MapCommand(PvPGame plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cこのコマンドはプレイヤーのみ使用できます");
            return true;
        }

        Player player = (Player) sender;

        // 権限チェック
        if (!player.hasPermission("myplg.admin")) {
            player.sendMessage("§cこのコマンドを使用する権限がありません");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "list":
                handleList(player);
                break;

            case "create":
                handleCreate(player, args);
                break;

            case "import":
                handleImport(player, args);
                break;

            case "stages":
                handleStages(player);
                break;

            case "edit":
                handleEdit(player, args);
                break;

            case "setteam":
                handleSetTeam(player, args);
                break;

            case "setgen":
                handleSetGenerator(player, args);
                break;

            case "setshop":
                handleSetShop(player, args);
                break;

            case "setshop2":
                handleSetShop2(player, args);
                break;

            case "setlobby":
                handleSetLobby(player);
                break;

            case "confirm":
                handleConfirm(player);
                break;

            case "save":
                handleSave(player);
                break;

            case "cancel":
                handleCancel(player);
                break;

            case "select":
                handleSelect(player, args);
                break;

            case "random":
                handleRandom(player);
                break;

            case "delete":
                handleDelete(player, args);
                break;

            case "info":
                handleInfo(player, args);
                break;

            case "reload":
                handleReload(player);
                break;

            case "setendmode":
                handleSetEndMode(player, args);
                break;

            default:
                sendHelp(player);
        }

        return true;
    }

    // ===== サブコマンド処理 =====

    private void sendHelp(Player player) {
        player.sendMessage("");
        player.sendMessage("§6§l══════ マップ管理コマンド ══════");
        player.sendMessage("§e/map list §7- マップ一覧を表示");
        player.sendMessage("§e/map create <ID> <表示名> §7- 新規マップ作成");
        player.sendMessage("§e/map import <ステージ名> <ID> <表示名> §7- ステージをインポート");
        player.sendMessage("§e/map stages §7- 利用可能なステージ一覧");
        player.sendMessage("§e/map edit <ID> §7- マップ編集モード開始");
        player.sendMessage("§e/map select <ID> §7- 次のゲームで使用するマップを選択");
        player.sendMessage("§e/map random §7- ランダムにマップを選択");
        player.sendMessage("§e/map info <ID> §7- マップ情報を表示");
        player.sendMessage("§e/map delete <ID> §7- マップを削除");
        player.sendMessage("§e/map reload §7- マップデータを再読み込み");
        player.sendMessage("");
        player.sendMessage("§6§l--- 編集中コマンド ---");
        player.sendMessage("§e/map setendmode <サイズ> [中心X] [中心Z] [縮小速度] [最終サイズ]");
        player.sendMessage("§6§l═══════════════════════════════");
        player.sendMessage("");
    }

    private void handleList(Player player) {
        player.sendMessage("");
        player.sendMessage("§6§l══════ マップ一覧 ══════");

        List<String> lines = plugin.getMapSelector().getMapListForDisplay();
        for (String line : lines) {
            player.sendMessage(line);
        }

        player.sendMessage("");
        player.sendMessage("§7合計: §e" + plugin.getMapDataManager().getMapCount() + " §7マップ");
        player.sendMessage("§7有効: §a" + plugin.getMapDataManager().getEnabledMaps().size() + " §7マップ");
        player.sendMessage("§6§l════════════════════════");
        player.sendMessage("");
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§c使用方法: /map create <ID> <表示名>");
            player.sendMessage("§7例: /map create rooftop 屋上");
            return;
        }

        String mapId = args[1].toLowerCase();
        String displayName = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        if (plugin.getMapDataManager().hasMap(mapId)) {
            player.sendMessage("§cマップID「" + mapId + "」は既に存在します");
            return;
        }

        MapData mapData = plugin.getMapDataManager().createMap(mapId, displayName);
        if (mapData != null) {
            player.sendMessage("§aマップ「" + displayName + "」を作成しました");
            player.sendMessage("§7編集するには: /map edit " + mapId);
        } else {
            player.sendMessage("§cマップの作成に失敗しました");
        }
    }

    private void handleImport(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage("§c使用方法: /map import <ステージ名> <ID> <表示名>");
            player.sendMessage("§7例: /map import rooftop rooftop 屋上");
            player.sendMessage("§7利用可能なステージは /map stages で確認できます");
            return;
        }

        String stageName = args[1];
        String mapId = args[2].toLowerCase();
        String displayName = String.join(" ", Arrays.copyOfRange(args, 3, args.length));

        player.sendMessage("§eインポート開始: " + stageName + " -> " + displayName);

        plugin.getMapImporter().importStage(stageName, mapId, displayName)
                .thenAccept(result -> {
                    if (result.isSuccess()) {
                        player.sendMessage("§a" + result.getMessage());
                        player.sendMessage("§7編集するには: /map edit " + mapId);
                    } else {
                        player.sendMessage("§c" + result.getMessage());
                    }
                });
    }

    private void handleStages(Player player) {
        player.sendMessage("");
        player.sendMessage("§6§l══════ 利用可能なステージ ══════");

        List<String> stages = plugin.getMapImporter().getAvailableStages();
        if (stages.isEmpty()) {
            player.sendMessage("§7ステージが見つかりません");
        } else {
            for (String stage : stages) {
                MapImporter.StageInfo info = plugin.getMapImporter().getStageInfo(stage);
                if (info != null) {
                    player.sendMessage("§e" + stage + " §7(" + info.getSizeFormatted() + ")");
                } else {
                    player.sendMessage("§e" + stage);
                }
            }
        }

        player.sendMessage("");
        player.sendMessage("§7インポート: /map import <ステージ名> <ID> <表示名>");
        player.sendMessage("§6§l════════════════════════════════");
        player.sendMessage("");
    }

    private void handleEdit(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c使用方法: /map edit <ID>");
            return;
        }

        String mapId = args[1].toLowerCase();
        MapData mapData = plugin.getMapDataManager().getMap(mapId);

        if (mapData == null) {
            player.sendMessage("§cマップ「" + mapId + "」が見つかりません");
            return;
        }

        // 既に編集中なら終了
        if (plugin.getMapSetupManager().isEditing(player)) {
            plugin.getMapSetupManager().endEditSession(player);
        }

        // マップワールドをロード
        player.sendMessage("§eマップワールドをロード中...");
        String worldName = mapData.getWorldFolderName();
        org.bukkit.World mapWorld = plugin.getMapImporter().loadMapWorld(worldName);

        if (mapWorld == null) {
            player.sendMessage("§cマップワールドのロードに失敗しました");
            return;
        }

        // プレイヤーをマップワールドにテレポート
        org.bukkit.Location spawnLoc = mapWorld.getSpawnLocation();
        player.teleport(spawnLoc);
        player.sendMessage("§aマップワールドに移動しました");

        // 編集セッション開始
        plugin.getMapSetupManager().startEditSession(player, mapData);
    }

    private void handleSetTeam(Player player, String[] args) {
        if (!plugin.getMapSetupManager().isEditing(player)) {
            player.sendMessage("§c先に /map edit <ID> でマップ編集を開始してください");
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§c使用方法: /map setteam <チーム名>");
            player.sendMessage("§7利用可能なチーム: " + String.join(", ", TEAM_NAMES));
            return;
        }

        String teamName = args[1];
        plugin.getMapSetupManager().startTeamSetup(player, teamName);
    }

    private void handleSetGenerator(Player player, String[] args) {
        if (!plugin.getMapSetupManager().isEditing(player)) {
            player.sendMessage("§c先に /map edit <ID> でマップ編集を開始してください");
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§c使用方法: /map setgen <種類> [チーム名]");
            player.sendMessage("§7種類: iron, gold, diamond, emerald");
            player.sendMessage("§7チーム名省略時は「共通」になります");
            return;
        }

        String type = args[1].toLowerCase();
        String teamName = args.length >= 3 ? args[2] : "共通";

        if (!GENERATOR_TYPES.contains(type)) {
            player.sendMessage("§c無効なジェネレータータイプです");
            player.sendMessage("§7利用可能: " + String.join(", ", GENERATOR_TYPES));
            return;
        }

        plugin.getMapSetupManager().startGeneratorSetup(player, type, teamName);
    }

    private void handleSetShop(Player player, String[] args) {
        if (!plugin.getMapSetupManager().isEditing(player)) {
            player.sendMessage("§c先に /map edit <ID> でマップ編集を開始してください");
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§c使用方法: /map setshop <チーム名>");
            player.sendMessage("§7通常ショップ（村人）の位置を設定します");
            return;
        }

        String teamName = args[1];
        plugin.getMapSetupManager().startShopSetup(player, teamName, false);
    }

    private void handleSetShop2(Player player, String[] args) {
        if (!plugin.getMapSetupManager().isEditing(player)) {
            player.sendMessage("§c先に /map edit <ID> でマップ編集を開始してください");
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§c使用方法: /map setshop2 <チーム名>");
            player.sendMessage("§7アップグレードショップ（スケルトン）の位置を設定します");
            return;
        }

        String teamName = args[1];
        plugin.getMapSetupManager().startShopSetup(player, teamName, true);
    }

    private void handleSetLobby(Player player) {
        if (!plugin.getMapSetupManager().isEditing(player)) {
            player.sendMessage("§c先に /map edit <ID> でマップ編集を開始してください");
            return;
        }

        plugin.getMapSetupManager().startLobbySpawnSetup(player);
    }

    private void handleConfirm(Player player) {
        if (!plugin.getMapSetupManager().isEditing(player)) {
            player.sendMessage("§c編集セッションがありません");
            return;
        }

        plugin.getMapSetupManager().handleConfirm(player);
    }

    private void handleSave(Player player) {
        if (!plugin.getMapSetupManager().isEditing(player)) {
            player.sendMessage("§c編集セッションがありません");
            return;
        }

        MapSetupManager.EditSession session = plugin.getMapSetupManager().getEditSession(player);
        MapData mapData = session.getMapData();

        // セットアップ完了チェック
        List<String> missing = mapData.validateSetup();
        if (!missing.isEmpty()) {
            player.sendMessage("§e警告: 以下の項目が未設定です:");
            for (String item : missing) {
                player.sendMessage("  §c• " + item);
            }
            player.sendMessage("§7保存しますが、このマップはゲームで使用できません");
        }

        // ワールドを保存（ベッドの色変更などを保持）
        String mapId = mapData.getId();
        org.bukkit.World mapWorld = org.bukkit.Bukkit.getWorld("map_" + mapId);
        if (mapWorld != null) {
            player.sendMessage("§eワールドを保存中...");
            mapWorld.save();

            // plugins/myplg/map_worlds/<mapId> にもコピー
            java.io.File serverWorldFolder = new java.io.File(org.bukkit.Bukkit.getWorldContainer(), "map_" + mapId);
            java.io.File pluginWorldFolder = new java.io.File(plugin.getDataFolder(), "map_worlds/" + mapId);

            if (serverWorldFolder.exists()) {
                try {
                    copyWorldFolder(serverWorldFolder, pluginWorldFolder);
                    player.sendMessage("§aワールドデータを保存しました");
                } catch (Exception e) {
                    player.sendMessage("§cワールドデータの保存に失敗: " + e.getMessage());
                    plugin.getLogger().severe("ワールド保存エラー: " + e.getMessage());
                }
            }
        }

        plugin.getMapSetupManager().endEditSession(player);
        player.sendMessage("§aマップ「" + mapData.getDisplayName() + "」を保存しました");
    }

    /**
     * ワールドフォルダをコピー
     */
    private void copyWorldFolder(java.io.File source, java.io.File dest) throws java.io.IOException {
        if (!dest.exists()) {
            dest.mkdirs();
        }

        java.io.File[] files = source.listFiles();
        if (files == null) return;

        for (java.io.File file : files) {
            String name = file.getName();
            // session.lock, uid.dat, playerdata, stats はスキップ
            if (name.equals("session.lock") || name.equals("uid.dat") ||
                name.equals("playerdata") || name.equals("stats")) {
                continue;
            }

            java.io.File destFile = new java.io.File(dest, name);
            if (file.isDirectory()) {
                copyWorldFolder(file, destFile);
            } else {
                java.nio.file.Files.copy(file.toPath(), destFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private void handleCancel(Player player) {
        if (!plugin.getMapSetupManager().isEditing(player)) {
            player.sendMessage("§c編集セッションがありません");
            return;
        }

        MapSetupManager.EditSession session = plugin.getMapSetupManager().getEditSession(player);
        String mapName = session.getMapData().getDisplayName();

        // セッションを終了（保存せずに）
        // MapDataManagerからリロード
        plugin.getMapDataManager().loadAllMaps();

        player.sendMessage("§c編集をキャンセルしました。変更は保存されていません。");
    }

    private void handleSelect(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c使用方法: /map select <ID>");
            return;
        }

        String mapId = args[1].toLowerCase();

        if (plugin.getMapSelector().selectMap(mapId)) {
            MapData map = plugin.getMapSelector().getSelectedMap();
            player.sendMessage("§a次のゲームでマップ「" + map.getDisplayName() + "」を使用します");
        } else {
            MapData map = plugin.getMapDataManager().getMap(mapId);
            if (map == null) {
                player.sendMessage("§cマップ「" + mapId + "」が見つかりません");
            } else if (!map.isEnabled()) {
                player.sendMessage("§cマップ「" + mapId + "」は無効になっています");
            } else if (!map.isSetupComplete()) {
                player.sendMessage("§cマップ「" + mapId + "」はセットアップが完了していません");
            }
        }
    }

    private void handleRandom(Player player) {
        MapData map = plugin.getMapSelector().selectRandomMap();
        if (map != null) {
            player.sendMessage("§aランダムでマップ「" + map.getDisplayName() + "」を選択しました");
        } else {
            player.sendMessage("§c利用可能なマップがありません");
        }
    }

    private void handleDelete(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c使用方法: /map delete <ID>");
            return;
        }

        String mapId = args[1].toLowerCase();

        if (!plugin.getMapDataManager().hasMap(mapId)) {
            player.sendMessage("§cマップ「" + mapId + "」が見つかりません");
            return;
        }

        MapData map = plugin.getMapDataManager().getMap(mapId);
        String displayName = map.getDisplayName();

        if (plugin.getMapDataManager().deleteMap(mapId)) {
            player.sendMessage("§aマップ「" + displayName + "」を削除しました");
        } else {
            player.sendMessage("§cマップの削除に失敗しました");
        }
    }

    private void handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c使用方法: /map info <ID>");
            return;
        }

        String mapId = args[1].toLowerCase();
        MapData map = plugin.getMapDataManager().getMap(mapId);

        if (map == null) {
            player.sendMessage("§cマップ「" + mapId + "」が見つかりません");
            return;
        }

        player.sendMessage("");
        player.sendMessage("§6§l══════ マップ情報 ══════");
        player.sendMessage("§7ID: §f" + map.getId());
        player.sendMessage("§7表示名: §f" + map.getDisplayName());
        player.sendMessage("§7ワールドフォルダ: §f" + map.getWorldFolderName());
        player.sendMessage("§7有効: " + (map.isEnabled() ? "§a有効" : "§c無効"));
        player.sendMessage("§7セットアップ: " + (map.isSetupComplete() ? "§a完了" : "§e未完了 (" + map.getSetupProgress() + "%)"));
        player.sendMessage("");
        player.sendMessage("§7チーム数: §e" + map.getTeamCount());
        if (!map.getTeams().isEmpty()) {
            StringBuilder teams = new StringBuilder("  ");
            for (String teamName : map.getTeams().keySet()) {
                MapTeam team = map.getTeam(teamName);
                teams.append(team.getChatColorCode()).append(teamName).append("§7, ");
            }
            player.sendMessage(teams.substring(0, teams.length() - 2));
        }
        player.sendMessage("");
        player.sendMessage("§7ジェネレーター数: §e" + map.getGenerators().size());
        player.sendMessage("");
        player.sendMessage("§7--- EndMode設定 ---");
        player.sendMessage("§7ボーダーサイズ: §e" + map.getEndModeBorderSize());
        player.sendMessage("§7中心座標: §e(" + String.format("%.1f", map.getEndModeCenterX()) + ", " + String.format("%.1f", map.getEndModeCenterZ()) + ")");
        player.sendMessage("§7縮小速度: §e" + map.getEndModeShrinkSpeed() + " ブロック/秒");
        player.sendMessage("§7最終サイズ: §e" + map.getEndModeFinalSize());
        player.sendMessage("§6§l══════════════════════");
        player.sendMessage("");
    }

    private void handleReload(Player player) {
        plugin.getMapDataManager().loadAllMaps();
        player.sendMessage("§aマップデータを再読み込みしました");
        player.sendMessage("§7読み込み: " + plugin.getMapDataManager().getMapCount() + " マップ");
    }

    private void handleSetEndMode(Player player, String[] args) {
        if (!plugin.getMapSetupManager().isEditing(player)) {
            player.sendMessage("§c先に /map edit <ID> でマップ編集を開始してください");
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§c使用方法: /map setendmode <ボーダーサイズ> [中心X] [中心Z] [縮小速度] [最終サイズ]");
            player.sendMessage("§7例: /map setendmode 234 0 0 1.0 10");
            player.sendMessage("");
            player.sendMessage("§7現在地を中心として設定する場合:");
            player.sendMessage("§e/map setendmode <サイズ> here");
            return;
        }

        MapSetupManager.EditSession session = plugin.getMapSetupManager().getEditSession(player);
        MapData mapData = session.getMapData();

        try {
            double borderSize = Double.parseDouble(args[1]);
            double centerX = mapData.getEndModeCenterX();
            double centerZ = mapData.getEndModeCenterZ();
            double shrinkSpeed = mapData.getEndModeShrinkSpeed();
            double finalSize = mapData.getEndModeFinalSize();

            // "here" が指定された場合はプレイヤーの現在地を中心に
            if (args.length >= 3 && args[2].equalsIgnoreCase("here")) {
                centerX = player.getLocation().getX();
                centerZ = player.getLocation().getZ();
            } else if (args.length >= 4) {
                centerX = Double.parseDouble(args[2]);
                centerZ = Double.parseDouble(args[3]);
            }

            if (args.length >= 5) {
                shrinkSpeed = Double.parseDouble(args[4]);
            }

            if (args.length >= 6) {
                finalSize = Double.parseDouble(args[5]);
            }

            // 設定を適用
            mapData.setEndModeBorderSize(borderSize);
            mapData.setEndModeCenterX(centerX);
            mapData.setEndModeCenterZ(centerZ);
            mapData.setEndModeShrinkSpeed(shrinkSpeed);
            mapData.setEndModeFinalSize(finalSize);

            player.sendMessage("§a===============================");
            player.sendMessage("§aEndMode設定を更新しました");
            player.sendMessage("§a===============================");
            player.sendMessage("§7ボーダーサイズ: §e" + borderSize);
            player.sendMessage("§7中心座標: §e(" + String.format("%.1f", centerX) + ", " + String.format("%.1f", centerZ) + ")");
            player.sendMessage("§7縮小速度: §e" + shrinkSpeed + " ブロック/秒");
            player.sendMessage("§7最終サイズ: §e" + finalSize);
            player.sendMessage("");
            player.sendMessage("§7保存するには §e/map save §7を実行してください");

        } catch (NumberFormatException e) {
            player.sendMessage("§c数値の形式が正しくありません");
            player.sendMessage("§7例: /map setendmode 234 0 0 1.0 10");
        }
    }

    // ===== タブ補完 =====

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList(
                    "list", "create", "import", "stages", "edit", "setteam", "setgen",
                    "setshop", "setshop2", "setlobby", "setendmode", "confirm", "save", "cancel",
                    "select", "random", "delete", "info", "reload"
            );
            for (String sub : subCommands) {
                if (sub.startsWith(args[0].toLowerCase())) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();

            switch (subCommand) {
                case "edit":
                case "select":
                case "delete":
                case "info":
                    // マップIDを補完
                    for (MapData map : plugin.getMapDataManager().getAllMaps()) {
                        if (map.getId().startsWith(args[1].toLowerCase())) {
                            completions.add(map.getId());
                        }
                    }
                    break;

                case "import":
                    // ステージ名を補完
                    for (String stage : plugin.getMapImporter().getAvailableStages()) {
                        if (stage.toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(stage);
                        }
                    }
                    break;

                case "setteam":
                case "setshop":
                case "setshop2":
                    // チーム名を補完
                    for (String team : TEAM_NAMES) {
                        if (team.startsWith(args[1])) {
                            completions.add(team);
                        }
                    }
                    break;

                case "setgen":
                    // ジェネレータータイプを補完
                    for (String type : GENERATOR_TYPES) {
                        if (type.startsWith(args[1].toLowerCase())) {
                            completions.add(type);
                        }
                    }
                    break;

                case "setendmode":
                    // デフォルト値を補完
                    if ("234".startsWith(args[1])) completions.add("234");
                    if ("100".startsWith(args[1])) completions.add("100");
                    if ("150".startsWith(args[1])) completions.add("150");
                    if ("200".startsWith(args[1])) completions.add("200");
                    break;
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();

            switch (subCommand) {
                case "setendmode":
                    // "here" オプションを補完
                    if ("here".startsWith(args[2].toLowerCase())) {
                        completions.add("here");
                    }
                    break;

                case "setgen":
                    // チーム名を補完（共通を含む）
                    List<String> teams = new ArrayList<>(TEAM_NAMES);
                    teams.add(0, "共通");
                    for (String team : teams) {
                        if (team.startsWith(args[2])) {
                            completions.add(team);
                        }
                    }
                    break;
            }
        }

        return completions;
    }
}
