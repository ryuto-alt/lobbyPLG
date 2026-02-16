package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
import myplg.myplg.map.MapSetupManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.entity.Player;

/**
 * マップ設定モード時のブロッククリックを処理するリスナー
 */
public class MapSetupListener implements Listener {
    private final PvPGame plugin;

    public MapSetupListener(PvPGame plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // マップ編集中かチェック
        if (!plugin.getMapSetupManager().isEditing(player)) {
            return;
        }

        // 右クリックのみ処理
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        // ブロックがあるか確認
        if (event.getClickedBlock() == null) {
            return;
        }

        // セットアップマネージャーでハンドリング
        boolean handled = plugin.getMapSetupManager().handleBlockClick(player, event.getClickedBlock());

        if (handled) {
            event.setCancelled(true);
        }
    }
}
