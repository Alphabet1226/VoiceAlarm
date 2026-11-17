package com.francis.voicealarm;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends Activity {

    private LinearLayout headerLayout;
    private LinearLayout heroBanner;
    private TextView tvBannerTitle;
    private TextView tvBannerCountdown;
    private ImageView ivBannerIcon;

    private LinearLayout alarmListContainer;
    private LinearLayout emptyContainer;
    private FrameLayout fabContainer;

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
                    "android.permission.POST_NOTIFICATIONS"
            }, 101);
        }
    }

    private void buildUi() {
        final FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(getResources().getColor(R.color.bg_light));

        final LinearLayout contentLayout = new LinearLayout(this);
        contentLayout.setOrientation(LinearLayout.VERTICAL);

        // 1. 顶部 Header 仪表盘
        headerLayout = new LinearLayout(this);
        headerLayout.setOrientation(LinearLayout.VERTICAL);
        headerLayout.setPadding(dp(24), dp(24), dp(24), dp(12));

        TextView title = new TextView(this);
        title.setText("语音报时闹钟");
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(getResources().getColor(R.color.text_main_light));
        headerLayout.addView(title);

        TextView subTitle = new TextView(this);
        subTitle.setText("响铃时自动朗读当前时间与日程备注");
        subTitle.setTextSize(14);
        subTitle.setTextColor(getResources().getColor(R.color.text_sub_light));
        subTitle.setPadding(0, dp(4), 0, 0);
        headerLayout.addView(subTitle);

        // 智能下次响铃 Hero Banner 卡片
        heroBanner = new LinearLayout(this);
        heroBanner.setOrientation(LinearLayout.HORIZONTAL);
        heroBanner.setGravity(Gravity.CENTER_VERTICAL);
        heroBanner.setBackgroundResource(R.drawable.bg_banner_gradient);
        heroBanner.setPadding(dp(18), dp(16), dp(18), dp(16));

        LinearLayout.LayoutParams heroLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        heroLp.setMargins(0, dp(16), 0, dp(4));
        heroBanner.setLayoutParams(heroLp);

        ivBannerIcon = new ImageView(this);
        ivBannerIcon.setImageResource(R.drawable.ic_alarm_clock);
        ivBannerIcon.setLayoutParams(new LinearLayout.LayoutParams(dp(24), dp(24)));
        heroBanner.addView(ivBannerIcon);

        LinearLayout bannerTextLayout = new LinearLayout(this);
        bannerTextLayout.setOrientation(LinearLayout.VERTICAL);
        bannerTextLayout.setPadding(dp(14), 0, 0, 0);

        tvBannerTitle = new TextView(this);
        tvBannerTitle.setTextSize(15);
        tvBannerTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvBannerTitle.setTextColor(getResources().getColor(R.color.text_main_light));
        bannerTextLayout.addView(tvBannerTitle);

        tvBannerCountdown = new TextView(this);
        tvBannerCountdown.setTextSize(13);
        tvBannerCountdown.setTextColor(getResources().getColor(R.color.text_sub_light));
        tvBannerCountdown.setPadding(0, dp(2), 0, 0);
        bannerTextLayout.addView(tvBannerCountdown);

        heroBanner.addView(bannerTextLayout, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        headerLayout.addView(heroBanner);

        contentLayout.addView(headerLayout);

        // 2. 闹钟列表容器
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);

        alarmListContainer = new LinearLayout(this);
        alarmListContainer.setOrientation(LinearLayout.VERTICAL);
        alarmListContainer.setPadding(dp(20), dp(8), dp(20), dp(100)); // 留白避让底部悬浮按钮

        // 空状态视图
        buildEmptyView();
        alarmListContainer.addView(emptyContainer);

        scrollView.addView(alarmListContainer);
        contentLayout.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        root.addView(contentLayout, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 3. 悬浮添加按钮 (FAB)
        fabContainer = new FrameLayout(this);
        fabContainer.setBackgroundResource(R.drawable.bg_fab);

        ImageView fabIcon = new ImageView(this);
        fabIcon.setImageResource(R.drawable.ic_add);
        FrameLayout.LayoutParams iconLp = new FrameLayout.LayoutParams(dp(28), dp(28));
        iconLp.gravity = Gravity.CENTER;
        fabContainer.addView(fabIcon, iconLp);

        FrameLayout.LayoutParams fabLp = new FrameLayout.LayoutParams(dp(62), dp(62));
        fabLp.gravity = Gravity.BOTTOM | Gravity.END;
        fabLp.setMargins(0, 0, dp(24), dp(28));
        fabContainer.setLayoutParams(fabLp);

        fabContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HapticUtil.lightClick(v);
                openAlarmEditor(null);
            }
        });
        root.addView(fabContainer);

        // 4. 适配现代 Android Edge-to-Edge 边到边全面屏避让
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override
                public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                    int topInset = insets.getSystemWindowInsetTop();
                    int bottomInset = insets.getSystemWindowInsetBottom();

                    headerLayout.setPadding(dp(24), topInset + dp(16), dp(24), dp(12));
                    FrameLayout.LayoutParams flp = (FrameLayout.LayoutParams) fabContainer.getLayoutParams();
                    flp.setMargins(0, 0, dp(24), bottomInset + dp(28));
                    fabContainer.setLayoutParams(flp);

                    alarmListContainer.setPadding(dp(20), dp(8), dp(20), bottomInset + dp(100));
                    return insets;
                }
            });
        }

        setContentView(root);
    }

    private void buildEmptyView() {
        emptyContainer = new LinearLayout(this);
        emptyContainer.setOrientation(LinearLayout.VERTICAL);
        emptyContainer.setGravity(Gravity.CENTER);
        emptyContainer.setPadding(dp(28), dp(60), dp(28), dp(40));

        ImageView ivEmpty = new ImageView(this);
        ivEmpty.setImageResource(R.drawable.ic_empty_state);
        emptyContainer.addView(ivEmpty, new LinearLayout.LayoutParams(dp(96), dp(96)));

        TextView tvTitle = new TextView(this);
        tvTitle.setText("生活张弛有度，时间清脆可闻");
        tvTitle.setTextSize(17);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(getResources().getColor(R.color.text_main_light));
        tvTitle.setPadding(0, dp(20), 0, dp(4));
        emptyContainer.addView(tvTitle);

        TextView tvDesc = new TextView(this);
        tvDesc.setText("添加闹钟，在清晨被温柔精确的报时声唤醒");
        tvDesc.setTextSize(14);
        tvDesc.setTextColor(getResources().getColor(R.color.text_sub_light));
        emptyContainer.addView(tvDesc);

        TextView btnAddFirst = new TextView(this);
        btnAddFirst.setText("+ 添加第一个闹钟");
        btnAddFirst.setTextSize(14);
        btnAddFirst.setTypeface(Typeface.DEFAULT_BOLD);
        btnAddFirst.setTextColor(Color.WHITE);
        btnAddFirst.setBackgroundResource(R.drawable.bg_button_primary);
        btnAddFirst.setPadding(dp(24), dp(12), dp(24), dp(12));

        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnLp.setMargins(0, dp(24), 0, 0);
        btnAddFirst.setLayoutParams(btnLp);

        btnAddFirst.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HapticUtil.lightClick(v);
                openAlarmEditor(null);
            }
        });
        emptyContainer.addView(btnAddFirst);
    }

    private void refreshAlarmList() {
        List<AlarmItem> alarms = AlarmStorage.loadAlarms(this);

        // 按时间先后排序展示
        Collections.sort(alarms, new Comparator<AlarmItem>() {
            @Override
            public int compare(AlarmItem o1, AlarmItem o2) {
                if (o1.hour != o2.hour) {
                    return Integer.compare(o1.hour, o2.hour);
                }
                return Integer.compare(o1.minute, o2.minute);
            }
        });

        alarmListContainer.removeAllViews();

        AlarmItem nextAlarm = findNextActiveAlarm(alarms);
        updateHeroBanner(nextAlarm);

        if (alarms.isEmpty()) {
            emptyContainer.setVisibility(View.VISIBLE);
            alarmListContainer.addView(emptyContainer);
        } else {
            emptyContainer.setVisibility(View.GONE);
            for (final AlarmItem alarm : alarms) {
                alarmListContainer.addView(createAlarmCardView(alarm));
            }
        }
    }

    private AlarmItem findNextActiveAlarm(List<AlarmItem> alarms) {
        AlarmItem next = null;
        long minTrigger = Long.MAX_VALUE;
        for (AlarmItem item : alarms) {
            if (item.enabled) {
                long trigger = item.getNextTriggerTimeMillis();
                if (trigger < minTrigger) {
                    minTrigger = trigger;
                    next = item;
                }
            }
        }
        return next;
    }

    private void updateHeroBanner(AlarmItem nextAlarm) {
        if (nextAlarm == null) {
            tvBannerTitle.setText("无已启用的闹钟");
            tvBannerCountdown.setText("轻点右下角「+」或列表中开关开启闹钟");
            ivBannerIcon.setColorFilter(getResources().getColor(R.color.text_tertiary_light));
        } else {
            tvBannerTitle.setText("下次响铃: " + nextAlarm.getFormattedTime() + " (" + nextAlarm.getPeriodLabel() + ")");
            tvBannerCountdown.setText("🔔 " + nextAlarm.getTimeRemainingDescription());
            ivBannerIcon.setColorFilter(getResources().getColor(R.color.primary_light));
        }
    }

    private View createAlarmCardView(final AlarmItem alarm) {
        final LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card_ripple);
        card.setPadding(dp(20), dp(18), dp(20), dp(18));

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(cardLp);

        // 主行：时间、周期、开关
        LinearLayout mainRow = new LinearLayout(this);
        mainRow.setOrientation(LinearLayout.HORIZONTAL);
        mainRow.setGravity(Gravity.CENTER_VERTICAL);

        // 左侧主要信息
        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);

        // 时间行 (如: 07:30 [早上])
        LinearLayout timeRow = new LinearLayout(this);
        timeRow.setOrientation(LinearLayout.HORIZONTAL);
        timeRow.setGravity(Gravity.BOTTOM);
        timeRow.setBaselineAligned(true);

        final TextView tvTime = new TextView(this);
        tvTime.setText(alarm.getFormattedTime());
        tvTime.setTextSize(36);
        tvTime.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        timeRow.addView(tvTime);

        final TextView tvPeriod = new TextView(this);
        tvPeriod.setText("  " + alarm.getPeriodLabel());
        tvPeriod.setTextSize(14);
        tvPeriod.setTypeface(Typeface.DEFAULT_BOLD);
        timeRow.addView(tvPeriod);

        left.addView(timeRow);

        // 标签与周期行
        LinearLayout descRow = new LinearLayout(this);
        descRow.setOrientation(LinearLayout.HORIZONTAL);
        descRow.setGravity(Gravity.CENTER_VERTICAL);
        descRow.setPadding(0, dp(6), 0, 0);

        // 周期胶囊 Badge
        final TextView tvBadge = new TextView(this);
        tvBadge.setText(alarm.getRepeatDescription());
        tvBadge.setTextSize(11);
        tvBadge.setTypeface(Typeface.DEFAULT_BOLD);
        tvBadge.setBackgroundResource(R.drawable.bg_badge_pill);
        tvBadge.setPadding(dp(10), dp(3), dp(10), dp(3));
        descRow.addView(tvBadge);

        // 语音备注
        if (alarm.label != null && !alarm.label.isEmpty()) {
            final TextView tvLabel = new TextView(this);
            tvLabel.setText(" · " + alarm.label);
            tvLabel.setTextSize(13);
            tvLabel.setMaxLines(1);
            descRow.addView(tvLabel);
        }

        left.addView(descRow);
        mainRow.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        // 右侧操作：试听小喇叭 + 开关
        final ImageButton btnPreview = new ImageButton(this);
        btnPreview.setImageResource(R.drawable.ic_volume_up);
        btnPreview.setBackgroundColor(Color.TRANSPARENT);
        btnPreview.setPadding(dp(10), dp(10), dp(10), dp(10));
        btnPreview.setLayoutParams(new LinearLayout.LayoutParams(dp(44), dp(44)));
        btnPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HapticUtil.lightClick(v);
                TTSPreviewHelper helper = TTSPreviewHelper.getInstance(MainActivity.this);
                if (helper.isPlaying()) {
                    helper.stopPreview();
                } else {
                    Toast.makeText(MainActivity.this, "正在试听语音播报...", Toast.LENGTH_SHORT).show();
                    helper.speakPreview(MainActivity.this, alarm.hour, alarm.minute, alarm.label, null);
                }
            }
        });
        mainRow.addView(btnPreview);

        Switch sw = new Switch(this);
        sw.setChecked(alarm.enabled);
        sw.setPadding(dp(8), 0, 0, 0);
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                HapticUtil.toggleSwitch(MainActivity.this);
                alarm.enabled = isChecked;
                AlarmStorage.updateAlarm(MainActivity.this, alarm);
                if (isChecked) {
                    AlarmScheduler.scheduleAlarm(MainActivity.this, alarm);
                    Toast.makeText(MainActivity.this, "已开启: " + alarm.getFormattedTime() + " (" + alarm.getTimeRemainingDescription() + ")", Toast.LENGTH_SHORT).show();
                } else {
                    AlarmScheduler.cancelAlarm(MainActivity.this, alarm);
                }
                refreshAlarmList();
            }
        });
        mainRow.addView(sw);

        card.addView(mainRow);

        // 设置开/关状态下的视觉淡化层次
        if (alarm.enabled) {
            card.setAlpha(1.0f);
            tvTime.setTextColor(getResources().getColor(R.color.text_main_light));
            tvPeriod.setTextColor(getResources().getColor(R.color.primary_light));
            tvBadge.setTextColor(getResources().getColor(R.color.primary_light));
            btnPreview.setColorFilter(getResources().getColor(R.color.primary_light));
        } else {
            card.setAlpha(0.55f);
            tvTime.setTextColor(getResources().getColor(R.color.text_tertiary_light));
            tvPeriod.setTextColor(getResources().getColor(R.color.text_tertiary_light));
            tvBadge.setTextColor(getResources().getColor(R.color.text_tertiary_light));
            btnPreview.setColorFilter(getResources().getColor(R.color.text_tertiary_light));
        }

        // 点击卡片直接打开编辑
        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HapticUtil.lightClick(v);
                openAlarmEditor(alarm);
            }
        });

        // 长按卡片弹出删除确认
        card.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                HapticUtil.heavyConfirm(MainActivity.this);
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("删除闹钟")
                        .setMessage("确定删除 " + alarm.getFormattedTime() + " 的语音闹钟吗？")
                        .setPositiveButton("删除", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                AlarmScheduler.cancelAlarm(MainActivity.this, alarm);
                                AlarmStorage.deleteAlarm(MainActivity.this, alarm.id);
                                refreshAlarmList();
                                Toast.makeText(MainActivity.this, "闹钟已删除", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
                return true;
            }
        });

        return card;
    }

    private void openAlarmEditor(final AlarmItem existing) {
        AlarmEditDialog dialog = new AlarmEditDialog(this, existing, new AlarmEditDialog.OnAlarmSaveListener() {
            @Override
            public void onSave(AlarmItem item) {
                AlarmStorage.updateAlarm(MainActivity.this, item);
                AlarmScheduler.scheduleAlarm(MainActivity.this, item);
                refreshAlarmList();
                Toast.makeText(MainActivity.this, "闹钟已设定: " + item.getFormattedTime() + " (" + item.getTimeRemainingDescription() + ")", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDelete(AlarmItem item) {
                AlarmScheduler.cancelAlarm(MainActivity.this, item);
                AlarmStorage.deleteAlarm(MainActivity.this, item.id);
                refreshAlarmList();
                Toast.makeText(MainActivity.this, "闹钟已删除", Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
