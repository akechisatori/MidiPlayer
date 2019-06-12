package org.primesoft.midiplayer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class gson {
    private static Gson gson;

    static {

        GsonBuilder gb =  new GsonBuilder();
        gb.serializeNulls();
        gson = gb.create();

    }

    public static JsonObject parseJSON(String json) {
        Gson gson = new Gson();
        JsonObject json_obj = new JsonParser().parse(json).getAsJsonObject();
        return json_obj;
    }

    public static String toJSON(Object map) {
        try {
            String result = gson.toJson(map);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }
}