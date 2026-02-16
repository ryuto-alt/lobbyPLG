package lobby.lobby.gui;

import lobby.lobby.Lobby;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * ベッド破壊エフェクト選択GUI（lobbyプラグイン）
 * myplgのplayer_effects.ymlを読み書きする
 * 全12種類のエフェクトを表示
 */
public class BedEffectSelectGUI {

    public static final String GUI_TITLE = "§b§lベッド破壊エフェクト選択";
    
    // エフェクト定義（myplgと完全同期）
    public enum BedEffect {
        // 無料エフェクト
        SIMPLE_EXPLOSION("simple_explosion", "§7シンプル爆発", "§f控えめな爆発エフェクト", Material.GUNPOWDER, false, 1),
        
        // 課金エフェクト（レベル2-4）
        FLAME_BURST("flame_burst", "§6炎の爆発", "§c螺旋状の炎と火柱が舞い上がる", Material.BLAZE_POWDER, true, 2),
        MAGIC_VANISH("magic_vanish", "§d魔法の消滅", "§5回転魔法陣と多重螺旋", Material.ENDER_EYE, true, 3),
        FIREWORK_FESTIVAL("firework_festival", "§e花火祭り", "§b連続花火とフィナーレ同時爆発！", Material.FIREWORK_ROCKET, true, 4),
        ICE_SHATTER("ice_shatter", "§b❄氷砕", "§f凍りついたベッドが砕け散る！", Material.BLUE_ICE, true, 4),
        NETHER_PORTAL("nether_portal", "§5🌀ネザーポータル", "§d異次元へ消え去る渦！", Material.OBSIDIAN, true, 4),
        SOUL_ASCEND("soul_ascend", "§3👻ソウルアセンド", "§b魂が天に昇っていく...", Material.SOUL_LANTERN, true, 4),
        
        // 課金エフェクト（レベル5 - 超派手）
        LIGHTNING_STRIKE("lightning_strike", "§b⚡雷撃", "§e雷神の怒りがベッドを破壊！", Material.LIGHTNING_ROD, true, 5),
        TORNADO("tornado", "§7🌪️竜巻", "§f激しい竜巻がベッドを吹き飛ばす！", Material.FEATHER, true, 5),
        DRAGON_BREATH("dragon_breath", "§5🐉ドラゴンブレス", "§dエンダードラゴンの息吹！", Material.DRAGON_BREATH, true, 5),
        BLACK_HOLE("black_hole", "§8🕳️ブラックホール", "§0全てを飲み込む暗黒の穴！", Material.ENDER_PEARL, true, 5),
        RAINBOW_BURST("rainbow_burst", "§c虹§6色§e爆§a発§b💥", "§f美しい虹色の爆発！", Material.PRISMARINE_SHARD, true, 5);
        
        private final String id;
        private final String displayName;
        private final String description;
        private final Material icon;
        private final boolean premium;
        private final int flashinessLevel;
        
        BedEffect(String id, String displayName, String description, Material icon, boolean premium, int flashinessLevel) {
            this.id = id;
            this.displayName = displayName;
            this.description = description;
            this.icon = icon;
            this.premium = premium;
            this.flashinessLevel = flashinessLevel;
        }
        
        public String getId() { return id; }
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public Material getIcon() { return icon; }
        public boolean isPremium() { return premium; }
        public int getFlashinessLevel() { return flashinessLevel; }
        
        public static BedEffect fromId(String id) {
            for (BedEffect e : values()) {
                if (e.getId().equals(id)) return e;
            }
            return SIMPLE_EXPLOSION;
        }
    }
    
    private final Lobby plugin;
    private File effectDataFile;
    private FileConfiguration effectConfig;
    private final Random random = new Random();
    
    public BedEffectSelectGUI(Lobby plugin) {
        this.plugin = plugin;
        loadEffectData();
    }
    
    /**
     * myplgのplayer_effects.ymlをロード
     */
    private void loadEffectData() {
        effectDataFile = new File(Bukkit.getPluginsFolder(), "myplg/player_effects.yml");
        if (!effectDataFile.exists()) {
            try {
                effectDataFile.getParentFile().mkdirs();
                effectDataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("player_effects.ymlの作成に失敗: " + e.getMessage());
            }
        }
        effectConfig = YamlConfiguration.loadConfiguration(effectDataFile);
    }
    
