package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
import myplg.myplg.Team;
import myplg.myplg.effects.BedDestructionEffect;
import myplg.myplg.effects.BedDestructionEffectManager;
import myplg.myplg.effects.PlayerEffectDataManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.time.Duration;
import java.util.Random;
import java.util.UUID;

public class BedBreakListener implements Listener {
    private final PvPGame plugin;
    private final Random random;

    public BedBreakListener(PvPGame plugin) {
        this.plugin = plugin;
        this.random = new Random();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBedBreak(BlockBreakEvent event) {
        if (!plugin.getGameManager().isGameRunning()) {
            return;
        }

        Block block = event.getBlock();
        Material blockType = block.getType();

        // Check if it's a bed
        if (!isBed(blockType)) {
            return;
        }

        // Mark that this event has been handled by bed listener
        event.setCancelled(false);

        Player player = event.getPlayer();
        String playerTeam = plugin.getGameManager().getPlayerTeam(player.getUniqueId());

        // Find which team's bed this is
        plugin.getLogger().info("========================================");
        plugin.getLogger().info("ベッドが破壊されました！");
        plugin.getLogger().info("  破壊されたブロック位置: " + block.getLocation());
        plugin.getLogger().info("  ブロック種類: " + blockType);
        plugin.getLogger().info("  破壊したプレイヤー: " + player.getName() + " (チーム: " + playerTeam + ")");
        plugin.getLogger().info("========================================");
        
        int teamCount = 0;
        for (Team team : plugin.getGameManager().getTeams().values()) {
            teamCount++;
            plugin.getLogger().info("チーム #" + teamCount + ": " + team.getName());
            
            Block teamBedBlock = team.getBedBlock();
            if (teamBedBlock == null) {
                plugin.getLogger().warning("  ⚠ ベッドブロックがnullです！");
                continue;
            }

            plugin.getLogger().info("  チームのベッド位置: " + teamBedBlock.getLocation());
            plugin.getLogger().info("  チームのベッド種類: " + teamBedBlock.getType());
            plugin.getLogger().info("  比較開始...");

            // Check if this bed belongs to this team
            if (isSameBed(block, teamBedBlock)) {
                plugin.getLogger().info("========================================");
                plugin.getLogger().info("✓ 一致しました！ チーム「" + team.getName() + "」のベッドが破壊されました");
                plugin.getLogger().info("========================================");
                // Check if player is breaking their own team's bed
                if (team.getName().equals(playerTeam)) {
                    // Cancel - player cannot break their own bed
                    event.setCancelled(true);
                    // Restore the bed block immediately (no message)
                    return;
                }

                // Enemy team breaking bed - allow it but prevent drops
                event.setDropItems(false);

                // Mark bed as destroyed
                plugin.getScoreboardManager().setBedStatus(team.getName(), false);

                // ベッド破壊エフェクトを再生（一旦無効化）
                // playBedDestructionEffect(player, block, team.getName());

                // Get team colors
                String attackerColor = getTeamColor(playerTeam);
                String victimColor = getTeamColor(team.getName());

                // Check for victory after bed destruction
                plugin.getPlayerDeathListener().checkVictoryCondition();

                // ENDモードのチェック（全ベッドが破壊されたか）
                plugin.getEndModeManager().checkAllBedsDestroyed();

                // Notify all players with sounds
                plugin.getLogger().info("全プレイヤーに通知を送信します...");
                plugin.getLogger().info("破壊されたチーム: " + team.getName());
                
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    String onlinePlayerTeam = plugin.getGameManager().getPlayerTeam(onlinePlayer.getUniqueId());
                    plugin.getLogger().info("  プレイヤー: " + onlinePlayer.getName() + ", チーム: " + onlinePlayerTeam);

                    if (team.getName().equals(onlinePlayerTeam)) {
                        plugin.getLogger().info("    → 破壊されたチームのメンバーです！タイトルとウィザー音を再生");
                        
                        // Show title to destroyed team members
                        onlinePlayer.sendTitle(
                            "§c§lベッドが破壊されました！",
                            "§7もうリスポーンできません！",
                            10,  // fade in (ticks)
                            60,  // stay (ticks)
                            20   // fade out (ticks)
                        );

                        // Also send chat message
                        onlinePlayer.sendMessage("§c§l⚠ あなたのチームのベッドが破壊されました！");

                        // Play wither death sound for destroyed team
                        onlinePlayer.playSound(onlinePlayer.getLocation(), Sound.ENTITY_WITHER_DEATH, 1.0f, 1.0f);
                        plugin.getLogger().info("    → ウィザー音を再生しました");
                    } else {
                        plugin.getLogger().info("    → 他のチームです。メッセージとエンドラ音を再生");
                        
                        // Send colored chat message to other teams
                        onlinePlayer.sendMessage(
                            attackerColor + playerTeam + "チーム§fの" +
                            attackerColor + player.getName() + "§fが" +
                            victimColor + team.getName() + "チーム§fのベッドを破壊！"
                        );

                        // Play random ender dragon sound for other teams
                        playRandomEnderDragonSound(onlinePlayer);
                        plugin.getLogger().info("    → エンドラ音を再生しました");
                    }
                }

                return;
            }
        }
        
        // ベッドが見つからなかった場合
        plugin.getLogger().warning("破壊されたベッドがどのチームにも一致しませんでした: " + block.getLocation());
    }

