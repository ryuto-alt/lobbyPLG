package myplg.myplg.commands;

import myplg.myplg.PvPGame;
import myplg.myplg.map.MapData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * マップの待機場所（lobbySpawn）を設定するコマンド
 * 現在立っている位置をマップの待機スポーンとして設定する
 */
public class SetLobbyCommand implements CommandExecutor {

    private final PvPGame plugin;

    public SetLobbyCommand(PvPGame plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cこのコマンドはプレイヤーのみ使用できます。");
            return true;
        }

        Player player = (Player) sender;
        String worldName = player.getWorld().getName();

        // 現在のワールドからマップを特定
        MapData mapData = plugin.getMapDataManager().getMapByWorldName(worldName);

        if (mapData == null) {
            player.sendMessage("§c現在のワールド「" + worldName + "」に対応するマップが見つかりません。");
            player.sendMessage("§7ゲームワールド内で実行してください。");
            return true;
        }

        // 現在位置をlobbySpawnとして設定
        mapData.setLobbySpawn(player.getLocation());

        // 保存
        plugin.getMapDataManager().saveMap(mapData);

        player.sendMessage("§a§l[マップ設定] §fマップ「" + mapData.getDisplayName() + "」の待機場所を設定しました！");
        player.sendMessage("§7座標: " + String.format("%.1f, %.1f, %.1f",
            player.getLocation().getX(),
            player.getLocation().getY(),
            player.getLocation().getZ()));

        return true;
    }
}
