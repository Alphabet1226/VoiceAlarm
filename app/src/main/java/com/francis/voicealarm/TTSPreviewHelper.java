package com.francis.voicealarm;

import android.content.Context;
import android.media.AudioAttributes;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Locale;

/**
 * 统一 TTS 文本构造与即时试听助手
 */
public class TTSPreviewHelper {

    private static TTSPreviewHelper sInstance;
    private TextToSpeech tts;
    private boolean isReady = false;
    private boolean isPlaying = false;
    private OnPlayStateListener currentListener;

    public interface OnPlayStateListener {
        void onStart();
        void onDone();
    }

    public static synchronized TTSPreviewHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new TTSPreviewHelper(context.getApplicationContext());
        }
        return sInstance;
    }

    private TTSPreviewHelper(final Context context) {
        tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = tts.setLanguage(Locale.CHINESE);
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts.setLanguage(Locale.getDefault());
                    }
                    tts.setPitch(1.0f);
                    tts.setSpeechRate(0.95f);
                    isReady = true;

                    tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override
                        public void onStart(String utteranceId) {
                            isPlaying = true;
                            if (currentListener != null) {
                                currentListener.onStart();
                            }
                        }

                        @Override
                        public void onDone(String utteranceId) {
                            isPlaying = false;
                            if (currentListener != null) {
                                currentListener.onDone();
                            }
                        }

                        @Override
                        public void onError(String utteranceId) {
                            isPlaying = false;
                            if (currentListener != null) {
                                currentListener.onDone();
                            }
                        }
                    });
                }
            }
        });
    }

    /**
     * 生成优雅自然的中文时间播报文案
     */
    public static String buildSpokenText(int hour, int minute, String label) {
        String period;
        int displayHour = hour;
        if (hour >= 0 && hour < 6) {
            period = "凌晨";
        } else if (hour < 9) {
            period = "早上";
        } else if (hour < 12) {
            period = "上午";
        } else if (hour == 12) {
            period = "中午";
        } else if (hour < 18) {
            period = "下午";
            displayHour = hour > 12 ? hour - 12 : hour;
        } else {
            period = "晚上";
            displayHour = hour > 12 ? hour - 12 : hour;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("现在时间，").append(period).append(displayHour).append("点");
        if (minute == 0) {
            sb.append("整");
        } else if (minute < 10) {
            sb.append("零").append(minute).append("分");
        } else {
            sb.append(minute).append("分");
        }

        if (label != null && !label.trim().isEmpty()) {
            sb.append("。").append(label.trim());
        }

        return sb.toString();
    }

    /**
     * 播放试听
     */
    public void speakPreview(Context context, int hour, int minute, String label, OnPlayStateListener listener) {
        if (!isReady || tts == null) {
            Toast.makeText(context, "TTS 语音引擎初始化中，请稍候...", Toast.LENGTH_SHORT).show();
            return;
        }

        this.currentListener = listener;
        String text = buildSpokenText(hour, minute, label);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build();
            tts.setAudioAttributes(audioAttributes);
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "voice_alarm_preview");
        } else {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    public void stopPreview() {
        if (tts != null) {
            tts.stop();
            isPlaying = false;
            if (currentListener != null) {
                currentListener.onDone();
            }
        }
    }

    public boolean isPlaying() {
        return isPlaying;
    }
}