    // Protect beds from explosions
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!plugin.getGameManager().isGameRunning()) {
            return;
        }

        event.blockList().removeIf(block -> {
            if (isBed(block.getType())) {
                // Check if this is a team bed
                for (Team team : plugin.getGameManager().getTeams().values()) {
                    Block teamBedBlock = team.getBedBlock();
                    if (teamBedBlock != null && isSameBed(block, teamBedBlock)) {
                        return true; // Remove from explosion list (protect bed)
                    }
                }
            }
            return false;
        });
    }

    // Protect beds from block explosions (TNT, etc.)
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (!plugin.getGameManager().isGameRunning()) {
            return;
        }

        event.blockList().removeIf(block -> {
            if (isBed(block.getType())) {
                // Check if this is a team bed
                for (Team team : plugin.getGameManager().getTeams().values()) {
                    Block teamBedBlock = team.getBedBlock();
                    if (teamBedBlock != null && isSameBed(block, teamBedBlock)) {
                        return true; // Remove from explosion list (protect bed)
                    }
                }
            }
            return false;
        });
    }

    private boolean isBed(Material material) {
        return material.name().contains("BED") && !material.name().equals("BEDROCK");
    }

    private boolean isSameBed(Block block1, Block block2) {
        // 両方のブロックがベッドであることを確認
        if (!isBed(block1.getType()) || !isBed(block2.getType())) {
            plugin.getLogger().info("  → ベッドではありません: " + block1.getType() + ", " + block2.getType());
            return false;
        }

        // 座標を取得
        int x1 = block1.getX(), y1 = block1.getY(), z1 = block1.getZ();
        int x2 = block2.getX(), y2 = block2.getY(), z2 = block2.getZ();
        
        plugin.getLogger().info("  → 比較: (" + x1 + "," + y1 + "," + z1 + ") vs (" + x2 + "," + y2 + "," + z2 + ")");

        // 同じ位置なら同じベッド
        if (x1 == x2 && y1 == y2 && z1 == z2) {
            plugin.getLogger().info("  → ✓ 完全一致！");
            return true;
        }

        // Y座標が違う場合は別のベッド
        if (y1 != y2) {
            plugin.getLogger().info("  → Y座標が異なります");
            return false;
        }

        // X座標またはZ座標が1つだけ違う場合はベッドの別パーツの可能性
        int xDiff = Math.abs(x1 - x2);
        int zDiff = Math.abs(z1 - z2);
        
        plugin.getLogger().info("  → 差分: xDiff=" + xDiff + ", zDiff=" + zDiff);

        // ベッドは2つのブロックからなるので、X方向またはZ方向に1ブロック隣接している
        if ((xDiff == 1 && zDiff == 0) || (xDiff == 0 && zDiff == 1)) {
            plugin.getLogger().info("  → ✓ 隣接ベッドパーツです！");
            return true;
        }

        plugin.getLogger().info("  → × 一致しません");
        return false;
    }

    /**
     * ベッドブロックからヘッドパートを取得
     */
    private Block getBedHeadBlock(Block bedBlock) {
        if (bedBlock == null || !isBed(bedBlock.getType())) {
            plugin.getLogger().info("    [getBedHeadBlock] null または ベッドではありません");
            return null;
        }

        if (!(bedBlock.getBlockData() instanceof org.bukkit.block.data.type.Bed)) {
            plugin.getLogger().info("    [getBedHeadBlock] Bed データではありません");
            return null;
        }

        org.bukkit.block.data.type.Bed bedData = (org.bukkit.block.data.type.Bed) bedBlock.getBlockData();
        org.bukkit.block.data.type.Bed.Part part = bedData.getPart();
        plugin.getLogger().info("    [getBedHeadBlock] パート: " + part + ", 位置: " + bedBlock.getLocation());
        
        // すでにヘッドパートの場合はそのまま返す
        if (part == org.bukkit.block.data.type.Bed.Part.HEAD) {
            plugin.getLogger().info("    [getBedHeadBlock] すでにヘッドパートです");
            return bedBlock;
        }
        
        // フットパートの場合、facing方向にヘッドパートがある
        org.bukkit.block.BlockFace facing = bedData.getFacing();
        plugin.getLogger().info("    [getBedHeadBlock] フットパートです。facing: " + facing);
        Block headBlock = bedBlock.getRelative(facing);
        
        // ヘッドパートが正しくベッドであることを確認
        if (isBed(headBlock.getType())) {
            plugin.getLogger().info("    [getBedHeadBlock] ヘッドパートを見つけました: " + headBlock.getLocation());
            return headBlock;
        }
        
        plugin.getLogger().info("    [getBedHeadBlock] ヘッドパートが見つかりませんでした。フォールバック");
        return bedBlock; // フォールバック
    }

    private void playRandomEnderDragonSound(Player player) {
        // Play ender dragon growl sound
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
    }

    private String getTeamColor(String teamName) {
        if (teamName == null) return "§f";

        switch (teamName) {
            case "レッド": return "§c";
            case "ブルー": return "§9";
            case "グリーン": return "§a";
            case "イエロー": return "§e";
            case "アクア": return "§b";
            case "ホワイト": return "§f";
            case "ピンク": return "§d";
            case "グレー": return "§7";
            default: return "§f";
        }
    }

    /**
     * ベッド破壊エフェクトを再生
     * @param breaker ベッドを壊したプレイヤー
     * @param bedBlock 破壊されたベッドのブロック
     * @param victimTeamName 破壊されたチーム名
     */
    private void playBedDestructionEffect(Player breaker, Block bedBlock, String victimTeamName) {
        BedDestructionEffectManager effectManager = plugin.getBedEffectManager();
        PlayerEffectDataManager dataManager = plugin.getPlayerEffectDataManager();
        
        if (effectManager == null || dataManager == null) {
            return;
        }
        
        // 壊したプレイヤーが設定しているエフェクトを取得
        BedDestructionEffect effect = dataManager.getSelectedEffect(breaker);
        
        // チームカラーを取得
        Color teamColor = getBukkitColor(victimTeamName);
        
        // エフェクトを再生
        effectManager.playEffect(effect, bedBlock.getLocation(), teamColor);
    }

    /**
     * チーム名からBukkitのColorを取得
     */
    private Color getBukkitColor(String teamName) {
        if (teamName == null) return Color.WHITE;

        switch (teamName) {
            case "レッド": return Color.RED;
            case "ブルー": return Color.BLUE;
            case "グリーン": return Color.GREEN;
            case "イエロー": return Color.YELLOW;
            case "アクア": return Color.AQUA;
            case "ホワイト": return Color.WHITE;
            case "ピンク": return Color.fromRGB(255, 105, 180);
            case "グレー": return Color.GRAY;
            default: return Color.WHITE;
        }
    }
}
