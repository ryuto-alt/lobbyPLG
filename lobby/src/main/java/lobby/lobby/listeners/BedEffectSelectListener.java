package lobby.lobby.listeners;

import lobby.lobby.Lobby;
import lobby.lobby.gui.BedEffectSelectGUI;
import lobby.lobby.gui.BedEffectSelectGUI.BedEffect;
import lobby.lobby.gui.ProfileMenuGUI;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * ベッド破壊エフェクト選択GUIのリスナー
 * 全12種類のエフェクトに対応
 */
public class BedEffectSelectListener implements Listener {
    
    private final Lobby plugin;
    private final BedEffectSelectGUI effectGUI;
    
    // エフェクトが配置されているスロット
    private static final int[] EFFECT_SLOTS = {10, 12, 14, 16, 19, 21, 23, 28, 30, 32, 34, 37};
    private static final int BACK_BUTTON_SLOT = 45;
    
    public BedEffectSelectListener(Lobby plugin, BedEffectSelectGUI effectGUI) {
        this.plugin = plugin;
        this.effectGUI = effectGUI;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        String title = event.getView().getTitle();
        if (!title.equals(BedEffectSelectGUI.GUI_TITLE)) return;
        
        event.setCancelled(true);
        
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        ItemStack clickedItem = event.getCurrentItem();
        ClickType clickType = event.getClick();
        
        if (clickedItem == null || clickedItem.getType().isAir()) return;
        
        // 戻るボタン
        if (slot == BACK_BUTTON_SLOT) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            ProfileMenuGUI.openProfileMenu(player, plugin);
            return;
        }
        
        // エフェクト選択スロットをチェック
        BedEffect[] effects = BedEffect.values();
        
        for (int i = 0; i < EFFECT_SLOTS.length && i < effects.length; i++) {
            if (slot == EFFECT_SLOTS[i]) {
                BedEffect effect = effects[i];
                
                // 右クリック = プレビュー
                if (clickType == ClickType.RIGHT || clickType == ClickType.SHIFT_RIGHT) {
                    handleEffectPreview(player, effect);
                } 
                // 左クリック = 選択
                else {
                    handleEffectSelect(player, effect);
                }
                return;
            }
        }
    }
    
    /**
     * エフェクト選択を処理
     */
    private void handleEffectSelect(Player player, BedEffect effect) {
        boolean isUnlocked = effectGUI.isUnlocked(player.getUniqueId(), effect);
        
        // 既に選択中のエフェクトか確認
        String currentSelected = effectGUI.getSelectedEffect(player.getUniqueId());
        if (currentSelected.equals(effect.getId())) {
            player.sendMessage("§7このエフェクトは既に選択されています。");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
            return;
        }
        
        // 解放されていない場合
        if (!isUnlocked) {
            player.sendMessage("§c§l✖ §cこのエフェクトはまだ解放されていません！");
            player.sendMessage("§7課金で解放できます。");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 0.8f);
            return;
        }
        
        // エフェクトを選択
        effectGUI.setSelectedEffect(player.getUniqueId(), effect.getId());
        player.sendMessage("§a§l✓ §aエフェクトを " + effect.getDisplayName() + " §aに変更しました！");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
        
        // GUIを更新
        effectGUI.openEffectSelectGUI(player);
    }
    
    /**
     * エフェクトプレビューを処理
     */
    private void handleEffectPreview(Player player, BedEffect effect) {
        boolean isUnlocked = effectGUI.isUnlocked(player.getUniqueId(), effect);
        
        // 未解放でもプレビューは可能（管理者、または全員）
        // プレビューを再生
        player.closeInventory();
        effectGUI.playPreview(player, effect);
        
        // 3秒後にGUIを再度開く
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                effectGUI.openEffectSelectGUI(player);
            }
        }, 80L); // 4秒後
    }
}
