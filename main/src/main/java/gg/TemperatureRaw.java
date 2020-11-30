package gg;

import com.google.gson.Gson;

public class TemperatureRaw {
    private final String wort;
    private final String room;

    protected TemperatureRaw(String wort, String room) {
        this.wort = wort;
        this.room = room;
    }

    public String toJSON() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
