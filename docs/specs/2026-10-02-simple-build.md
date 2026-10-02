# 简易版（feat-mobile-simple）：内置点播源的手机版

术语见 `CONTEXT.md` 的「简易版」小节；不可逆取舍见 `docs/adr/0008-simple-build-is-a-deleted-code-branch.md`。
分支基线：`feat-subscription`。本 spec 不写实现代码，只定范围、接缝与验收。

## 问题陈述

现有手机版要求用户自己填资源地址（`Config URL`），或先解锁订阅门禁（ADR-0007）再从订阅里挑一条。不懂技术的普通用户既不知道去哪儿找地址，也没有能力判断哪条源失效了——对他们来说这套配置面全是障碍。同时包里装着直播、壁纸、TV(leanback) 三坨他们永远用不到的代码。

需要一版「装上就能看」的手机版：只有一个内置 `点播源`，来源由 `webhtv-sub` 的 `sub.txt` 下发（`内置点播源`）。分发出去的 APK 冷启动先要过 `启动口令门禁`，通过后才是真正的首页。界面上的设置类入口只有两处：首页顶栏右上角的带 icon 下拉菜单（刷新 / 更新资源 / 清除缓存 / GitHub 加速 / 检查更新）与播放页的 `播放设置`（D23 明确保留）；其余配置一律走默认值且不存在修改入口，并且**代码真的不在包里**，不是灰掉或隐藏。分发由维护者手动完成。

## 方案概述

在 `feat-mobile-simple` 分支上做减法式改造，一条竖切路径贯穿：冷启动 → `启动口令门禁`（缓存口令比对缓存码表）→ 后台拉 `sub.txt` → 取第一条 → 走既有 `VodConfig.load` 生效 → 首页点播可用。其余全部是删除：

- 删（不是隐藏）mobile 的直播/壁纸页面、**整个底部导航栏与其后的设置/播放器/增强/弹幕四个页面**、订阅 UI 与门禁整套；设置类动作收敛为首页顶栏右上角的一个带 icon 下拉菜单：刷新 / 更新资源 / 清除缓存 / GitHub 加速 / 检查更新。播放页的 `播放设置` 保留（D23）。
- 删 `leanback` 整个 flavor，`mode` 维度只留 `mobile`。
- 删数据层的直播/壁纸：`LiveConfig`/`WallConfig`/`bean.Live`/`LiveDao`、Room 实体表与内建 HTTP 服务的对应端点。
- 保留并复用 `SubscriptionParser` + `SubscriptionLoader`（含 ADR-0006 的 GitHub 4 跳降级与 good 键自愈），删 `SubscriptionStore`/`bean.Subscription`/`seedDefaults`；`SubscriptionCodes` 随 D18 复活，只服务启动口令门禁。
- 包名 `com.fongmi.android.tv.simple`，launcher 名「今日影视」，`versionName = 5.6.0.1` 形态。
- `webhtv-sub` 侧新增 `sub.txt`，由现有 `check-resources.yml` 日巡检顺带生成。
- 不出 Release、不接自更新通道，APK 由本地 `assembleMobile*` 产出后手动分发。本分支只把两个 workflow 里已不存在的 `assembleLeanback*` 任务删掉（D14 修订），让 `workflow_dispatch` 在本分支不至于必然失败。

## 用户故事

普通用户视角：

