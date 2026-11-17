package com.francis.voicealarm;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class AlarmStorage {
    private static final String PREF_NAME = "voice_alarm_prefs";
    private static final String KEY_ALARMS = "alarms_list";

    public static List<AlarmItem> loadAlarms(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String raw = sp.getString(KEY_ALARMS, null);
        List<AlarmItem> list = new ArrayList<>();
        if (raw == null || raw.isEmpty()) {
            return list;
        }
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                AlarmItem item = AlarmItem.fromJson(arr.getJSONObject(i));
                if (item != null) {
                    list.add(item);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static void saveAlarms(Context context, List<AlarmItem> list) {
        JSONArray arr = new JSONArray();
        for (AlarmItem item : list) {
            arr.put(item.toJson());
        }
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_ALARMS, arr.toString()).apply();
    }

    public static AlarmItem findAlarm(Context context, long id) {
        List<AlarmItem> list = loadAlarms(context);
        for (AlarmItem item : list) {
            if (item.id == id) {
                return item;
            }
        }
        return null;
    }

    public static void updateAlarm(Context context, AlarmItem target) {
        List<AlarmItem> list = loadAlarms(context);
        boolean found = false;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id == target.id) {
                list.set(i, target);
                found = true;
                break;
            }
        }
        if (!found) {
            list.add(target);
        }
        saveAlarms(context, list);
    }

    public static void deleteAlarm(Context context, long id) {
        List<AlarmItem> list = loadAlarms(context);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id == id) {
                list.remove(i);
                break;
            }
        }
        saveAlarms(context, list);
    }
}
