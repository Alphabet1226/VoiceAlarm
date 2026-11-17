# VoiceAlarm 语音报时闹钟 (Android)

一款极简、纯净、开源的 Android 语音自动报时闹钟。

## ✨ 核心特性

1. **自动语音报时**：闹钟响铃时，直接通过 Android 系统 TTS（文字转语音）引擎朗读当前精确时间（如：“现在时间，早上 7 点 30 分”）。
2. **支持备注朗读**：可为每个闹钟添加自定义备注（如：“该起床跑步了”、“晨会准备”），闹钟响起时会无缝衔接播报。
3. **连贯报时体验**：响铃期间采用“朗读 → 呼吸间隔 → 再次朗读”的温和循环，同时伴随渐进震动唤醒。
4. **锁屏高优先级唤醒**：基于原生 `AlarmManager.setAlarmClock()`，支持穿透锁屏全屏提醒、勿扰模式与系统电池优化。
5. **稍后提醒 (Snooze)**：提供 5 分钟贪睡模式。
6. **奥卡姆剃刀设计**：纯原生实现，零第三方体积包袱，离线运行，保护隐私。

## 📁 目录结构

```text
VoiceAlarm语音闹钟/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/francis/voicealarm/
│       │   ├── AlarmItem.java          # 闹钟数据模型与触发周期计算
│       │   ├── AlarmStorage.java       # 本地轻量持久化
│       │   ├── AlarmScheduler.java     # 闹钟底层精准编排
│       │   ├── AlarmReceiver.java      # 系统广播接收器（开机/时间变更自适应）
│       │   ├── AlarmService.java       # 前台报时服务与TTS语音合成调度
│       │   ├── AlarmAlertActivity.java # 锁屏全屏响铃展示交互
│       │   └── MainActivity.java       # 主列表界面与时间设置交互
│       └── res/                        # 现代 Material 3 色彩与样式定义
├── gradle/
├── build.gradle
├── settings.gradle
└── gradlew
```

## 🚀 编译与运行

在 Android Studio 中直接打开本项目，或在终端执行：

```bash
./gradlew assembleDebug
```
生成的 APK 文件位于 `app/build/outputs/apk/debug/app-debug.apk`。
