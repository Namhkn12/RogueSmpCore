package com.roguesmp.dungeon.objective_;

public abstract class BaseObjective implements IObjective{
    protected boolean completed = false;
    protected ObjCallBack callBack;

    @Override
    public void callBack(ObjCallBack callBack){
        this.callBack = callBack;
    }

    @Override
    public boolean isCompleted(){
        return completed;
    }

    @Override
    public void process(){

    }

    protected void onComplete(){
        if (completed) return;
        completed = true;
        if (callBack != null) callBack.onComplete(this);
    }

}
