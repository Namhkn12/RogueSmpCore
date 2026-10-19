package com.roguesmp.trade;

import com.roguesmp.codec.Codec;

import java.util.List;

public class TradeShop {

    public static final Codec<TradeShop> CODEC = Codec.composite(
            Codec.STRING.fieldOf("id").forGetter(TradeShop::getId),
            Codec.STRING.optionalFieldOf("title", "<dark_gray>Cửa hàng").forGetter(TradeShop::getTitle),
            TradeLayout.CODEC.optionalFieldOf("layout", TradeLayout.DEFAULT).forGetter(TradeShop::getLayout),
            Codec.listOf(TradeOffer.CODEC).fieldOf("offers").forGetter(TradeShop::getOffers),
            TradeShop::new
    );

    private final String id;
    private final String title;
    private final TradeLayout layout;
    private final List<TradeOffer> offers;

    public TradeShop(String id, String title, TradeLayout layout, List<TradeOffer> offers) {
        this.id = id;
        this.title = title;
        this.layout = layout;
        this.offers = offers;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public TradeLayout getLayout() {
        return layout;
    }

    public List<TradeOffer> getOffers() {
        return offers;
    }
}