1. 作为新装机用户，我输一次启动口令之后就不需要填任何东西，等几秒就能看到点播内容。
1b. 作为新装机用户，第一次打开 app 落在全屏 `启动口令门禁` 页，输入 `codes.txt` 里的 4 位口令才进首页；没通过时看不到任何内容，`sub.txt` 也不会被拉取。
1c. 作为已解锁用户，之后每次冷启动离线直接进首页（含飞行模式）；口令错误提示「口令错误，请重新输入」并留在口令页，码表拉不到提示「网络不可用，请稍后重试」。
2. 作为新装机用户，如果此刻取不到 `sub.txt`，我在首页看到「正在获取点播源」的空态提示，并且 app 会自动重试，我不需要操作。
3. 作为老用户，网络断了或 `sub.txt` 失效时，我仍然用上次能看的那条源，app 不弹窗、不清空我已生效的配置。
4. 作为用户，我打开 app 直接就是点播首页，没有底部导航栏，也没有任何设置页；顶栏右上角只有一个带 icon 的下拉菜单，里面恰好五项：刷新、更新资源、清除缓存、GitHub 加速、检查更新，点每一项都有真实反馈。不存在能打开输入框让我填地址的入口，也没有点了没反应的死按钮。
4b. 作为用户，首启 app 会自己在后台拉 `sub.txt` 并把第一条配成点播源，我不用点任何东西；如果我在等的时候打开过首页（空白），我也可以手动点「更新资源」立刻重拉一次并马上生效。
5. 作为用户，我在首页看不到直播和壁纸，也不会误触进去。
6. 作为用户，我拿到维护者给我的新 APK，覆盖安装即完成升级，观看历史、收藏、在追全部保留。
7. 作为用户，我搜索、收藏、历史、投屏这些点播内部能力照常可用；播放页的 `播放设置` 也照常可用（D23）——它是看片时的临时调节，不是装机配置。

维护者视角：

8. 作为维护者，我在 `vod.json` 里增删条目，第二天的 `sub.txt` 第一行就是当天实测可用的源，不用碰 app。
9. 作为维护者，`sub.txt` 里每行是一个点播 `Config URL`，可带 `#名称`；行序即优先级。
10. 作为维护者，当第一条源加载失败时，app 会顺位尝试下一行，而不是卡在坏源上。
11. 作为维护者，简易版与完整版包名不同，可以在同一台设备上共存，互不覆盖数据。
12. 作为维护者，完整版 APK 不会被简易版误认作更新安装（包名不匹配时 `Updater` 直接拒绝）。
13. 作为维护者，在 `feat-mobile-simple` 上 `assembleMobileArm64_v8aRelease` / `assembleMobileArmeabi_v7aRelease` 各出 1 个 APK，push 分支不会触发本仓库任何 workflow。
14. 作为维护者，我在 `feat-mobile-simple` 上删掉的代码不会因为我没同步改数据层而留下编译错误——每一层删除都能独立编译收敛。
15. 作为维护者，GitHub 降级链的实现仍然只有 `SubscriptionLoader` 一份，简易版没有第二份拷贝。

## 已定决策

