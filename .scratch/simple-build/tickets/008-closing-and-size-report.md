# 008 收尾：包体对比 + 文档 + 真机回归清单

**Blocked by**: 003, 004, 005, 006, 007

## 目标
简易版可交付：有体积数字、有文档口径、有一份跑过的真机验收清单。

## 涉及层
- [x] 度量：以 001 记录的基线体积对比本票产物（arm64 与 armeabi-v7a 各一个差值），并说明差值主要来自哪一层删除
- [x] 文档：`README.md` 增加「简易版」章节——分支、包名 `.simple`、版本线 `5.6.0.x`、sub.txt 来源、本地 `assembleMobile*` 打包与手动分发口径（明确不改 `android-release.yml`/`apk-build.yml`）
- [x] 文档：`docs/specs/2026-10-02-simple-build.md` 与 `docs/adr/0008` 若实现中有偏差，就地修正，不留矛盾表述
- [x] 回归：按 spec「完成」定义逐项跑真机清单并记录结果（全新安装首启出内容 / 断网空态 / 坏清单沿用旧源 / 设置页恰好一行 / 首页无直播壁纸 / 覆盖安装数据保留）
- [x] 代码审查：对本分支累计 diff 走 `code-review`（标准轴 + 规格轴）

## 验收标准
- 报告里有删除前后的 APK 体积数字与差值
- README 简易版章节不含真实 IP、内网域名、绝对路径、设备型号与 adb 序列号（公开仓库口径）
- 6 项真机清单逐条有实测结论，未跑到的项明确标注"未验证"而不是留空
- `app/src/test` 全量绿 + 两个 mobile release 任务出包
- 若用户已确认可提交：`task_guard.sh finish` 一次成票一提交，并创建 `recovery/simple-build-008/<timestamp>` 本地 tag；未经要求不 push

## 约束
- 不新增 flavor、不接 Release 自动更新、不做首启引导与图标改动
- 默认不提交

---

## 实现补记（2026-10-02）

### 包体对比
同一组任务、同一台构建机、`--offline`，基线取票 001 记录的 HEAD `290f4f9` 产物：

| 变体 | 基线（B） | 简易版（B） | 差值 |
| --- | --- | --- | --- |
| mobile-arm64_v8a.apk | 139,213,451 | 138,494,401 | −719,050（−0.52%） |
| mobile-armeabi_v7a.apk | 115,111,608 | 114,392,558 | −719,050（−0.62%） |

**两个 ABI 的差值逐字节相同** ⇒ 减量全部来自与 ABI 无关的层（dex + 资源 + arsc），native 库一点没动。
简易版 arm64 包的构成实测：`assets/` 64.0 MiB（48.7%）、`lib/` 55.4 MiB（42.1%）、dex 7.1 MiB（5.4%）、
`res/` + `resources.arsc` 3.7 MiB（2.8%）。也就是说这个分支删掉的代码面很大，但**包体几乎不会变小**——
90.8% 的体积是 IJK/MPV/Chaquopy 的 native 与 assets，本票一行都没碰。想要体积收益必须动 native 侧，超出简易版范围。

实测确认 `tvbus-release.aar` 在包里**没有任何产物**（`unzip -l` 无 `tvbus` 条目），所以它的 keep 规则和依赖声明
只是源码级死重，不是包体死重；票 007 把它列为"减重项"的假设不成立，按原样留着即可。

### 真机回归（release 包，本机测试机）
| # | 项 | 结论 |
| --- | --- | --- |
| 1 | 全新安装首启出内容 | **通过**（卸载重装后零数据冷启动，约 15 s 内出现分类栏 推荐/热门电影/热播剧集/热播综艺 与海报墙，logcat 无 FATAL） |
| 2 | 断网首启空态 | **未做设备级验证**：MIUI 上 `appops set … READ_WRITE_NETWORK ignore` 报 `Unknown operation string`，未去开关整机网络。等价分支由单测覆盖（`BuiltinSourceTest.emptyListYieldsNoCandidate`、`blankUrlOnlyListNeverActivates`），且票 003 在 `sub.txt` 尚未上线期间实测过冷启动空态「正在获取点播源…」 |
| 3 | 坏清单沿用旧源不弹窗 | **通过（等价路径）**：清单首行 `xhztv.top/4k.json` 在真机上加载失败，app 顺位切到下一行并正常出内容、无弹窗、无报错打断（见下方"顺位实测"） |
| 4 | 首页无直播/壁纸入口 | **通过**：顶栏只有 logo（纯装饰）+ 标题（切换站点）+ 搜索 + 最近观看 + 五项下拉；类型栏只有点播分类 |
| 5 | 五项下拉逐个可操作 | **通过**（票 004/007 已逐个实测，本轮复验图标可见、菜单可开） |
| 6 | 覆盖安装数据保留 | **通过**：`install -r` 同一 APK 后 `/manage/configs` 仍返回原有 `Config` 行、首页正常渲染 |

**顺位实测**：全新安装后 `Config` 表出现两行点播记录 —— 首行 `time=0`（尝试过但加载失败），第二行 `active=true` 且 `time` 为当次启动时间戳。
说明 `BuiltinSource.activate()` 的"第一条失败就顺位试下一行"在真机上按用户故事 10 工作。
同时说明 `sub.txt` 当前第一行对这个 app 不可用 ⇒ `webhtv-sub` 的日巡检判定与实际加载结果存在偏差，属分发侧问题，不在本票修。

