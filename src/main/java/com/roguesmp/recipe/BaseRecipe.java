package com.roguesmp.recipe;

import com.roguesmp.constant.RecipeType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseRecipe {
    private final String id;
    private final RecipeType type;
    private final List<ItemStack> inputs;
    private final List<ItemStack> outputs;

    public BaseRecipe(String id, RecipeType type){
        this.id = id;
        this.type = type;
        this.inputs = new ArrayList<>();
        this.outputs = new ArrayList<>();
    }

    public BaseRecipe addInput(ItemStack item){
        this.inputs.add(item);
        return this;
    }
    public BaseRecipe addInput(List<ItemStack> items){
        this.inputs.addAll(items);
        return this;
    }

    public BaseRecipe addOutput(ItemStack item){
        this.outputs.add(item);
        return this;
    }

    public BaseRecipe addOutput(List<ItemStack> items){
        this.outputs.addAll(items);
        return this;
    }

    public List<ItemStack> getInputs() {return new ArrayList<>(inputs);}
    public List<ItemStack> getOutputs() {return new ArrayList<>(outputs);}
    public RecipeType getType() {return type;}
    public String getId() {return id;}
}
