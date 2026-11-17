package com.francis.voicealarm;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.Calendar;
import java.util.List;

public class MainActivity extends Activity {

    private LinearLayout alarmListContainer;
    private TextView emptyHint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestPermissionsIfNeeded();

        buildUi();
        refreshAlarmList();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAlarmList();
    }

    private void requestPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(new String[]{
                    "android.permission.POST_NOTIFICATIONS",
                    "android.permission.SCHEDULE_EXACT_ALARM"
            }, 101);
        }
    }

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.parseColor("#F8F9FA"));

        LinearLayout contentLayout = new LinearLayout(this);
        contentLayout.setOrientation(LinearLayout.VERTICAL);

        // 1. 顶部现代化标题栏
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(24), dp(48), dp(24), dp(16));
        header.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("语音报时闹钟");
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.parseColor("#202124"));
        header.addView(title);

        TextView subTitle = new TextView(this);
        subTitle.setText("响铃时自动朗读当前时间几点几分");
        subTitle.setTextSize(14);
        subTitle.setTextColor(Color.parseColor("#5F6368"));
        subTitle.setPadding(0, dp(4), 0, 0);
        header.addView(subTitle);

        contentLayout.addView(header);

        // 顶部分割线
        View divider = new View(this);
        divider.setBackgroundColor(Color.parseColor("#E0E0E0"));
        contentLayout.addView(divider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));

        // 2. 闹钟列表容器 (带滚动)
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);

        alarmListContainer = new LinearLayout(this);
        alarmListContainer.setOrientation(LinearLayout.VERTICAL);
        alarmListContainer.setPadding(dp(16), dp(16), dp(16), dp(96)); // 底部留白给悬浮添加按钮

        emptyHint = new TextView(this);
        emptyHint.setText("暂无闹钟\n点击右下角「+」添加你的第一个语音闹钟");
        emptyHint.setGravity(Gravity.CENTER);
        emptyHint.setLineSpacing(dp(6), 1.0f);
        emptyHint.setTextColor(Color.parseColor("#9AA0A6"));
        emptyHint.setTextSize(16);
        emptyHint.setPadding(dp(32), dp(100), dp(32), dp(32));
        alarmListContainer.addView(emptyHint);

        scrollView.addView(alarmListContainer);
        contentLayout.addView(scrollView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        root.addView(contentLayout, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 3. 底部极简悬浮添加按钮 (FAB)
        Button btnAdd = new Button(this);
        btnAdd.setText("+");
        btnAdd.setTextSize(32);
        btnAdd.setTextColor(Color.WHITE);
        btnAdd.setGravity(Gravity.CENTER);

        GradientDrawable fabBg = new GradientDrawable();
        fabBg.setShape(GradientDrawable.OVAL);
        fabBg.setColor(Color.parseColor("#1A73E8"));
        btnAdd.setBackground(fabBg);

        FrameLayout.LayoutParams fabLp = new FrameLayout.LayoutParams(dp(60), dp(60));
        fabLp.gravity = Gravity.BOTTOM | Gravity.END;
        fabLp.setMargins(0, 0, dp(24), dp(32));
        btnAdd.setLayoutParams(fabLp);

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditAlarmDialog(null);
            }
        });

        root.addView(btnAdd);

        setContentView(root);
    }

    private void refreshAlarmList() {
        List<AlarmItem> alarms = AlarmStorage.loadAlarms(this);
        alarmListContainer.removeAllViews();

        if (alarms.isEmpty()) {
            emptyHint.setVisibility(View.VISIBLE);
            alarmListContainer.addView(emptyHint);
        } else {
            emptyHint.setVisibility(View.GONE);
            for (final AlarmItem alarm : alarms) {
                alarmListContainer.addView(createAlarmCardView(alarm));
            }
        }
    }

    private View createAlarmCardView(final AlarmItem alarm) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(20), dp(18), dp(20), dp(18));

        // 卡片圆角背景
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setShape(GradientDrawable.RECTANGLE);
        cardBg.setCornerRadius(dp(16));
        cardBg.setColor(Color.WHITE);
        card.setBackground(cardBg);

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(cardLp);

        // 左侧：时间、周期、备注
        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);

        TextView tvTime = new TextView(this);
        tvTime.setText(alarm.getFormattedTime());
        tvTime.setTextSize(34);
        tvTime.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        tvTime.setTextColor(alarm.enabled ? Color.parseColor("#202124") : Color.parseColor("#9AA0A6"));
        left.addView(tvTime);

        TextView tvDesc = new TextView(this);
        String desc = alarm.getRepeatDescription();
        if (alarm.label != null && !alarm.label.isEmpty()) {
            desc += " · " + alarm.label;
        }
        tvDesc.setText(desc);
        tvDesc.setTextSize(14);
        tvDesc.setTextColor(alarm.enabled ? Color.parseColor("#5F6368") : Color.parseColor("#BDC1C6"));
        tvDesc.setPadding(0, dp(2), 0, 0);
        left.addView(tvDesc);

        card.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        // 右侧：开关
        Switch sw = new Switch(this);
        sw.setChecked(alarm.enabled);
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                alarm.enabled = isChecked;
                AlarmStorage.updateAlarm(MainActivity.this, alarm);
                if (isChecked) {
                    AlarmScheduler.scheduleAlarm(MainActivity.this, alarm);
                    Toast.makeText(MainActivity.this, "闹钟已开启: " + alarm.getFormattedTime(), Toast.LENGTH_SHORT).show();
                } else {
                    AlarmScheduler.cancelAlarm(MainActivity.this, alarm);
                }
                refreshAlarmList();
            }
        });
        card.addView(sw);

        // 点击卡片进入编辑
        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditAlarmDialog(alarm);
            }
        });

        // 长按卡片弹出删除
        card.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("删除闹钟")
                        .setMessage("确定删除 " + alarm.getFormattedTime() + " 的闹钟吗？")
                        .setPositiveButton("删除", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                AlarmScheduler.cancelAlarm(MainActivity.this, alarm);
                                AlarmStorage.deleteAlarm(MainActivity.this, alarm.id);
                                refreshAlarmList();
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
                return true;
            }
        });

        return card;
    }

    private void showEditAlarmDialog(final AlarmItem existing) {
        final AlarmItem item = (existing != null) ? existing : new AlarmItem();
        Calendar cal = Calendar.getInstance();
        int initialHour = existing != null ? item.hour : cal.get(Calendar.HOUR_OF_DAY);
        int initialMinute = existing != null ? item.minute : cal.get(Calendar.MINUTE);

        TimePickerDialog tpd = new TimePickerDialog(
                this,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, final int hourOfDay, final int minute) {
                        item.hour = hourOfDay;
                        item.minute = minute;
                        showDetailConfigDialog(item, existing == null);
                    }
                },
                initialHour,
                initialMinute,
                true
        );
        tpd.setTitle(existing == null ? "设定闹钟时间" : "修改闹钟时间");
        tpd.show();
    }

    private void showDetailConfigDialog(final AlarmItem item, final boolean isNew) {
        LinearLayout dialogView = new LinearLayout(this);
        dialogView.setOrientation(LinearLayout.VERTICAL);
        dialogView.setPadding(dp(24), dp(16), dp(24), dp(8));

        // 备注输入框
        final EditText etLabel = new EditText(this);
        etLabel.setHint("播报备注 (例如: 起床、早会、服药)");
        etLabel.setText(item.label);
        dialogView.addView(etLabel);

        // 重复选项
        final String[] repeatOptions = {"仅响一次", "每天", "工作日 (周一至周五)", "周末 (周六周日)"};
        final int[] repeatValues = {0, 127, 62, 65};

        int selectedIdx = 0;
        if (item.repeatDays == 127) selectedIdx = 1;
        else if (item.repeatDays == 62) selectedIdx = 2;
        else if (item.repeatDays == 65) selectedIdx = 3;

        final int[] chosenIdx = {selectedIdx};

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("闹钟设定: " + item.getFormattedTime())
                .setView(dialogView)
                .setSingleChoiceItems(repeatOptions, selectedIdx, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        chosenIdx[0] = which;
                    }
                })
                .setPositiveButton("保存", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        item.label = etLabel.getText().toString().trim();
                        item.repeatDays = repeatValues[chosenIdx[0]];
                        item.enabled = true;

                        AlarmStorage.updateAlarm(MainActivity.this, item);
                        AlarmScheduler.scheduleAlarm(MainActivity.this, item);

                        refreshAlarmList();
                        Toast.makeText(MainActivity.this, "已保存语音闹钟: " + item.getFormattedTime(), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null);

        if (!isNew) {
            builder.setNeutralButton("删除", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    AlarmScheduler.cancelAlarm(MainActivity.this, item);
                    AlarmStorage.deleteAlarm(MainActivity.this, item.id);
                    refreshAlarmList();
                }
            });
        }

        builder.show();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
