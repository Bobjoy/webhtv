# 001 分支与身份：feat-mobile-simple + .simple 包名

**Blocked by**: 无

## 目标
在 `feat-mobile-simple` 分支上产出一个可与完整版共存安装、版本线可辨识的简易版 APK，作为后续所有删除与自动生效的工作基线。

## 涉及层
- [x] 构建层：从 `feat-subscription` 切 `feat-mobile-simple`；`app/build.gradle` 用 `applicationId "com.fongmi.android.tv.simple"`（未用 `applicationIdSuffix`），`versionName` 改为 `5.6.0.1` 形态并递增 `versionCode`（5601）
- [x] 资源层：launcher 名后续追加需求改为「今日影视」（`app_name`，原口径「不动」已作废），不改图标
- [x] 基线：记录删除前的 release APK 体积（arm64 + armeabi-v7a 各一个数字），供 008 对比
- [x] 验证：本地 `assembleMobileArm64_v8aRelease` 出包并装机

## 验收标准
- `git branch --show-current` = `feat-mobile-simple`，且 `git merge-base` 落在 `feat-subscription` 的 HEAD 上
- 简易版 APK 与完整版 APK 在同一台设备上并存，互不覆盖（`adb shell pm list packages | grep fongmi` 出现两个包名）
- 简易版打开后仍能看到现有的点播/直播/壁纸（本票只动身份，不动功能）
- 基线 APK 体积数字已写入本票尾部的 `Baseline` 小节
- 既有 dirty 文件未被卷入提交（`AGENTS.md` 保护的 dirty 路径原样保留）

## 约束
- 默认不提交；需要提交时走 `task_guard.sh finish` 并创建 recovery tag

## Baseline
删除前的 release 体积基线，取自 `feat-mobile-simple` 分支 HEAD `290f4f9`（与 `feat-subscription` 同码），Gradle `--offline assembleMobileArm64_v8aRelease assembleMobileArmeabi_v7aRelease` BUILD SUCCESSFUL：

| 变体 | 字节 | MiB |
| --- | --- | --- |
| mobile-arm64_v8a.apk | 139,213,451 | 132.8 |
| mobile-armeabi_v7a.apk | 115,111,608 | 109.8 |

008 用同一组任务、同一台构建机重新量一次作差值对比。

## 版本线规则
`versionCode = 主基线 versionCode × 10 + 简易版序号`（`560` → `5601`），`versionName = 主版本号.序号`（`5.6.0` → `5.6.0.1`）。上游主版本号递增时按同一公式重算，保证简易版线内单调递增。

## 验证记录（2026-10-02 11:42 +08:00，本机 + 真机）

- 分支：`git branch --show-current` = `feat-mobile-simple`，HEAD 基点 `290f4f9`（= `feat-subscription` HEAD）。
- 出包：`bash ./gradlew --offline assembleMobileArm64_v8aRelease assembleMobileArmeabi_v7aRelease` BUILD SUCCESSFUL。两个变体 `aapt2 dump badging` 均为
  `package: name='com.fongmi.android.tv.simple' versionCode='5601' versionName='5.6.0.1'`。
- 体积变化：arm64 `139,213,451 → 139,213,455`（+4 B），armeabi-v7a `115,111,608 → 115,111,612`（+4 B）。身份改动对体积无实质影响。
- 共存：设备上 `pm list packages | grep fongmi` 同时给出 `com.fongmi.android.tv`（versionCode 560）与 `com.fongmi.android.tv.simple`（versionCode 5601），互不覆盖。
- 功能面：简易版冷启动进入 `com.fongmi.android.tv.simple/com.fongmi.android.tv.ui.activity.HomeActivity`，首屏渲染正常，底部导航与设置入口在位（新装无数据源，主区域为空态，与完整版新装一致）。
- 装机/首启被 MIUI 拦了两类弹窗（USB 安装确认、通知权限 + 所有文件访问），需手动或 `input tap` 放行，非应用缺陷。

## 词汇修正

「launcher 名沿用 TV」按字面成立（`app_name` 未动），但 `values-zh-rCN/strings.xml:4` 把 `app_name` 覆盖为**影视**，所以中文机型上桌面上看到的是两个同名「影视」图标，不是两个「TV」。ADR-0008 与本票的后果描述以「影视」为准。