    /**
     * 設定を保存
     */
    private void saveEffectData() {
        try {
            effectConfig.save(effectDataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("player_effects.ymlの保存に失敗: " + e.getMessage());
        }
    }
    
    /**
     * エフェクト選択GUIを開く
     */
    public void openEffectSelectGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, GUI_TITLE);
        
        String selectedId = getSelectedEffect(player.getUniqueId());
        Set<String> unlockedIds = getUnlockedEffects(player.getUniqueId());
        boolean isAdmin = plugin.getAdminDataManager().isAdmin(player);
        
        // エフェクトを配置（2行に6つずつ）
        // 1行目: 基本〜中級エフェクト（スロット10-16）
        // 2行目: 上級エフェクト（スロット28-34）
        int[] row1Slots = {10, 12, 14, 16, 19, 21, 23};  // 7つ
        int[] row2Slots = {28, 30, 32, 34, 37};           // 5つ
        
        BedEffect[] effects = BedEffect.values();
        
        // 1行目配置
        for (int i = 0; i < Math.min(7, effects.length); i++) {
            BedEffect effect = effects[i];
            boolean isUnlocked = !effect.isPremium() || unlockedIds.contains(effect.getId()) || isAdmin;
            boolean isSelected = effect.getId().equals(selectedId);
            
            ItemStack item = createEffectItem(effect, isUnlocked, isSelected, isAdmin);
            gui.setItem(row1Slots[i], item);
        }
        
        // 2行目配置
        for (int i = 7; i < effects.length; i++) {
            BedEffect effect = effects[i];
            boolean isUnlocked = !effect.isPremium() || unlockedIds.contains(effect.getId()) || isAdmin;
            boolean isSelected = effect.getId().equals(selectedId);
            
            ItemStack item = createEffectItem(effect, isUnlocked, isSelected, isAdmin);
            gui.setItem(row2Slots[i - 7], item);
        }
        
        // ヘッダー装飾
        ItemStack header = new ItemStack(Material.NETHER_STAR);
        ItemMeta headerMeta = header.getItemMeta();
        if (headerMeta != null) {
            headerMeta.setDisplayName("§e§l✦ ベッド破壊エフェクト ✦");
            headerMeta.setLore(Arrays.asList(
                "§7敵チームのベッドを破壊した時に",
                "§7表示されるエフェクトを選べます！",
                "",
                "§a● 緑色§7: 選択中",
                "§f○ 白色§7: 解放済み",
                "§c✖ 赤色§7: 未解放"
            ));
            header.setItemMeta(headerMeta);
        }
        gui.setItem(4, header);
        
        // プレビューボタン
        ItemStack previewButton = new ItemStack(Material.SPYGLASS);
        ItemMeta previewMeta = previewButton.getItemMeta();
        if (previewMeta != null) {
            previewMeta.setDisplayName("§d§lプレビュー");
            previewMeta.setLore(Arrays.asList(
                "§7エフェクトをクリックすると",
                "§7目の前でプレビューできます！",
                "",
                "§e右クリック§7でプレビュー"
            ));
            previewButton.setItemMeta(previewMeta);
        }
        gui.setItem(49, previewButton);
        
