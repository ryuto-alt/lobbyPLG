package myplg.myplg.effects;

import myplg.myplg.PvPGame;
import myplg.myplg.AdminUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * プレイヤーのベッド破壊エフェクト設定を管理
 * データはYMLファイルに保存（将来的にDB移行可能）
 */
public class PlayerEffectDataManager {
    
    private final PvPGame plugin;
    private final File dataFile;
    private FileConfiguration config;
    
    // プレイヤーの選択エフェクト
    private final Map<UUID, BedDestructionEffect> selectedEffects = new HashMap<>();
    
    // プレイヤーの解放済みエフェクト
    private final Map<UUID, Set<BedDestructionEffect>> unlockedEffects = new HashMap<>();
    
    public PlayerEffectDataManager(PvPGame plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "player_effects.yml");
        loadData();
    }
    
    /**
     * データをファイルからロード
     */
    public void loadData() {
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("player_effects.ymlの作成に失敗: " + e.getMessage());
            }
        }
        
        config = YamlConfiguration.loadConfiguration(dataFile);
        
        // 全プレイヤーのデータをロード
        if (config.contains("players")) {
            for (String uuidStr : config.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    
                    // 選択エフェクト
                    String selectedId = config.getString("players." + uuidStr + ".selected", "simple_explosion");
                    selectedEffects.put(uuid, BedDestructionEffect.fromId(selectedId));
                    
                    // 解放済みエフェクト
                    List<String> unlockedList = config.getStringList("players." + uuidStr + ".unlocked");
                    Set<BedDestructionEffect> unlocked = new HashSet<>();
                    for (String id : unlockedList) {
                        unlocked.add(BedDestructionEffect.fromId(id));
                    }
                    // 無料エフェクトは常に解放
                    unlocked.add(BedDestructionEffect.SIMPLE_EXPLOSION);
                    unlockedEffects.put(uuid, unlocked);
                    
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("無効なUUID: " + uuidStr);
                }
            }
        }
        
        plugin.getLogger().info("プレイヤーエフェクトデータをロードしました（" + selectedEffects.size() + "人）");
    }
    
    /**
     * データをファイルに保存
     */
    public void saveData() {
        for (Map.Entry<UUID, BedDestructionEffect> entry : selectedEffects.entrySet()) {
            String path = "players." + entry.getKey().toString();
            config.set(path + ".selected", entry.getValue().getId());
            
            Set<BedDestructionEffect> unlocked = unlockedEffects.get(entry.getKey());
            if (unlocked != null) {
                List<String> unlockedIds = new ArrayList<>();
                for (BedDestructionEffect effect : unlocked) {
                    unlockedIds.add(effect.getId());
                }
                config.set(path + ".unlocked", unlockedIds);
            }
        }
        
        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("player_effects.ymlの保存に失敗: " + e.getMessage());
        }
    }
    
    /**
     * プレイヤーの選択エフェクトを取得
     * lobbyで変更された可能性があるため、毎回ファイルから読み込む
     */
    public BedDestructionEffect getSelectedEffect(UUID playerId) {
        // ファイルから最新の設定を読み込む（lobbyとの同期のため）
        FileConfiguration freshConfig = YamlConfiguration.loadConfiguration(dataFile);
        String path = "players." + playerId.toString() + ".selected";

        if (freshConfig.contains(path)) {
            String selectedId = freshConfig.getString(path, "simple_explosion");
            BedDestructionEffect effect = BedDestructionEffect.fromId(selectedId);
            // キャッシュも更新
            selectedEffects.put(playerId, effect);
            return effect;
        }

        return selectedEffects.getOrDefault(playerId, BedDestructionEffect.getDefault());
    }

    /**
     * プレイヤーの選択エフェクトを取得（Player）
     */
    public BedDestructionEffect getSelectedEffect(Player player) {
        return getSelectedEffect(player.getUniqueId());
    }
    
    /**
     * プレイヤーの選択エフェクトを設定
     */
    public void setSelectedEffect(UUID playerId, BedDestructionEffect effect) {
        selectedEffects.put(playerId, effect);
        saveData();
    }
    
    /**
     * プレイヤーの選択エフェクトを設定（Player）
     */
    public void setSelectedEffect(Player player, BedDestructionEffect effect) {
        setSelectedEffect(player.getUniqueId(), effect);
    }
    
    /**
     * エフェクトが解放されているかチェック
     */
    public boolean isUnlocked(UUID playerId, BedDestructionEffect effect) {
        // 無料エフェクトは常に解放
        if (!effect.isPremium()) {
            return true;
        }
        
        // adminは全て解放
        if (AdminUtil.isAdmin(playerId)) {
            return true;
        }
        
        Set<BedDestructionEffect> unlocked = unlockedEffects.get(playerId);
        return unlocked != null && unlocked.contains(effect);
    }
    
    /**
     * エフェクトが解放されているかチェック（Player）
     */
    public boolean isUnlocked(Player player, BedDestructionEffect effect) {
        return isUnlocked(player.getUniqueId(), effect);
    }
    
    /**
     * エフェクトを解放
     */
    public void unlockEffect(UUID playerId, BedDestructionEffect effect) {
        Set<BedDestructionEffect> unlocked = unlockedEffects.computeIfAbsent(playerId, k -> new HashSet<>());
        unlocked.add(effect);
        // 無料エフェクトも追加
        unlocked.add(BedDestructionEffect.SIMPLE_EXPLOSION);
        saveData();
    }
    
    /**
     * エフェクトを解放（Player）
     */
    public void unlockEffect(Player player, BedDestructionEffect effect) {
        unlockEffect(player.getUniqueId(), effect);
    }
    
    /**
     * プレイヤーの解放済みエフェクト一覧を取得
     */
    public Set<BedDestructionEffect> getUnlockedEffects(UUID playerId) {
        Set<BedDestructionEffect> unlocked = new HashSet<>();
        
        // 無料エフェクトは常に含む
        unlocked.add(BedDestructionEffect.SIMPLE_EXPLOSION);
        
        // adminは全て解放
        if (AdminUtil.isAdmin(playerId)) {
            unlocked.addAll(Arrays.asList(BedDestructionEffect.values()));
            return unlocked;
        }
        
        // プレイヤーの解放済みを追加
        Set<BedDestructionEffect> playerUnlocked = unlockedEffects.get(playerId);
        if (playerUnlocked != null) {
            unlocked.addAll(playerUnlocked);
        }
        
        return unlocked;
    }
    
    /**
     * プレイヤーの解放済みエフェクト一覧を取得（Player）
     */
    public Set<BedDestructionEffect> getUnlockedEffects(Player player) {
        return getUnlockedEffects(player.getUniqueId());
    }
    
    /**
     * プレイヤーが使用可能かチェック（選択かつ解放済み）
     */
    public boolean canUseEffect(UUID playerId, BedDestructionEffect effect) {
        return isUnlocked(playerId, effect);
    }
    
    /**
     * プラグイン無効化時に呼び出し
     */
    public void onDisable() {
        saveData();
    }
}
