package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
import myplg.myplg.gui.TeamEditConfirmGUI;
import myplg.myplg.gui.TeamEditGUI;
import myplg.myplg.gui.TerritoryRadiusGUI;
import myplg.myplg.map.MapTeam;
import myplg.myplg.map.TeamEditToolManager;
import myplg.myplg.map.TeamEditToolManager.EditMode;
import myplg.myplg.map.TeamEditToolManager.TeamEditSession;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/**
 * チーム編集ツールのリスナー
 */
public class TeamEditListener implements Listener {

    private final PvPGame plugin;
    private final TeamEditGUI teamEditGUI;
    private final TeamEditConfirmGUI confirmGUI;
    private final TerritoryRadiusGUI radiusGUI;

    public TeamEditListener(PvPGame plugin) {
        this.plugin = plugin;
        this.teamEditGUI = new TeamEditGUI(plugin);
        this.confirmGUI = new TeamEditConfirmGUI(plugin);
        this.radiusGUI = new TerritoryRadiusGUI(plugin);
    }

    /**
     * アイテム右クリック/左クリック処理
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        TeamEditToolManager manager = plugin.getTeamEditToolManager();

        if (!manager.isEditing(player)) {
            return;
        }

        // オフハンドは無視
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }

        ItemStack item = event.getItem();
        TeamEditSession session = manager.getSession(player);

        // エディットツール（羊毛）の右クリック → メニューGUI
        if (manager.isEditTool(item)) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);
                teamEditGUI.openMenu(player);
                return;
            }
        }

        // 終了ツール（バリアブロック）の右クリック → 終了確認GUI
        if (manager.isExitTool(item)) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);
                confirmGUI.openConfirmGUI(player);
                return;
            }
        }

        // 設定モード中の位置設定
        if (session != null && session.getEditMode() != EditMode.NONE) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);
                handlePositionSet(player, session, event);
            }
        }
    }

    /**
     * 位置設定処理
     */
    private void handlePositionSet(Player player, TeamEditSession session, PlayerInteractEvent event) {
        Location loc;

        // ブロックをクリックした場合はそのブロックの上面、空気の場合はプレイヤーの位置
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock != null) {
            loc = clickedBlock.getLocation().add(0.5, 1, 0.5);
        } else {
            loc = player.getLocation();
        }

        MapTeam team = session.getTeam();
        if (team == null) {
            player.sendMessage("§cチームが見つかりません。");
            return;
        }

        EditMode mode = session.getEditMode();

        switch (mode) {
            case SPAWN:
                team.setSpawnLocation(loc);
                // スポーン位置にパーティクルを表示
                loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 20, 0.5, 0.5, 0.5, 0);
                player.sendMessage("§a§l[チーム編集] §fスポーン位置を設定しました: " + formatLocation(loc));
                break;

            case BED:
                // 実際にベッドを設置
                Location bedLoc = placeBed(loc, team.getName(), player);
                if (bedLoc != null) {
                    team.setBedLocation(bedLoc);
                    player.sendMessage("§a§l[チーム編集] §fベッドを設置しました: " + formatLocation(bedLoc));
                } else {
                    player.sendMessage("§c§l[エラー] §fベッドの設置に失敗しました");
                }
                break;

            case ITEM_SHOP:
                // 既存のショップ村人を削除してから新しいのをスポーン（プレイヤーの方を向く）
                float yaw = getYawToFacePlayer(loc, player);
                loc.setYaw(yaw);
                spawnItemShopVillager(loc, team.getName());
                team.setShopLocation(loc);
                player.sendMessage("§a§l[チーム編集] §fアイテムショップをスポーンしました: " + formatLocation(loc));
                break;

            case UPGRADE_SHOP:
                // アップグレードショップをスポーン（プレイヤーの方を向く）
                float upgradeYaw = getYawToFacePlayer(loc, player);
                loc.setYaw(upgradeYaw);
                spawnUpgradeShopVillager(loc, team.getName());
                team.setUpgradeShopLocation(loc);
                player.sendMessage("§a§l[チーム編集] §fアップグレードショップをスポーンしました: " + formatLocation(loc));
                break;

