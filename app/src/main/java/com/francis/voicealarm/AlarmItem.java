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
    public int repeatDays; // 位掩码: 1=周日, 2=周一, 4=周二, 8=周三, 16=周四, 32=周五, 64=周六 (0表示仅一次)

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

    public String getPeriodLabel() {
        if (hour >= 0 && hour < 6) {
            return "凌晨";
        } else if (hour < 9) {
            return "早上";
        } else if (hour < 12) {
            return "上午";
        } else if (hour == 12) {
            return "中午";
        } else if (hour < 18) {
            return "下午";
        } else {
            return "晚上";
        }
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

    public boolean isDaySelected(int dayIndex) { // 0=Sun, 1=Mon, ..., 6=Sat
        return (repeatDays & (1 << dayIndex)) != 0;
    }

    public void setDaySelected(int dayIndex, boolean selected) {
        if (selected) {
            repeatDays |= (1 << dayIndex);
        } else {
            repeatDays &= ~(1 << dayIndex);
        }
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
            // 仅响一次：若今天该时间已过，定为明天
            if (target.getTimeInMillis() <= now.getTimeInMillis()) {
                target.add(Calendar.DAY_OF_YEAR, 1);
            }
            return target.getTimeInMillis();
        }

        // 重复闹钟：跨度循环查找未来14天内最近的匹配星期
        for (int dayOffset = 0; dayOffset < 14; dayOffset++) {
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

        target.add(Calendar.DAY_OF_YEAR, 1);
        return target.getTimeInMillis();
    }

    /**
     * 计算剩余响铃倒计时的人性化文案
     */
    public String getTimeRemainingDescription() {
        long triggerAt = getNextTriggerTimeMillis();
        long diff = triggerAt - System.currentTimeMillis();
        if (diff <= 0) {
            return "即将响铃";
        }

        long totalMinutes = diff / (1000 * 60);
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;

        if (hours == 0) {
            return minutes + " 分钟后响铃";
        } else if (hours < 24) {
            if (minutes == 0) {
                return hours + " 小时后响铃";
            }
            return hours + " 小时 " + minutes + " 分钟后响铃";
        } else {
            long days = hours / 24;
            long remHours = hours % 24;
            return days + " 天 " + remHours + " 小时后响铃";
        }
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
