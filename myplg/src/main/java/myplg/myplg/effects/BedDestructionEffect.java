package myplg.myplg.effects;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;

/**
 * ベッド破壊時のエフェクト定義
 */
public enum BedDestructionEffect {
    
    /**
     * シンプル爆発 - 無料のデフォルトエフェクト
     * 控えめな爆発パーティクル
     */
    SIMPLE_EXPLOSION(
        "simple_explosion",
        "§7シンプル爆発",
        "§f控えめな爆発エフェクト",
        Material.GUNPOWDER,
        false, // 無料
        1 // 派手さレベル
    ),
    
    /**
     * 炎の爆発 - 課金エフェクト
     * 炎と火花の華やかなエフェクト
     */
    FLAME_BURST(
        "flame_burst",
        "§6炎の爆発",
        "§c炎と火花が舞い散る",
        Material.BLAZE_POWDER,
        true, // 課金
        2 // 派手さレベル
    ),
    
    /**
     * 魔法の消滅 - 課金エフェクト
     * エンチャント風の神秘的なエフェクト
     */
    MAGIC_VANISH(
        "magic_vanish",
        "§d魔法の消滅",
        "§5神秘的な魔法で消える",
        Material.ENDER_EYE,
        true, // 課金
        3 // 派手さレベル
    ),
    
    /**
     * 花火祭り - 課金エフェクト
     * 超派手な花火風エフェクト
     */
    FIREWORK_FESTIVAL(
        "firework_festival",
        "§e花火祭り",
        "§b超派手な花火が打ち上がる！",
        Material.FIREWORK_ROCKET,
        true, // 課金
        4 // 派手さレベル
    ),
    
    /**
     * 雷撃 - 課金エフェクト
     * 雷が落ちて衝撃波が広がる
     */
    LIGHTNING_STRIKE(
        "lightning_strike",
        "§b⚡雷撃",
        "§e雷神の怒りがベッドを破壊！",
        Material.LIGHTNING_ROD,
        true,
        5
    ),
    
    /**
     * 竜巻 - 課金エフェクト
     * 渦巻くパーティクルの竜巻
     */
    TORNADO(
        "tornado",
        "§7🌪️竜巻",
        "§f激しい竜巻がベッドを吹き飛ばす！",
        Material.FEATHER,
        true,
        5
    ),
    
    /**
     * ドラゴンブレス - 課金エフェクト
     * 紫色の炎が渦巻く
     */
    DRAGON_BREATH(
        "dragon_breath",
        "§5🐉ドラゴンブレス",
        "§dエンダードラゴンの息吹！",
        Material.DRAGON_BREATH,
        true,
        5
    ),
    
    /**
     * ブラックホール - 課金エフェクト
     * 全てを吸い込む闇
     */
    BLACK_HOLE(
        "black_hole",
        "§8🕳️ブラックホール",
        "§0全てを飲み込む暗黒の穴！",
        Material.ENDER_PEARL,
        true,
        5
    ),
    
    /**
     * 氷砕 - 課金エフェクト
     * 凍結して砕け散る
     */
    ICE_SHATTER(
        "ice_shatter",
        "§b❄氷砕",
        "§f凍りついたベッドが砕け散る！",
        Material.BLUE_ICE,
        true,
        4
    ),
    
    /**
     * ネザーポータル - 課金エフェクト
     * ポータルの渦
     */
    NETHER_PORTAL(
        "nether_portal",
        "§5🌀ネザーポータル",
        "§d異次元へ消え去る！",
        Material.OBSIDIAN,
        true,
        4
    ),
    
    /**
     * 虹爆発 - 課金エフェクト
     * 7色の虹が広がる
     */
    RAINBOW_BURST(
        "rainbow_burst",
        "§c虹§6色§e爆§a発§b💥",
        "§f美しい虹色の爆発！",
        Material.PRISMARINE_SHARD,
        true,
        5
    ),
    
    /**
     * ソウルアセンド - 課金エフェクト
     * 魂が昇天する
     */
    SOUL_ASCEND(
        "soul_ascend",
        "§3👻ソウルアセンド",
        "§b魂が天に昇っていく...",
        Material.SOUL_LANTERN,
        true,
        4
    );
    
    private final String id;
    private final String displayName;
    private final String description;
    private final Material icon;
    private final boolean premium;
    private final int flashinessLevel;
    
    BedDestructionEffect(String id, String displayName, String description, 
                         Material icon, boolean premium, int flashinessLevel) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
        this.premium = premium;
        this.flashinessLevel = flashinessLevel;
    }
    
    public String getId() {
        return id;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public Material getIcon() {
        return icon;
    }
    
    /**
     * 課金エフェクトかどうか
     */
    public boolean isPremium() {
        return premium;
    }
    
    /**
     * 派手さレベル (1-4)
     */
    public int getFlashinessLevel() {
        return flashinessLevel;
    }
    
    /**
     * IDからエフェクトを取得
     */
    public static BedDestructionEffect fromId(String id) {
        for (BedDestructionEffect effect : values()) {
            if (effect.getId().equals(id)) {
                return effect;
            }
        }
        return SIMPLE_EXPLOSION; // デフォルト
    }
    
    /**
     * デフォルトのエフェクトを取得（無料）
     */
    public static BedDestructionEffect getDefault() {
        return SIMPLE_EXPLOSION;
    }
}
