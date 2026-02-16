package myplg.myplg.listeners;

import myplg.myplg.DebugBook;
import myplg.myplg.Generator;
import myplg.myplg.PvPGame;
import myplg.myplg.Team;
import myplg.myplg.gui.DebugGUI;
import myplg.myplg.effects.BedDestructionEffect;
import myplg.myplg.effects.BedDestructionEffectManager;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * デバッグGUIのクリックイベント処理
 */
public class DebugGUIListener implements Listener {

    private final PvPGame plugin;
    private final DebugGUI debugGUI;
    
    // ジェネレーターの元の速度を保存
    private final Map<String, Integer> originalGeneratorSpeeds = new HashMap<>();
    private boolean generatorsPaused = false;
    
    // ダミーベッドの位置を追跡（プレイヤーUUID -> ベッド位置）
    private final Map<UUID, Location> dummyBedLocations = new HashMap<>();
    
    // プレビュー用に選択されているエフェクト
    private final Map<UUID, BedDestructionEffect> previewEffects = new HashMap<>();

    public DebugGUIListener(PvPGame plugin) {
        this.plugin = plugin;
        this.debugGUI = new DebugGUI(plugin);
    }

    public DebugGUI getDebugGUI() {
        return debugGUI;
    }

    /**
     * ダミーベッドの破壊イベント（最高優先度で先に処理）
     */
    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        // ベッドでない場合はスキップ
        if (!block.getType().name().contains("BED") || block.getType().name().equals("BEDROCK")) {
            return;
        }

        Player player = event.getPlayer();

