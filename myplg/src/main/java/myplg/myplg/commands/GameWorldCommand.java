package myplg.myplg.commands;

import myplg.myplg.PvPGame;
import myplg.myplg.gui.ManagementGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GameWorldCommand implements CommandExecutor {
    private final PvPGame plugin;
    private final ManagementGUI managementGUI;

    public GameWorldCommand(PvPGame plugin) {
        this.plugin = plugin;
        this.managementGUI = new ManagementGUI(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage("§cこのコマンドはOP権限が必要です。");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("§cこのコマンドはプレイヤーのみ使用できます。");
            return true;
        }

        Player player = (Player) sender;

        // マップセレクトGUIを開く
        managementGUI.openMapSelection(player);

        return true;
    }
}
