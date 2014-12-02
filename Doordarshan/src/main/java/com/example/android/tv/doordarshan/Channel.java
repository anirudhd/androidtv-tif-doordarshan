package com.example.android.tv.doordarshan;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by anirudhd on 11/26/14.
 */
public class Channel {

    public String name;
    public String url;

    public Channel(String name, String url, String logoUrl) {
        this.name = name;
        this.url = url;
        this.logoUrl = logoUrl;
    }

    public String logoUrl;


    public Channel(String name, String url) {
        this.name = name;
        this.url = url;
    }

}
