package com.codamasters.grbus;

/**
 * Created by Juan on 05/07/2016.
 */
public class Bus {

    private String name;
    private String time;

    public Bus(String  name, String time){
        this.name = name;
        this.time = time;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
