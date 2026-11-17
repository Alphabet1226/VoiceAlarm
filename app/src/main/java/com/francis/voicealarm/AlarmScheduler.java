package com.francis.voicealarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.List;

public class AlarmScheduler {

    public static final String ACTION_TRIGGER = "com.francis.voicealarm.ACTION_ALARM_TRIGGER";
    public static final String EXTRA_ALARM_ID = "extra_alarm_id";

    public static void scheduleAlarm(Context context, AlarmItem item) {
        if (!item.enabled) {
            cancelAlarm(context, item);
            return;
        }

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        long triggerAt = item.getNextTriggerTimeMillis();

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(ACTION_TRIGGER);
        intent.putExtra(EXTRA_ALARM_ID, item.id);

        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                (int) item.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 使用 setAlarmClock：系统最高优先级闹钟机制，状态栏显示闹钟图标，并能穿透各种省电优化与勿扰限制
        Intent showIntent = new Intent(context, MainActivity.class);
        PendingIntent showPi = PendingIntent.getActivity(
                context,
                (int) item.id,
                showIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager.AlarmClockInfo clockInfo = new AlarmManager.AlarmClockInfo(triggerAt, showPi);
        am.setAlarmClock(clockInfo, pi);
    }

    public static void cancelAlarm(Context context, AlarmItem item) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(ACTION_TRIGGER);
        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                (int) item.id,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );

        if (pi != null) {
            am.cancel(pi);
            pi.cancel();
        }
    }

    public static void rescheduleAll(Context context) {
        List<AlarmItem> list = AlarmStorage.loadAlarms(context);
        for (AlarmItem item : list) {
            if (item.enabled) {
                scheduleAlarm(context, item);
            }
        }
    }
}
