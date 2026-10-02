# 简易版用独立分支删代码，只保留 mobile 点播

`feat-mobile-simple` 从 `feat-subscription` 切出，用**删除代码**而不是隐藏入口的方式做面向普通用户的手机版：**整个设置页与底部导航栏删除**（2026-10-02 三次追加需求推翻原「设置页只留两行」口径），设置类动作收敛为首页顶栏右上角一个带 icon 的下拉：刷新/更新资源/清除缓存/GitHub 加速/检查更新；订阅 UI、订阅种子（`seedDefaults`）、门禁（`SubscriptionCodes` + GateDialog + 连击判定 + `isSubscriptionUnlocked`）、mobile 的直播与壁纸页面、`leanback` 整个 flavor、Room 的 `LiveDao`/Live entity（version 重置为 1）、内建 HTTP 服务的直播端点移除（实测后收敛：`/tvbus` 删，`/manage/config*` 与 remote 侧保留骨架并对 `type != 0` 回 400）。点播源改为冷启动后台拉取 `webhtv-sub` 的 `sub.txt`，**复用** `SubscriptionParser` + `SubscriptionLoader`（含 ADR-0006 的 GitHub 降级链与 good 键自愈），取第一条生效；拉不到就沿用上次生效源且不弹窗，不内置兜底源。包名加 `.simple` 后缀，`versionName` 用主版本号加后缀（如 `5.6.0.1`）。分发为手动编译，本期不接 Release 自更新通道（两个 workflow 里的 `assembleLeanback*` 任务见下方修订）。

**理由**：备选有三条——编译期开关隐藏、新增 `simple/full` flavor、分支真删。前两条都减少不了一字节 dex 和 res，与"减小 APK 体积"的目标无关；flavor 还会把 APK 矩阵从 4 个翻成 8 个，牵动 CI、签名与 Release 资产命名。真删的代价是分支分叉，但简易版的目标用户不需要上游播放器实验特性（`fongmi-sync` 那条线），分叉成本可以接受。同时选择复用订阅链路而不是新写一个极简加载器，避免刚调好的 4 跳降级逻辑出现两份实现并漂移。

**后果**：

- `feat-mobile-simple` 无法自动接收 `main`/`fongmi-sync` 的上游合并，改动只能手工挑回合；反向合并（简易版改动回主干）基本不可行，因为代码是被删除而不是被切换。日后要收敛两条线，得先重建被删的功能。
- Robolectric 只挂在 `testLeanbackImplementation`（`app/build.gradle:205`）。删掉 leanback 后 UI 层失去自动化测试通道，简易版的验证只剩编译收敛 + 真机手测；共享的 `app/src/test` 纯 JVM 测试仍然有效。
- 删除内建 HTTP 服务的直播端点可能静默影响 WebHome 与 DLNA。实施时以实测为准，不臆断影响面；若确有外部消费方，宁可保留端点骨架也不破坏协议。**实测结论（2026-10-02）**：`/tvbus` 的唯一消费方是同一 APK 内的 tvbus native 核心，删除；`/manage/configs`、`/manage/config/use` 与 remote 侧 `config/use` 保留骨架并拒绝 `type != 0`；`/`、`/device`、`/m3u8` 经 `adb forward` 实测仍正常响应。
- 共存装机后桌面会出现两个应用，靠图标区分不了：`app_name` 在本分支已改为「今日影视」（`values/strings.xml` 与 `values-zh-rCN/strings.xml` 同步，推翻原「launcher 名沿用 TV」口径），完整版显示的是「影视」。
- `sub.txt` 的行序即优先级、由 webhtv-sub 日巡检自动生成，从此成为面向普通用户的对外契约：格式或语义变更必须同时兼容线上旧版简易版 app。
- 全新安装且 GitHub raw 完全不可达时，首启只能看到空态提示（决策明确接受该后果）。

## 修订（2026-10-02 晚）：门禁以「启动口令门禁」复活

上文「门禁（`SubscriptionCodes` + GateDialog + 连击判定 + `isSubscriptionUnlocked`）全套删除」的口径被追加需求推翻：分发出去的 app 必须先输入 `codes.txt` 的 4 位口令才能使用，未通过不拉 `sub.txt`。

- 复活的只有口令校验本身（`SubscriptionCodes` 纯函数 + `SubscriptionLoader` 的 GitHub 4 跳降级链）。连击入口、解锁标记、订阅 UI 仍然不存在，也不是原来的"隐藏功能"语义，而是阻塞式。
- 校验凭据是「缓存口令 + 缓存码表」：冷启动用缓存码表离线比对缓存口令，联网时静默刷新码表。断网可用；换码/吊销在用户下次联网启动后才生效。
- 明确不做：内置码表兜底、服务端校验、口令页代理输入。首装且所有降级跳失败即锁死、只提示重试 —— 这是本次取舍**主动接受**的后果。
- 定位仍是"防误入"而不是访问控制：`codes.txt` 在公开仓库明文可读，`curl` 即可拿到全部口令。
- 新增后果：`HomeActivity` 必须自带"未解锁即重定向"的守卫，否则 `ACTION_SEND`/`VIEW` 深链可以绕过口令页直接进首页。
- 落地形态（工单 009，2026-10-02 晚）：`GateActivity` 取代 `HomeActivity` 成为 launcher；`Gate` 只做编排，pref 键 `gate_code`（口令）与 `gate_codes`（码表原文）不进 `Backup.APP_PREFS` 白名单，进程内 `unlocked` 标记只在冷启动判定一次。真机实测：冷启动落口令页、`SEND`/`VIEW` 深链直指 `HomeActivity` 被弹回、错误口令提示「口令错误，请重新输入」并留在口令页、关 Wi-Fi 与数据后冷启动直接进首页。

## 修订（2026-10-02 晚）：CI workflow 与播放设置两处口径收敛

- **workflow 不再是"不碰"**：`android-release.yml` 是 `workflow_dispatch`，任何分支都能手动触发，本分支上它仍调用已被删除的 `assembleLeanbackArm64_v8aRelease` / `assembleLeanbackArmeabi_v7aRelease`，必然构建失败。两个 workflow 各删掉这两行任务（`apk-build.yml` 同改以免合并回 `feat-subscription` 时带坏构建）。分发口径不变：仍手动 `assembleMobile*` 出包、不出 Release、不接自更新。
- **"所有设置走默认值"有一条例外：播放设置保留**。`VideoActivity.onSetting()` → `ControlDialog`（解码方式、画面比例、弹幕等）不删。理由：简易版删的是**装机配置**（资源地址、订阅、直播、壁纸、增强、缓存策略），播放设置是普通用户看片时的应急手段——硬解放不出来要能切软解，删掉会把"播不出来"变成无解。因此"设置类动作收敛为首页顶栏一个下拉"应读作"装机配置类动作收敛为一个下拉"。
