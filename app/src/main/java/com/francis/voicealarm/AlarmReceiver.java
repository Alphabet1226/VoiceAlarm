package com.francis.voicealarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
            Intent.ACTION_TIME_CHANGED.equals(action) ||
            Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
            // 系统重启或时区变更时，重新编排所有活跃闹钟
            AlarmScheduler.rescheduleAll(context);
            return;
        }

        if (AlarmScheduler.ACTION_TRIGGER.equals(action)) {
            long alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1);
            if (alarmId == -1) return;

            AlarmItem item = AlarmStorage.findAlarm(context, alarmId);
            if (item == null || !item.enabled) return;

            // 启动前台语音报时服务
            Intent serviceIntent = new Intent(context, AlarmService.class);
            serviceIntent.putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }

            // 如果该闹钟只响一次，响完后自动置为未启用并保存
            if (item.repeatDays == 0) {
                item.enabled = false;
                AlarmStorage.updateAlarm(context, item);
            } else {
                // 如果是周期闹钟，立即编排下一次触发
                AlarmScheduler.scheduleAlarm(context, item);
            }
        }
    }
}
