package com.roguesmp.constant;

import com.google.gson.annotations.SerializedName;

public enum RecipeType {
    @SerializedName("crafting")
    CRAFTING,
    @SerializedName("processing")
    PROCESSING,
    @SerializedName("energy")
    ENERGY
}
