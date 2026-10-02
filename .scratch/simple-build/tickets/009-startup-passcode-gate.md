# 009 启动口令门禁：口令不对不许用 app，解锁后才拉 sub.txt

**Blocked by**: 008（本票建立在 001–008 已完成的简易版之上；决策见 spec D18–D22，推翻 D7）

## 目标
分发出去的简易版 APK 冷启动先落在一个全屏口令页：输入 `codes.txt` 里的 4 位口令并通过后缓存起来，之后每次冷启动离线比对缓存口令直接进首页；口令不对或没输过就停在口令页，**不拉 `sub.txt`、看不到任何内容**。

## 涉及层
- [x] 数据层：恢复基线 `290f4f9` 的 `api/subscription/SubscriptionCodes.java`（`codes.txt` 解析与比对纯函数，含 `#` 注释行规则）与它的 `SubscriptionCodesTest`；新增 `api/subscription/Gate.java` 只做编排——`gate_code`（用户口令）与 `gate_codes`（最近一次成功拉到的码表原文）两个 pref 键 + 进程内 `unlocked` 标记
- [x] 逻辑层：冷启动 `Gate.verifyCached()` 用 `SubscriptionCodes.check(gate_codes, gate_code)` 同步判定；`Gate.submit(code)` 优先用缓存码表比对，无缓存时走 `SubscriptionLoader.text(CODES_URL)`（自带 GitHub 4 跳降级与 good 键自愈）拉码表；进首页后后台静默刷新码表，失败保留旧缓存
- [x] UI 层：新增 `ui/activity/GateActivity` + `activity_gate.xml`（沿用旧 `dialog_subscription_gate.xml` 的 `TextInputLayout`/`numberPassword`/`maxLength=4` 形态，去掉取消按钮）；`mobile/AndroidManifest.xml` 的 `MAIN`/`LAUNCHER` 从 `HomeActivity` 移到 `GateActivity`（`Theme.Splash` 一起带过去）；`HomeActivity.onCreate` 未解锁即跳回口令页并 `finish()`，堵住 `ACTION_SEND`/`VIEW` 深链绕过
- [x] 门禁与源的先后：`HomeActivity.initConfig()` 里的 `BuiltinSource.refresh(...)` 天然只在解锁后被调用；「更新资源」菜单项只在首页可达，无需额外守卫
- [x] 不复活：连击/长按入口、`isSubscriptionUnlocked`、订阅 UI、`GateDialog`（旧门禁是"隐藏入口 + 解锁标记"，本票是"阻塞式校验"，只有 `SubscriptionCodes` 与 `SubscriptionLoader` 复用）
- [x] 测试：`app/src/test` 恢复 `SubscriptionCodesTest`；`Gate` 只剩 IO 编排不新开接缝，页面行为只能真机手测（leanback 已删，无 Robolectric 通道）

## 验收标准
- 全新安装首启是全屏口令页，看不到首页空态；`Config` 表为空、无网络拉取 `sub.txt` 的痕迹
- 输入不在码表里的口令 → 提示「口令错误，请重新输入」，留在口令页；连续错误不写入任何标记
- 输入正确口令 → 立刻进首页，`sub.txt` 自动拉取并生效出内容
- 杀进程重开不再要求输入（含飞行模式断网重开），直接进首页 —— 离线可用来自"缓存码表比对缓存口令"，不是来自跳过校验
- 未解锁时用 `am start -a android.intent.action.SEND -t text/plain` 或 `VIEW` 深链直接拉 `HomeActivity`，会被重定向回口令页
- 备份/还原一次再重装还原，仍要求输入口令（`gate_code`/`gate_codes` 不进 `Backup.APP_PREFS` 白名单）
- `compileMobileArm64_v8aReleaseJavaWithJavac` 与 `assembleMobileArm64_v8aRelease` 收敛；`testMobileArm64_v8aDebugUnitTest` 相对基线（1768/41 failed/1 skipped）无新增失败

## 约束
- 不内置码表、不做服务端校验、口令页不留代理输入（D21）：首装且所有降级跳失败就是锁死，只提示重试
- `codes.txt` 明文公开，防误入不防技术用户；不把任何新增口令写进 app 仓库
- 默认不提交；`adb` 一律带 `-s <serial>`

## 实测结果（2026-10-02 晚，真机（序列号已抹），`mobile-arm64_v8a.apk` 138 MB）

| # | 验收项 | 结果 | 证据 |
| --- | --- | --- | --- |
| 1 | 冷启动落全屏口令页 | ✅（口径见下） | `monkey -c LAUNCHER` 后 `dumpsys window` = `GateActivity`；页面上只有「启动口令/请输入 4 位启动口令后使用/口令/进入」，首页未创建。`sub.txt` 无拉取路径：`BuiltinSource.refresh` 只有 `HomeActivity:115` 与 `VodFragment:256` 两个调用点，都在守卫之后 |
| 2 | 错误口令提示且留在口令页 | ✅ | 输入非码表 4 位码 → 截图 toast 原文「口令错误，请重新输入」，焦点仍是 `GateActivity`，按钮恢复可点 |
| 3 | 正确口令进首页并出内容 | ✅ | 输入码表内口令 → 焦点 `HomeActivity`，截图为点播海报墙（站点标题「【免费分享】」），说明 `sub.txt` 已拉取生效 |
| 4 | 冷启动离线直进首页 | ✅ | `force-stop` 重开 → `dumpsys activity activities` 里本包只有 `HomeActivity`（口令页已 finish）；`svc wifi disable; svc data disable` 后重开仍直接 `HomeActivity`，随后已恢复 Wi-Fi（`ping 223.5.5.5` 0% 丢包） |
| 5 | 深链绕不过门禁 | ✅ | `am start -n .../HomeActivity -a SEND -t text/plain` 与 `-a VIEW -d magnet:?...` 两条，最终焦点都是 `GateActivity` |
| 6 | 备份还原不构成绕过 | ✅（静态） | `Backup.isAppPref()` 只放行 `APP_PREFS` 显式白名单与 `danmaku_`/`playback_performance_`/`perf_*` 前缀，`gate_code`/`gate_codes` 均不匹配；未做"备份→重装→还原"的完整往返 |
| 7 | 编译与测试 | ✅ | `compileMobileArm64_v8aReleaseJavaWithJavac` 收敛；`assembleMobileArm64_v8aRelease` 出包；`testMobileArm64_v8aDebugUnitTest` = **1777 tests / 41 failed / 1 skipped**，基线 1768/41/1 —— 多的 9 条正是恢复的 `SubscriptionCodesTest`，失败数不变 |

偏差与遗留：
- 第 1 项用 `install -r` 覆盖安装（保留历史数据，未清 `Config` 表），所以"全新安装 + `Config` 表为空"没被逐字复现；门禁键是新增键，首次落口令页的行为与全新安装一致。要逐字验证需要 `pm clear` 或卸载重装，那会清掉观看历史，未擅自执行。
- 真机 `screencap` 写 `/sdcard` 被 MIUI 拦成 0 字节文件，截图一律走 `adb exec-out screencap -p > 本地文件`。
- 码表轮换导致"下次冷启动重新锁上"（D19 的吊销语义）没有实测条件：`codes.txt` 未轮换，无法在不改公开仓库的前提下构造。
