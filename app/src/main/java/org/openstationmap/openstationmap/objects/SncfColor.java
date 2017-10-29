package org.openstationmap.openstationmap.objects;

import android.graphics.Color;

/**
 * Created by sebastian on 10/15/17.
 */

public enum SncfColor {

    roomStroke(Color.parseColor("#7E7760")),
    corridor(Color.parseColor("#97999B")),
    commercial(Color.parseColor("#00A1AA")),
    infrastructure(Color.parseColor("#FFB300")),
    emergency(Color.parseColor("#DA291C")),
    service(Color.parseColor("#6C1B72")),
    stairs(Color.BLACK),


    facilities(Color.parseColor("#066C9E")),
    footway(Color.parseColor("#E3E3E3")),
    steps(Color.parseColor("#7e7f80")),
    door(Color.parseColor("#EDEDED"));

    private final int color;

    SncfColor(int color) {
        this.color = color;
    }

    public int getColor() {
        return color;
    }


}