### 测试与出包
- `:app:testMobileArm64_v8aDebugUnitTest --continue`：**1768 tests, 41 failed, 1 skipped**。
  41 个失败全部落在四个既有失败类：`ExoCompressedAudioDirectPolicyTest`(35)、`Avs3ExtractionTest`(3)、
  `MpvPreloadControllerTest`(2)、`MpvConfigStoreTest`(1) —— 全是播放器/解码侧，本分支一行没碰，
  与票 001 起记录的既有失败集合完全一致（**无新增失败**）。
- 简易版接缝测试全绿：`BuiltinSourceTest` 8/8、`SubscriptionParserTest` 16/16、`SubscriptionLoaderTest` 6/6。
- `assembleMobileArm64_v8aRelease` + `assembleMobileArmeabi_v7aRelease`：BUILD SUCCESSFUL in 8m26s，两个变体均出包。

### 文档
- `README.md` 新增「简易版（`feat-mobile-simple` 分支）」章节（置于「目录结构」之前）。
- `docs/specs/2026-10-02-simple-build.md`：D5 补 logo 入口真删口径；D13 由"实施前必须实测"改写为实测结论。
- `docs/adr/0008-*`：修正三处被后续决策推翻的表述（设置页只留两行 → 整页删除；本期接通 Release 自更新 → 手动分发不接；
  直播/壁纸端点全部移除 → 端点实测收敛结论）。
- 公开口径清理：spec 与票 003 里原先写死的测试机型号与 adb 序列号已替换为泛称。

### 审查修复补记（2026-10-02，编号对应双轴报告处置项）
| # | 位置 | 处置 |
|---|------|------|
| 1 | `Manage.configs()/isCurrentConfig()`、`RemoteConfigOps.data()/isCurrent()` | `currentConfig(int)`/`current(int)` 忽略入参恒返回点播配置 ⇒ 去掉参数，`isCurrent` 加 `getType() == 0` 前置。列表仍枚举 `type 0..2`（还原进来的直播/壁纸条目照旧可见、照旧带 `typeName`），但只有点播条目可能被标 `active` |
| 2 | `BuiltinSource.refresh(Config, Consumer<Boolean>)` | 加 `AtomicBoolean mRunning` 入口 `compareAndSet` 短路，`finish()` 里释放。定时重试跑在 `Task.scheduler`、手动「更新资源」跑在 5 线程池，此前可并发进 `VodConfig.load` 让 `clear()` 与另一路加载交错 |
| 3 | `api/config/ConfigActivator.java` | 全仓零引用，删 |
| 4 | `ConfigEvent.live()/wall()/boot()/isLive()` + `Type.LIVE/WALL/BOOT`、`RefreshEvent.live()` + `Type.LIVE`、`Action.onRefresh` 的 `case "live"`、`CustomWallView.onConfigEvent` + EventBus 注册、`LiveSetting` 零调用访问器 | 直播/壁纸事件链在本分支已无人投递也无人订阅，全部真删。`LiveSetting` 只留 `isInvert()`（`CustomKeyDown:181,183` 仍在读 `invert` 键）；`Backup.APP_PREFS` 的键白名单没动，还原语义不变 |
| 5 | `HomeActivity`（`View`/`FileChooser`/`UrlUtil`）、`VodConfig`（`TextUtils`）、`WebHomeChromeController`（`View`/`ViewGroup`） | 未使用 import 删除 |
| 6 | `db/AppDatabase.create()` 的 7 条 `addMigrations(MIGRATION_30_31…36_37)` + `db/Migrations.java` | `VERSION = 1` + `fallbackToDestructiveMigration(true)` 下永不触发，整类删除 |
| 7 | `ApkPushDialog`、`ApkPushMethodDialog`、`PushPlayDialog`、`OneKeySyncDialog`、`SyncDeviceAdapter` + 独有 layout（`dialog_apk_push_method.xml`/`dialog_one_key_sync.xml`/`adapter_sync_device.xml`）+ `color/selector_nav.xml`、`drawable/ic_fab_link.xml` | 零调用点孤儿删除。`dialog_device.xml`、`ScanTask`、`NsdDeviceDiscovery`、`ApkPushProgressDialog` 仍被 `CastDialog`/`SyncDialog`/`ApkUrlPush` 使用，保留 |
| 8 | `menu_vod.xml:38` | `@string/live_refresh` → `@string/vod_refresh`（三个 values 文件的键同步改名，文案不变） |
| 9 | `mobile/AndroidManifest.xml` VIEW 过滤器 | 删掉 `text/plain`（保留 video/audio/torrent）：`checkType()` 只会把它交给 `VideoActivity.push()`，等于允许任何 app 用 `.txt`/`.m3u` 让本机播放任意 URL。`ACTION_SEND` 的 `text/plain` 保留 |
| 10 | `HomeWebController.Listener.openSetting()` 默认空实现 | **审查报告误报**：全仓没有任何 App 内控件调用它，`assets` 里也无调用点，它只是给外部 WebHome 页面的 JS API（`window.fongmi.app.openSetting`）。删掉会让第三方页面对方抛 `TypeError`，保留空实现是最小影响 ⇒ 不动 |

| 11 | `.github/workflows/android-release.yml:177-178`、`apk-build.yml:60-61` | 用户拍板「工作流仍调 `assembleLeanback*` 修复掉」⇒ 两个 workflow 各删掉 `assembleLeanbackArm64_v8aRelease`/`assembleLeanbackArmeabi_v7aRelease`，step 名 `Build four release APKs` → `Build release APKs`。D14 的"不碰 workflow"随之作废（见 spec D14 修订与 ADR-0008 第二条修订） |
| 12 | `VideoActivity` 的「设置」按钮 → `ControlDialog` | 用户拍板「支持播放设置」⇒ 保留不删。口径收敛为"装机配置不可改，播放设置可改"，写进 spec D23、ADR-0008 修订、CONTEXT.md「简易版」定义与 README 简易版章节 |
