package com.francis.voicealarm;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.Calendar;

/**
 * 顶级一站式闹钟设定与编辑交互面板
 * 无缝整合时间选取、周期胶囊、独立周几勾选、语音备注建议与实时 TTS 试听
 */
public class AlarmEditDialog extends Dialog {

    public interface OnAlarmSaveListener {
        void onSave(AlarmItem item);
        void onDelete(AlarmItem item);
    }

    private final AlarmItem alarmItem;
    private final boolean isNew;
    private final OnAlarmSaveListener listener;

    private TimePicker timePicker;
    private EditText etLabel;
    private Button btnPreview;
    private final TextView[] dayButtons = new TextView[7];
    private final TextView[] quickChips = new TextView[4];

    public AlarmEditDialog(Context context, AlarmItem existing, OnAlarmSaveListener listener) {
        super(context, R.style.AppTheme_Dialog);
        this.isNew = (existing == null);
        this.alarmItem = (existing != null) ? cloneAlarm(existing) : new AlarmItem();
        this.listener = listener;

        initDialog();
    }

    private AlarmItem cloneAlarm(AlarmItem src) {
        AlarmItem item = new AlarmItem();
        item.id = src.id;
        item.hour = src.hour;
        item.minute = src.minute;
        item.enabled = src.enabled;
        item.label = src.label;
        item.repeatDays = src.repeatDays;
        return item;
    }

    private void initDialog() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setCancelable(true);
        setCanceledOnTouchOutside(true);

