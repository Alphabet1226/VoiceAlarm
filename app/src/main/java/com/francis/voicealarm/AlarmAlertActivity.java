package com.francis.voicealarm;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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
        root.setBackgroundColor(Color.parseColor("#0A0E17")); // 极简深邃夜色
        root.setPadding(dp(28), dp(48), dp(28), dp(36));

        // 1. 顶部呼吸小标签
        TextView tvSub = new TextView(this);
        tvSub.setText("VOICE ALARM · 语音报时中");
        tvSub.setTextColor(Color.parseColor("#8AB4F8"));
        tvSub.setTextSize(13);
        tvSub.setTypeface(Typeface.DEFAULT_BOLD);
        tvSub.setLetterSpacing(0.12f);
        root.addView(tvSub);

        // 2. 居中动态声波同心圆动效
        FrameLayout pulseContainer = new FrameLayout(this);
        LinearLayout.LayoutParams pulseLp = new LinearLayout.LayoutParams(dp(180), dp(180));
        pulseLp.setMargins(0, dp(16), 0, dp(12));
        pulseContainer.setLayoutParams(pulseLp);

        PulseWaveView pulseView = new PulseWaveView(this);
        pulseContainer.addView(pulseView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 中心叠加小闹钟图标
        ImageView centerIcon = new ImageView(this);
        centerIcon.setImageResource(R.drawable.ic_alarm_clock);
        centerIcon.setColorFilter(Color.WHITE);
        FrameLayout.LayoutParams iconLp = new FrameLayout.LayoutParams(dp(28), dp(28));
        iconLp.gravity = Gravity.CENTER;
        pulseContainer.addView(centerIcon, iconLp);

        root.addView(pulseContainer);

        // 3. 巨幅动态时间
        tvTime = new TextView(this);
        tvTime.setTextColor(Color.WHITE);
        tvTime.setTextSize(64);
        tvTime.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
        root.addView(tvTime);

        // 4. 日期与星期
        tvDate = new TextView(this);
        tvDate.setTextColor(Color.parseColor("#8B949E"));
        tvDate.setTextSize(16);
        tvDate.setPadding(0, dp(4), 0, dp(16));
        root.addView(tvDate);

        // 5. 闹钟语音备注高光卡片
        LinearLayout labelCard = new LinearLayout(this);
        labelCard.setOrientation(LinearLayout.VERTICAL);
        labelCard.setGravity(Gravity.CENTER);
        labelCard.setPadding(dp(20), dp(12), dp(20), dp(12));

        GradientDrawable labelBg = new GradientDrawable();
        labelBg.setShape(GradientDrawable.RECTANGLE);
        labelBg.setCornerRadius(dp(16));
        labelBg.setColor(Color.parseColor("#161B22"));
        labelCard.setBackground(labelBg);

        tvLabel = new TextView(this);
        String labelStr = (item != null && !item.label.isEmpty()) ? item.label : "清晨已至，美好的一天从现在开始";
        tvLabel.setText(labelStr);
        tvLabel.setTextColor(Color.parseColor("#F0F6FC"));
        tvLabel.setTextSize(16);
        tvLabel.setGravity(Gravity.CENTER);
        labelCard.addView(tvLabel);

        LinearLayout.LayoutParams lcLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lcLp.setMargins(dp(16), 0, dp(16), 0);
        root.addView(labelCard, lcLp);

        // 6. 弹性占位
        View spacer = new View(this);
        root.addView(spacer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        // 7. 底部操作按键 (稍后提醒 + 关闭)
        LinearLayout btnContainer = new LinearLayout(this);
        btnContainer.setOrientation(LinearLayout.VERTICAL);
        btnContainer.setGravity(Gravity.CENTER_HORIZONTAL);

        // 稍后提醒按键 (5分钟)
        Button btnSnooze = new Button(this);
        btnSnooze.setText("稍后提醒 (5分钟)");
        btnSnooze.setTextColor(Color.parseColor("#8AB4F8"));
        btnSnooze.setTextSize(16);
        btnSnooze.setTypeface(Typeface.DEFAULT_BOLD);
        btnSnooze.setBackgroundResource(R.drawable.bg_button_secondary);
        LinearLayout.LayoutParams snoozeLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(54));
        snoozeLp.setMargins(0, 0, 0, dp(14));
        btnContainer.addView(btnSnooze, snoozeLp);

        // 立即关闭闹钟按键
        Button btnDismiss = new Button(this);
        btnDismiss.setText("关闭闹钟");
        btnDismiss.setTextColor(Color.WHITE);
        btnDismiss.setTextSize(17);
        btnDismiss.setTypeface(Typeface.DEFAULT_BOLD);
        btnDismiss.setBackgroundResource(R.drawable.bg_button_danger);
        LinearLayout.LayoutParams dismissLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(56));
        btnContainer.addView(btnDismiss, dismissLp);

        root.addView(btnContainer);

        setContentView(root);

        // 点击事件
        btnSnooze.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerSnooze();
            }
        });

        btnDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HapticUtil.heavyConfirm(AlarmAlertActivity.this);
                Intent stopIntent = new Intent(AlarmAlertActivity.this, AlarmService.class);
                stopIntent.setAction(AlarmService.ACTION_STOP_ALARM);
                startService(stopIntent);
                finish();
            }
        });
    }

    private void triggerSnooze() {
        HapticUtil.lightClick(getWindow().getDecorView());
        Toast.makeText(this, "闹钟已推迟 5 分钟后再次提醒", Toast.LENGTH_SHORT).show();
        Intent snoozeIntent = new Intent(AlarmAlertActivity.this, AlarmService.class);
        snoozeIntent.setAction(AlarmService.ACTION_SNOOZE_ALARM);
        snoozeIntent.putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId);
        startService(snoozeIntent);
        finish();
    }

    private void updateTimeDisplay() {
        Date now = new Date();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("M月d日 EEEE", Locale.CHINESE);
        tvTime.setText(timeFormat.format(now));
        tvDate.setText(dateFormat.format(now));
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // 允许用户在睡梦中盲按音量键推迟闹钟 (Snooze)
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            triggerSnooze();
            return true;
        }
        return super.onKeyDown(keyCode, event);
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
        // 防止误触返回键意外退出闹钟
    }
}