| # | 决策 | 备注 |
|---|---|---|
| D1 | `sub.txt` 是一行一条 `Config URL` 的纯文本清单 | `SubscriptionParser` 的行解析已支持，含 `#名称`、注释行、非 URL 行跳过。**硬约束**：`lines()` 按第一个空白切分，所以名称前必须留一个空格写成 `<url> #<名称>`；写成 `url#名称` 会把名称粘进 URL。名称内空格换成 `-`（见票 002） |
| D2 | 用**删代码**实现精简，不新增 flavor，不留编译期/运行期开关 | 见 ADR-0008 |
| D3 | 分支从 `feat-subscription` 切 | 订阅链路只在这一条线上存在 |
| D4 | 复用 `SubscriptionParser` + `SubscriptionLoader`；删 `SubscriptionStore`、`bean.Subscription`、`SubscriptionCodes`、`SubscriptionActivity/ItemActivity/EditDialog/GateDialog`、`seedDefaults` | 避免降级链出现两份实现 |
| D5 | **底部导航栏整体删除**，`SettingFragment`/`SettingPlayerFragment`/`SettingEnhanceFragment`/`SettingDanmakuFragment` 连同布局一起删；首页顶栏右上角下拉菜单**只保留五项且每项带 icon**：刷新、更新资源（手动拉 `sub.txt` 并生效）、清除缓存、GitHub 加速（`GithubProxyDialog`）、检查更新（`Updater.force()`） | 2026-10-02 三次追加需求取代原「设置页只留一行」口径——既然只剩点播，分页导航本身就是冗余；但加速链路、自更新和"手动重拉一次源"是普通用户唯一需要的三个"能自己修"的开关，所以从设置页提上来。源地址类、订阅、外观、无痕、DoH、备份/恢复、一键同步、推送 APK、推送播放、收藏入口随设置页消失。首页左上角图标不挂任何入口（原 `HistoryDialog` 列表条目取 `Config.getDesc()`，简易版里就是裸源 URL，2026-10-02 真机验收时被指出「点左上角图标会弹出链接地址」，已连同 `ConfigAdapter`/`ConfigListener`/`VodFragment.setConfig` 一起真删，`dialog_history.xml` 因 `MpvConfigHistoryDialog` 共用而保留）；切换站点仍在标题点击上 |
| D6 | 只留点播：mobile 的直播/壁纸页面删除，`leanback` 整个 flavor 从分支移除 | `mode` flavor 收成单值 |
| D7 | ~~门禁全套删除~~ **已作废（2026-10-02 晚追加需求）**：门禁以「启动硬门禁」形态复活，见 D18–D22；连击入口与订阅 UI 仍然不存在 | 原理由是"简易版没有可解锁的东西"；追加需求把门禁改成"不输入口令就不能用 app" |
| D8 | 冷启动后台拉一次；失败沿用上次生效源，不弹窗；不内置兜底源，只给空态提示 | 空态需自动重试（退避），不阻塞 UI 线程 |
| D9 | 保留 GitHub 4 跳降级与 good 键自愈，**默认不提供设置入口** | `Setting` 的 `update_github_proxy*` 走默认值。例外：D5 的三次追加需求把「GitHub 加速」(`GithubProxyDialog`) 提回首页下拉，作为普通用户唯一能自己修拉取失败的开关 |
| D10 | `applicationId = com.fongmi.android.tv.simple`；launcher 名 `app_name` 改为「今日影视」（`values/strings.xml` 与 `values-zh-rCN/strings.xml` 同步） | 原口径「沿用 TV」后被推翻：给普通用户要看得懂的名字。代价是桌面图标名与完整版不同但图标本身相同，需靠名字区分 |
| D11 | `versionName = 5.6.0.1` 形态（主版本号 + 后缀） | 保留可辨识的版本线与 `versionCode` 递增，便于覆盖安装与排障 |
| D12 | Room 删 `Live.class` + `LiveDao`，`VERSION` 重置为 1，破坏性迁移 | 简易版是新包，没有存量数据要迁；`Config.type` 列保留不动，只是不再写 1/2 |
| D13 | 内建 HTTP 服务的直播端点删除（**实施后实测收敛**） | `/tvbus` 唯一消费方是同一 APK 内的 tvbus native 核心，直接删；`/manage/configs`、`/manage/config/use` 与 remote 侧的 `config/use` 保留骨架，`type != 0` 一律回 `400 Config type not supported`，`/m3u8`、`/device` 与 WebHome 静态页不动。真机 `adb forward` 实测：`/` 200、`/manage/configs` 只返回 `type:0`、`config/use?type=1` 400 |
| D14 | ~~不改 `android-release.yml` 与 `apk-build.yml`~~ **2026-10-02 晚修订：两个 workflow 里已不存在的 `assembleLeanback*` 任务删掉**。分发口径不变：仍手动编译 `assembleMobile*` 出包，不出 Release、不接自更新 | 原决定假设"不碰 workflow"。但 `android-release.yml` 是 `workflow_dispatch`，任何分支都能手动触发，在本分支跑必然因 `assembleLeanbackArm64_v8aRelease` 任务不存在而失败；`apk-build.yml` 只在 `feat-subscription` 触发，同批改掉是为了合并时不带坏构建。签名 secrets 仍不是本需求的阻塞项 |
| D15 | `webhtv-sub` 的 `sub.txt` 由 `check-resources.yml` 日巡检自动生成，人工只维护 `vod.json` | 只写当天实测可用项，顺序沿用 `vod.json` 的验证结果 |
| D16 | `Setting` 里遗留的直播/壁纸 pref 键不追求清空 | 只删读它们的代码路径，避免无谓改动面 |
| D17 | 首启拉取只发生在冷启动一次，不做前台轮询 | 降低流量与省电风险 |
| D18 | **启动硬门禁复活**：分发出去的 app 必须先输入 `codes.txt` 里的 4 位口令才能使用；口令不对或没输过就不允许进入 app，且**不拉 `sub.txt`** | 2026-10-02 晚追加需求，推翻 D7 的「门禁全套删除」。删除的订阅 UI / 连击入口 / `isSubscriptionUnlocked` 不复活，只复活"口令校验"这一件事 |
| D19 | 校验凭据 = **缓存口令 + 缓存码表**：验证成功后把用户输入的口令和当次码表都落盘；每次冷启动用缓存码表离线比对缓存口令，同时后台静默刷新码表 | 断网可用；换码/吊销在用户下次联网启动后生效。首次安装必须联网成功过一次才能解锁（明示接受的代价） |
| D20 | 校验粒度 = **仅冷启动**（进程创建时一次），从后台切回前台不重复校验 | 避免看片中途被锁 |
| D21 | 失败口径两类提示：口令不在码表 → 「口令错误，请重新输入」并留在口令页；码表拉不到且无缓存 → 「网络不可用，请稍后重试」。**不内置码表、不做服务端校验、口令页不留代理入口** | 他选定的定位是"防误入"而不是访问控制：`codes.txt` 在公开仓库明文可读，任何人 `curl` 即可拿到全部口令；服务端校验（旧订阅工单 007 的 Deno 方案）明确不做。死锁（首装 + 所有降级跳失败）是该决策明确接受的后果 |
| D22 | 形态 = **独立 `GateActivity` 作 launcher 前置页**，解锁后才 `startActivity(HomeActivity)`；`HomeActivity` 自身也要校验（覆盖 `ACTION_SEND`/`VIEW` 深链绕过 launcher 的直接入口）。口令与码表的 pref 键不进 `Backup.APP_PREFS` 白名单 | 全屏口令页比"首页上盖一个不可取消对话框"语义干净，用户不会看到空态闪现；备份还原不得成为绕开门禁的途径（沿用订阅工单 009 的「门禁标记不进备份」口径） |
| D23 | **「所有设置走默认值且不支持修改」有一条例外：播放设置保留**（`VideoActivity.onSetting()` → `ControlDialog`，含解码方式、画面比例、弹幕等播放期调节），`VideoActivity` 的「设置」按钮与播放控制条入口不删 | 2026-10-02 审查冲突项由他拍板「支持播放设置」。简易版删的是**装机配置**（资源地址、订阅、直播、壁纸、增强、缓存策略等）；播放设置是普通用户看片时也会用到的应急手段（硬解不行切软解），删掉会把"播不出来"变成无解。D4/D5 的"唯一设置类入口是首页下拉"按此口径理解为装机配置类入口 |