        // 戻るボタン
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§c§l戻る");
            backMeta.setLore(Arrays.asList("§7プロフィールに戻る"));
            backButton.setItemMeta(backMeta);
        }
        gui.setItem(45, backButton);
        
        // カテゴリ分け表示
        ItemStack basicLabel = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta basicMeta = basicLabel.getItemMeta();
        if (basicMeta != null) {
            basicMeta.setDisplayName("§a§l基本〜中級エフェクト");
            basicLabel.setItemMeta(basicMeta);
        }
        gui.setItem(9, basicLabel);
        gui.setItem(25, basicLabel);
        
        ItemStack advancedLabel = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta advancedMeta = advancedLabel.getItemMeta();
        if (advancedMeta != null) {
            advancedMeta.setDisplayName("§5§l上級エフェクト §7(★★★★★)");
            advancedLabel.setItemMeta(advancedMeta);
        }
        gui.setItem(27, advancedLabel);
        gui.setItem(43, advancedLabel);
        
        // 背景を埋める
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(" ");
            filler.setItemMeta(fillerMeta);
        }
        
        for (int i = 0; i < 54; i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, filler);
            }
        }
        
        player.openInventory(gui);
    }
    
    /**
     * エフェクトアイテムを作成
     */
    private ItemStack createEffectItem(BedEffect effect, boolean isUnlocked, boolean isSelected, boolean isAdmin) {
        ItemStack item = new ItemStack(effect.getIcon());
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            // 名前（状態に応じてプレフィックス）
            String prefix;
            if (isSelected) {
                prefix = "§a§l● ";
            } else if (isUnlocked) {
                prefix = "§f○ ";
            } else {
                prefix = "§c✖ ";
            }
            meta.setDisplayName(prefix + effect.getDisplayName());
            
            // 説明
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(effect.getDescription());
            lore.add("");
            
            // 派手さレベル表示（星5段階）
            StringBuilder stars = new StringBuilder("§e派手さ: ");
            for (int i = 0; i < 5; i++) {
                if (i < effect.getFlashinessLevel()) {
                    stars.append("§6★");
                } else {
                    stars.append("§8☆");
                }
            }
            lore.add(stars.toString());
            lore.add("");
            
            // 状態表示
            if (isSelected) {
                lore.add("§a§l✓ 現在選択中");
            } else if (isUnlocked) {
                lore.add("§e左クリック§7で選択");
                lore.add("§d右クリック§7でプレビュー");
            } else {
                lore.add("§c§l✖ 未解放");
                lore.add("§7課金で解放できます");
                if (isAdmin) {
                    lore.add("");
                    lore.add("§d§l[管理者特権で使用可能]");
                }
            }
            
            // 無料/課金表示
            lore.add("");
            if (!effect.isPremium()) {
                lore.add("§a§l【無料】");
            } else {
                lore.add("§6§l【プレミアム】");
            }
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * エフェクトのプレビューを再生
     */
    public void playPreview(Player player, BedEffect effect) {
        Location loc = player.getLocation().add(player.getLocation().getDirection().multiply(3));
        loc.setY(player.getLocation().getY());
        
        player.sendMessage("§d§l[プレビュー] §f" + effect.getDisplayName() + " §7を再生中...");
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.5f);
        
        // エフェクトに応じたプレビューを再生
        switch (effect) {
            case SIMPLE_EXPLOSION:
                playSimpleExplosionPreview(loc, player);
                break;
            case FLAME_BURST:
                playFlameBurstPreview(loc, player);
                break;
            case MAGIC_VANISH:
                playMagicVanishPreview(loc, player);
                break;
            case FIREWORK_FESTIVAL:
                playFireworkFestivalPreview(loc, player);
                break;
            case LIGHTNING_STRIKE:
                playLightningStrikePreview(loc, player);
                break;
            case TORNADO:
                playTornadoPreview(loc, player);
                break;
            case DRAGON_BREATH:
                playDragonBreathPreview(loc, player);
                break;
            case BLACK_HOLE:
                playBlackHolePreview(loc, player);
                break;
            case ICE_SHATTER:
                playIceShatterPreview(loc, player);
                break;
            case NETHER_PORTAL:
                playNetherPortalPreview(loc, player);
                break;
            case RAINBOW_BURST:
                playRainbowBurstPreview(loc, player);
                break;
            case SOUL_ASCEND:
                playSoulAscendPreview(loc, player);
                break;
        }
    }
    
    // ===== プレビューエフェクト実装 =====
    
    private void playSimpleExplosionPreview(Location loc, Player player) {
        loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 2, 0.3, 0.3, 0.3, 0);
        loc.getWorld().spawnParticle(Particle.SMOKE, loc, 30, 1.0, 1.0, 1.0, 0.05);
        loc.getWorld().spawnParticle(Particle.FLAME, loc, 20, 0.5, 0.5, 0.5, 0.03);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.0f);
        
        Particle.DustOptions dust = new Particle.DustOptions(Color.WHITE, 2.5f);
        new BukkitRunnable() {
            int ticks = 0;
            double radius = 0;
            @Override
            public void run() {
                if (ticks >= 30) {
                    cancel();
                    return;
                }
                radius += 0.2;
                for (int i = 0; i < 16; i++) {
                    double angle = Math.toRadians(i * 22.5);
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(x, 0.2, z), 1, 0, 0, 0, 0, dust);
                }
                ticks += 3;
            }
        }.runTaskTimer(plugin, 0L, 3L);
    }
    
    private void playFlameBurstPreview(Location loc, Player player) {
        loc.getWorld().playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.8f);
        loc.getWorld().spawnParticle(Particle.FLAME, loc, 60, 1.0, 1.0, 1.0, 0.1);
        loc.getWorld().spawnParticle(Particle.LAVA, loc, 15, 0.8, 0.5, 0.8, 0);
        
        Particle.DustOptions orangeDust = new Particle.DustOptions(Color.ORANGE, 2.0f);
        new BukkitRunnable() {
            int ticks = 0;
            double angle = 0;
            @Override
            public void run() {
                if (ticks >= 40) {
                    cancel();
                    return;
                }
                for (double y = 0; y < 2.5; y += 0.3) {
                    double a1 = angle + y * 1.5;
                    double a2 = a1 + Math.PI;
                    double r = 0.6;
                    loc.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(Math.cos(a1)*r, y, Math.sin(a1)*r), 1, 0, 0, 0, 0);
                    loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(Math.cos(a2)*r, y, Math.sin(a2)*r), 1, 0, 0, 0, 0, orangeDust);
                }
                angle += 0.3;
                ticks += 4;
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }
    
    private void playMagicVanishPreview(Location loc, Player player) {
        loc.getWorld().playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
        loc.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.8f);
        loc.getWorld().spawnParticle(Particle.ENCHANT, loc, 50, 1.0, 1.0, 1.0, 0.5);
        loc.getWorld().spawnParticle(Particle.PORTAL, loc, 40, 0.5, 0.5, 0.5, 0.5);
        
        Particle.DustOptions purpleDust = new Particle.DustOptions(Color.fromRGB(148, 0, 211), 1.8f);
        new BukkitRunnable() {
            int ticks = 0;
            double angle = 0;
            @Override
            public void run() {
                if (ticks >= 50) {
                    cancel();
                    return;
                }
                // 魔法陣
                for (int i = 0; i < 12; i++) {
                    double a = angle + (i * Math.PI / 6);
                    double x = Math.cos(a) * 1.2;
                    double z = Math.sin(a) * 1.2;
                    loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(x, 0.1, z), 1, 0, 0, 0, 0, purpleDust);
                }
                // 螺旋
                for (int s = 0; s < 3; s++) {
                    double h = (ticks / 50.0) * 3;
                    double a = angle + (s * Math.PI * 2 / 3) + h * 1.5;
                    double r = 0.8 - (h / 3) * 0.5;
                    loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(Math.cos(a)*r, h, Math.sin(a)*r), 1, 0, 0, 0, 0);
                }
                angle += 0.15;
                ticks += 4;
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }
    
    private void playFireworkFestivalPreview(Location loc, Player player) {
        loc.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.0f);
        loc.getWorld().spawnParticle(Particle.FIREWORK, loc, 50, 1.5, 1.5, 1.5, 0.1);
        loc.getWorld().spawnParticle(Particle.FLASH, loc, 1, 0, 0, 0, 0);
        
        Color[] colors = {Color.RED, Color.YELLOW, Color.LIME, Color.AQUA, Color.FUCHSIA};
        new BukkitRunnable() {
            int burst = 0;
            @Override
            public void run() {
                if (burst >= 4) {
                    cancel();
                    return;
                }
                Location burstLoc = loc.clone().add((random.nextDouble()-0.5)*2, random.nextDouble()*2, (random.nextDouble()-0.5)*2);
                Particle.DustOptions dust = new Particle.DustOptions(colors[burst % colors.length], 2.5f);
                for (int i = 0; i < 30; i++) {
                    double theta = random.nextDouble() * Math.PI * 2;
                    double phi = random.nextDouble() * Math.PI;
                    double r = 0.8 + random.nextDouble() * 0.3;
                    double x = r * Math.sin(phi) * Math.cos(theta);
                    double y = r * Math.cos(phi);
                    double z = r * Math.sin(phi) * Math.sin(theta);
                    loc.getWorld().spawnParticle(Particle.DUST, burstLoc.clone().add(x, y, z), 1, 0, 0, 0, 0, dust);
                }
                loc.getWorld().playSound(burstLoc, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 0.6f, 1.0f + random.nextFloat()*0.3f);
                burst++;
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }
    
    private void playLightningStrikePreview(Location loc, Player player) {
        loc.getWorld().strikeLightningEffect(loc);
        loc.getWorld().playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.8f);
        loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc, 80, 1.5, 1.5, 1.5, 0.3);
        loc.getWorld().spawnParticle(Particle.FLASH, loc, 2, 0.3, 0.3, 0.3, 0);
        
        Particle.DustOptions yellowDust = new Particle.DustOptions(Color.YELLOW, 2.0f);
        new BukkitRunnable() {
            int ticks = 0;
            double radius = 0;
            @Override
            public void run() {
                if (ticks >= 30) {
                    cancel();
                    return;
                }
                radius += 0.25;
                for (int i = 0; i < 24; i++) {
                    double angle = Math.toRadians(i * 15);
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(x, 0.1, z), 1, 0, 0, 0, 0, yellowDust);
                    loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc.clone().add(x, 0.1, z), 1, 0, 0, 0, 0);
                }
                ticks += 4;
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }
    
    private void playTornadoPreview(Location loc, Player player) {
        loc.getWorld().playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.5f);
        
        Particle.DustOptions grayDust = new Particle.DustOptions(Color.GRAY, 1.5f);
        new BukkitRunnable() {
            int ticks = 0;
            double rotation = 0;
            @Override
            public void run() {
                if (ticks >= 60) {
                    cancel();
                    return;
                }
                double maxHeight = Math.min(ticks * 0.12, 4);
                for (double y = 0; y < maxHeight; y += 0.3) {
                    double radius = 0.3 + (y / 4) * 1.2;
                    for (int i = 0; i < 3; i++) {
                        double angle = rotation + (y * 2) + (i * Math.PI * 2 / 3);
                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;
                        loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(x, y, z), 1, 0, 0, 0, 0, grayDust);
                        loc.getWorld().spawnParticle(Particle.CLOUD, loc.clone().add(x, y, z), 1, 0.05, 0.05, 0.05, 0);
                    }
                }
                rotation += 0.4;
                ticks += 4;
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }
    
    private void playDragonBreathPreview(Location loc, Player player) {
        loc.getWorld().playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.8f);
        loc.getWorld().spawnParticle(Particle.DRAGON_BREATH, loc, 100, 1.5, 1.0, 1.5, 0.08);
        
        Particle.DustOptions purpleDust = new Particle.DustOptions(Color.fromRGB(138, 43, 226), 2.0f);
        new BukkitRunnable() {
            int ticks = 0;
            double angle = 0;
            @Override
            public void run() {
                if (ticks >= 50) {
                    cancel();
                    return;
                }
                loc.getWorld().spawnParticle(Particle.DRAGON_BREATH, loc, 20, 1.0, 0.8, 1.0, 0.03);
                for (double y = 0; y < 2.5; y += 0.25) {
                    double a1 = angle + y * 2;
                    double a2 = a1 + Math.PI;
                    double r = 0.7;
                    loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(Math.cos(a1)*r, y, Math.sin(a1)*r), 1, 0, 0, 0, 0, purpleDust);
                    loc.getWorld().spawnParticle(Particle.PORTAL, loc.clone().add(Math.cos(a2)*r, y, Math.sin(a2)*r), 2, 0.1, 0.1, 0.1, 0);
                }
                angle += 0.3;
                ticks += 4;
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }
    
    private void playBlackHolePreview(Location loc, Player player) {
        loc.getWorld().playSound(loc, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 0.3f);
        loc.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.5f);
        
        Particle.DustOptions blackDust = new Particle.DustOptions(Color.fromRGB(10, 10, 10), 2.0f);
        Particle.DustOptions purpleDust = new Particle.DustOptions(Color.fromRGB(75, 0, 130), 1.5f);
        
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 60) {
                    cancel();
                    return;
                }
                // 吸い込まれるパーティクル
                double outerRadius = 3.0 - (ticks / 60.0) * 1.5;
                for (int i = 0; i < 15; i++) {
                    double theta = random.nextDouble() * Math.PI * 2;
                    double r = outerRadius * (0.5 + random.nextDouble() * 0.5);
                    double x = r * Math.cos(theta);
                    double z = r * Math.sin(theta);
                    loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(x, 0.5, z), 1, 0, 0, 0, 0, purpleDust);
                }
                // 中心ディスク
                for (int i = 0; i < 20; i++) {
                    double angle = (ticks * 0.2) + (i * Math.PI * 2 / 20);
                    double radius = 0.3 + (i / 20.0) * 0.5;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(x, 0.3, z), 1, 0, 0, 0, 0, blackDust);
                }
                loc.getWorld().spawnParticle(Particle.SQUID_INK, loc.clone().add(0, 0.3, 0), 3, 0.15, 0.15, 0.15, 0);
                ticks += 4;
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }
    
    private void playIceShatterPreview(Location loc, Player player) {
        loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 1.2f, 0.8f);
        loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.8f, 0.8f);
        loc.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 80, 1.2, 1.0, 1.2, 0.05);
        loc.getWorld().spawnParticle(Particle.BLOCK, loc, 40, 1.0, 0.8, 1.0, 0.1, Material.BLUE_ICE.createBlockData());
        
        Particle.DustOptions iceDust = new Particle.DustOptions(Color.fromRGB(173, 216, 230), 2.0f);
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 40) {
                    cancel();
                    return;
                }
                loc.getWorld().spawnParticle(Particle.DUST, loc, 15, 1.5, 0.8, 1.5, 0, iceDust);
                loc.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 5, 1.0, 0.5, 1.0, 0.05);
                ticks += 4;
            }
        }.runTaskTimer(plugin, 5L, 4L);
    }
    
    private void playNetherPortalPreview(Location loc, Player player) {
        loc.getWorld().playSound(loc, Sound.BLOCK_PORTAL_TRIGGER, 0.8f, 0.8f);
        loc.getWorld().playSound(loc, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 0.5f);
        
        Particle.DustOptions purpleDust = new Particle.DustOptions(Color.fromRGB(128, 0, 128), 2.0f);
        new BukkitRunnable() {
            int ticks = 0;
            double rotation = 0;
            double height = 0;
            @Override
            public void run() {
                if (ticks >= 50) {
                    cancel();
                    return;
                }
                if (ticks < 25) {
                    height = Math.min(height + 0.15, 2.5);
                }
                for (double y = 0; y < height; y += 0.3) {
                    double waveRadius = 0.7 + Math.sin(y * 2 + rotation) * 0.15;
                    for (int i = 0; i < 8; i++) {
                        double angle = rotation + (i * Math.PI / 4) + (y * 0.5);
                        double x = Math.cos(angle) * waveRadius;
                        double z = Math.sin(angle) * waveRadius;
                        loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(x, y, z), 1, 0, 0, 0, 0, purpleDust);
                    }
                }
                loc.getWorld().spawnParticle(Particle.PORTAL, loc.clone().add(0, height/2, 0), 15, 0.5, height/2, 0.5, 0.3);
                rotation += 0.25;
                ticks += 4;
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }
    
    private void playRainbowBurstPreview(Location loc, Player player) {
        loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.5f);
        loc.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1.0f, 1.0f);
        loc.getWorld().spawnParticle(Particle.FLASH, loc, 1, 0, 0, 0, 0);
        
        Color[] rainbow = {Color.RED, Color.ORANGE, Color.YELLOW, Color.LIME, Color.AQUA, Color.BLUE, Color.fromRGB(148, 0, 211)};
        new BukkitRunnable() {
            int ticks = 0;
            double ringRadius = 0;
            @Override
            public void run() {
                if (ticks >= 50) {
                    cancel();
                    return;
                }
                ringRadius += 0.12;
                for (int c = 0; c < rainbow.length; c++) {
                    double layerRadius = ringRadius - (c * 0.12);
                    if (layerRadius > 0) {
                        Particle.DustOptions dust = new Particle.DustOptions(rainbow[c], 2.0f);
                        for (int i = 0; i < 16; i++) {
                            double angle = Math.toRadians(i * 22.5);
                            double x = Math.cos(angle) * layerRadius;
                            double z = Math.sin(angle) * layerRadius;
                            loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(x, 0.5, z), 1, 0, 0, 0, 0, dust);
                        }
                    }
                }
                loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0, 1.5, 0), 3, 1.0, 1.0, 1.0, 0.02);
                ticks += 4;
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }
    
    private void playSoulAscendPreview(Location loc, Player player) {
        loc.getWorld().playSound(loc, Sound.PARTICLE_SOUL_ESCAPE, 1.0f, 0.8f);
        loc.getWorld().playSound(loc, Sound.ENTITY_VEX_AMBIENT, 0.6f, 0.5f);
        loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 30, 0.8, 0.3, 0.8, 0.02);
        
        Particle.DustOptions soulDust = new Particle.DustOptions(Color.fromRGB(64, 224, 208), 2.0f);
        new BukkitRunnable() {
            int ticks = 0;
            int soulCount = 0;
            double[][] soulPos = new double[4][2];
            double[] soulHeight = new double[4];
            @Override
            public void run() {
                if (ticks >= 60) {
                    cancel();
                    return;
                }
                if (ticks % 12 == 0 && soulCount < 4) {
                    soulPos[soulCount][0] = (random.nextDouble() - 0.5) * 1.5;
                    soulPos[soulCount][1] = (random.nextDouble() - 0.5) * 1.5;
                    soulHeight[soulCount] = 0;
                    soulCount++;
                }
                for (int i = 0; i < soulCount; i++) {
                    soulHeight[i] += 0.1 + random.nextDouble() * 0.03;
                    double swayX = Math.sin(ticks * 0.2 + i) * 0.2;
                    double swayZ = Math.cos(ticks * 0.15 + i * 2) * 0.2;
                    Location soulLoc = loc.clone().add(soulPos[i][0] + swayX, soulHeight[i], soulPos[i][1] + swayZ);
                    loc.getWorld().spawnParticle(Particle.SOUL, soulLoc, 2, 0.08, 0.08, 0.08, 0);
                    loc.getWorld().spawnParticle(Particle.DUST, soulLoc, 3, 0.1, 0.15, 0.1, 0, soulDust);
                }
                loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 3, 0.5, 0.2, 0.5, 0.01);
                ticks += 4;
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }
    
    // ===== データアクセスメソッド =====
    
    /**
     * プレイヤーの選択エフェクトを取得
     */
    public String getSelectedEffect(UUID playerId) {
        loadEffectData();
        return effectConfig.getString("players." + playerId.toString() + ".selected", "simple_explosion");
    }
    
    /**
     * プレイヤーの選択エフェクトを設定
     */
    public void setSelectedEffect(UUID playerId, String effectId) {
        loadEffectData();
        effectConfig.set("players." + playerId.toString() + ".selected", effectId);
        saveEffectData();
    }
    
    /**
     * プレイヤーの解放済みエフェクトを取得
     */
    public Set<String> getUnlockedEffects(UUID playerId) {
        loadEffectData();
        Set<String> unlocked = new HashSet<>();
        unlocked.add("simple_explosion");
        
        List<String> list = effectConfig.getStringList("players." + playerId.toString() + ".unlocked");
        unlocked.addAll(list);
        
        return unlocked;
    }
    
    /**
     * エフェクトが解放されているかチェック
     */
    public boolean isUnlocked(UUID playerId, BedEffect effect) {
        if (!effect.isPremium()) return true;
        if (plugin.getAdminDataManager().isAdmin(playerId)) return true;
        return getUnlockedEffects(playerId).contains(effect.getId());
    }
    
    /**
     * エフェクトを解放
     */
    public void unlockEffect(UUID playerId, String effectId) {
        loadEffectData();
        List<String> unlocked = effectConfig.getStringList("players." + playerId.toString() + ".unlocked");
        if (!unlocked.contains(effectId)) {
            unlocked.add(effectId);
            effectConfig.set("players." + playerId.toString() + ".unlocked", unlocked);
            saveEffectData();
        }
    }
    
    /**
     * エフェクトスロット位置を取得
     */
    public int[] getEffectSlots() {
        return new int[]{10, 12, 14, 16, 19, 21, 23, 28, 30, 32, 34, 37};
    }
}
