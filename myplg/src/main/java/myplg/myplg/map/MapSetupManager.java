package myplg.myplg.map;

import myplg.myplg.PvPGame;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * マップセットアップのステップバイステップガイドを管理するクラス
 */
public class MapSetupManager {
    private final PvPGame plugin;

    // プレイヤーごとの編集セッション
    private final Map<UUID, EditSession> editSessions = new HashMap<>();

    public MapSetupManager(PvPGame plugin) {
        this.plugin = plugin;
    }

    /**
     * 編集セッションを開始
     */
    public void startEditSession(Player player, MapData mapData) {
        EditSession session = new EditSession(mapData);
        editSessions.put(player.getUniqueId(), session);

        sendWelcomeMessage(player, mapData);
        sendNextStep(player);
    }

    /**
     * 編集セッションを終了
     */
    public void endEditSession(Player player) {
        EditSession session = editSessions.remove(player.getUniqueId());
        if (session != null) {
            // 保存
            plugin.getMapDataManager().saveMap(session.getMapData());
            player.sendMessage("§a§l[マップ設定] §7編集を終了しました。");
        }
    }

    /**
     * 編集セッションを取得
     */
    public EditSession getEditSession(Player player) {
        return editSessions.get(player.getUniqueId());
    }

    /**
     * 編集中かどうか
     */
    public boolean isEditing(Player player) {
        return editSessions.containsKey(player.getUniqueId());
    }

    /**
     * ウェルカムメッセージ送信
     */
    private void sendWelcomeMessage(Player player, MapData mapData) {
        player.sendMessage("");
        player.sendMessage("§6§l══════════════════════════════════════");
        player.sendMessage("§e§l   マップ設定モード: §f" + mapData.getDisplayName());
        player.sendMessage("§6§l══════════════════════════════════════");
        player.sendMessage("");
        player.sendMessage("§7以下のコマンドでマップを設定してください:");
        player.sendMessage("§e/map setteam <チーム名> §7- チームのベッド・スポーン設定");
        player.sendMessage("§e/map setgen <種類> §7- ジェネレーター設定");
        player.sendMessage("§e/map setshop §7- ショップ位置設定");
        player.sendMessage("§e/map setlobby §7- ロビースポーン設定");
        player.sendMessage("§e/map save §7- 保存して終了");
        player.sendMessage("§c/map cancel §7- 保存せずに終了");
        player.sendMessage("");
    }

    /**
     * 次のステップを案内
     */
    public void sendNextStep(Player player) {
        EditSession session = editSessions.get(player.getUniqueId());
        if (session == null) return;

        MapData mapData = session.getMapData();
        List<String> missing = mapData.validateSetup();

        player.sendMessage("");
        if (missing.isEmpty()) {
            player.sendMessage("§a§l[マップ設定] §a全ての必須項目が設定されました！");
            player.sendMessage("§7§o/map save で保存して完了するか、追加の設定を行ってください。");

            // セットアップ完了フラグを立てる
            mapData.setSetupComplete(true);
        } else {
            player.sendMessage("§e§l[マップ設定] §7残りの設定項目:");
            for (String item : missing) {
                player.sendMessage("  §c• " + item);
            }
            player.sendMessage("");
            player.sendMessage("§7進捗: §e" + mapData.getSetupProgress() + "%");
        }
        player.sendMessage("");
    }

    // ===== チーム設定 =====

    /**
     * チーム設定モードを開始
     */
    public void startTeamSetup(Player player, String teamName) {
        EditSession session = editSessions.get(player.getUniqueId());
        if (session == null) {
            player.sendMessage("§c編集セッションが見つかりません。/map edit <マップID> で開始してください。");
            return;
        }

        session.setCurrentTeam(teamName);
        session.setSetupStep(SetupStep.TEAM_BED);

        // チームがなければ作成
        MapData mapData = session.getMapData();
        if (!mapData.hasTeam(teamName)) {
            mapData.addTeam(new MapTeam(teamName));
        }

        player.sendMessage("");
        player.sendMessage("§e§l[チーム設定] §fチーム「" + teamName + "」の設定を開始します");
        player.sendMessage("");
        player.sendMessage("§7ステップ1: §fベッドの頭の部分を右クリックしてください");
        player.sendMessage("§7§o(ベッドの頭側（枕がある方）をクリック)");
        player.sendMessage("");
    }