## 测试决策

接缝（seam）优先复用，全案只新开一个：

1. **既有接缝 `VodConfig.load(Config, Callback)`** — 自动生效仍从这里进，不新增第二条生效路径。这条接缝之上的行为靠真机验证。（票 008 审查时确认基线里那个从未被引用的 `ConfigActivator` 中间层已随死代码删除，真正的接缝一直是 `VodConfig.load`。）
2. **唯一新接缝：挑源纯函数** — 从 `List<SubscriptionParser.Item>` 里决定"下一条要试哪条"（含与当前生效源的比较）。它没有 Android 依赖，落在 `app/src/test`（共享 JVM 源集，264 个既有测试同处），fixture 复用 `app/src/test/resources/subscription` 的真实形态。口令门禁复用基线既有的纯函数接缝 `SubscriptionCodes.check(content, input)`，不再新开接缝。
3. **删除类改动没有行为可测**，验收方式是：`assembleMobileArm64_v8aRelease` 编译收敛 + `app/src/test` 全量绿。

明确的盲区与补偿：`app/build.gradle:205` 的 Robolectric 只挂在 `testLeanbackImplementation`，D6 删掉 leanback 后 **UI 层没有自动化测试通道**。因此首页/顶栏可见性、空态提示、自动生效这几项只能靠真机手测（本机已连接的测试机，`adb` 一律带 `-s <serial>`），不假装被测试覆盖。

