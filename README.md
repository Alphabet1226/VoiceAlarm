# VoiceAlarm 语音报时闹钟 (Android)

一款极致纯净、现代优雅、开源无广告的 Android 智能语音自动报时闹钟。

> 坚持**第一性原理**与**奥卡姆剃刀原则**：纯原生 Android 实现，零第三方臃肿库，离线运行，安装包仅 **218KB**。

---

## ✨ 核心特性

1. **自动自然语音报时**：闹钟响铃时，通过 Android 内部 TTS 引擎自动生成精准的中文语音（如：“现在时间，早上 7 点整”）。
2. **日程与备注朗读**：可为每个闹钟添加自定义播报备注（如：“该起床准备晨跑了”、“晨会准备”），无缝融入报时语音。
3. **All-in-One 一站式设定面板**：在同一弹窗内流畅选择时间、周期快捷胶囊、周一至周日独立勾选，并提供常用备注标签一键填入。
4. **实时语音试听体验**：设置时与列表中均提供小喇叭试听按键，一键预听真实朗读语感。
5. **智能下次响铃仪表盘**：主界面顶部实时计算并展示距离下一次响铃的剩余倒计时（如：“明天 07:00 · 还有 8 小时 15 分钟”）。
6. **锁屏全屏高优先级唤醒**：基于原生 `AlarmManager.setAlarmClock()`，穿透系统电池优化、勿扰模式与锁屏状态。
7. **同心圆声波脉冲动效**：响铃全屏界面配备 3 层动态扩散声波环（`PulseWaveView`），配合晨起护眼暗黑色彩。
8. **物理音量键贪睡**：支持清晨盲按音量加减键推迟 5 分钟响铃（Snooze）。
9. **全局细腻触感反馈**：开关、点击、长按操作均搭载精准的物理微触震动。
10. **全面屏 Edge-to-Edge**：自适应任意刘海屏、打孔屏与系统手势横条安全区。

---

## 📁 项目工程结构

```text
VoiceAlarm语音闹钟/
├── docs/
│   └── CHANGELOG.md            # 项目开发更新日志（遵循语义化版本规范）
├── releases/
│   ├── VoiceAlarm-v1.0.0.apk   # 初始发布版本
│   └── VoiceAlarm-v1.1.0.apk   # 最新顶级 UI/UX 焕新发布版本 (218KB)
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/francis/voicealarm/
│       │   ├── AlarmItem.java          # 闹钟数据模型、时效计算与位运算周期管理
│       │   ├── AlarmStorage.java       # 本地轻量持久化
│       │   ├── AlarmScheduler.java     # 闹钟高优先级 AlarmClock 编排
│       │   ├── AlarmReceiver.java      # 系统广播接收器（开机、时区、时间自适应）
│       │   ├── AlarmService.java       # 前台报时服务与TTS语音调度
│       │   ├── AlarmAlertActivity.java # 锁屏全屏响铃展示交互（含音量键贪睡与脉冲动画）
│       │   ├── AlarmEditDialog.java    # 一体化时间、周期、自选星期与试听设定面板
│       │   ├── PulseWaveView.java      # 纯原生同心圆声波脉冲环呼吸动效控件
│       │   ├── TTSPreviewHelper.java   # 统一中文语音文案构造与试听单例管理器
│       │   ├── HapticUtil.java         # 物理触觉回馈工具类
│       │   └── MainActivity.java       # 主列表界面、智能倒计时横幅与安全区适配
│       └── res/                        # 现代 Material 3 色彩、无损矢量图标与样式
├── gradle/
├── build.gradle
├── settings.gradle
└── gradlew
```

---

## 🚀 编译与运行

在 Android Studio 中直接打开本项目，或在终端执行：

```bash
# 编译 Release APK
./gradlew assembleRelease
```
生成的 APK 文件位于 `app/build/outputs/apk/release/app-release.apk`，已签名版已同步至 `releases/VoiceAlarm-v1.1.0.apk`。

---

## 📝 更新日志

开发团队遵循严格的更新日志规范：每一次版本迭代与功能变更均会详细记入 [docs/CHANGELOG.md](file:///Users/francis/Library/Mobile%20Documents/com~apple~CloudDocs/Code/VoiceAlarm%E8%AF%AD%E9%9F%B3%E9%97%B9%E9%92%9F/docs/CHANGELOG.md)。
