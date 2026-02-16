package myplg.myplg.effects;

import myplg.myplg.PvPGame;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

/**
 * ベッド破壊エフェクトの再生を管理
 */
public class BedDestructionEffectManager {

    private final PvPGame plugin;
    private final Random random = new Random();

    public BedDestructionEffectManager(PvPGame plugin) {
        this.plugin = plugin;
    }

    // ===== 強制表示パーティクルヘルパーメソッド =====
    // force=true で軽量化MOD使用時でもパーティクルを表示

    private void spawnDust(World world, Location loc, int count, double dx, double dy, double dz, Particle.DustOptions dust) {
        world.spawnParticle(Particle.DUST, loc, count, dx, dy, dz, 0, dust, true);
    }

    private void spawnParticle(World world, Particle particle, Location loc, int count, double dx, double dy, double dz, double extra) {
        world.spawnParticle(particle, loc, count, dx, dy, dz, extra, null, true);
    }

    private <T> void spawnParticleData(World world, Particle particle, Location loc, int count, double dx, double dy, double dz, double extra, T data) {
        world.spawnParticle(particle, loc, count, dx, dy, dz, extra, data, true);
    }
    
    /**
     * エフェクトを再生
     * @param effect 再生するエフェクト
     * @param location ベッドの位置
     * @param teamColor チームの色（パーティクルに反映）
     */
    public void playEffect(BedDestructionEffect effect, Location location, Color teamColor) {
        switch (effect) {
            case SIMPLE_EXPLOSION:
                playSimpleExplosion(location, teamColor);
                break;
            case FLAME_BURST:
                playFlameBurst(location, teamColor);
                break;
            case MAGIC_VANISH:
                playMagicVanish(location, teamColor);
                break;
            case FIREWORK_FESTIVAL:
                playFireworkFestival(location, teamColor);
                break;
            case LIGHTNING_STRIKE:
                playLightningStrike(location, teamColor);
                break;
            case TORNADO:
                playTornado(location, teamColor);
                break;
            case DRAGON_BREATH:
                playDragonBreath(location, teamColor);
                break;
            case BLACK_HOLE:
                playBlackHole(location, teamColor);
                break;
            case ICE_SHATTER:
                playIceShatter(location, teamColor);
                break;
            case NETHER_PORTAL:
                playNetherPortal(location, teamColor);
                break;
            case RAINBOW_BURST:
                playRainbowBurst(location, teamColor);
                break;
            case SOUL_ASCEND:
                playSoulAscend(location, teamColor);
                break;
        }
    }
    
