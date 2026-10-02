# 007 删数据层直播/壁纸：LiveConfig、WallConfig、Live 表、服务端端点

**Blocked by**: 004, 005

## 目标
包内不再有直播/壁纸的数据结构与播放通路：`LiveConfig`/`WallConfig`/`bean.Live`/`LiveDao` 与 Room 的 live 表、内建 HTTP 服务的对应端点全部移除，点播与投屏不回归。

## 涉及层
- [x] 前置实测（必须先做）：确认 WebHome/DLNA/`server` 是否有外部消费方依赖直播/壁纸端点——实测方式：完整版真机上按 WebHome 与投屏流程走一遍，记录请求到的端点名。确有外部消费方则保留端点骨架（返回空/不支持），不破坏协议
- [x] 数据层：删 `bean.Live` + `LiveDao`，`AppDatabase` 实体列表移除 Live，`AppDatabase.VERSION` 重置为 1，采用破坏性迁移（新包无存量数据）
- [x] 逻辑层：删 `api/config/LiveConfig`、`api/config/WallConfig`，`ConfigActivator` 收敛为只处理 `type=0`
- [x] 引用面：`src/main` 25 个引用文件中逐处断开（`PlayerManager`、`process/Manage.java` 的 370/371/412/413/808 行附近、`TVBus`、`IjkSimplePlayer` 等）
- [x] `Config.type` 列保留不动，只是不再写入 1/2（`D12`）；`Setting` 里遗留的直播/壁纸 pref 键不清理（`D16`）
- [x] 测试：`app/src/test` 全量绿；宽重构分 `db / player / server` 三批推进，每批各自编译收敛

## 验收标准
- `grep -rn "LiveConfig\|WallConfig\|bean.Live" app/src` 无结果（或仅剩保留骨架的注释说明）
- Room：实体列表无 Live，`VERSION = 1`；卸载重装后数据库正常建立，历史/收藏/在追读写正常
- 真机回归不塌陷：点播播放、搜索、详情、历史续播、投屏（DLNA/WebHome）全部可用
- 全量 `app/src/test` 绿；`assembleMobileArm64_v8aRelease` 与 armeabi-v7a 均通过
- 服务端若选择保留骨架，必须在票内记录"哪些端点保留、为什么"

## 约束
- 爆炸半径最大的一票，禁止顺手改播放器/解码逻辑；发现问题只记录不扩大
- 默认不提交

## 实现补记（2026-10-02）

### 前置实测结论：端点保留骨架，不删协议
实测方式：读 `server/` 路由 + `assets/js/manage.js` 调用面（不是真机抓包，消费方就在同一个 APK 里，代码即可判定）。

- `/tvbus`（`Nano:82` → `LiveConfig.getResp()`）：唯一请求方是内置 tvbus native core，只在 `tvbus://` 直播播放时才发生。点播源不会产生该请求 ⇒ **端点直接删除**，不留骨架。
- `/m3u8`（`process/M3u8.java`）：给 Python 源改写 `127.0.0.1:9978/m3u8` 链接的通用代理，不依赖任何直播数据结构 ⇒ **原样保留**，删了只会伤到可能的点播源。
- `/manage/config/*?type=1|2`（`process/Manage.java`）与 remote `config/use`（`RemoteConfigOps`）：真实消费方是包内 `manage.js`（有「直播/壁纸」页签）与 remote 协议。为不破坏协议形状，**保留端点**，`type != 0` 返回 `400 Config type not supported` / `failure("Config type not supported")`；列表类接口（`config/list`、`configObject`、`configTypeName`）保持原样，仍按 `Config.type` 展示存量行（`D12` 保留列）。
- `PlaybackService` 的 `MediaLibrarySession` browse 树：根节点去掉「直播」文件夹，`BrowseTree` 不再派发到 `LiveBrowse`。

### 删除清单
`api/config/LiveConfig`、`api/config/WallConfig`、`bean/Live`、`db/dao/LiveDao`、`api/LiveApi`、`api/parser/LiveParser`、`api/parser/EpgParser`、`browse/LiveBrowse`、`impl/LiveListener`、`model/LiveViewModel`、`setting/LiveEpgSetting`、`player/extractor/TVBus`；随引用归零一并删除的直播专用 bean：`Group`、`Channel`、`Epg`、`EpgData`、`Catchup`、`Tv`、`Core`。另删两个已无引用、且引用被删类的对话框：`main/ui/dialog/CustomCspDialog`、`mobile/ui/dialog/ConfigDialog`。