    /**
     * ベッド位置を設定（ブロッククリック時に呼ばれる）
     */
    public boolean handleBlockClick(Player player, Block block) {
        EditSession session = editSessions.get(player.getUniqueId());
        if (session == null) return false;

        SetupStep step = session.getSetupStep();
        if (step == null) return false;

        MapData mapData = session.getMapData();
        String teamName = session.getCurrentTeam();

        switch (step) {
            case TEAM_BED:
                if (block.getType().name().contains("BED")) {
                    MapTeam team = mapData.getTeam(teamName);
                    if (team != null) {
                        // ベッドの色を変更
                        changeBedColor(block, teamName);

                        team.setBedLocation(block.getLocation());
                        player.sendMessage("§a✓ ベッド位置を設定し、色を変更しました");
                        player.sendMessage("");
                        player.sendMessage("§7ステップ2: §fスポーン位置に立って §e/map confirm §fと入力してください");
                        player.sendMessage("§7§o(プレイヤーがリスポーンする場所)");
                        session.setSetupStep(SetupStep.TEAM_SPAWN);
                        return true;
                    }
                } else {
                    player.sendMessage("§cベッドをクリックしてください");
                }
                return true;

            case GENERATOR_POS1:
                session.setTempLocation1(block.getLocation());
                player.sendMessage("§a✓ 角1を設定しました: " + formatLocation(block.getLocation()));
                player.sendMessage("");
                player.sendMessage("§7対角の角のブロックを右クリックしてください");
                session.setSetupStep(SetupStep.GENERATOR_POS2);
                return true;

            case GENERATOR_POS2:
                session.setTempLocation2(block.getLocation());
                player.sendMessage("§a✓ 角2を設定しました: " + formatLocation(block.getLocation()));

                // ジェネレーター作成
                String genTeam = session.getCurrentGeneratorTeam();
                Material genMaterial = session.getCurrentGeneratorMaterial();
                String genId = MapGenerator.generateId(genTeam, genMaterial);

                MapGenerator generator = new MapGenerator(genId, genTeam, genMaterial);
                generator.setCorner1(session.getTempLocation1());
                generator.setCorner2(session.getTempLocation2());
                mapData.addGenerator(generator);

                player.sendMessage("§a✓ ジェネレーターを追加しました: " + generator.getMaterialDisplayName() + " (" + genTeam + ")");
                session.setSetupStep(null);
                sendNextStep(player);
                return true;

            case SHOP_LOCATION:
                mapData.addShopLocation(session.getCurrentTeam(), block.getLocation().add(0.5, 0, 0.5));
                player.sendMessage("§a✓ 通常ショップ位置を追加しました");
                player.sendMessage("§7続けて他のショップ位置をクリックするか、§e/map confirm §7で完了");
                return true;

            case UPGRADE_SHOP_LOCATION:
                mapData.setUpgradeShopLocation(session.getCurrentTeam(), block.getLocation().add(0.5, 0, 0.5));
                player.sendMessage("§a✓ アップグレードショップ位置を設定しました");
                player.sendMessage("§e/map confirm §7で完了");
                return true;

            default:
                return false;
        }
    }