    /**
     * シンプル爆発エフェクト（レベル1 - 控えめだが見栄えする）
     * 改善版：球状の衝撃波と放射状のパーティクル
     */
    private void playSimpleExplosion(Location location, Color teamColor) {
        Location center = location.clone();
        World world = location.getWorld();
        if (world == null) return;

        // サウンド（よりインパクトのある組み合わせ）
        world.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.2f);
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 1.0f);
        world.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.8f, 0.8f);

        Particle.DustOptions teamDust = new Particle.DustOptions(teamColor, 3.0f);
        Particle.DustOptions whiteDust = new Particle.DustOptions(Color.WHITE, 2.5f);
        Particle.DustOptions smallTeamDust = new Particle.DustOptions(teamColor, 1.5f);

        // 初期の爆発
        world.spawnParticle(Particle.EXPLOSION, center, 3, 0.3, 0.3, 0.3, 0);
        world.spawnParticle(Particle.EXPLOSION_EMITTER, center, 1, 0, 0, 0, 0);
        world.spawnParticle(Particle.FLASH, center, 1, 0, 0, 0, 0);

        // 放射状のパーティクル（初期）
        for (int i = 0; i < 16; i++) {
            double angle = Math.toRadians(i * 22.5);
            for (int j = 0; j < 5; j++) {
                double radius = j * 0.4;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                world.spawnParticle(Particle.DUST, center.clone().add(x, 0.3, z), 2, 0.1, 0.1, 0.1, 0, teamDust);
            }
        }

        new BukkitRunnable() {
            int ticks = 0;
            final int duration = 50;
            double shockwaveRadius = 0;

            @Override
            public void run() {
                if (ticks >= duration) {
                    cancel();
                    return;
                }

                // 広がる衝撃波リング
                shockwaveRadius += 0.2;
                if (shockwaveRadius < 3.5) {
                    for (int i = 0; i < 24; i++) {
                        double angle = Math.toRadians(i * 15);
                        double x = Math.cos(angle) * shockwaveRadius;
                        double z = Math.sin(angle) * shockwaveRadius;
                        Location ringLoc = center.clone().add(x, 0.2, z);
                        world.spawnParticle(Particle.DUST, ringLoc, 1, 0.05, 0.05, 0.05, 0, smallTeamDust);
                    }
                }

                // 上昇する煙柱
                double smokeHeight = ticks * 0.1;
                world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, center.clone().add(0, smokeHeight, 0), 3, 0.3, 0.2, 0.3, 0.01);
                world.spawnParticle(Particle.SMOKE, center.clone().add(0, smokeHeight * 0.5, 0), 8, 0.5, 0.3, 0.5, 0.02);

                // チームカラーのダスト
                world.spawnParticle(Particle.DUST, center, 15, 1.0, 0.8, 1.0, 0, teamDust);
                world.spawnParticle(Particle.DUST, center, 8, 0.6, 0.5, 0.6, 0, whiteDust);

                // 火花（前半のみ）
                if (ticks < 25) {
                    world.spawnParticle(Particle.FLAME, center, 8, 0.6, 0.4, 0.6, 0.03);
                    world.spawnParticle(Particle.CRIT, center, 5, 0.8, 0.8, 0.8, 0.2);
                }

                ticks += 4;
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }
    
    /**
     * 炎の爆発エフェクト（レベル2 - 中程度）
     * 改善版：螺旋状の炎と火柱
     */
    private void playFlameBurst(Location location, Color teamColor) {
        Location center = location.clone();
        World world = location.getWorld();
        if (world == null) return;

        // サウンド（よりドラマチック）
        world.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.9f);
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.7f);
        world.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.2f, 0.8f);
        world.playSound(center, Sound.ITEM_FIRECHARGE_USE, 1.0f, 0.6f);
        
        Particle.DustOptions teamDust = new Particle.DustOptions(teamColor, 2.5f);
        Particle.DustOptions orangeDust = new Particle.DustOptions(Color.ORANGE, 2.0f);
        Particle.DustOptions yellowDust = new Particle.DustOptions(Color.YELLOW, 1.5f);
        
        // 初期の炎の爆発
        world.spawnParticle(Particle.EXPLOSION, center, 2, 0.3, 0.3, 0.3, 0);
        world.spawnParticle(Particle.FLAME, center, 80, 1.5, 1.0, 1.5, 0.15);
        world.spawnParticle(Particle.LAVA, center, 20, 1.0, 0.5, 1.0, 0);
        
        new BukkitRunnable() {
            int ticks = 0;
            final int duration = 60;
            double spiralAngle = 0;
            
            @Override
            public void run() {
                if (ticks >= duration) {
                    cancel();
                    return;
                }
                
                // 螺旋状の炎（ダブルヘリックス）
                for (double y = 0; y < 3; y += 0.25) {
                    double angle1 = spiralAngle + y * 1.5;
                    double angle2 = spiralAngle + y * 1.5 + Math.PI;
                    double radius = 0.6 + Math.sin(y * 2) * 0.2;
                    
                    double x1 = Math.cos(angle1) * radius;
                    double z1 = Math.sin(angle1) * radius;
                    double x2 = Math.cos(angle2) * radius;
                    double z2 = Math.sin(angle2) * radius;
                    
                    Location loc1 = center.clone().add(x1, y, z1);
                    Location loc2 = center.clone().add(x2, y, z2);
                    
                    world.spawnParticle(Particle.FLAME, loc1, 2, 0.05, 0.05, 0.05, 0.01);
                    world.spawnParticle(Particle.FLAME, loc2, 2, 0.05, 0.05, 0.05, 0.01);
                    world.spawnParticle(Particle.DUST, loc1, 1, 0.05, 0.05, 0.05, 0, orangeDust);
                }
                
                // 炎の輪（地面）
                for (int i = 0; i < 12; i++) {
                    double angle = spiralAngle + (i * Math.PI / 6);
                    double radius = 1.2;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    
                    Location ringLoc = center.clone().add(x, 0.1, z);
                    world.spawnParticle(Particle.FLAME, ringLoc, 2, 0.1, 0.1, 0.1, 0.02);
                }
                
                // 飛び散る火花
                for (int i = 0; i < 5; i++) {
                    double angle = random.nextDouble() * Math.PI * 2;
                    double radius = random.nextDouble() * 2;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    double y = random.nextDouble() * 2;
                    
                    Location sparkLoc = center.clone().add(x, y, z);
                    world.spawnParticle(Particle.DUST, sparkLoc, 1, 0.05, 0.05, 0.05, 0, teamDust);
                    world.spawnParticle(Particle.DUST, sparkLoc, 1, 0.05, 0.05, 0.05, 0, yellowDust);
                }
                
                // 溶岩と煙
                world.spawnParticle(Particle.LAVA, center, 3, 0.8, 0.3, 0.8, 0);
                world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, center.clone().add(0, 2, 0), 2, 0.2, 0.3, 0.2, 0.01);
                
                spiralAngle += 0.3;
                ticks += 4;
            }
        }.runTaskTimer(plugin, 0L, 4L);
        
        // 追加のサウンド
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            world.playSound(center, Sound.ENTITY_BLAZE_HURT, 0.8f, 0.5f);
            world.spawnParticle(Particle.FLAME, center.clone().add(0, 1, 0), 40, 1.0, 0.5, 1.0, 0.1);
        }, 15L);
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            world.playSound(center, Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 0.8f);
        }, 45L);
    }
    
    /**
     * 魔法の消滅エフェクト（レベル3 - 派手）
     * 改善版：魔法陣と多重螺旋
     */
    private void playMagicVanish(Location location, Color teamColor) {
        Location center = location.clone();
        World world = location.getWorld();
        if (world == null) return;

        // サウンド（より神秘的）
        world.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 1.0f);
        world.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f);
        world.playSound(center, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.2f, 1.0f);
        world.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.5f);
        
        Particle.DustOptions teamDust = new Particle.DustOptions(teamColor, 2.0f);
        Particle.DustOptions purpleDust = new Particle.DustOptions(Color.fromRGB(148, 0, 211), 1.8f);
        Particle.DustOptions pinkDust = new Particle.DustOptions(Color.fromRGB(255, 105, 180), 1.5f);
        Particle.DustOptions cyanDust = new Particle.DustOptions(Color.AQUA, 1.5f);
        
        // 初期の魔法陣
        for (int ring = 0; ring < 3; ring++) {
            double radius = 1.0 + ring * 0.5;
            for (int i = 0; i < 24; i++) {
                double angle = Math.toRadians(i * 15);
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                Location ringLoc = center.clone().add(x, 0.1, z);
                world.spawnParticle(Particle.DUST, ringLoc, 2, 0.05, 0.02, 0.05, 0, purpleDust);
            }
        }
        
        new BukkitRunnable() {
            int ticks = 0;
            final int duration = 70;
            double spiralAngle = 0;
            double magicCircleAngle = 0;
            
            @Override
            public void run() {
                if (ticks >= duration) {
                    cancel();
                    return;
                }
                
                // 回転する魔法陣
                for (int i = 0; i < 12; i++) {
                    double angle = magicCircleAngle + (i * Math.PI / 6);
                    double radius = 1.5;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    
                    Location circleLoc = center.clone().add(x, 0.1, z);
                    world.spawnParticle(Particle.DUST, circleLoc, 1, 0.02, 0.02, 0.02, 0, purpleDust);
                    
                    // 内側のリング
                    double innerRadius = 0.8;
                    double ix = Math.cos(angle + Math.PI / 12) * innerRadius;
                    double iz = Math.sin(angle + Math.PI / 12) * innerRadius;
                    world.spawnParticle(Particle.DUST, center.clone().add(ix, 0.1, iz), 1, 0.02, 0.02, 0.02, 0, pinkDust);
                }
                
                // 多重螺旋（3本の螺旋が上昇）
                double progress = ticks / (double) duration;
                double maxHeight = 4.0;
                
                for (int spiral = 0; spiral < 3; spiral++) {
                    for (double h = 0; h < maxHeight * progress; h += 0.2) {
                        double baseAngle = spiralAngle + (spiral * Math.PI * 2 / 3);
                        double angle = baseAngle + h * 1.5;
                        double radius = 1.0 - (h / maxHeight) * 0.7; // 上に行くほど収束
                        
                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;
                        
                        Location spiralLoc = center.clone().add(x, h, z);
                        Particle.DustOptions dust = (spiral == 0) ? teamDust : (spiral == 1) ? purpleDust : cyanDust;
                        world.spawnParticle(Particle.DUST, spiralLoc, 1, 0.03, 0.03, 0.03, 0, dust);
                    }
                }
                
                // エンチャントとポータル
                world.spawnParticle(Particle.ENCHANT, center, 30, 0.8, 1.0, 0.8, 1.0);
                world.spawnParticle(Particle.PORTAL, center, 20, 0.5, 0.5, 0.5, 0.5);
                world.spawnParticle(Particle.END_ROD, center.clone().add(0, progress * maxHeight, 0), 5, 0.3, 0.3, 0.3, 0.02);
                
                // キラキラ
                if (ticks % 8 == 0) {
                    world.spawnParticle(Particle.FIREWORK, center.clone().add(0, 1, 0), 10, 1.0, 1.0, 1.0, 0.05);
                }
                
                spiralAngle += 0.25;
                magicCircleAngle += 0.1;
                ticks += 4;
            }
        }.runTaskTimer(plugin, 0L, 4L);
        
        // 消滅エフェクト
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            world.playSound(center, Sound.ENTITY_ENDER_EYE_DEATH, 1.0f, 1.5f);
            world.playSound(center, Sound.BLOCK_BEACON_DEACTIVATE, 0.8f, 1.0f);
            world.spawnParticle(Particle.FLASH, center.clone().add(0, 2, 0), 2, 0.3, 0.3, 0.3, 0);
            world.spawnParticle(Particle.END_ROD, center.clone().add(0, 2, 0), 50, 1.0, 1.0, 1.0, 0.2);
        }, 50L);
    }
    
    /**
     * 花火祭りエフェクト（レベル4 - 超派手）
     * 改善版：連続花火と光の軌跡
     */
    private void playFireworkFestival(Location location, Color teamColor) {
        Location center = location.clone();
        World world = location.getWorld();
        if (world == null) return;

        // サウンド（祭り感）
        world.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.9f);
        world.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f);
        world.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.2f, 0.9f);
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);

        // 多彩な色
        Color[] colors = {
            teamColor,
            Color.fromRGB(255, 215, 0),  // 金
            Color.WHITE,
            Color.fromRGB(255, 105, 180), // ピンク
            Color.RED,
            Color.fromRGB(0, 255, 127), // 春緑
            Color.fromRGB(0, 191, 255)  // ディープスカイブルー
        };

        // 初期の大爆発（より派手に）
        world.spawnParticle(Particle.EXPLOSION_EMITTER, center, 1, 0, 0, 0, 0);
        world.spawnParticle(Particle.EXPLOSION, center, 5, 1.0, 1.0, 1.0, 0);
        fireworkBurstEnhanced(center, teamColor, world);
        fireworkBurstEnhanced(center.clone().add(0, 1, 0), Color.WHITE, world);

        new BukkitRunnable() {
            int ticks = 0;
            final int duration = 80;
            int burstCount = 0;
            final int maxBursts = 12;

            @Override
            public void run() {
                if (ticks >= duration) {
                    cancel();
                    return;
                }

                // 連続花火（ランダムな位置に打ち上げ）- より頻繁に
                if (ticks % 6 == 0 && burstCount < maxBursts) {
                    double offsetX = (random.nextDouble() - 0.5) * 4;
                    double offsetY = random.nextDouble() * 3 + 1;
                    double offsetZ = (random.nextDouble() - 0.5) * 4;

                    Location burstLoc = center.clone().add(offsetX, offsetY, offsetZ);
                    Color burstColor = colors[burstCount % colors.length];
                    fireworkBurstEnhanced(burstLoc, burstColor, world);

                    // サウンド
                    Sound[] sounds = {
                        Sound.ENTITY_FIREWORK_ROCKET_BLAST,
                        Sound.ENTITY_FIREWORK_ROCKET_BLAST_FAR,
                        Sound.ENTITY_FIREWORK_ROCKET_TWINKLE,
                        Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST
                    };
                    world.playSound(burstLoc, sounds[random.nextInt(sounds.length)],
                        1.0f, 0.8f + random.nextFloat() * 0.4f);

                    burstCount++;
                }

                // 上昇する光の軌跡（より多く）
                for (int i = 0; i < 6; i++) {
                    double angle = (ticks * 0.15) + (i * Math.PI * 2 / 6);
                    double radius = 0.8;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    double y = (ticks % 20) * 0.2;

                    Location trailLoc = center.clone().add(x, y, z);
                    Particle.DustOptions dust = new Particle.DustOptions(colors[i % colors.length], 2.5f);
                    world.spawnParticle(Particle.DUST, trailLoc, 5, 0.1, 0.1, 0.1, 0, dust);
                    world.spawnParticle(Particle.FIREWORK, trailLoc, 3, 0.1, 0.1, 0.1, 0);
                }

                // 常にキラキラ（大量）
                world.spawnParticle(Particle.FIREWORK, center, 40, 2.5, 2.5, 2.5, 0.1);
                world.spawnParticle(Particle.END_ROD, center.clone().add(0, 2, 0), 15, 2.0, 1.5, 2.0, 0.05);
                world.spawnParticle(Particle.TOTEM_OF_UNDYING, center, 20, 1.5, 1.5, 1.5, 0.3);

                // 星が降り注ぐ（より多く）
                for (int i = 0; i < 10; i++) {
                    Location starLoc = center.clone().add(
                        (random.nextDouble() - 0.5) * 5,
                        4 - (ticks % 30) * 0.13,
                        (random.nextDouble() - 0.5) * 5
                    );
                    Particle.DustOptions starDust = new Particle.DustOptions(colors[random.nextInt(colors.length)], 2.5f);
                    world.spawnParticle(Particle.DUST, starLoc, 4, 0.15, 0.15, 0.15, 0, starDust);
                }

                ticks += 4;
            }
        }.runTaskTimer(plugin, 0L, 4L);

        // フィナーレ
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            world.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 2.0f, 0.7f);
            world.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE_FAR, 1.5f, 1.0f);
            world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.5f);

            // 全色同時爆発
            for (Color color : colors) {
                fireworkBurstEnhanced(center.clone().add(
                    (random.nextDouble() - 0.5) * 3,
                    2 + random.nextDouble() * 2,
                    (random.nextDouble() - 0.5) * 3
                ), color, world);
            }
            world.spawnParticle(Particle.EXPLOSION_EMITTER, center.clone().add(0, 2, 0), 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.TOTEM_OF_UNDYING, center, 100, 2.0, 2.0, 2.0, 0.5);
        }, 60L);
    }
    
    /**
     * 花火の爆発を生成
     */
    private void fireworkBurst(Location loc, Color color, World world) {
        Particle.DustOptions dust = new Particle.DustOptions(color, 2.0f);
        
        // 球状に広がるパーティクル
        for (int i = 0; i < 30; i++) {
            double theta = random.nextDouble() * Math.PI * 2;
            double phi = random.nextDouble() * Math.PI;
            double radius = 0.8 + random.nextDouble() * 0.4;
            
            double x = radius * Math.sin(phi) * Math.cos(theta);
            double y = radius * Math.cos(phi);
            double z = radius * Math.sin(phi) * Math.sin(theta);
            
            Location particleLoc = loc.clone().add(x, y, z);
            world.spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0, dust);
        }
        
        // 中心に爆発
        world.spawnParticle(Particle.FLASH, loc, 1, 0, 0, 0, 0);
    }
    
    /**
     * 強化版花火バースト（より派手な球状爆発）
     */
    private void fireworkBurstEnhanced(Location loc, Color color, World world) {
        Particle.DustOptions mainDust = new Particle.DustOptions(color, 2.5f);
        Particle.DustOptions smallDust = new Particle.DustOptions(color, 1.5f);
        Particle.DustOptions whiteDust = new Particle.DustOptions(Color.WHITE, 1.0f);
        
        // 球状に広がるパーティクル（より多く、より美しく）
        for (int i = 0; i < 50; i++) {
            double theta = random.nextDouble() * Math.PI * 2;
            double phi = random.nextDouble() * Math.PI;
            double radius = 1.0 + random.nextDouble() * 0.5;
            
            double x = radius * Math.sin(phi) * Math.cos(theta);
            double y = radius * Math.cos(phi);
            double z = radius * Math.sin(phi) * Math.sin(theta);
            
            Location particleLoc = loc.clone().add(x, y, z);
            world.spawnParticle(Particle.DUST, particleLoc, 2, 0.05, 0.05, 0.05, 0, mainDust);
        }
        
        // 放射状のライン
        for (int line = 0; line < 8; line++) {
            double theta = line * Math.PI / 4;
            for (double r = 0.2; r < 1.5; r += 0.2) {
                double x = r * Math.cos(theta);
                double z = r * Math.sin(theta);
                Location lineLoc = loc.clone().add(x, 0, z);
                world.spawnParticle(Particle.DUST, lineLoc, 1, 0.02, 0.02, 0.02, 0, smallDust);
            }
        }
        
        // 中心のフラッシュ
        world.spawnParticle(Particle.FLASH, loc, 1, 0, 0, 0, 0);
        world.spawnParticle(Particle.FIREWORK, loc, 20, 0.5, 0.5, 0.5, 0.1);
        
        // 白い星（キラキラ）
        for (int i = 0; i < 10; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double r = random.nextDouble() * 1.2;
            double x = r * Math.cos(angle);
            double z = r * Math.sin(angle);
            double y = (random.nextDouble() - 0.5) * 1.2;
            world.spawnParticle(Particle.DUST, loc.clone().add(x, y, z), 1, 0, 0, 0, 0, whiteDust);
        }
    }
    
    /**
     * 雷撃エフェクト（レベル5 - 超派手）
     * 雷が落ちて衝撃波が広がる
     */
    private void playLightningStrike(Location location, Color teamColor) {
        Location center = location.clone();
        World world = location.getWorld();
        if (world == null) return;

        // 雷を実際に落とす（ダメージなし）
        world.strikeLightningEffect(center);
        
        // 追加サウンド
        world.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 0.8f);
        world.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.2f, 1.0f);
        world.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 1.5f);
        
        Particle.DustOptions teamDust = new Particle.DustOptions(teamColor, 3.0f);
        Particle.DustOptions whiteDust = new Particle.DustOptions(Color.WHITE, 2.5f);
        Particle.DustOptions yellowDust = new Particle.DustOptions(Color.YELLOW, 2.0f);
        
        // 初期の強烈なフラッシュ
        world.spawnParticle(Particle.FLASH, center, 3, 0.5, 0.5, 0.5, 0);
        world.spawnParticle(Particle.ELECTRIC_SPARK, center, 100, 2.0, 2.0, 2.0, 0.5);
        
        new BukkitRunnable() {
            int ticks = 0;
            final int duration = 60;
            double shockwaveRadius = 0;
            
            @Override
            public void run() {
                if (ticks >= duration) {
                    cancel();
                    return;
                }
                
                // 衝撃波（外側に広がる円）
                shockwaveRadius += 0.3;
                if (shockwaveRadius < 5) {
                    for (int i = 0; i < 36; i++) {
                        double angle = Math.toRadians(i * 10);
                        double x = Math.cos(angle) * shockwaveRadius;
                        double z = Math.sin(angle) * shockwaveRadius;
                        Location ringLoc = center.clone().add(x, 0.1, z);
                        world.spawnParticle(Particle.DUST, ringLoc, 2, 0.1, 0.1, 0.1, 0, yellowDust);
                        world.spawnParticle(Particle.ELECTRIC_SPARK, ringLoc, 1, 0.1, 0.1, 0.1, 0);
                    }
                }
                
                // 中心から放電
                if (ticks < 30) {
                    world.spawnParticle(Particle.ELECTRIC_SPARK, center, 20, 1.0, 1.5, 1.0, 0.1);
                    world.spawnParticle(Particle.DUST, center, 15, 0.8, 1.0, 0.8, 0, teamDust);
                }
                
                // 上昇する光の柱
                for (int y = 0; y < 4; y++) {
                    Location pillarLoc = center.clone().add(0, y * 0.5, 0);
                    world.spawnParticle(Particle.DUST, pillarLoc, 5, 0.3, 0.1, 0.3, 0, whiteDust);
                    world.spawnParticle(Particle.END_ROD, pillarLoc, 2, 0.2, 0.1, 0.2, 0.01);
                }
                
                ticks += 4;
            }
        }.runTaskTimer(plugin, 0L, 4L);
        
        // 追加の雷（遅延）
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            world.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1.2f);
            world.spawnParticle(Particle.FLASH, center.clone().add(0, 2, 0), 1, 0, 0, 0, 0);
        }, 15L);
    }
    
    /**
     * 竜巻エフェクト（レベル5 - 超派手）
     * 渦巻くパーティクルの竜巻 - 軽量化MOD対応版
     */
    private void playTornado(Location location, Color teamColor) {
        Location center = location.clone();
        World world = location.getWorld();
        if (world == null) return;

        // サウンド
        world.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 2.0f, 0.5f);
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.7f);
        world.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.2f, 1.0f);

        // 見やすい色のダスト（大きめサイズ）
        Particle.DustOptions teamDust = new Particle.DustOptions(teamColor, 4.0f);
        Particle.DustOptions whiteDust = new Particle.DustOptions(Color.WHITE, 3.5f);
        Particle.DustOptions grayDust = new Particle.DustOptions(Color.fromRGB(180, 180, 180), 3.0f);

        // 初期爆発（force=true）
        spawnParticle(world, Particle.EXPLOSION_EMITTER, center, 1, 0, 0, 0, 0);
        spawnParticle(world, Particle.EXPLOSION, center, 3, 0.5, 0.5, 0.5, 0);
        spawnParticle(world, Particle.FLAME, center, 50, 1.5, 0.5, 1.5, 0.1);

        new BukkitRunnable() {
            int ticks = 0;
            final int duration = 80;
            double rotation = 0;

            @Override
            public void run() {
                if (ticks >= duration) {
                    cancel();
                    return;
                }

                double maxHeight = Math.min(ticks * 0.2, 8);

                // メイン竜巻螺旋（DUST + FLAME で確実に見える）
                for (double y = 0; y < maxHeight; y += 0.2) {
                    double radius = 0.5 + (y / maxHeight) * 2.5;

                    for (int i = 0; i < 4; i++) {
                        double angle = rotation + (y * 2.0) + (i * Math.PI / 2);
                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;

                        Location particleLoc = center.clone().add(x, y, z);

                        // 大きなDUSTパーティクル（force=true）
                        spawnDust(world, particleLoc, 3, 0.1, 0.1, 0.1, teamDust);
                        spawnDust(world, particleLoc, 2, 0.1, 0.1, 0.1, whiteDust);

                        // FLAMEで輝きを追加
                        if (y < maxHeight * 0.7) {
                            spawnParticle(world, Particle.FLAME, particleLoc, 1, 0.05, 0.05, 0.05, 0.01);
                        }
                    }
                }

                // 地面の吸い込みエフェクト（DUSTとFLAME）
                for (int i = 0; i < 12; i++) {
                    double angle = rotation * 1.5 + (i * Math.PI / 6);
                    double radius = 3.5 - (ticks % 25) * 0.14;
                    if (radius > 0.3) {
                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;
                        Location groundLoc = center.clone().add(x, 0.2, z);
                        spawnDust(world, groundLoc, 3, 0.15, 0.05, 0.15, grayDust);
                        spawnParticle(world, Particle.CRIT, groundLoc, 2, 0.1, 0.05, 0.1, 0.05);
                    }
                }

                // 上部のエフェクト
                Location topLoc = center.clone().add(0, maxHeight, 0);
                spawnParticle(world, Particle.FIREWORK, topLoc, 10, 1.5, 0.5, 1.5, 0.1);
                spawnDust(world, topLoc, 15, 1.0, 0.3, 1.0, whiteDust);

                // 飛び散る破片
                if (ticks % 4 == 0) {
                    for (int i = 0; i < 8; i++) {
                        double angle = random.nextDouble() * Math.PI * 2;
                        double r = random.nextDouble() * 2.5;
                        double y = random.nextDouble() * maxHeight;
                        Location debrisLoc = center.clone().add(Math.cos(angle) * r, y, Math.sin(angle) * r);
                        spawnParticle(world, Particle.CRIT, debrisLoc, 3, 0.1, 0.1, 0.1, 0.15);
                        spawnParticle(world, Particle.END_ROD, debrisLoc, 1, 0.05, 0.05, 0.05, 0.02);
                    }
                }

                // 音
                if (ticks % 12 == 0) {
                    world.playSound(center, Sound.ENTITY_PHANTOM_FLAP, 1.0f, 0.4f + random.nextFloat() * 0.3f);
                }

                rotation += 0.5;
                ticks += 4;
            }
        }.runTaskTimer(plugin, 0L, 4L);

        // 最後の爆発
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);
            spawnParticle(world, Particle.EXPLOSION_EMITTER, center.clone().add(0, 4, 0), 1, 0, 0, 0, 0);
            spawnParticle(world, Particle.EXPLOSION, center, 5, 1.5, 1.5, 1.5, 0);
            spawnParticle(world, Particle.FLAME, center, 100, 3.0, 2.5, 3.0, 0.2);
            spawnDust(world, center, 80, 3.0, 2.0, 3.0, whiteDust);
        }, 70L);
    }
    
    /**
     * ドラゴンブレスエフェクト（レベル5 - 超派手）
     * 紫色の炎が渦巻く
     */
    private void playDragonBreath(Location location, Color teamColor) {
        Location center = location.clone();
        World world = location.getWorld();
        if (world == null) return;

        // ドラゴンサウンド
        world.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.7f);
        world.playSound(center, Sound.ENTITY_ENDER_DRAGON_HURT, 1.0f, 1.2f);
        world.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.5f, 0.8f);
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.6f);

        Particle.DustOptions purpleDust = new Particle.DustOptions(Color.fromRGB(138, 43, 226), 3.0f);
        Particle.DustOptions magentaDust = new Particle.DustOptions(Color.fromRGB(255, 0, 255), 2.5f);
        Particle.DustOptions pinkDust = new Particle.DustOptions(Color.fromRGB(255, 105, 180), 2.0f);
        Particle.DustOptions teamDust = new Particle.DustOptions(teamColor, 2.5f);

        // 初期のドラゴンブレス爆発（より派手に）
        world.spawnParticle(Particle.DRAGON_BREATH, center, 300, 2.5, 2.0, 2.5, 0.2);
        world.spawnParticle(Particle.EXPLOSION_EMITTER, center, 1, 0, 0, 0, 0);
        world.spawnParticle(Particle.EXPLOSION, center, 5, 1.0, 1.0, 1.0, 0);
        world.spawnParticle(Particle.FLAME, center, 80, 2.0, 1.0, 2.0, 0.15);

        new BukkitRunnable() {
            int ticks = 0;
            final int duration = 70;
            double spiralAngle = 0;

            @Override
            public void run() {
                if (ticks >= duration) {
                    cancel();
                    return;
                }

                // ドラゴンブレスパーティクル（大量）
                world.spawnParticle(Particle.DRAGON_BREATH, center, 80, 2.0, 1.5, 2.0, 0.1);

                // 紫色の螺旋（トリプルヘリックス）
                for (double y = 0; y < 5; y += 0.15) {
                    for (int h = 0; h < 3; h++) {
                        double angle = spiralAngle + y * 2.5 + (h * Math.PI * 2 / 3);
                        double radius = 1.0 + Math.sin(y + ticks * 0.1) * 0.4;

                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;
                        Location loc = center.clone().add(x, y, z);

                        Particle.DustOptions dust = (h == 0) ? purpleDust : (h == 1) ? magentaDust : pinkDust;
                        world.spawnParticle(Particle.DUST, loc, 3, 0.08, 0.08, 0.08, 0, dust);
                    }
                }

                // 地面を這う炎
                for (int i = 0; i < 12; i++) {
                    double angle = spiralAngle + (i * Math.PI / 6);
                    double radius = 1.5 + Math.sin(ticks * 0.2 + i) * 0.5;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    Location groundLoc = center.clone().add(x, 0.2, z);
                    world.spawnParticle(Particle.DRAGON_BREATH, groundLoc, 5, 0.2, 0.1, 0.2, 0.02);
                    world.spawnParticle(Particle.FLAME, groundLoc, 2, 0.1, 0.1, 0.1, 0.02);
                }

                // エンドパーティクル（より多く）
                world.spawnParticle(Particle.PORTAL, center, 50, 1.5, 1.0, 1.5, 0.8);
                world.spawnParticle(Particle.REVERSE_PORTAL, center, 30, 0.8, 0.8, 0.8, 0.2);
                world.spawnParticle(Particle.END_ROD, center.clone().add(0, 2, 0), 10, 1.0, 1.0, 1.0, 0.05);

                // チームカラーのアクセント
                world.spawnParticle(Particle.DUST, center.clone().add(0, 2, 0), 20, 1.5, 1.5, 1.5, 0, teamDust);

                // サウンド追加
                if (ticks % 15 == 0) {
                    world.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.6f, 0.8f + random.nextFloat() * 0.4f);
                }

                spiralAngle += 0.35;
                ticks += 4;
            }
        }.runTaskTimer(plugin, 0L, 4L);

        // 最後の咆哮
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            world.playSound(center, Sound.ENTITY_ENDER_DRAGON_DEATH, 0.6f, 1.5f);
            world.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
            world.spawnParticle(Particle.DRAGON_BREATH, center, 200, 3.0, 2.5, 3.0, 0.2);
            world.spawnParticle(Particle.EXPLOSION, center.clone().add(0, 2, 0), 3, 0.5, 0.5, 0.5, 0);
        }, 50L);
    }
    
    /**
     * ブラックホールエフェクト（レベル5 - 超派手）
     * 全てを吸い込む闇 - 軽量化MOD対応版
     */
    private void playBlackHole(Location location, Color teamColor) {
        Location center = location.clone();
        World world = location.getWorld();
        if (world == null) return;

        // 不気味なサウンド
        world.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.5f, 0.3f);
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.5f);
        world.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.5f);

        // 明るく見やすい紫系の色（大きめサイズ）
        Particle.DustOptions brightPurpleDust = new Particle.DustOptions(Color.fromRGB(180, 0, 255), 4.0f);
        Particle.DustOptions magentaDust = new Particle.DustOptions(Color.fromRGB(255, 0, 200), 3.5f);
        Particle.DustOptions pinkDust = new Particle.DustOptions(Color.fromRGB(255, 100, 255), 3.0f);
        Particle.DustOptions teamDust = new Particle.DustOptions(teamColor, 3.5f);
        Particle.DustOptions whiteDust = new Particle.DustOptions(Color.WHITE, 2.5f);

        // 初期の衝撃（force=true）
        spawnParticle(world, Particle.EXPLOSION_EMITTER, center, 1, 0, 0, 0, 0);
        spawnParticle(world, Particle.EXPLOSION, center, 5, 1.0, 1.0, 1.0, 0);

        new BukkitRunnable() {
            int ticks = 0;
            final int duration = 80;
            double rotation = 0;

            @Override
            public void run() {
                if (ticks >= duration) {
                    cancel();
                    return;
                }

                // 吸い込まれるパーティクル（外から中心へ）
                double outerRadius = 5.0 - (ticks / (double) duration) * 2.0;
                if (outerRadius > 0.5) {
                    for (int i = 0; i < 30; i++) {
                        double theta = random.nextDouble() * Math.PI * 2;
                        double phi = random.nextDouble() * Math.PI;
                        double r = outerRadius * (0.4 + random.nextDouble() * 0.6);

                        double x = r * Math.sin(phi) * Math.cos(theta);
                        double y = r * Math.cos(phi) * 0.7;
                        double z = r * Math.sin(phi) * Math.sin(theta);

                        Location particleLoc = center.clone().add(x, y + 1.0, z);
                        Particle.DustOptions dust = (i % 3 == 0) ? brightPurpleDust : (i % 3 == 1) ? magentaDust : pinkDust;
                        spawnDust(world, particleLoc, 3, 0.08, 0.08, 0.08, dust);

                        // 光の線で吸い込み感を演出
                        spawnParticle(world, Particle.END_ROD, particleLoc, 1, 0.05, 0.05, 0.05, 0.01);
                    }
                }

                // 回転するスパイラル（4本）
                for (int spiral = 0; spiral < 4; spiral++) {
                    double baseAngle = rotation + (spiral * Math.PI / 2);
                    for (double r = 0.3; r < 3.5; r += 0.25) {
                        double angle = baseAngle + r * 2.0;
                        double x = Math.cos(angle) * r;
                        double z = Math.sin(angle) * r;
                        double y = 0.8 + Math.sin(r * 3) * 0.4;

                        Location spiralLoc = center.clone().add(x, y, z);
                        spawnDust(world, spiralLoc, 2, 0.05, 0.05, 0.05, magentaDust);
                        spawnParticle(world, Particle.CRIT, spiralLoc, 1, 0.03, 0.03, 0.03, 0);
                    }
                }

                // 中心の降着円盤（DUSTで明るく）
                for (int i = 0; i < 30; i++) {
                    double angle = rotation * 3 + (i * Math.PI * 2 / 30);
                    double radius = 0.3 + (i / 30.0) * 1.2;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    double y = Math.sin(angle * 4) * 0.15;

                    Location diskLoc = center.clone().add(x, y + 1.0, z);
                    spawnDust(world, diskLoc, 2, 0.03, 0.03, 0.03, brightPurpleDust);
                }

                // 中心のコア（FLAMEとEND_RODで輝く）
                Location coreLoc = center.clone().add(0, 1.0, 0);
                spawnParticle(world, Particle.END_ROD, coreLoc, 15, 0.3, 0.3, 0.3, 0.03);
                spawnParticle(world, Particle.ENCHANT, coreLoc, 30, 0.5, 0.5, 0.5, 1.0);
                spawnDust(world, coreLoc, 20, 0.4, 0.4, 0.4, whiteDust);

                // チームカラーのエッジ輝き
                for (int i = 0; i < 8; i++) {
                    double angle = rotation * 2 + (i * Math.PI / 4);
                    double x = Math.cos(angle) * 1.5;
                    double z = Math.sin(angle) * 1.5;
                    Location edgeLoc = center.clone().add(x, 1.0, z);
                    spawnDust(world, edgeLoc, 4, 0.1, 0.1, 0.1, teamDust);
                    spawnParticle(world, Particle.FLAME, edgeLoc, 1, 0.05, 0.05, 0.05, 0);
                }

                // 音
                if (ticks % 15 == 0) {
                    world.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 0.3f);
                }

                rotation += 0.25;
                ticks += 4;
            }
        }.runTaskTimer(plugin, 0L, 4L);

        // 最後の消滅
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.5f);
            world.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 0.5f);
            spawnParticle(world, Particle.EXPLOSION_EMITTER, center, 2, 0.5, 0.5, 0.5, 0);
            spawnParticle(world, Particle.FLASH, center, 3, 0.3, 0.3, 0.3, 0);
            spawnParticle(world, Particle.END_ROD, center, 100, 2.5, 2.5, 2.5, 0.3);
            spawnDust(world, center, 150, 3.0, 2.0, 3.0, brightPurpleDust);
            spawnParticle(world, Particle.TOTEM_OF_UNDYING, center, 50, 2.0, 2.0, 2.0, 0.5);
        }, 60L);
    }
    
    /**
     * 氷砕エフェクト（レベル4 - 派手）
     * 凍結して砕け散る
     */
    private void playIceShatter(Location location, Color teamColor) {
        Location center = location.clone();
        World world = location.getWorld();
        if (world == null) return;

        // 凍結サウンド
        world.playSound(center, Sound.BLOCK_GLASS_BREAK, 2.0f, 0.8f);
        world.playSound(center, Sound.BLOCK_POWDER_SNOW_BREAK, 1.5f, 0.5f);
        world.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.5f, 0.8f);
        world.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 1.5f);

        Particle.DustOptions iceDust = new Particle.DustOptions(Color.fromRGB(173, 216, 230), 3.0f);
        Particle.DustOptions cyanDust = new Particle.DustOptions(Color.AQUA, 2.5f);
        Particle.DustOptions whiteDust = new Particle.DustOptions(Color.WHITE, 2.5f);
        Particle.DustOptions teamDust = new Particle.DustOptions(teamColor, 2.0f);

        // 凍結フェーズ（最初）- より派手に
        world.spawnParticle(Particle.SNOWFLAKE, center, 200, 2.0, 2.0, 2.0, 0.1);
        world.spawnParticle(Particle.BLOCK, center, 50, 1.5, 1.5, 1.5, 0.15, Material.BLUE_ICE.createBlockData());
        world.spawnParticle(Particle.END_ROD, center, 30, 1.5, 1.5, 1.5, 0.05);

        new BukkitRunnable() {
            int ticks = 0;
            final int freezePhase = 25;
            final int duration = 70;
            boolean shattered = false;
            double iceFormation = 0;

            @Override
            public void run() {
                if (ticks >= duration) {
                    cancel();
                    return;
                }

                if (ticks < freezePhase) {
                    // 凍結フェーズ - 収束する氷（より派手に）
                    double progress = ticks / (double) freezePhase;
                    double radius = 3.0 * (1 - progress);
                    iceFormation += 0.15;

                    // 収束する氷パーティクル
                    for (int i = 0; i < 40; i++) {
                        double angle = random.nextDouble() * Math.PI * 2;
                        double r = radius * (0.3 + random.nextDouble() * 0.7);
                        double x = Math.cos(angle) * r;
                        double z = Math.sin(angle) * r;
                        double y = random.nextDouble() * 2;

                        Location particleLoc = center.clone().add(x, y, z);
                        Particle.DustOptions dust = (i % 3 == 0) ? iceDust : (i % 3 == 1) ? cyanDust : whiteDust;
                        world.spawnParticle(Particle.DUST, particleLoc, 2, 0.1, 0.1, 0.1, 0, dust);
                    }

                    // 成長する氷の結晶
                    for (int i = 0; i < 6; i++) {
                        double angle = i * Math.PI / 3;
                        for (double h = 0; h < iceFormation; h += 0.3) {
                            double x = Math.cos(angle) * h * 0.3;
                            double z = Math.sin(angle) * h * 0.3;
                            Location crystalLoc = center.clone().add(x, h * 0.5, z);
                            world.spawnParticle(Particle.DUST, crystalLoc, 2, 0.05, 0.1, 0.05, 0, cyanDust);
                        }
                    }

                    world.spawnParticle(Particle.SNOWFLAKE, center, 30, 1.0, 1.0, 1.0, 0.05);
                    world.spawnParticle(Particle.END_ROD, center, 5, 0.5, 0.5, 0.5, 0.02);

                    // 凍結音
                    if (ticks % 8 == 0) {
                        world.playSound(center, Sound.BLOCK_POWDER_SNOW_STEP, 0.8f, 0.5f + random.nextFloat() * 0.3f);
                    }

                } else if (!shattered) {
                    // 砕けるフェーズ
                    shattered = true;
                    world.playSound(center, Sound.BLOCK_GLASS_BREAK, 2.5f, 0.5f);
                    world.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.5f);
                    world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);

                    // 大爆発
                    world.spawnParticle(Particle.EXPLOSION_EMITTER, center, 1, 0, 0, 0, 0);
                    world.spawnParticle(Particle.FLASH, center, 2, 0.3, 0.3, 0.3, 0);

                    // 大量の氷の破片（四方八方に飛び散る）
                    world.spawnParticle(Particle.BLOCK, center, 150, 3.0, 2.0, 3.0, 0.3, Material.BLUE_ICE.createBlockData());
                    world.spawnParticle(Particle.BLOCK, center, 100, 2.5, 1.5, 2.5, 0.2, Material.ICE.createBlockData());
                    world.spawnParticle(Particle.ITEM, center, 80, 2.0, 1.5, 2.0, 0.2, new org.bukkit.inventory.ItemStack(Material.ICE));

                    // 衝撃波
                    for (int i = 0; i < 24; i++) {
                        double angle = i * Math.PI / 12;
                        double x = Math.cos(angle) * 2;
                        double z = Math.sin(angle) * 2;
                        Location shockLoc = center.clone().add(x, 0.3, z);
                        world.spawnParticle(Particle.DUST, shockLoc, 5, 0.2, 0.1, 0.2, 0, whiteDust);
                        world.spawnParticle(Particle.SNOWFLAKE, shockLoc, 3, 0.1, 0.1, 0.1, 0.05);
                    }

                } else {
                    // 砕けた後 - 散らばる破片（継続）
                    world.spawnParticle(Particle.DUST, center, 30, 3.0, 1.5, 3.0, 0, whiteDust);
                    world.spawnParticle(Particle.DUST, center, 20, 2.5, 1.0, 2.5, 0, iceDust);
                    world.spawnParticle(Particle.DUST, center, 15, 2.0, 0.8, 2.0, 0, teamDust);
                    world.spawnParticle(Particle.SNOWFLAKE, center, 20, 2.5, 1.0, 2.5, 0.15);

                    // 落ちてくる氷の粉
                    for (int i = 0; i < 10; i++) {
                        Location fallLoc = center.clone().add(
                            (random.nextDouble() - 0.5) * 4,
                            3 - (ticks - freezePhase) * 0.1,
                            (random.nextDouble() - 0.5) * 4
                        );
                        world.spawnParticle(Particle.DUST, fallLoc, 2, 0.1, 0.1, 0.1, 0, cyanDust);
                    }
                }

                ticks += 4;
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }
    
    /**
     * ネザーポータルエフェクト（レベル4 - 派手）
     * ポータルの渦 - 軽量化MOD対応版
     */
    private void playNetherPortal(Location location, Color teamColor) {
        Location center = location.clone();
        World world = location.getWorld();
        if (world == null) return;

        // ポータルサウンド
        world.playSound(center, Sound.BLOCK_PORTAL_TRIGGER, 1.5f, 0.8f);
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.7f);
        world.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.7f);

        // 明るく見やすい紫系の色（大きめサイズ）
        Particle.DustOptions brightPurpleDust = new Particle.DustOptions(Color.fromRGB(200, 50, 255), 4.0f);
        Particle.DustOptions magentaDust = new Particle.DustOptions(Color.fromRGB(255, 0, 180), 3.5f);
        Particle.DustOptions pinkDust = new Particle.DustOptions(Color.fromRGB(255, 150, 255), 3.0f);
        Particle.DustOptions teamDust = new Particle.DustOptions(teamColor, 3.5f);
        Particle.DustOptions whiteDust = new Particle.DustOptions(Color.WHITE, 2.5f);

        // 初期爆発
        spawnParticle(world, Particle.EXPLOSION, center, 3, 0.5, 0.5, 0.5, 0);

        new BukkitRunnable() {
            int ticks = 0;
            final int duration = 70;
            double rotation = 0;
            double portalHeight = 0;

            @Override
            public void run() {
                if (ticks >= duration) {
                    cancel();
                    return;
                }

                // ポータルの高さ（開いてから閉じる）
                if (ticks < 30) {
                    portalHeight = Math.min(portalHeight + 0.2, 4.0);
                } else if (ticks > 50) {
                    portalHeight = Math.max(portalHeight - 0.25, 0);
                }

                // 回転するポータル枠（外側）
                for (double y = 0; y < portalHeight; y += 0.2) {
                    double waveRadius = 1.2 + Math.sin(y * 2 + rotation) * 0.3;

                    for (int i = 0; i < 16; i++) {
                        double angle = rotation + (i * Math.PI * 2 / 16) + (y * 0.3);
                        double x = Math.cos(angle) * waveRadius;
                        double z = Math.sin(angle) * waveRadius;

                        Location ringLoc = center.clone().add(x, y, z);
                        spawnDust(world, ringLoc, 2, 0.05, 0.05, 0.05, brightPurpleDust);
                    }
                }

                // 内側の渦巻き（螺旋状に中心へ）
                for (int spiral = 0; spiral < 3; spiral++) {
                    double baseAngle = rotation * 2 + (spiral * Math.PI * 2 / 3);
                    for (double r = 1.0; r > 0.1; r -= 0.15) {
                        for (double y = 0; y < portalHeight; y += 0.4) {
                            double angle = baseAngle + (1.0 - r) * 3 + y * 0.5;
                            double x = Math.cos(angle) * r;
                            double z = Math.sin(angle) * r;

                            Location spiralLoc = center.clone().add(x, y, z);
                            spawnDust(world, spiralLoc, 1, 0.03, 0.03, 0.03, magentaDust);
                        }
                    }
                }

                // 中心の光の柱
                for (double y = 0; y < portalHeight; y += 0.3) {
                    Location pillarLoc = center.clone().add(0, y, 0);
                    spawnDust(world, pillarLoc, 5, 0.2, 0.1, 0.2, pinkDust);
                    spawnParticle(world, Particle.END_ROD, pillarLoc, 2, 0.15, 0.1, 0.15, 0.01);
                }

                // 地面のリング
                for (int i = 0; i < 20; i++) {
                    double angle = rotation * 1.5 + (i * Math.PI / 10);
                    double radius = 1.5;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    Location groundLoc = center.clone().add(x, 0.1, z);
                    spawnDust(world, groundLoc, 2, 0.1, 0.05, 0.1, brightPurpleDust);
                    spawnParticle(world, Particle.FLAME, groundLoc, 1, 0.05, 0.02, 0.05, 0);
                }

                // エンチャントエフェクト（吸い込み感）
                spawnParticle(world, Particle.ENCHANT, center.clone().add(0, portalHeight / 2, 0), 40, 1.0, portalHeight / 2, 1.0, 1.5);

                // チームカラーのフラッシュ
                if (ticks % 6 == 0) {
                    spawnDust(world, center.clone().add(0, portalHeight / 2, 0), 20, 0.8, portalHeight / 2, 0.8, teamDust);
                    spawnParticle(world, Particle.FIREWORK, center.clone().add(0, portalHeight / 2, 0), 5, 0.5, 0.5, 0.5, 0.05);
                }

                // 音
                if (ticks % 20 == 0 && portalHeight > 1) {
                    world.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.8f, 0.5f);
                }

                rotation += 0.2;
                ticks += 4;
            }
        }.runTaskTimer(plugin, 0L, 4L);

        // 消滅エフェクト
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            world.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 1.2f);
            world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
            spawnParticle(world, Particle.EXPLOSION_EMITTER, center.clone().add(0, 2, 0), 1, 0, 0, 0, 0);
            spawnParticle(world, Particle.FLASH, center.clone().add(0, 2, 0), 3, 0.5, 0.5, 0.5, 0);
            spawnDust(world, center, 100, 2.0, 3.0, 2.0, brightPurpleDust);
            spawnParticle(world, Particle.END_ROD, center.clone().add(0, 2, 0), 80, 2.0, 2.0, 2.0, 0.2);
            spawnParticle(world, Particle.TOTEM_OF_UNDYING, center, 50, 1.5, 2.0, 1.5, 0.5);
        }, 55L);
    }
    
    /**
     * 虹爆発エフェクト（レベル5 - 超派手）
     * 7色の虹が広がる
     */
    private void playRainbowBurst(Location location, Color teamColor) {
        Location center = location.clone();
        World world = location.getWorld();
        if (world == null) return;

        // 華やかなサウンド
        world.playSound(center, Sound.ENTITY_PLAYER_LEVELUP, 1.5f, 1.5f);
        world.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 2.0f, 1.0f);
        world.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 1.8f);
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);
        world.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1.5f, 1.0f);

        // 虹の7色
        Color[] rainbowColors = {
            Color.RED,
            Color.ORANGE,
            Color.YELLOW,
            Color.fromRGB(0, 255, 0),
            Color.fromRGB(0, 191, 255),
            Color.fromRGB(65, 105, 225),
            Color.fromRGB(148, 0, 211)
        };

        // 初期の爆発（より派手に）
        world.spawnParticle(Particle.EXPLOSION_EMITTER, center, 1, 0, 0, 0, 0);
        world.spawnParticle(Particle.EXPLOSION, center, 5, 1.0, 1.0, 1.0, 0);
        world.spawnParticle(Particle.FLASH, center, 3, 0.5, 0.5, 0.5, 0);
        world.spawnParticle(Particle.TOTEM_OF_UNDYING, center, 100, 2.0, 2.0, 2.0, 0.5);

        // 初期の虹の球状爆発
        for (int colorIndex = 0; colorIndex < rainbowColors.length; colorIndex++) {
            Particle.DustOptions dust = new Particle.DustOptions(rainbowColors[colorIndex], 3.5f);
            double radius = 0.5 + colorIndex * 0.3;
            for (int i = 0; i < 30; i++) {
                double theta = random.nextDouble() * Math.PI * 2;
                double phi = random.nextDouble() * Math.PI;
                double x = radius * Math.sin(phi) * Math.cos(theta);
                double y = radius * Math.cos(phi);
                double z = radius * Math.sin(phi) * Math.sin(theta);
                world.spawnParticle(Particle.DUST, center.clone().add(x, y + 1, z), 3, 0.1, 0.1, 0.1, 0, dust);
            }
        }

        new BukkitRunnable() {
            int ticks = 0;
            final int duration = 80;
            double ringRadius = 0;
            double rotation = 0;

            @Override
            public void run() {
                if (ticks >= duration) {
                    cancel();
                    return;
                }

                // 広がる虹のリング（より太く、より多く）
                ringRadius += 0.2;
                if (ringRadius < 6) {
                    for (int colorIndex = 0; colorIndex < rainbowColors.length; colorIndex++) {
                        double layerRadius = ringRadius - (colorIndex * 0.2);
                        if (layerRadius > 0) {
                            Particle.DustOptions dust = new Particle.DustOptions(rainbowColors[colorIndex], 3.0f);
                            for (int i = 0; i < 36; i++) {
                                double angle = Math.toRadians(i * 10) + rotation;
                                double x = Math.cos(angle) * layerRadius;
                                double z = Math.sin(angle) * layerRadius;
                                double y = Math.sin(layerRadius * 0.5 + colorIndex * 0.3) * 0.5;

                                Location ringLoc = center.clone().add(x, y + 0.5, z);
                                world.spawnParticle(Particle.DUST, ringLoc, 2, 0.08, 0.08, 0.08, 0, dust);
                            }
                        }
                    }
                }

                // 上昇する虹の螺旋柱（より密に）
                double maxHeight = Math.min(ticks * 0.15, 6);
                for (int colorIndex = 0; colorIndex < rainbowColors.length; colorIndex++) {
                    double baseAngle = rotation * 2 + (colorIndex * Math.PI * 2 / 7);

                    for (double y = 0; y < maxHeight; y += 0.2) {
                        double columnRadius = 0.6 + Math.sin(y + ticks * 0.1) * 0.2;
                        double angle = baseAngle + y * 1.5;
                        double x = Math.cos(angle) * columnRadius;
                        double z = Math.sin(angle) * columnRadius;

                        Particle.DustOptions dust = new Particle.DustOptions(rainbowColors[colorIndex], 2.5f);
                        world.spawnParticle(Particle.DUST, center.clone().add(x, y, z), 2, 0.05, 0.05, 0.05, 0, dust);
                    }
                }

                // キラキラ（大量）
                world.spawnParticle(Particle.END_ROD, center.clone().add(0, 2, 0), 15, 2.0, 2.0, 2.0, 0.05);
                world.spawnParticle(Particle.FIREWORK, center, 25, 2.5, 1.5, 2.5, 0.15);
                world.spawnParticle(Particle.TOTEM_OF_UNDYING, center.clone().add(0, 1, 0), 10, 1.5, 1.0, 1.5, 0.2);

                // 降り注ぐ虹の粒
                for (int i = 0; i < 15; i++) {
                    Color color = rainbowColors[random.nextInt(rainbowColors.length)];
                    Particle.DustOptions dust = new Particle.DustOptions(color, 2.0f);
                    Location fallLoc = center.clone().add(
                        (random.nextDouble() - 0.5) * 5,
                        4 + random.nextDouble() * 2,
                        (random.nextDouble() - 0.5) * 5
                    );
                    world.spawnParticle(Particle.DUST, fallLoc, 3, 0.1, 0.3, 0.1, 0, dust);
                }

                rotation += 0.1;
                ticks += 4;
            }
        }.runTaskTimer(plugin, 0L, 4L);

        // 中間の花火
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            world.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.5f, 1.2f);
            for (Color color : rainbowColors) {
                Particle.DustOptions dust = new Particle.DustOptions(color, 4.0f);
                world.spawnParticle(Particle.DUST, center.clone().add(0, 3, 0), 40, 2.5, 1.5, 2.5, 0, dust);
            }
        }, 30L);

        // フィナーレ
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            world.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 2.0f, 0.8f);
            world.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE_FAR, 1.5f, 1.2f);

            // 全色同時大爆発
            for (int colorIndex = 0; colorIndex < rainbowColors.length; colorIndex++) {
                Particle.DustOptions dust = new Particle.DustOptions(rainbowColors[colorIndex], 4.0f);
                double radius = 1.5 + colorIndex * 0.4;
                for (int i = 0; i < 50; i++) {
                    double theta = random.nextDouble() * Math.PI * 2;
                    double phi = random.nextDouble() * Math.PI;
                    double x = radius * Math.sin(phi) * Math.cos(theta);
                    double y = radius * Math.cos(phi);
                    double z = radius * Math.sin(phi) * Math.sin(theta);
                    world.spawnParticle(Particle.DUST, center.clone().add(x, y + 2, z), 3, 0.15, 0.15, 0.15, 0, dust);
                }
            }
            world.spawnParticle(Particle.TOTEM_OF_UNDYING, center, 150, 3.0, 3.0, 3.0, 0.8);
            world.spawnParticle(Particle.FLASH, center.clone().add(0, 2, 0), 3, 0.5, 0.5, 0.5, 0);
        }, 60L);
    }
    
    /**
     * ソウルアセンドエフェクト（レベル4 - 派手）
     * 魂が昇天する
     */
    private void playSoulAscend(Location location, Color teamColor) {
        Location center = location.clone();
        World world = location.getWorld();
        if (world == null) return;

        // 幽玄なサウンド
        world.playSound(center, Sound.BLOCK_SOUL_SAND_BREAK, 1.0f, 0.5f);
        world.playSound(center, Sound.PARTICLE_SOUL_ESCAPE, 1.5f, 0.8f);
        world.playSound(center, Sound.ENTITY_VEX_AMBIENT, 0.8f, 0.5f);
        
        Particle.DustOptions soulDust = new Particle.DustOptions(Color.fromRGB(64, 224, 208), 2.0f);
        Particle.DustOptions blueDust = new Particle.DustOptions(Color.fromRGB(0, 191, 255), 1.5f);
        Particle.DustOptions teamDust = new Particle.DustOptions(teamColor, 1.8f);
        
        new BukkitRunnable() {
            int ticks = 0;
            final int duration = 80;
            int soulCount = 0;
            final int maxSouls = 5;
            final double[][] soulPositions = new double[maxSouls][2]; // x, z offsets
            final double[] soulHeights = new double[maxSouls];
            
            @Override
            public void run() {
                if (ticks >= duration) {
                    cancel();
                    return;
                }
                
                // 新しい魂を生成
                if (ticks % 12 == 0 && soulCount < maxSouls) {
                    soulPositions[soulCount][0] = (random.nextDouble() - 0.5) * 2;
                    soulPositions[soulCount][1] = (random.nextDouble() - 0.5) * 2;
                    soulHeights[soulCount] = 0;
                    soulCount++;
                    world.playSound(center, Sound.PARTICLE_SOUL_ESCAPE, 0.5f, 0.8f + random.nextFloat() * 0.4f);
                }
                
                // 各魂を描画・上昇
                for (int i = 0; i < soulCount; i++) {
                    soulHeights[i] += 0.12 + random.nextDouble() * 0.05;
                    
                    // 魂の揺らぎ
                    double swayX = Math.sin(ticks * 0.2 + i) * 0.3;
                    double swayZ = Math.cos(ticks * 0.15 + i * 2) * 0.3;
                    
                    Location soulLoc = center.clone().add(
                        soulPositions[i][0] + swayX,
                        soulHeights[i],
                        soulPositions[i][1] + swayZ
                    );
                    
                    // 魂の本体
                    world.spawnParticle(Particle.SOUL, soulLoc, 3, 0.1, 0.1, 0.1, 0.01);
                    world.spawnParticle(Particle.DUST, soulLoc, 5, 0.15, 0.2, 0.15, 0, soulDust);
                    
                    // 魂の軌跡
                    world.spawnParticle(Particle.DUST, soulLoc.clone().add(0, -0.3, 0), 2, 0.1, 0.1, 0.1, 0, blueDust);
                }
                
                // ソウルファイア
                world.spawnParticle(Particle.SOUL_FIRE_FLAME, center, 10, 0.8, 0.3, 0.8, 0.02);
                
                // 地面のエフェクト
                if (ticks < 40) {
                    for (int i = 0; i < 8; i++) {
                        double angle = (ticks * 0.1) + (i * Math.PI / 4);
                        double radius = 1.0;
                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;
                        
                        Location groundLoc = center.clone().add(x, 0.1, z);
                        world.spawnParticle(Particle.DUST, groundLoc, 2, 0.1, 0.05, 0.1, 0, teamDust);
                    }
                }
                
                // 幽玄な音
                if (ticks % 25 == 0) {
                    world.playSound(center, Sound.ENTITY_VEX_AMBIENT, 0.3f, 0.3f + random.nextFloat() * 0.3f);
                }
                
                ticks += 4;
            }
        }.runTaskTimer(plugin, 0L, 4L);
        
        // 最後の昇天
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            world.playSound(center, Sound.ENTITY_ALLAY_DEATH, 1.0f, 0.5f);
            world.spawnParticle(Particle.SOUL, center.clone().add(0, 4, 0), 30, 1.0, 0.5, 1.0, 0.1);
        }, 60L);
    }
    
    /**
     * チーム名からカラーを取得
     */
    public Color getTeamColor(String teamName) {
        if (teamName == null) return Color.WHITE;
        
        switch (teamName.toLowerCase()) {
            case "red":
                return Color.RED;
            case "blue":
                return Color.BLUE;
            case "green":
                return Color.GREEN;
            case "yellow":
                return Color.YELLOW;
            case "aqua":
            case "cyan":
                return Color.AQUA;
            case "white":
                return Color.WHITE;
            case "pink":
                return Color.fromRGB(255, 105, 180);
            case "gray":
            case "grey":
                return Color.GRAY;
            default:
                return Color.WHITE;
        }
    }
    
    /**
     * プレビュー用にエフェクトを再生（デバッグ用）
     */
    public void playPreview(BedDestructionEffect effect, Player player) {
        Location location = player.getLocation().add(
            player.getLocation().getDirection().multiply(3)
        );
        location.setY(player.getLocation().getY());
        
        // プレビューは白色で表示
        playEffect(effect, location, Color.WHITE);
    }
}
