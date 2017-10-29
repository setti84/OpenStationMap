package org.openstationmap.openstationmap.objects;

import android.graphics.Color;

/**
 * Created by sebastian on 10/13/17.
 */

public enum BahnColor {

    corridor(Color.parseColor("#EDEDED")),
    room(Color.parseColor("#FCC21F")),
    roomStroke(Color.parseColor("#7E7760")),
    roomNoAccess(Color.parseColor("#D5D5D5")),
    platform(Color.parseColor("#C1C1C0")),
    elevator(Color.parseColor("#C4C8CC")),
    facilities(Color.parseColor("#066C9E")),
    service(Color.parseColor("#E7342B")),
    footway(Color.parseColor("#E3E3E3")),
    steps(Color.parseColor("#7e7f80")),
    door(Color.parseColor("#EDEDED"));

    private final int color;

    BahnColor(int color) {
        this.color = color;
    }

    public int getColor() {
        return color;
    }


}