        // ダミーベッドかチェック（ゲーム実行中でもデバッグモードならダミーベッドを優先処理）
        if (isDummyBed(block)) {
            event.setCancelled(true);
            event.setDropItems(false);
            handleDummyBedBreak(player, block);
        }
    }

    /**
     * ブロックがダミーベッドかチェック
     */
    private boolean isDummyBed(Block block) {
        for (Location bedLoc : dummyBedLocations.values()) {
            if (isSameBed(block, bedLoc.getBlock())) {
                return true;
            }
        }
        return false;
    }

    /**
     * デバッグコンパスの右クリックでGUIを開く
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // 右クリックかどうか
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        // デバッグコンパスかどうか
        if (!DebugBook.isDebugCompass(item)) {
            return;
        }

        // デバッグモードかどうか
        if (!plugin.getGameManager().isDebugMode()) {
            player.sendMessage("§c§l[DEBUG] §cデバッグモードでのみ使用可能です。");
            return;
        }

        event.setCancelled(true);
        debugGUI.openMainGUI(player);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.0f);
    }

    /**
     * GUI内のクリックイベント処理
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        // エフェクトプレビューGUI（§d§lで始まる）を先にチェック
        if (title.equals(DebugGUI.EFFECT_PREVIEW_GUI_TITLE)) {
            event.setCancelled(true);
            handleEffectPreviewClick(player, event.getSlot());
            return;
        }

        // デバッグGUIかどうか（§c§lで始まる）
        if (!title.startsWith("§c§l")) return;

        // メインGUI
        if (title.equals(DebugGUI.MAIN_GUI_TITLE)) {
            event.setCancelled(true);
            handleMainGUIClick(player, event.getSlot(), event.getClick());
            return;
        }

        // ベッド破壊チーム選択
        if (title.equals(DebugGUI.TEAM_SELECT_BED_TITLE)) {
            event.setCancelled(true);
            handleBedSelectClick(player, event.getSlot());
            return;
        }

        // テレポートチーム選択
        if (title.equals(DebugGUI.TEAM_SELECT_TP_TITLE)) {
            event.setCancelled(true);
            handleTPSelectClick(player, event.getSlot());
            return;
        }

        // チーム変更選択
        if (title.equals("§c§lチーム変更 - チーム選択")) {
            event.setCancelled(true);
            handleTeamChangeClick(player, event.getSlot());
            return;
        }

        // ジェネレーター設定
        if (title.equals(DebugGUI.GENERATOR_GUI_TITLE)) {
            event.setCancelled(true);
            handleGeneratorGUIClick(player, event.getSlot());
            return;
        }
    }

    /**
     * メインGUIのクリック処理
     */
    private void handleMainGUIClick(Player player, int slot, ClickType clickType) {
        switch (slot) {
            // ===== ゲーム制御 =====
            case 10: // ゲーム終了
                player.closeInventory();
                player.performCommand("end");
                break;

            case 11: // ベッド全破壊
                destroyAllBeds(player);
                break;

            case 12: // 特定チームベッド破壊
                debugGUI.openTeamSelectForBed(player);
                break;

            case 13: // タイマー操作
                handleTimerControl(player, clickType);
                break;

            // ===== プレイヤー =====
            case 19: // 無敵モード
                toggleInvincibility(player);
                debugGUI.openMainGUI(player); // GUIを更新
                break;

            case 20: // 透明化
                toggleInvisibility(player);
                debugGUI.openMainGUI(player); // GUIを更新
                break;

            case 21: // チーム変更
                debugGUI.openTeamSelectForChange(player);
                break;

            case 22: // テレポート
                debugGUI.openTeamSelectForTP(player);
                break;

            // ===== アイテム =====
            case 28: // リソース取得
                giveResources(player);
                break;

            case 29: // フル装備
                giveFullArmor(player);
                break;

            case 30: // 全ツール
                giveAllTools(player);
                break;

            case 31: // 特殊アイテム
                giveSpecialItems(player);
                break;

            // ===== ジェネレーター =====
            case 37: // ジェネレーター設定
                debugGUI.openGeneratorGUI(player);
                break;

            case 38: // ジェネレーター一時停止/再開
                toggleGenerators(player);
                break;

            case 39: // ジェネレーター速度2倍
                changeGeneratorSpeed(player, 0.5); // interval半分 = 速度2倍
                break;

            case 40: // ジェネレーター速度リセット
                resetGeneratorSpeed(player);
                break;

            // ===== エフェクトプレビュー =====
            case 42: // ダミーベッド配置/削除
                if (clickType.isShiftClick()) {
                    removeDummyBed(player);
                } else {
                    placeDummyBed(player);
                }
                break;

            case 43: // エフェクトプレビュー
                debugGUI.openEffectPreviewGUI(player);
                break;
        }
    }

    /**
     * ベッド破壊チーム選択のクリック処理
     */
    private void handleBedSelectClick(Player player, int slot) {
        if (slot == 22) { // 戻る
            debugGUI.openMainGUI(player);
            return;
        }

        if (slot >= 10 && slot <= 16) {
            int index = slot - 10;
            List<Team> teams = new ArrayList<>(plugin.getGameManager().getTeams().values());
            if (index < teams.size()) {
                Team team = teams.get(index);
                destroyTeamBed(player, team);
                debugGUI.openTeamSelectForBed(player); // GUIを更新
            }
        }
    }

    /**
     * テレポートチーム選択のクリック処理
     */
    private void handleTPSelectClick(Player player, int slot) {
        if (slot == 22) { // 戻る
            debugGUI.openMainGUI(player);
            return;
        }

        if (slot >= 10 && slot <= 16) {
            int index = slot - 10;
            List<Team> teams = new ArrayList<>(plugin.getGameManager().getTeams().values());
            if (index < teams.size()) {
                Team team = teams.get(index);
                teleportToTeamSpawn(player, team);
            }
        }
    }

    /**
     * チーム変更選択のクリック処理
     */
    private void handleTeamChangeClick(Player player, int slot) {
        if (slot == 22) { // 戻る
            debugGUI.openMainGUI(player);
            return;
        }

        if (slot >= 10 && slot <= 16) {
            int index = slot - 10;
            List<Team> teams = new ArrayList<>(plugin.getGameManager().getTeams().values());
            if (index < teams.size()) {
                Team team = teams.get(index);
                changePlayerTeam(player, team);
            }
        }
    }

    /**
     * ジェネレーターGUIのクリック処理
     */
    private void handleGeneratorGUIClick(Player player, int slot) {
        switch (slot) {
            case 10: // 全停止/再開
                toggleGenerators(player);
                break;
            case 11: // 速度 x0.5
                changeGeneratorSpeed(player, 2.0); // interval2倍 = 速度0.5倍
                break;
            case 12: // 速度 x2
                changeGeneratorSpeed(player, 0.5);
                break;
            case 13: // 速度 x4
                changeGeneratorSpeed(player, 0.25);
                break;
            case 14: // 速度リセット
                resetGeneratorSpeed(player);
                break;
            case 22: // 戻る
                debugGUI.openMainGUI(player);
                break;
        }
    }

    // ===== 機能実装メソッド =====

    private void destroyAllBeds(Player player) {
        int destroyed = 0;
        for (Team team : plugin.getGameManager().getTeams().values()) {
            if (plugin.getScoreboardManager().isBedAlive(team.getName())) {
                plugin.getScoreboardManager().setBedStatus(team.getName(), false);
                destroyed++;
            }
        }

        // ENDモードチェック
        plugin.getEndModeManager().checkAllBedsDestroyed();

        player.sendMessage("§c§l[DEBUG] §e" + destroyed + "チームのベッドを破壊しました。");
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.5f, 1.0f);
        
        // 全プレイヤーに通知
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.equals(player)) {
                p.sendMessage("§c§l[DEBUG] §e全てのベッドが破壊されました！");
            }
        }
    }

    private void destroyTeamBed(Player player, Team team) {
        if (!plugin.getScoreboardManager().isBedAlive(team.getName())) {
            player.sendMessage("§c§l[DEBUG] §cそのチームのベッドは既に破壊されています。");
            return;
        }

        plugin.getScoreboardManager().setBedStatus(team.getName(), false);
        plugin.getEndModeManager().checkAllBedsDestroyed();

        player.sendMessage("§c§l[DEBUG] §e" + team.getName() + "チームのベッドを破壊しました。");
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.0f);
    }

    private void handleTimerControl(Player player, ClickType clickType) {
        // TODO: タイマー機能が実装されていれば連携
        if (clickType == ClickType.LEFT) {
            player.sendMessage("§c§l[DEBUG] §e30秒スキップ（タイマー機能と連携予定）");
        } else if (clickType == ClickType.RIGHT) {
            player.sendMessage("§c§l[DEBUG] §e1分スキップ（タイマー機能と連携予定）");
        } else if (clickType.isShiftClick()) {
            player.sendMessage("§c§l[DEBUG] §eタイマーリセット（タイマー機能と連携予定）");
        }
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f);
    }

    private void toggleInvincibility(Player player) {
        boolean newState = !player.isInvulnerable();
        player.setInvulnerable(newState);
        player.sendMessage("§c§l[DEBUG] §e無敵モード: " + (newState ? "§aON" : "§cOFF"));
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.5f, newState ? 1.5f : 0.5f);
    }

    private void toggleInvisibility(Player player) {
        boolean newState = !player.isInvisible();
        player.setInvisible(newState);
        
        if (newState) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));
        } else {
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
        }
        
        player.sendMessage("§c§l[DEBUG] §e透明化: " + (newState ? "§aON" : "§cOFF"));
        player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 0.5f, 1.0f);
    }

    private void changePlayerTeam(Player player, Team newTeam) {
        String oldTeam = plugin.getGameManager().getPlayerTeam(player.getUniqueId());
        
        // 古いチームから削除
        if (oldTeam != null) {
            Team old = plugin.getGameManager().getTeam(oldTeam);
            if (old != null) {
                old.removeMember(player.getUniqueId());
            }
        }
        
        // 新しいチームに追加
        newTeam.addMember(player.getUniqueId());
        plugin.getGameManager().setPlayerTeam(player.getUniqueId(), newTeam.getName());
        
        // スコアボード更新（全プレイヤー）
        plugin.getScoreboardManager().updateAllScoreboards();
        
        player.sendMessage("§c§l[DEBUG] §e" + newTeam.getName() + "チームに変更しました。");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.0f);
        
        debugGUI.openTeamSelectForChange(player);
    }

    private void teleportToTeamSpawn(Player player, Team team) {
        Location spawn = team.getSpawnLocation();
        if (spawn == null) {
            player.sendMessage("§c§l[DEBUG] §cそのチームのスポーン地点が設定されていません。");
            return;
        }

        player.teleport(spawn);
        player.sendMessage("§c§l[DEBUG] §e" + team.getName() + "チームのスポーン地点にテレポートしました。");
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.0f);
    }

    private void giveResources(Player player) {
        player.getInventory().addItem(
                new ItemStack(Material.IRON_INGOT, 64),
                new ItemStack(Material.GOLD_INGOT, 64),
                new ItemStack(Material.DIAMOND, 64),
                new ItemStack(Material.EMERALD, 64)
        );
        player.sendMessage("§c§l[DEBUG] §eリソースを取得しました（各64個）");
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.0f);
    }

    private void giveFullArmor(Player player) {
        player.getInventory().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
        player.getInventory().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
        player.getInventory().setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
        player.getInventory().setBoots(new ItemStack(Material.DIAMOND_BOOTS));
        player.getInventory().addItem(new ItemStack(Material.DIAMOND_SWORD));
        player.sendMessage("§c§l[DEBUG] §eダイヤフル装備を取得しました。");
        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_DIAMOND, 1.0f, 1.0f);
    }

    private void giveAllTools(Player player) {
        player.getInventory().addItem(
                new ItemStack(Material.DIAMOND_PICKAXE),
                new ItemStack(Material.DIAMOND_AXE),
                new ItemStack(Material.DIAMOND_SHOVEL),
                new ItemStack(Material.SHEARS)
        );
        player.sendMessage("§c§l[DEBUG] §e全ツールを取得しました。");
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.0f);
    }

    private void giveSpecialItems(Player player) {
        // TNT
        player.getInventory().addItem(new ItemStack(Material.TNT, 8));
        
        // ファイヤーボール
        ItemStack fireball = new ItemStack(Material.FIRE_CHARGE, 4);
        ItemMeta fireballMeta = fireball.getItemMeta();
        if (fireballMeta != null) {
            fireballMeta.setDisplayName("§c§lファイヤーボール");
            fireball.setItemMeta(fireballMeta);
        }
        player.getInventory().addItem(fireball);
        
        // エンダーパール
        player.getInventory().addItem(new ItemStack(Material.ENDER_PEARL, 4));
        
        // ブリッジエッグ
        ItemStack bridgeEgg = new ItemStack(Material.EGG, 8);
        ItemMeta eggMeta = bridgeEgg.getItemMeta();
        if (eggMeta != null) {
            eggMeta.setDisplayName("§a§lブリッジエッグ");
            List<String> lore = new ArrayList<>();
            lore.add("§7投げた方向に橋を作る");
            eggMeta.setLore(lore);
            eggMeta.setCustomModelData(1001); // BridgeBuilderListenerの識別子
            bridgeEgg.setItemMeta(eggMeta);
        }
        player.getInventory().addItem(bridgeEgg);
        
        player.sendMessage("§c§l[DEBUG] §e特殊アイテムを取得しました。");
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.0f);
    }

    private void toggleGenerators(Player player) {
        if (generatorsPaused) {
            // 再開
            plugin.getGeneratorManager().startAllGenerators();
            generatorsPaused = false;
            player.sendMessage("§c§l[DEBUG] §aジェネレーターを再開しました。");
        } else {
            // 停止
            plugin.getGeneratorManager().stopAllGenerators();
            generatorsPaused = true;
            player.sendMessage("§c§l[DEBUG] §cジェネレーターを停止しました。");
        }
        player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, 0.5f, 1.0f);
    }

    private void changeGeneratorSpeed(Player player, double multiplier) {
        // 元の速度を保存（初回のみ）
        if (originalGeneratorSpeeds.isEmpty()) {
            for (Generator gen : plugin.getGeneratorManager().getGenerators().values()) {
                originalGeneratorSpeeds.put(gen.getId(), gen.getSpawnInterval());
            }
        }

        // 速度変更（ファイルには保存しない = 一時的な変更）
        for (Generator gen : plugin.getGeneratorManager().getGenerators().values()) {
            int originalSpeed = originalGeneratorSpeeds.getOrDefault(gen.getId(), gen.getSpawnInterval());
            int newInterval = (int) (originalSpeed * multiplier);
            if (newInterval < 1) newInterval = 1; // 最小値
            plugin.getGeneratorManager().updateGeneratorInterval(gen.getId(), newInterval, false);
        }

        String speedText = multiplier < 1 ? "x" + (int)(1/multiplier) : "x" + String.format("%.1f", 1/multiplier);
        player.sendMessage("§c§l[DEBUG] §eジェネレーター速度を " + speedText + " に変更しました。");
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.5f);
    }

    private void resetGeneratorSpeed(Player player) {
        if (originalGeneratorSpeeds.isEmpty()) {
            player.sendMessage("§c§l[DEBUG] §c速度は既に初期状態です。");
            return;
        }

        for (Generator gen : plugin.getGeneratorManager().getGenerators().values()) {
            Integer originalSpeed = originalGeneratorSpeeds.get(gen.getId());
            if (originalSpeed != null) {
                plugin.getGeneratorManager().updateGeneratorInterval(gen.getId(), originalSpeed, false);
            }
        }

        originalGeneratorSpeeds.clear();
        player.sendMessage("§c§l[DEBUG] §eジェネレーター速度を初期状態に戻しました。");
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 0.5f);
    }

    // ===== エフェクトプレビュー関連 =====

    /**
     * エフェクトプレビューGUIのクリック処理
     */
    private void handleEffectPreviewClick(Player player, int slot) {
        if (slot == 49) { // 戻る
            debugGUI.openMainGUI(player);
            return;
        }

        // エフェクト選択スロット（全12種類対応）
        int[] effectSlots = {
            10, 11, 12, 13, 14, 15, 16,  // 1行目 (7スロット)
            19, 20, 21, 22, 23, 24, 25,  // 2行目 (7スロット)
            28, 29, 30, 31, 32, 33, 34   // 3行目 (7スロット)
        };
        BedDestructionEffect[] effects = BedDestructionEffect.values();

        for (int i = 0; i < effectSlots.length && i < effects.length; i++) {
            if (slot == effectSlots[i]) {
                BedDestructionEffect effect = effects[i];
                selectAndPreviewEffect(player, effect);
                return;
            }
        }
    }

    /**
     * エフェクトを選択してプレビュー再生（クリックで即座にプレビュー）
     */
    private void selectAndPreviewEffect(Player player, BedDestructionEffect effect) {
        // 課金エフェクトはadminのみ使用可能
        if (effect.isPremium() && !player.hasPermission("myplg.admin") && !player.isOp()) {
            player.sendMessage("§c§l[エフェクト] §cこのエフェクトは管理者のみ使用できます。");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
            return;
        }

        BedDestructionEffectManager effectManager = plugin.getBedEffectManager();
        if (effectManager == null) {
            player.sendMessage("§c§l[DEBUG] §cエフェクトマネージャーが初期化されていません。");
            plugin.getLogger().warning("[DEBUG] BedEffectManagerがnullです");
            return;
        }

        // プレビュー用エフェクトを保存（ダミーベッド破壊時に使用）
        previewEffects.put(player.getUniqueId(), effect);

        // GUIを閉じる
        player.closeInventory();

        // エフェクト再生位置を決定
        Location effectLoc;
        if (dummyBedLocations.containsKey(player.getUniqueId())) {
            // ダミーベッドの位置でプレビュー
            effectLoc = dummyBedLocations.get(player.getUniqueId()).clone().add(0.5, 0.5, 0.5);
        } else {
            // プレイヤーの前方3ブロックでプレビュー
            effectLoc = player.getLocation().add(player.getLocation().getDirection().multiply(3));
            effectLoc.setY(player.getLocation().getY() + 0.5);
        }

        // 少し遅延してからエフェクトを再生（GUIが閉じた後に見えるように）
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // エフェクトを即時再生
            effectManager.playEffect(effect, effectLoc, Color.RED);
            plugin.getLogger().info("[DEBUG] プレビュー再生: " + effect.getDisplayName() + " at " + effectLoc);

            player.sendMessage("§c§l[DEBUG] §e" + effect.getDisplayName() + " §eをプレビュー！");
            if (dummyBedLocations.containsKey(player.getUniqueId())) {
                player.sendMessage("§7ダミーベッドを壊してもこのエフェクトが再生されます。");
            }
        }, 5L);

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
    }

    /**
     * ダミーベッドを配置
     */
    private void placeDummyBed(Player player) {
        // 既存のダミーベッドがあれば削除
        removeDummyBed(player);

        // プレイヤーの前方3ブロックの位置
        Location playerLoc = player.getLocation();
        Location bedLoc = playerLoc.clone().add(playerLoc.getDirection().multiply(3));
        bedLoc.setY(playerLoc.getY());

        // ベッドの位置を整える（ブロック座標）
        bedLoc.setX(Math.floor(bedLoc.getX()));
        bedLoc.setZ(Math.floor(bedLoc.getZ()));
        bedLoc.setY(Math.floor(bedLoc.getY()));

        BlockFace facing = getPlayerFacing(player);
        Block footBlock = bedLoc.getBlock();
        Block headBlock = footBlock.getRelative(facing);

        // 空気ブロックでない場合は配置できない
        if (footBlock.getType() != Material.AIR || headBlock.getType() != Material.AIR) {
            player.sendMessage("§c§l[DEBUG] §cベッドを配置するスペースがありません。");
            return;
        }

        // FOOTパートを先に配置（BlockDataを先に準備）
        Bed footData = (Bed) Material.RED_BED.createBlockData();
        footData.setPart(Bed.Part.FOOT);
        footData.setFacing(facing);
        footBlock.setBlockData(footData);

        // HEADパートを配置
        Bed headData = (Bed) Material.RED_BED.createBlockData();
        headData.setPart(Bed.Part.HEAD);
        headData.setFacing(facing);
        headBlock.setBlockData(headData);

        // 位置を保存（footの位置）
        dummyBedLocations.put(player.getUniqueId(), footBlock.getLocation());

        // デフォルトエフェクトを設定
        if (!previewEffects.containsKey(player.getUniqueId())) {
            previewEffects.put(player.getUniqueId(), BedDestructionEffect.SIMPLE_EXPLOSION);
        }

        BedDestructionEffect currentEffect = previewEffects.get(player.getUniqueId());
        player.sendMessage("§c§l[DEBUG] §aダミーベッドを配置しました。");
        player.sendMessage("§7現在のエフェクト: §e" + currentEffect.getDisplayName());
        player.sendMessage("§7壊すとエフェクトが再生されます。エフェクトプレビューGUIで変更可能。");
        player.playSound(player.getLocation(), Sound.BLOCK_WOOD_PLACE, 1.0f, 1.0f);
    }

    /**
     * ダミーベッドを削除
     */
    private void removeDummyBed(Player player) {
        Location bedLoc = dummyBedLocations.remove(player.getUniqueId());
        if (bedLoc == null) {
            return;
        }

        Block bedBlock = bedLoc.getBlock();

        // ベッドの両パートを削除（物理更新なしでアイテム化防止）
        if (bedBlock.getType().name().contains("BED") && !bedBlock.getType().name().equals("BEDROCK")) {
            // 先に隣接するベッドパートを特定
            Block adjacentBed = null;
            for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
                Block adjacent = bedBlock.getRelative(face);
                if (adjacent.getType().name().contains("BED") && !adjacent.getType().name().equals("BEDROCK")) {
                    adjacentBed = adjacent;
                    break;
                }
            }

            // 両方同時に削除
            if (adjacentBed != null) {
                adjacentBed.setType(Material.AIR, false);
            }
            bedBlock.setType(Material.AIR, false);
        }

        player.sendMessage("§c§l[DEBUG] §eダミーベッドを削除しました。");
    }

    /**
     * プレイヤーの向きからBlockFaceを取得
     */
    private BlockFace getPlayerFacing(Player player) {
        float yaw = player.getLocation().getYaw();
        if (yaw < 0) yaw += 360;

        if (yaw >= 315 || yaw < 45) {
            return BlockFace.SOUTH;
        } else if (yaw >= 45 && yaw < 135) {
            return BlockFace.WEST;
        } else if (yaw >= 135 && yaw < 225) {
            return BlockFace.NORTH;
        } else {
            return BlockFace.EAST;
        }
    }

    /**
     * ダミーベッドの破壊を処理（BlockBreakEventから呼び出される）
     */
    public void handleDummyBedBreak(Player player, Block block) {
        // このブロックがダミーベッドかチェック
        UUID ownerToRemove = null;
        Location bedLocation = null;

        for (Map.Entry<UUID, Location> entry : dummyBedLocations.entrySet()) {
            Location bedLoc = entry.getValue();
            if (isSameBed(block, bedLoc.getBlock())) {
                ownerToRemove = entry.getKey();
                bedLocation = bedLoc;
                break;
            }
        }

        if (ownerToRemove == null || bedLocation == null) {
            return;
        }

        // エフェクトを再生
        BedDestructionEffect effect = previewEffects.getOrDefault(player.getUniqueId(),
            BedDestructionEffect.SIMPLE_EXPLOSION);

        BedDestructionEffectManager effectManager = plugin.getBedEffectManager();
        if (effectManager != null) {
            // ベッドの中心位置でエフェクトを再生
            Location effectLoc = block.getLocation().add(0.5, 0.5, 0.5);
            effectManager.playEffect(effect, effectLoc, Color.RED);
            plugin.getLogger().info("[DEBUG] エフェクト再生: " + effect.getDisplayName() + " at " + effectLoc);
        } else {
            plugin.getLogger().warning("[DEBUG] BedEffectManagerがnullです");
        }

        // ベッドを削除（両パート、物理更新なしでアイテム化を防止）
        Block footBlock = bedLocation.getBlock();

        // 先に両パートを特定してから削除（物理更新でドロップを防ぐ）
        Block headBlock = null;
        for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
            Block adjacent = footBlock.getRelative(face);
            if (adjacent.getType().name().contains("BED") && !adjacent.getType().name().equals("BEDROCK")) {
                headBlock = adjacent;
                break;
            }
        }

        // 両方同時に削除（applyPhysics=falseでアイテム化防止）
        if (headBlock != null) {
            headBlock.setType(Material.AIR, false);
        }
        footBlock.setType(Material.AIR, false);

        // 壊したブロック自体も確認
        if (block.getType().name().contains("BED") && !block.getType().name().equals("BEDROCK")) {
            block.setType(Material.AIR, false);
        }

        dummyBedLocations.remove(ownerToRemove);

        // ベッド破壊音を再生（通常のベッドと同じ）
        Location soundLoc = block.getLocation();
        player.getWorld().playSound(soundLoc, Sound.BLOCK_WOOD_BREAK, 1.0f, 1.0f);
        player.getWorld().playSound(soundLoc, Sound.BLOCK_WOOL_BREAK, 1.0f, 0.8f);

        player.sendMessage("§c§l[DEBUG] §e" + effect.getDisplayName() + " §eエフェクトを再生！");
    }

    /**
     * 同じベッドかチェック
     */
    private boolean isSameBed(Block block1, Block block2) {
        if (block1.getLocation().equals(block2.getLocation())) {
            return true;
        }
        
        // 隣接するベッドパートもチェック
        for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
            Block adjacent = block1.getRelative(face);
            if (adjacent.getLocation().equals(block2.getLocation())) {
                return true;
            }
        }
        return false;
    }

    /**
     * ダミーベッドの位置を取得
     */
    public Map<UUID, Location> getDummyBedLocations() {
        return dummyBedLocations;
    }

    /**
     * ゲーム終了時にデバッグ状態をリセット
     * resetGameStateから呼び出される
     */
    public void reset() {
        // ジェネレーター速度の記録をクリア（新しいゲームでは新しい値を使う）
        originalGeneratorSpeeds.clear();
        generatorsPaused = false;

        // ダミーベッドをすべて削除（物理更新なしでアイテム化防止）
        for (Location bedLoc : new ArrayList<>(dummyBedLocations.values())) {
            Block bedBlock = bedLoc.getBlock();
            if (bedBlock.getType().name().contains("BED") && !bedBlock.getType().name().equals("BEDROCK")) {
                // 先に隣接するベッドパートを特定
                Block adjacentBed = null;
                for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
                    Block adjacent = bedBlock.getRelative(face);
                    if (adjacent.getType().name().contains("BED") && !adjacent.getType().name().equals("BEDROCK")) {
                        adjacentBed = adjacent;
                        break;
                    }
                }
                // 両方同時に削除
                if (adjacentBed != null) {
                    adjacentBed.setType(Material.AIR, false);
                }
                bedBlock.setType(Material.AIR, false);
            }
        }
        dummyBedLocations.clear();
        previewEffects.clear();

        // プレイヤーの無敵・透明状態をリセット
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isInvulnerable()) {
                player.setInvulnerable(false);
            }
            if (player.isInvisible()) {
                player.setInvisible(false);
                player.removePotionEffect(PotionEffectType.INVISIBILITY);
            }
        }

        plugin.getLogger().info("[DEBUG] デバッグ状態をリセットしました。");
    }
}
