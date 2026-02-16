package myplg.myplg.commands;

import myplg.myplg.PvPGame;
import myplg.myplg.gui.ManagementGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * マップ選択GUIを開くコマンド
 */
public class MapSelectCommand implements CommandExecutor {

    private final PvPGame plugin;
    private final ManagementGUI managementGUI;

    public MapSelectCommand(PvPGame plugin) {
        this.plugin = plugin;
        this.managementGUI = new ManagementGUI(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cこのコマンドはプレイヤーのみ使用できます。");
            return true;
        }

        Player player = (Player) sender;
        managementGUI.openMapSelection(player);
        return true;
    }
}