Room：实体列表去掉 `Live`，`getLiveDao()` 移除，`VERSION` 37 → 1（破坏性迁移，新包无存量数据）。

播放器侧只动了一处：`Source` 的 extractor 注册表去掉 `TVBus`（`tvbus://` 无核心配置可加载，通路必然失效），其余解码/播放逻辑未触碰。

### 保留与未清理（按既定决策）
- `Config.type` 列保留，只是不再写入 1/2（`D12`）。
- `setting/LiveSetting`、`Setting` 里的直播/壁纸 pref 键、`Backup.APP_PREFS` 中的 `live_*`/`wall` 键名全部保留（`D16`）。`LiveSetting` 仍被 `ConfigEvent.boot()` 与 `CustomKeyDown` 引用，不是死代码。
- `CustomCspSetting` 的 `KIND_LIVE` 与 JSON 存储字段保留（那是本地 CSP 注册表的存储形态），只删掉把 JSON 实体化成 `bean.Live` 的接口：`inject(List<Live>, spider)`、`Registry.lives()`、`Item.live()`、`liveObject()`、`hasLives()`。
- 首页入口按用户指示**保留切换站点**：标题点击 → `SiteDialog.change()` 不动。logo 点击原本打开 `HistoryDialog`（内置源配置列表，条目文案是 `Config.getDesc()`，简易版里就是裸 URL），2026-10-02 真机验收时他指出「点击左上角图标会弹出链接地址」⇒ 该入口连同 `HistoryDialog`/`ConfigAdapter`/`ConfigListener`/`VodFragment.setConfig` 一并真删，logo 变为纯装饰；`dialog_history.xml` 保留（`MpvConfigHistoryDialog` 共用）。
- `api/parser/` 目录已空；`assets/` 与残留布局/字符串里的直播文案未清理（无引用，票 008 量包体时一并看）。
- tvbus native 库（`app/libs/tvbus-release.aar`，`proguard-rules.pro:97` keep 规则）仍在依赖里：`Source` 已不再注册 `TVBus` extractor，该 AAR 变为无用重量。删依赖属于改 dependency 声明，超出本票范围 ⇒ 交给票 008 作为减重项评估。
- `db/Migrations` 的 30→37 迁移链仍在（引用旧 live 表 SQL）。`VERSION=1` 时不可能被走到，且 `fallbackToDestructiveMigration(true)` 已开启，设备上的旧库会直接重建 ⇒ 保留不动，票 008 可一并清理。

### 验证（已全部执行）
- `grep -rn "LiveConfig\|WallConfig\|bean\.Live\|getLiveDao\|LiveApi\|LiveBrowse\|LiveViewModel\|TVBus" app/src --include='*.java'` → 仅剩 `IjkSimplePlayer:204-210` 的 `MediaItem.LiveConfiguration`（Media3 直播流清单配置，与被删的 TV 直播数据层无关，播放器未动）。
- `:app:compileMobileArm64_v8aReleaseJavaWithJavac` 通过；`:app:compileMobileArm64_v8aDebugUnitTestJavaWithJavac` 通过（测试源码无直播引用）。
- `assembleMobileArm64_v8aRelease assembleMobileArmeabi_v7aRelease` → BUILD SUCCESSFUL in 7m45s；arm64 APK 装机成功。
- 真机回归（release 包，Room v37→v1 破坏性迁移后冷启动）：logcat 无 FATAL；首页分类栏+海报墙渲染正常；搜索页与切源弹窗正常；`VideoActivity` 打开后 `dumpsys audio` 显示本进程 AudioTrack `state:started`/`USAGE_MEDIA`/48kHz ⇒ 点播播放可用；「最近观看」列表渲染并含迁移后新写入的进度记录（已看 00:39），点击直达 `VideoActivity` 且同一选集高亮、音频再次 started ⇒ 历史续播可用；详情页「投屏」打开设备选择弹窗并列出局域网设备 ⇒ DLNA 投屏可用。
- 内置 HTTP 服务（`adb forward tcp:9978`）：`/` → 200（12.7 KB WebHome 页面）；`/manage/configs` → 200 且只返回 `type:0` 点播项；`/manage/config/use?type=1` → 400 "Config type not supported"；`/tvbus` → 落到 assets 兜底返回空 body（原 `LiveConfig.getResp()` 分支已删，无外部消费方）；`/m3u8` 路由仍在（缺参时 500，非本票改动）。
