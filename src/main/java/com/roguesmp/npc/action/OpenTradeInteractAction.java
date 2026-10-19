package com.roguesmp.npc.action;

import com.roguesmp.codec.Codec;
import com.roguesmp.registry.Holder;
import com.roguesmp.registry.Registries;
import com.roguesmp.registry.Registry;
import com.roguesmp.trade.TradeShop;
import com.roguesmp.trade.gui.TradeGui;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class OpenTradeInteractAction implements NpcAction {

    public static final String TYPE_KEY = "open_trade";
    public static final Codec<OpenTradeInteractAction> CODEC = Codec.composite(
            Registry.referenceCodec(() -> Registries.TRADE_SHOP).fieldOf("shop").forGetter(OpenTradeInteractAction::getShop),
            OpenTradeInteractAction::new
    );

    private final Holder<TradeShop> shop;

    public OpenTradeInteractAction(Holder<TradeShop> shop) {
        this.shop = shop;
    }

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    public Holder<TradeShop> getShop() {
        return shop;
    }

    @Override
    public void onRightClick(PlayerInteractEntityEvent event) {
        if (!shop.isBound()) return;
        Player player = event.getPlayer();
        new TradeGui(player, shop.value()).showInventory(player);
    }
}
