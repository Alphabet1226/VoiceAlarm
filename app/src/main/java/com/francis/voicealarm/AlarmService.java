package com.francis.voicealarm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import java.util.Calendar;
import java.util.Locale;

public class AlarmService extends Service implements TextToSpeech.OnInitListener {

    public static final String ACTION_STOP_ALARM = "com.francis.voicealarm.ACTION_STOP_ALARM";
    public static final String ACTION_SNOOZE_ALARM = "com.francis.voicealarm.ACTION_SNOOZE_ALARM";
    private static final String CHANNEL_ID = "voice_alarm_channel";
    private static final int NOTIFICATION_ID = 2026;

    private PowerManager.WakeLock wakeLock;
    private TextToSpeech tts;
    private Vibrator vibrator;
    private Handler handler;
    private boolean isTtsReady = false;
    private boolean isDestroyed = false;

    private long currentAlarmId = -1;
    private String alarmLabel = "";
    private int currentHour = 7;
    private int currentMinute = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "VoiceAlarm::WakeLock");
            wakeLock.acquire(10 * 60 * 1000L); // 锁定唤醒，确保播报不受休眠阻断
        }

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        tts = new TextToSpeech(this, this);

        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "VoiceAlarm 语音报时",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("用于闹钟响起时的语音播报及全屏提醒");
            channel.setSound(null, null); // 声音由 TTS 独占控制
            channel.enableVibration(true);
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_STOP_ALARM.equals(action)) {
                stopSelf();
                return START_NOT_STICKY;
            } else if (ACTION_SNOOZE_ALARM.equals(action)) {
                long alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1);
                snooze(alarmId);
                stopSelf();
                return START_NOT_STICKY;
            }

            currentAlarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1);
            AlarmItem item = AlarmStorage.findAlarm(this, currentAlarmId);
            if (item != null) {
                alarmLabel = item.label;
                currentHour = item.hour;
                currentMinute = item.minute;
            } else {
                Calendar c = Calendar.getInstance();
                currentHour = c.get(Calendar.HOUR_OF_DAY);
                currentMinute = c.get(Calendar.MINUTE);
            }
        }

        // 构建全屏 Intent
        Intent fullScreenIntent = new Intent(this, AlarmAlertActivity.class);
        fullScreenIntent.putExtra(AlarmScheduler.EXTRA_ALARM_ID, currentAlarmId);
        fullScreenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                this,
                0,
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent stopIntent = new Intent(this, AlarmService.class);
        stopIntent.setAction(ACTION_STOP_ALARM);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this,
                1,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        Notification notification = builder
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("VoiceAlarm 语音闹钟正在响铃")
                .setContentText(TTSPreviewHelper.buildSpokenText(currentHour, currentMinute, alarmLabel))
                .setPriority(Notification.PRIORITY_MAX)
                .setCategory(Notification.CATEGORY_ALARM)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setContentIntent(fullScreenPendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "关闭闹钟", stopPendingIntent)
                .setOngoing(true)
                .build();

        startForeground(NOTIFICATION_ID, notification);

        startAlertRoutine();

        return START_STICKY;
    }

    private void snooze(long alarmId) {
        AlarmItem item = AlarmStorage.findAlarm(this, alarmId);
        AlarmItem snoozeItem = new AlarmItem();
        Calendar c = Calendar.getInstance();
        c.add(Calendar.MINUTE, 5);
        snoozeItem.id = System.currentTimeMillis();
        snoozeItem.hour = c.get(Calendar.HOUR_OF_DAY);
        snoozeItem.minute = c.get(Calendar.MINUTE);
        snoozeItem.enabled = true;
        snoozeItem.label = (item != null && !item.label.isEmpty()) ? (item.label + " (稍后)") : "稍后提醒";
        snoozeItem.repeatDays = 0;
        AlarmScheduler.scheduleAlarm(this, snoozeItem);
    }

    private void startAlertRoutine() {
        if (vibrator != null) {
            long[] pattern = {0, 600, 400, 600, 1000};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createWaveform(pattern, 0));
            } else {
                vibrator.vibrate(pattern, 0);
            }
        }

        scheduleSpeechCycle();
    }

    private void scheduleSpeechCycle() {
        if (isDestroyed) return;
        if (isTtsReady) {
            speakCurrentTime();
        } else {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scheduleSpeechCycle();
                }
            }, 800);
        }
    }

    private void speakCurrentTime() {
        if (tts == null || isDestroyed) return;

        Calendar c = Calendar.getInstance();
        String text = TTSPreviewHelper.buildSpokenText(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), alarmLabel);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build();
            tts.setAudioAttributes(audioAttributes);
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "voice_alarm_speech");
        } else {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }

        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {}

            @Override
            public void onDone(String utteranceId) {
                if (isDestroyed) return;
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        speakCurrentTime();
                    }
                }, 3000);
            }

            @Override
            public void onError(String utteranceId) {
                if (isDestroyed) return;
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        speakCurrentTime();
                    }
                }, 3500);
            }
        });
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.CHINESE);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.setLanguage(Locale.getDefault());
            }
            tts.setPitch(1.0f);
            tts.setSpeechRate(0.95f);
            isTtsReady = true;
        }
    }

    @Override
    public void onDestroy() {
        isDestroyed = true;
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        if (vibrator != null) {
            vibrator.cancel();
        }
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
