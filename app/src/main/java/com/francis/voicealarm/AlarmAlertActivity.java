package com.francis.voicealarm;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AlarmAlertActivity extends Activity {

    private TextView tvTime;
    private TextView tvDate;
    private TextView tvLabel;
    private Handler handler;
    private Runnable timeUpdater;
    private long alarmId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupWindowFlags();

        alarmId = getIntent().getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1);
        AlarmItem item = AlarmStorage.findAlarm(this, alarmId);

        buildUi(item);

        handler = new Handler(Looper.getMainLooper());
        timeUpdater = new Runnable() {
            @Override
            public void run() {
                updateTimeDisplay();
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(timeUpdater);
    }

    private void setupWindowFlags() {
        Window win = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            win.addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                            | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                            | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                            | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            );
        }
        win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void buildUi(AlarmItem item) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(Color.parseColor("#0F141C")); // 深邃夜空黑蓝
        root.setPadding(dp(28), dp(60), dp(28), dp(40));

        // 顶部小标签
        TextView tvSub = new TextView(this);
        tvSub.setText("语音报时闹钟响铃中");
        tvSub.setTextColor(Color.parseColor("#8AB4F8"));
        tvSub.setTextSize(16);
        tvSub.setLetterSpacing(0.08f);
        root.addView(tvSub);

        // 巨幅动态时间
        tvTime = new TextView(this);
        tvTime.setTextColor(Color.WHITE);
        tvTime.setTextSize(68);
        tvTime.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
        tvTime.setPadding(0, dp(12), 0, 0);
        root.addView(tvTime);

        // 日期与星期
        tvDate = new TextView(this);
        tvDate.setTextColor(Color.parseColor("#9AA0A6"));
        tvDate.setTextSize(17);
        tvDate.setPadding(0, dp(4), 0, dp(24));
        root.addView(tvDate);

        // 闹钟备注
        tvLabel = new TextView(this);
        String labelStr = (item != null && !item.label.isEmpty()) ? item.label : "准时出发，美好的一天";
        tvLabel.setText(labelStr);
        tvLabel.setTextColor(Color.parseColor("#E8EAED"));
        tvLabel.setTextSize(20);
        tvLabel.setGravity(Gravity.CENTER);
        root.addView(tvLabel);

        // 伸缩占位 Space
        View spacer = new View(this);
        LinearLayout.LayoutParams spacerLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
        root.addView(spacer, spacerLp);

        // 底部操作区 (稍后提醒 + 关闭)
        LinearLayout btnContainer = new LinearLayout(this);
        btnContainer.setOrientation(LinearLayout.VERTICAL);
        btnContainer.setGravity(Gravity.CENTER_HORIZONTAL);

        // 稍后提醒 (5分钟)
        Button btnSnooze = new Button(this);
        btnSnooze.setText("稍后提醒 (5分钟)");
        btnSnooze.setTextColor(Color.parseColor("#8AB4F8"));
        btnSnooze.setBackgroundColor(Color.parseColor("#252E3E"));
        btnSnooze.setTextSize(16);
        btnSnooze.setPadding(dp(24), dp(16), dp(24), dp(16));
        LinearLayout.LayoutParams snoozeLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        snoozeLp.setMargins(0, 0, 0, dp(16));
        btnContainer.addView(btnSnooze, snoozeLp);

        // 立即关闭大按钮
        Button btnDismiss = new Button(this);
        btnDismiss.setText("关闭闹钟");
        btnDismiss.setTextColor(Color.WHITE);
        btnDismiss.setBackgroundColor(Color.parseColor("#D93025"));
        btnDismiss.setTextSize(18);
        btnDismiss.setTypeface(Typeface.DEFAULT_BOLD);
        btnDismiss.setPadding(dp(24), dp(18), dp(24), dp(18));
        LinearLayout.LayoutParams dismissLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnContainer.addView(btnDismiss, dismissLp);

        root.addView(btnContainer);

        setContentView(root);

        // 点击事件
        btnSnooze.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent snoozeIntent = new Intent(AlarmAlertActivity.this, AlarmService.class);
                snoozeIntent.setAction(AlarmService.ACTION_SNOOZE_ALARM);
                snoozeIntent.putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId);
                startService(snoozeIntent);
                finish();
            }
        });

        btnDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent stopIntent = new Intent(AlarmAlertActivity.this, AlarmService.class);
                stopIntent.setAction(AlarmService.ACTION_STOP_ALARM);
                startService(stopIntent);
                finish();
            }
        });
    }

    private void updateTimeDisplay() {
        Date now = new Date();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("M月d日 EEEE", Locale.CHINESE);
        tvTime.setText(timeFormat.format(now));
        tvDate.setText(dateFormat.format(now));
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onDestroy() {
        if (handler != null && timeUpdater != null) {
            handler.removeCallbacks(timeUpdater);
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        // 防止意外误触返回键关闭闹钟
    }
}
