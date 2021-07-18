package com.yashoid.openpainting;

import org.json.JSONException;
import org.json.JSONObject;

public class Touch implements Comparable<Touch> {

    public int x;
    public int y;
    public String color;

    public Touch(int x, int y, String color) {
        this.x = x;
        this.y = y;
        this.color = color;
    }

    public Touch(JSONObject jTouch) throws JSONException {
        x = jTouch.getInt("x");
        y = jTouch.getInt("y");
        color = jTouch.getString("c");
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();

        try {
            json.put("x", x);
            json.put("y", y);
            json.put("c", color);
        } catch (JSONException e) { }

        return json;
    }

    @Override
    public int compareTo(Touch o) {
        if (y != o.y) {
            return y - o.y;
        }

        return x - o.x;
    }

}
