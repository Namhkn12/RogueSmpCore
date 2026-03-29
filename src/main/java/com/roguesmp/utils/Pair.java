package com.roguesmp.utils;

public class Pair<T,E> {
    T first;
    E second;

    public Pair(T first, E second){
        this.first = first;
        this.second = second;
    }

    public void setFirst(T value){
        this.first = value;
    }

    public T getFirst(){
        return first;
    }

    public void setSecond(E value){
        this.second = value;
    }

    public E getSecond(){
        return second;
    }
}
