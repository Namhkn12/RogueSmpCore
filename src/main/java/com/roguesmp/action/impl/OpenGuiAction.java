package com.roguesmp.action.impl;

import com.roguesmp.action.ActionContext;
import com.roguesmp.action.GuiOpenActions;
import com.roguesmp.action.PlayerAction;
import com.roguesmp.codec.Codec;

public record OpenGuiAction(String gui) implements PlayerAction {

    public static final String TYPE_KEY = "open_gui";
    public static final Codec<OpenGuiAction> CODEC = Codec.composite(
            Codec.STRING.fieldOf("gui").forGetter(OpenGuiAction::gui),
            OpenGuiAction::new
    );

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    @Override
    public boolean run(ActionContext context) {
        return GuiOpenActions.runOpenAction(gui, context.player());
    }
}
