package org.openstationmap.openstationmap.objects;

import android.app.ProgressDialog;

import com.google.android.gms.maps.model.LatLngBounds;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;


/**
 * Created by sebastian on 10/8/17.
 */

public class Station {

    String name;
    HashMap data;
    LatLngBounds bounds;

    public Station(String name, HashMap data, LatLngBounds bounds) {
        this.name = name;
        this.data = data;
        this.bounds = bounds;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public HashMap getData() {
        return data;
    }

    public void setData(HashMap data) {
        this.data = data;
    }

    public LatLngBounds getBounds() {
        return bounds;
    }

    public void setBounds(LatLngBounds bounds) {
        this.bounds = bounds;
    }

}