        ScrollView scrollView = new ScrollView(getContext());
        scrollView.setFillViewport(true);

        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(24));

        GradientDrawable rootBg = new GradientDrawable();
        rootBg.setShape(GradientDrawable.RECTANGLE);
        rootBg.setCornerRadius(dp(24));
        rootBg.setColor(getContext().getResources().getColor(R.color.surface_light));
        root.setBackground(rootBg);

        // 1. 顶部 Header
        LinearLayout header = new LinearLayout(getContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvTitle = new TextView(getContext());
        tvTitle.setText(isNew ? "添加语音闹钟" : "编辑语音闹钟");
        tvTitle.setTextSize(20);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(getContext().getResources().getColor(R.color.text_main_light));
        header.addView(tvTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView btnClose = new TextView(getContext());
        btnClose.setText("✕");
        btnClose.setTextSize(18);
        btnClose.setTextColor(getContext().getResources().getColor(R.color.text_tertiary_light));
        btnClose.setPadding(dp(12), dp(8), dp(4), dp(8));
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TTSPreviewHelper.getInstance(getContext()).stopPreview();
                dismiss();
            }
        });
        header.addView(btnClose);
        root.addView(header);

        // 2. 原生 TimePicker
        timePicker = new TimePicker(getContext());
        timePicker.setIs24HourView(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            timePicker.setHour(alarmItem.hour);
            timePicker.setMinute(alarmItem.minute);
        } else {
            timePicker.setCurrentHour(alarmItem.hour);
            timePicker.setCurrentMinute(alarmItem.minute);
        }
        LinearLayout.LayoutParams tpLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tpLp.gravity = Gravity.CENTER_HORIZONTAL;
        tpLp.setMargins(0, dp(12), 0, dp(8));
        root.addView(timePicker, tpLp);

        // 3. 周期模式快捷胶囊
        TextView tvRepeatTitle = new TextView(getContext());
        tvRepeatTitle.setText("重复周期");
        tvRepeatTitle.setTextSize(13);
        tvRepeatTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvRepeatTitle.setTextColor(getContext().getResources().getColor(R.color.text_sub_light));
        tvRepeatTitle.setPadding(dp(4), dp(8), 0, dp(8));
        root.addView(tvRepeatTitle);

        LinearLayout chipsContainer = new LinearLayout(getContext());
        chipsContainer.setOrientation(LinearLayout.HORIZONTAL);

        final String[] chipLabels = {"仅响一次", "工作日", "每天", "周末"};
        final int[] chipValues = {0, 62, 127, 65};

        for (int i = 0; i < 4; i++) {
            final int index = i;
            TextView chip = new TextView(getContext());
            chip.setText(chipLabels[i]);
            chip.setTextSize(13);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(14), dp(7), dp(14), dp(7));
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            if (i > 0) clp.setMargins(dp(6), 0, 0, 0);
            chip.setLayoutParams(clp);

            chip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    HapticUtil.lightClick(v);
                    alarmItem.repeatDays = chipValues[index];
                    updateRepeatViews();
                }
            });
            quickChips[i] = chip;
            chipsContainer.addView(chip);
        }
        root.addView(chipsContainer);

        // 4. 周一至周日 7 颗圆球自选
        LinearLayout daysContainer = new LinearLayout(getContext());
        daysContainer.setOrientation(LinearLayout.HORIZONTAL);
        daysContainer.setGravity(Gravity.CENTER);
        daysContainer.setPadding(0, dp(12), 0, dp(16));

        final String[] dayLabels = {"日", "一", "二", "三", "四", "五", "六"};
        for (int i = 0; i < 7; i++) {
            final int dayIndex = i;
            TextView dayBtn = new TextView(getContext());
            dayBtn.setText(dayLabels[i]);
            dayBtn.setTextSize(13);
            dayBtn.setTypeface(Typeface.DEFAULT_BOLD);
            dayBtn.setGravity(Gravity.CENTER);

            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(36), dp(36));
            if (i > 0) dlp.setMargins(dp(6), 0, 0, 0);
            dayBtn.setLayoutParams(dlp);

            dayBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    HapticUtil.lightClick(v);
                    boolean currentSelected = alarmItem.isDaySelected(dayIndex);
                    alarmItem.setDaySelected(dayIndex, !currentSelected);
                    updateRepeatViews();
                }
            });

            dayButtons[i] = dayBtn;
            daysContainer.addView(dayBtn);
        }
        root.addView(daysContainer);

        // 5. 语音备注输入与快捷建议标签
        TextView tvLabelTitle = new TextView(getContext());
        tvLabelTitle.setText("语音播报备注");
        tvLabelTitle.setTextSize(13);
        tvLabelTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvLabelTitle.setTextColor(getContext().getResources().getColor(R.color.text_sub_light));
        tvLabelTitle.setPadding(dp(4), 0, 0, dp(8));
        root.addView(tvLabelTitle);

        etLabel = new EditText(getContext());
        etLabel.setHint("响铃时自动朗读 (如: 该起床准备晨跑了)");
        etLabel.setText(alarmItem.label);
        etLabel.setTextSize(14);
        etLabel.setTextColor(getContext().getResources().getColor(R.color.text_main_light));
        etLabel.setHintTextColor(getContext().getResources().getColor(R.color.text_tertiary_light));
        etLabel.setBackgroundResource(R.drawable.bg_input_field);
        etLabel.setPadding(dp(14), dp(12), dp(14), dp(12));
        root.addView(etLabel);

        // 快捷词横向滚动条
        HorizontalScrollView hsvQuickWords = new HorizontalScrollView(getContext());
        hsvQuickWords.setHorizontalScrollBarEnabled(false);
        hsvQuickWords.setPadding(0, dp(8), 0, dp(14));

        LinearLayout quickWordsLayout = new LinearLayout(getContext());
        quickWordsLayout.setOrientation(LinearLayout.HORIZONTAL);

        String[] quickWords = {"起床", "早间会议", "记得吃药", "外出晨跑", "喝杯温水", "出门上班"};
        for (final String word : quickWords) {
            TextView wordChip = new TextView(getContext());
            wordChip.setText(word);
            wordChip.setTextSize(12);
            wordChip.setTextColor(getContext().getResources().getColor(R.color.text_sub_light));
            wordChip.setBackgroundResource(R.drawable.bg_chip_pill);
            wordChip.setPadding(dp(12), dp(6), dp(12), dp(6));

            LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            wlp.setMargins(0, 0, dp(8), 0);
            wordChip.setLayoutParams(wlp);

            wordChip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    HapticUtil.lightClick(v);
                    etLabel.setText(word);
                    etLabel.setSelection(word.length());
                }
            });
            quickWordsLayout.addView(wordChip);
        }
        hsvQuickWords.addView(quickWordsLayout);
        root.addView(hsvQuickWords);

        // 6. 实时语音试听按键
        btnPreview = new Button(getContext());
        btnPreview.setText("🔊 试听语音播报效果");
        btnPreview.setTextSize(14);
        btnPreview.setTextColor(getContext().getResources().getColor(R.color.primary_light));
        btnPreview.setBackgroundResource(R.drawable.bg_button_secondary);
        btnPreview.setPadding(dp(16), dp(10), dp(16), dp(10));
        LinearLayout.LayoutParams prevLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        prevLp.setMargins(0, 0, 0, dp(18));
        btnPreview.setLayoutParams(prevLp);

        btnPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HapticUtil.lightClick(v);
                final TTSPreviewHelper helper = TTSPreviewHelper.getInstance(getContext());
                if (helper.isPlaying()) {
                    helper.stopPreview();
                    btnPreview.setText("🔊 试听语音播报效果");
                    return;
                }

                syncTimeFromPicker();
                String label = etLabel.getText().toString().trim();
                btnPreview.setText("⏹ 正在播报中... 点击停止");
                helper.speakPreview(getContext(), alarmItem.hour, alarmItem.minute, label, new TTSPreviewHelper.OnPlayStateListener() {
                    @Override
                    public void onStart() {
                    }

                    @Override
                    public void onDone() {
                        if (btnPreview != null) {
                            btnPreview.post(new Runnable() {
                                @Override
                                public void run() {
                                    btnPreview.setText("🔊 试听语音播报效果");
                                }
                            });
                        }
                    }
                });
            }
        });
        root.addView(btnPreview);

        // 7. 底部操作栏 (取消/保存/删除)
        LinearLayout bottomActions = new LinearLayout(getContext());
        bottomActions.setOrientation(LinearLayout.HORIZONTAL);
        bottomActions.setGravity(Gravity.CENTER_VERTICAL);

        if (!isNew) {
            Button btnDelete = new Button(getContext());
            btnDelete.setText("删除");
            btnDelete.setTextSize(14);
            btnDelete.setTextColor(getContext().getResources().getColor(R.color.accent_danger));
            btnDelete.setBackgroundResource(R.drawable.bg_button_secondary);
            LinearLayout.LayoutParams delLp = new LinearLayout.LayoutParams(dp(72), dp(46));
            delLp.setMargins(0, 0, dp(8), 0);
            btnDelete.setLayoutParams(delLp);
            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    HapticUtil.heavyConfirm(getContext());
                    TTSPreviewHelper.getInstance(getContext()).stopPreview();
                    if (listener != null) {
                        listener.onDelete(alarmItem);
                    }
                    dismiss();
                }
            });
            bottomActions.addView(btnDelete);
        }

        Button btnCancel = new Button(getContext());
        btnCancel.setText("取消");
        btnCancel.setTextSize(15);
        btnCancel.setTextColor(getContext().getResources().getColor(R.color.text_sub_light));
        btnCancel.setBackgroundResource(R.drawable.bg_button_secondary);
        LinearLayout.LayoutParams cancelLp = new LinearLayout.LayoutParams(0, dp(46), 1.0f);
        cancelLp.setMargins(0, 0, dp(8), 0);
        btnCancel.setLayoutParams(cancelLp);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TTSPreviewHelper.getInstance(getContext()).stopPreview();
                dismiss();
            }
        });
        bottomActions.addView(btnCancel);

        Button btnSave = new Button(getContext());
        btnSave.setText("保存闹钟");
        btnSave.setTextSize(15);
        btnSave.setTypeface(Typeface.DEFAULT_BOLD);
        btnSave.setTextColor(Color.WHITE);
        btnSave.setBackgroundResource(R.drawable.bg_button_primary);
        LinearLayout.LayoutParams saveLp = new LinearLayout.LayoutParams(0, dp(46), 1.2f);
        btnSave.setLayoutParams(saveLp);
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HapticUtil.heavyConfirm(getContext());
                TTSPreviewHelper.getInstance(getContext()).stopPreview();
                syncTimeFromPicker();
                alarmItem.label = etLabel.getText().toString().trim();
                alarmItem.enabled = true;
                if (listener != null) {
                    listener.onSave(alarmItem);
                }
                dismiss();
            }
        });
        bottomActions.addView(btnSave);

        root.addView(bottomActions);

        scrollView.addView(root);
        setContentView(scrollView);

        updateRepeatViews();
    }

    private void syncTimeFromPicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmItem.hour = timePicker.getHour();
            alarmItem.minute = timePicker.getMinute();
        } else {
            alarmItem.hour = timePicker.getCurrentHour();
            alarmItem.minute = timePicker.getCurrentMinute();
        }
    }

    private void updateRepeatViews() {
        int repeat = alarmItem.repeatDays;
        int activeChipIdx = -1;
        if (repeat == 0) activeChipIdx = 0;
        else if (repeat == 62) activeChipIdx = 1;
        else if (repeat == 127) activeChipIdx = 2;
        else if (repeat == 65) activeChipIdx = 3;

        for (int i = 0; i < 4; i++) {
            if (i == activeChipIdx) {
                quickChips[i].setBackgroundResource(R.drawable.bg_chip_pill_selected);
                quickChips[i].setTextColor(Color.WHITE);
            } else {
                quickChips[i].setBackgroundResource(R.drawable.bg_chip_pill);
                quickChips[i].setTextColor(getContext().getResources().getColor(R.color.text_sub_light));
            }
        }

        // 7个周几圆球状态
        for (int i = 0; i < 7; i++) {
            boolean selected = alarmItem.isDaySelected(i);
            if (selected) {
                dayButtons[i].setBackgroundResource(R.drawable.bg_weekday_circle_selected);
                dayButtons[i].setTextColor(Color.WHITE);
            } else {
                dayButtons[i].setBackgroundResource(R.drawable.bg_weekday_circle);
                dayButtons[i].setTextColor(getContext().getResources().getColor(R.color.text_sub_light));
            }
        }
    }

    private int dp(int value) {
        return (int) (value * getContext().getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onStop() {
        TTSPreviewHelper.getInstance(getContext()).stopPreview();
        super.onStop();
    }
}