    /**
     * 確定コマンド処理
     */
    public void handleConfirm(Player player) {
        EditSession session = editSessions.get(player.getUniqueId());
        if (session == null) {
            player.sendMessage("§c編集セッションがありません");
            return;
        }

        SetupStep step = session.getSetupStep();
        if (step == null) {
            player.sendMessage("§c確定する項目がありません");
            return;
        }

        MapData mapData = session.getMapData();

        switch (step) {
            case TEAM_SPAWN:
                String teamName = session.getCurrentTeam();
                MapTeam team = mapData.getTeam(teamName);
                if (team != null) {
                    team.setSpawnLocation(player.getLocation());
                    player.sendMessage("§a✓ スポーン位置を設定しました");
                    player.sendMessage("§a§lチーム「" + teamName + "」の設定が完了しました！");
                    session.setSetupStep(null);
                    session.setCurrentTeam(null);
                    sendNextStep(player);
                }
                break;

            case SHOP_LOCATION:
                player.sendMessage("§a✓ 通常ショップ位置の設定を完了しました");
                session.setSetupStep(null);
                sendNextStep(player);
                break;

            case UPGRADE_SHOP_LOCATION:
                player.sendMessage("§a✓ アップグレードショップ位置の設定を完了しました");
                session.setSetupStep(null);
                sendNextStep(player);
                break;

            case LOBBY_SPAWN:
                mapData.setLobbySpawn(player.getLocation());
                player.sendMessage("§a✓ ロビースポーン位置を設定しました");
                session.setSetupStep(null);
                sendNextStep(player);
                break;

            default:
                player.sendMessage("§c確定できる項目がありません");
        }
    }

    // ===== ジェネレーター設定 =====

    /**
     * ジェネレーター設定モードを開始
     */
    public void startGeneratorSetup(Player player, String type, String teamName) {
        EditSession session = editSessions.get(player.getUniqueId());
        if (session == null) {
            player.sendMessage("§c編集セッションが見つかりません");
            return;
        }

        Material material = MapGenerator.getMaterialFromType(type);
        session.setCurrentGeneratorMaterial(material);
        session.setCurrentGeneratorTeam(teamName);
        session.setSetupStep(SetupStep.GENERATOR_POS1);

        player.sendMessage("");
        player.sendMessage("§e§l[ジェネレーター設定] §f" + new MapGenerator("", teamName, material).getMaterialDisplayName() + " (" + teamName + ")");
        player.sendMessage("");
        player.sendMessage("§7ジェネレーター範囲の1つ目の角をクリックしてください");
        player.sendMessage("");
    }

    // ===== ショップ設定 =====

    /**
     * ショップ設定モードを開始
     * @param isUpgradeShop true=アップグレードショップ(shop2), false=通常ショップ(shop1)
     */
    public void startShopSetup(Player player, String teamName, boolean isUpgradeShop) {
        EditSession session = editSessions.get(player.getUniqueId());
        if (session == null) {
            player.sendMessage("§c編集セッションが見つかりません");
            return;
        }

        session.setCurrentTeam(teamName);
        session.setUpgradeShop(isUpgradeShop);

        if (isUpgradeShop) {
            session.setSetupStep(SetupStep.UPGRADE_SHOP_LOCATION);
            player.sendMessage("");
            player.sendMessage("§e§l[アップグレードショップ設定] §fチーム「" + teamName + "」");
            player.sendMessage("");
            player.sendMessage("§7アップグレードショップ（スケルトン）を設置するブロックをクリック");
            player.sendMessage("§7完了したら §e/map confirm §7と入力してください");
            player.sendMessage("");
        } else {
            session.setSetupStep(SetupStep.SHOP_LOCATION);
            player.sendMessage("");
            player.sendMessage("§e§l[ショップ設定] §fチーム「" + teamName + "」");
            player.sendMessage("");
            player.sendMessage("§7通常ショップ（村人）を設置するブロックをクリック");
            player.sendMessage("§7複数設置可能です。完了したら §e/map confirm §7と入力してください");
            player.sendMessage("");
        }
    }

    // ===== ロビースポーン設定 =====

    /**
     * ロビースポーン設定モードを開始
     */
    public void startLobbySpawnSetup(Player player) {
        EditSession session = editSessions.get(player.getUniqueId());
        if (session == null) {
            player.sendMessage("§c編集セッションが見つかりません");
            return;
        }

        session.setSetupStep(SetupStep.LOBBY_SPAWN);

        player.sendMessage("");
        player.sendMessage("§e§l[ロビースポーン設定]");
        player.sendMessage("");
        player.sendMessage("§7ロビーのスポーン位置に立って §e/map confirm §7と入力してください");
        player.sendMessage("");
    }

    private String formatLocation(Location loc) {
        return String.format("(%.1f, %.1f, %.1f)", loc.getX(), loc.getY(), loc.getZ());
    }