            case IRON_GENERATOR:
                team.setIronGeneratorLocation(loc);
                // ジェネレーター位置にパーティクルを表示
                loc.getWorld().spawnParticle(Particle.CLOUD, loc, 30, 0.3, 0.3, 0.3, 0);
                player.sendMessage("§a§l[チーム編集] §fアイアンジェネレーター位置を設定しました: " + formatLocation(loc));
                break;

            case GOLD_GENERATOR:
                team.setGoldGeneratorLocation(loc);
                // ジェネレーター位置にパーティクルを表示
                loc.getWorld().spawnParticle(Particle.FLAME, loc, 30, 0.3, 0.3, 0.3, 0);
                player.sendMessage("§a§l[チーム編集] §fゴールドジェネレーター位置を設定しました: " + formatLocation(loc));
                break;

            default:
                return;
        }

        // 設定モードを解除
        session.setEditMode(EditMode.NONE);
        player.sendMessage("§7羊毛を右クリックしてメニューに戻れます");
    }

    /**
     * ベッドを設置
     */
    private Location placeBed(Location loc, String teamName, Player player) {
        Material bedMaterial = getBedMaterialForTeam(teamName);
        BlockFace facing = player.getFacing().getOppositeFace();

        // 頭部分の位置を取得
        Block headBlock = loc.getBlock();

        // 足部分の位置を計算（プレイヤーが向いている方向と逆方向に足がくる）
        Block footBlock = headBlock.getRelative(facing.getOppositeFace());

        // 設置できるかチェック
        if (!canPlaceBed(headBlock, footBlock)) {
            return null;
        }

        // 頭部分を設置
        headBlock.setType(bedMaterial);
        if (headBlock.getBlockData() instanceof Bed) {
            Bed headData = (Bed) headBlock.getBlockData();
            headData.setPart(Bed.Part.HEAD);
            headData.setFacing(facing);
            headBlock.setBlockData(headData);
        }

        // 足部分を設置
        footBlock.setType(bedMaterial);
        if (footBlock.getBlockData() instanceof Bed) {
            Bed footData = (Bed) footBlock.getBlockData();
            footData.setPart(Bed.Part.FOOT);
            footData.setFacing(facing);
            footBlock.setBlockData(footData);
        }

        return headBlock.getLocation();
    }

    /**
     * ベッドを設置できるかチェック
     */
    private boolean canPlaceBed(Block head, Block foot) {
        return (head.getType() == Material.AIR || head.getType().name().endsWith("_BED")) &&
               (foot.getType() == Material.AIR || foot.getType().name().endsWith("_BED"));
    }

    /**
     * チーム名に対応するベッドの色を取得
     */
    private Material getBedMaterialForTeam(String teamName) {
        switch (teamName) {
            case "レッド": return Material.RED_BED;
            case "ブルー": return Material.BLUE_BED;
            case "グリーン": return Material.GREEN_BED;
            case "イエロー": return Material.YELLOW_BED;
            case "アクア": return Material.CYAN_BED;
            case "ホワイト": return Material.WHITE_BED;
            case "ピンク": return Material.PINK_BED;
            case "グレー": return Material.GRAY_BED;
            case "オレンジ": return Material.ORANGE_BED;
            case "パープル": return Material.PURPLE_BED;
            default: return Material.WHITE_BED;
        }
    }

    /**
     * プレイヤーの方を向くYaw角度を計算
     */
    private float getYawToFacePlayer(Location loc, Player player) {
        double dx = player.getLocation().getX() - loc.getX();
        double dz = player.getLocation().getZ() - loc.getZ();
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        return yaw;
    }

    /**
     * アイテムショップ村人をスポーン（既存のショップシステムと互換）
     */
    private void spawnItemShopVillager(Location loc, String teamName) {
        NamespacedKey shopTypeKey = new NamespacedKey(plugin, "shop_type");
        NamespacedKey shopTeamKey = new NamespacedKey(plugin, "shop_team");

        // 周囲の既存ショップ村人を削除
        removeNearbyShopEntities(loc, "shop1");

        // 村人をスポーン
        Villager villager = (Villager) loc.getWorld().spawnEntity(loc, EntityType.VILLAGER);
        villager.setCustomName("§a§lショップ §7(" + teamName + ")");
        villager.setCustomNameVisible(true);
        villager.setAI(false);
        villager.setInvulnerable(true);
        villager.setSilent(true);
        villager.setRemoveWhenFarAway(false);

        // PersistentDataContainerにショップ情報を設定（既存システムと互換）
        villager.getPersistentDataContainer().set(shopTypeKey, PersistentDataType.STRING, "shop1");
        villager.getPersistentDataContainer().set(shopTeamKey, PersistentDataType.STRING, teamName);

        // 向きを設定
        villager.setRotation(loc.getYaw(), 0);

        // ShopDataManagerに保存
        plugin.getShopDataManager().saveShopVillager("shop1", villager.getUniqueId(), loc, teamName);

        plugin.getLogger().info("アイテムショップ村人をスポーン: " + teamName + " at " + formatLocation(loc));
    }

    /**
     * アップグレードショップ（スケルトン）をスポーン（既存のショップシステムと互換）
     */
    private void spawnUpgradeShopVillager(Location loc, String teamName) {
        NamespacedKey shopTypeKey = new NamespacedKey(plugin, "shop_type");
        NamespacedKey shopTeamKey = new NamespacedKey(plugin, "shop_team");

        // 周囲の既存ショップを削除
        removeNearbyShopEntities(loc, "shop2");

        // スケルトンをスポーン（アップグレードショップ）
        Skeleton skeleton = (Skeleton) loc.getWorld().spawnEntity(loc, EntityType.SKELETON);
        skeleton.setCustomName("§6§lアップグレード §7(" + teamName + ")");
        skeleton.setCustomNameVisible(true);
        skeleton.setAI(false);
        skeleton.setInvulnerable(true);
        skeleton.setSilent(true);
        skeleton.setRemoveWhenFarAway(false);

        // 武器を外す
        skeleton.getEquipment().clear();

        // PersistentDataContainerにショップ情報を設定（既存システムと互換）
        skeleton.getPersistentDataContainer().set(shopTypeKey, PersistentDataType.STRING, "shop2");
        skeleton.getPersistentDataContainer().set(shopTeamKey, PersistentDataType.STRING, teamName);

        // 向きを設定
        skeleton.setRotation(loc.getYaw(), 0);

        // ShopDataManagerに保存
        plugin.getShopDataManager().saveShopVillager("shop2", skeleton.getUniqueId(), loc, teamName);

        plugin.getLogger().info("アップグレードショップをスポーン: " + teamName + " at " + formatLocation(loc));
    }

    /**
     * 指定位置付近の既存ショップエンティティを削除
     */
    private void removeNearbyShopEntities(Location loc, String shopType) {
        NamespacedKey shopTypeKey = new NamespacedKey(plugin, "shop_type");

        for (Entity entity : loc.getWorld().getNearbyEntities(loc, 3, 3, 3)) {
            if (entity instanceof Villager || entity instanceof Skeleton) {
                String type = entity.getPersistentDataContainer().get(shopTypeKey, PersistentDataType.STRING);
                if (type != null && type.equals(shopType)) {
                    entity.remove();
                }
            }
        }
    }

    /**
     * GUIクリック処理
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        String title = event.getView().getTitle();

        // チーム編集メニュー
        if (title.startsWith(TeamEditGUI.GUI_TITLE_PREFIX)) {
            event.setCancelled(true);
            handleTeamEditGUIClick(player, event);
            return;
        }

        // 終了確認GUI
        if (title.equals(TeamEditConfirmGUI.GUI_TITLE)) {
            event.setCancelled(true);
            handleConfirmGUIClick(player, event);
            return;
        }

        // 領域半径設定GUI
        if (title.equals(TerritoryRadiusGUI.GUI_TITLE)) {
            event.setCancelled(true);
            handleRadiusGUIClick(player, event);
            return;
        }
    }

    /**
     * チーム編集メニューのクリック処理
     */
    private void handleTeamEditGUIClick(Player player, InventoryClickEvent event) {
        TeamEditToolManager manager = plugin.getTeamEditToolManager();
        TeamEditSession session = manager.getSession(player);
        if (session == null) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        int slot = event.getSlot();

        switch (slot) {
            case 10: // スポーン位置設定
                session.setEditMode(EditMode.SPAWN);
                player.closeInventory();
                player.sendMessage("");
                player.sendMessage("§e§l[スポーン位置設定] §fブロックまたは空気を右クリックして位置を設定してください");
                player.sendMessage("");
                break;

            case 11: // ベッド位置設定
                session.setEditMode(EditMode.BED);
                player.closeInventory();
                player.sendMessage("");
                player.sendMessage("§d§l[ベッド位置設定] §fブロックまたは空気を右クリックして位置を設定してください");
                player.sendMessage("");
                break;

            case 12: // 領域設定
                radiusGUI.openRadiusGUI(player);
                break;

            case 14: // アイテムショップ位置設定
                session.setEditMode(EditMode.ITEM_SHOP);
                player.closeInventory();
                player.sendMessage("");
                player.sendMessage("§2§l[アイテムショップ位置設定] §fブロックまたは空気を右クリックして位置を設定してください");
                player.sendMessage("");
                break;

            case 15: // アップグレードショップ位置設定
                session.setEditMode(EditMode.UPGRADE_SHOP);
                player.closeInventory();
                player.sendMessage("");
                player.sendMessage("§b§l[アップグレードショップ位置設定] §fブロックまたは空気を右クリックして位置を設定してください");
                player.sendMessage("");
                break;

            case 19: // アイアンジェネレーター位置設定
                session.setEditMode(EditMode.IRON_GENERATOR);
                player.closeInventory();
                player.sendMessage("");
                player.sendMessage("§f§l[アイアンジェネレーター位置設定] §fブロックまたは空気を右クリックして位置を設定してください");
                player.sendMessage("");
                break;

            case 20: // ゴールドジェネレーター位置設定
                session.setEditMode(EditMode.GOLD_GENERATOR);
                player.closeInventory();
                player.sendMessage("");
                player.sendMessage("§6§l[ゴールドジェネレーター位置設定] §fブロックまたは空気を右クリックして位置を設定してください");
                player.sendMessage("");
                break;

            case 22: // 閉じる
                player.closeInventory();
                break;
        }
    }

    /**
     * 終了確認GUIのクリック処理
     */
    private void handleConfirmGUIClick(Player player, InventoryClickEvent event) {
        TeamEditToolManager manager = plugin.getTeamEditToolManager();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        int slot = event.getSlot();

        if (slot == 11) {
            // 「はい」- 保存して終了
            player.closeInventory();
            manager.endEditSession(player, true);
        } else if (slot == 15) {
            // 「いいえ」- メニューに戻る
            player.closeInventory();
        }
    }

    /**
     * 領域半径設定GUIのクリック処理
     */
    private void handleRadiusGUIClick(Player player, InventoryClickEvent event) {
        TeamEditToolManager manager = plugin.getTeamEditToolManager();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        int slot = event.getSlot();

        switch (slot) {
            case 10: // -5
                radiusGUI.updateRadius(player, -5);
                break;
            case 11: // -1
                radiusGUI.updateRadius(player, -1);
                break;
            case 15: // +1
                radiusGUI.updateRadius(player, 1);
                break;
            case 16: // +5
                radiusGUI.updateRadius(player, 5);
                break;
            case 21: // 確定
                radiusGUI.confirmRadius(player);
                break;
            case 23: // 戻る
                manager.stopParticleTask(player);
                teamEditGUI.openMenu(player);
                break;
        }
    }

    /**
     * プレイヤー退出時にセッションをクリーンアップ
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        TeamEditToolManager manager = plugin.getTeamEditToolManager();
        if (manager.isEditing(player)) {
            manager.endEditSession(player, false);
        }
    }

    /**
     * 座標をフォーマット
     */
    private String formatLocation(Location loc) {
        return String.format("%.1f, %.1f, %.1f", loc.getX(), loc.getY(), loc.getZ());
    }
}