「完成」的定义：真机上全新安装 → 首启自动出内容；断网首启 → 空态提示且恢复网络后自动生效；已有源时把 `sub.txt` 改坏 → 保持旧源且不弹窗；首页无底部导航栏、无直播/壁纸入口，顶栏右上角带 icon 的下拉菜单五项（刷新/更新资源/清除缓存/GitHub 加速/检查更新）逐个可操作有反馈；覆盖安装新 APK 后历史/收藏/在追仍在。

## 明确不做

- 不做 `simple/full` flavor，不做运行期开关，不做远程配置裁剪。
- 不内置兜底点播源，不做 sub.txt 的镜像/多源灾备（jsDelivr、Fastly 等）。
- 不出 GitHub Release，不接自动更新通道，不在首页下拉以外保留任何更新入口。两个 workflow 只按 D14 修订删掉 `assembleLeanback*` 任务，不新增分支触发条件。
- 不做首启引导页、教程、图标重绘、应用名改造。
- 不在本分支支持 TV(leanback) 端，也不为 leanback 保留任何直播能力。
- 不动 `webhtv-sub` 的 `vod.json`/`live.json`/`codes.txt` 格式与语义。`codes.txt` 现在同时服务完整版订阅门禁与简易版的启动口令门禁（D18），格式仍是「一行一个 4 位数字、`#` 注释」。
- 不清理 `Setting` 中不再被读的直播/壁纸 pref 键。
- 不做简易版的订阅/自定义源能力，不提供任何"逃生口"。
- 不承诺 `feat-mobile-simple` 与 `main`/`fongmi-sync` 的双向合并策略，本期只单向手工挑回合。

## 补充说明

- **检查更新走的是完整版仓库的 Release 通道，且简易版没有自己的通道**（D5 把「检查更新」放回首页下拉，D14 又决定手动分发不出 Release）：资产名取 `BuildConfig.FLAVOR_mode + "-" + FLAVOR_abi`（`Updater.java:96-98`），与完整版完全同名；`Update.hasUpdate()` 是 `code != VERSION_CODE || !AppVersion.isCurrent(name)` 的**不等**判断，所以完整版只要发布过任何带 `mobile-arm64_v8a.json` 的 release，`5.6.0.1` 的简易版就会判"有更新"并去下完整版的 APK，再被 `Updater.java:500` 的 `APPLICATION_ID` 校验拦下报「应用身份或签名不一致」——即"提示有更新但永远装不上"。早期"真机实测弹的是已是最新版本"的结论来自当时还没有可匹配的 release 资产，不成立为常态。彻底解法是简易版用自己的 release/tag 或资产名，属分发侧，不在本期代码内；分发前建议直接把这一项从下拉里去掉。
- `bean.Live`（Room 实体，直播频道）与 `bean.Subscription`（订阅条目）不是同一个东西，删除时不要混淆。
- `History`/`Keep`/`Track` 与 `Config.type` 有耦合；D12 保留 `type` 列正是为了避免跨表改动，只删 `Live` 一张表。
- 删除顺序遵循「扩展—收缩」的收缩版：先删 UI 层（mobile 直播/壁纸页面、设置行、订阅与门禁、更新入口）→ 再删 flavor（leanback、`mode` 维度）→ 最后删数据层（`LiveConfig`/`WallConfig`/`bean.Live`/`LiveDao`/Room/server 端点）。每层收尾都必须编译过一次，避免一次巨型 diff 无法定位回归。
- mobile 侧直接引用直播/壁纸的文件共 8 个（`LiveActivity`、`HomeActivity`、`LiveAdapter`、`LiveDialog`、`HistoryDialog`、`ConfigDialog`、`LiveControlDialog`、`SettingFragment`）；`src/main` 侧 25 个；`process/Manage.java` 的直播通路至少有 4 处（370/371/412/413/808 行附近）。这些是删除工作量的实测基线。
- 全程遵守 `AGENTS.md`：任务开始前起 `task_guard`，保留既有 dirty 文件，**默认不提交**。
