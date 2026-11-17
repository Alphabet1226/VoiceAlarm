package com.francis.voicealarm;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Locale;

public class AlarmItem {
    public long id;
    public int hour;
    public int minute;
    public boolean enabled;
    public String label;
    public int repeatDays; // 位掩码: 1=周日, 2=周一, 4=周二, ..., 64=周六 (0表示仅一次)

    public AlarmItem() {
        this.id = System.currentTimeMillis();
        this.hour = 7;
        this.minute = 0;
        this.enabled = true;
        this.label = "";
        this.repeatDays = 0;
    }

    public AlarmItem(long id, int hour, int minute, boolean enabled, String label, int repeatDays) {
        this.id = id;
        this.hour = hour;
        this.minute = minute;
        this.enabled = enabled;
        this.label = label == null ? "" : label;
        this.repeatDays = repeatDays;
    }

    public String getFormattedTime() {
        return String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
    }

    public String getRepeatDescription() {
        if (repeatDays == 0) {
            return "仅响一次";
        }
        if (repeatDays == 127) { // 1+2+4+8+16+32+64
            return "每天";
        }
        if (repeatDays == 62) { // 周一至周五 (2+4+8+16+32)
            return "工作日";
        }
        if (repeatDays == 65) { // 周六周日 (1+64)
            return "周末";
        }

        StringBuilder sb = new StringBuilder();
        String[] days = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
        for (int i = 0; i < 7; i++) {
            if ((repeatDays & (1 << i)) != 0) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(days[i]);
            }
        }
        return sb.toString();
    }

    /**
     * 计算下一次响铃的绝对时间毫秒数
     */
    public long getNextTriggerTimeMillis() {
        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.set(Calendar.HOUR_OF_DAY, hour);
        target.set(Calendar.MINUTE, minute);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);

        if (repeatDays == 0) {
            // 仅响一次：如果今天时间已过，定为明天
            if (target.getTimeInMillis() <= now.getTimeInMillis()) {
                target.add(Calendar.DAY_OF_YEAR, 1);
            }
            return target.getTimeInMillis();
        }

        // 重复闹钟：寻找最近匹配的星期几
        for (int dayOffset = 0; dayOffset < 7; dayOffset++) {
            Calendar candidate = (Calendar) target.clone();
            candidate.add(Calendar.DAY_OF_YEAR, dayOffset);
            if (candidate.getTimeInMillis() <= now.getTimeInMillis()) {
                continue;
            }
            int dayOfWeek = candidate.get(Calendar.DAY_OF_WEEK); // 1 = Sunday, 7 = Saturday
            int mask = 1 << (dayOfWeek - 1);
            if ((repeatDays & mask) != 0) {
                return candidate.getTimeInMillis();
            }
        }

        // 如果当天同一时间但还没到（例如刚好匹配）
        target.add(Calendar.DAY_OF_YEAR, 7);
        return target.getTimeInMillis();
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("id", id);
            obj.put("hour", hour);
            obj.put("minute", minute);
            obj.put("enabled", enabled);
            obj.put("label", label);
            obj.put("repeatDays", repeatDays);
        } catch (JSONException ignored) {
        }
        return obj;
    }

    public static AlarmItem fromJson(JSONObject obj) {
        if (obj == null) return null;
        AlarmItem item = new AlarmItem();
        item.id = obj.optLong("id", System.currentTimeMillis());
        item.hour = obj.optInt("hour", 7);
        item.minute = obj.optInt("minute", 0);
        item.enabled = obj.optBoolean("enabled", true);
        item.label = obj.optString("label", "");
        item.repeatDays = obj.optInt("repeatDays", 0);
        return item;
    }
}
