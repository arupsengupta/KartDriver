package com.example.mapwithmarker;

/**
 * Created by DELL on 06-01-2018.
 */

public class UserCard {
    private String name;
    private String phone;
    private String pickup;
    private String drop;

    public UserCard(){}

    public UserCard(String name, String phone, String pickup, String drop) {
        this.name = name;
        this.phone = phone;
        this.pickup = pickup;
        this.drop = drop;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone(){ return phone; }

    public void setPhone(String phone) { this.phone = phone; }

    public String getPickup() {
        return pickup;
    }

    public void setPickup(String pickup) {
        this.pickup = pickup;
    }

    public String getDrop() {
        return drop;
    }

    public void setDrop(String drop) {
        this.drop = drop;
    }
}
