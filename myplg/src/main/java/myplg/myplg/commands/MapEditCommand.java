package myplg.myplg.commands;

import myplg.myplg.PvPGame;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * /mapedit コマンド
 * チーム編集ツールを取得してチーム設定を行う
 */
public class MapEditCommand implements CommandExecutor, TabCompleter {
    
    private final PvPGame plugin;

    public MapEditCommand(PvPGame plugin) {
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

        if (args.length < 1) {
            player.sendMessage("§c使用方法: /mapedit <チーム名>");
            player.sendMessage("§7利用可能なチーム: " + String.join(", ", plugin.getTeamEditToolManager().getAvailableTeamNames()));
            return true;
        }

        String teamName = args[0];

        // チーム編集モードを開始
        plugin.getTeamEditToolManager().startEditSession(player, teamName);

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // チーム名を補完
            for (String team : plugin.getTeamEditToolManager().getAvailableTeamNames()) {
                if (team.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(team);
                }
            }
        }

        return completions;
    }
}