    /**
     * ベッドの色をチームカラーに変更
     */
    private void changeBedColor(Block bedBlock, String teamName) {
        Material newBedMaterial = getTeamBedMaterial(teamName);
        if (newBedMaterial == null) return;

        // ベッドのデータを取得
        if (!(bedBlock.getBlockData() instanceof Bed)) return;
        Bed bedData = (Bed) bedBlock.getBlockData();
        Bed.Part part = bedData.getPart();
        org.bukkit.block.BlockFace facing = bedData.getFacing();

        // 頭と足の両方のブロックを取得
        Block headBlock, footBlock;
        if (part == Bed.Part.HEAD) {
            headBlock = bedBlock;
            footBlock = bedBlock.getRelative(facing.getOppositeFace());
        } else {
            footBlock = bedBlock;
            headBlock = bedBlock.getRelative(facing);
        }

        // 頭を変更
        headBlock.setType(newBedMaterial, false);
        Bed newHeadData = (Bed) headBlock.getBlockData();
        newHeadData.setPart(Bed.Part.HEAD);
        newHeadData.setFacing(facing);
        headBlock.setBlockData(newHeadData, false);

        // 足を変更
        footBlock.setType(newBedMaterial, false);
        Bed newFootData = (Bed) footBlock.getBlockData();
        newFootData.setPart(Bed.Part.FOOT);
        newFootData.setFacing(facing);
        footBlock.setBlockData(newFootData, false);
    }

    /**
     * チーム名に対応するベッドのMaterialを取得
     */
    private Material getTeamBedMaterial(String teamName) {
        switch (teamName) {
            case "レッド": return Material.RED_BED;
            case "ブルー": return Material.BLUE_BED;
            case "グリーン": return Material.LIME_BED;
            case "イエロー": return Material.YELLOW_BED;
            case "アクア": return Material.CYAN_BED;
            case "ホワイト": return Material.WHITE_BED;
            case "ピンク": return Material.PINK_BED;
            case "グレー": return Material.GRAY_BED;
            case "オレンジ": return Material.ORANGE_BED;
            case "パープル": return Material.PURPLE_BED;
            default: return null;
        }
    }

    // ===== 内部クラス =====

    /**
     * セットアップステップ
     */
    public enum SetupStep {
        TEAM_BED,
        TEAM_SPAWN,
        GENERATOR_POS1,
        GENERATOR_POS2,
        SHOP_LOCATION,
        UPGRADE_SHOP_LOCATION,
        LOBBY_SPAWN
    }

    /**
     * 編集セッション
     */
    public static class EditSession {
        private final MapData mapData;
        private SetupStep setupStep;
        private String currentTeam;
        private Material currentGeneratorMaterial;
        private String currentGeneratorTeam;
        private Location tempLocation1;
        private Location tempLocation2;
        private boolean isUpgradeShop;

        public EditSession(MapData mapData) {
            this.mapData = mapData;
        }

        public MapData getMapData() { return mapData; }
        public SetupStep getSetupStep() { return setupStep; }
        public void setSetupStep(SetupStep step) { this.setupStep = step; }
        public String getCurrentTeam() { return currentTeam; }
        public void setCurrentTeam(String team) { this.currentTeam = team; }
        public Material getCurrentGeneratorMaterial() { return currentGeneratorMaterial; }
        public void setCurrentGeneratorMaterial(Material material) { this.currentGeneratorMaterial = material; }
        public String getCurrentGeneratorTeam() { return currentGeneratorTeam; }
        public void setCurrentGeneratorTeam(String team) { this.currentGeneratorTeam = team; }
        public Location getTempLocation1() { return tempLocation1; }
        public void setTempLocation1(Location loc) { this.tempLocation1 = loc; }
        public Location getTempLocation2() { return tempLocation2; }
        public void setTempLocation2(Location loc) { this.tempLocation2 = loc; }
        public boolean isUpgradeShop() { return isUpgradeShop; }
        public void setUpgradeShop(boolean upgradeShop) { this.isUpgradeShop = upgradeShop; }
    }
}
