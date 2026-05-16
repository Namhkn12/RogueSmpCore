package com.roguesmp.item.component.impl;

import com.roguesmp.context.ItemDataContext;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.registry.SkinRegistry;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import org.jetbrains.annotations.NotNull;

public class PlayerHeadSkinComponent implements ItemComponent {

    private final String skinId;

    public PlayerHeadSkinComponent(String skinId) {
        this.skinId = skinId;
    }

    @Override
    public @NotNull ItemComponent copy() {
        return this;
    }

    public String getSkinId() {
        return skinId;
    }

    @Override
    public void modifyStack(ItemDataContext context) {
        if (skinId != null) {
            SkinRegistry.SkinData data = SkinRegistry.getInstance().getSkin(skinId);
            if (data != null) {
                context.newStack().setData(DataComponentTypes.PROFILE, ResolvableProfile.resolvableProfile(data.getProfile()));
            }
        }
    }
}
