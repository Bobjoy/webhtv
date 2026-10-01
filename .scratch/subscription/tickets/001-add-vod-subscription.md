# 001 添加并看到一条点播订阅

**Blocked by**: 无

## 目标
用户在手机设置页点「订阅」进入管理页，能添加一条点播订阅并在列表里看到它，杀掉进程重开依然存在。

## 涉及层
- [x] 数据层：`app/src/main/java/com/fongmi/android/tv/setting/SubscriptionStore.java`（Prefers 键 `subscription` 存一条 JSON 数组，仿 `GitAccountStore` / `PlaybackWebhookStore`）；`bean/Subscription.java`（`name` / `url` / `type`）。`Backup.APP_PREFS` 白名单加 `subscription`。
- [x] 逻辑层：新增/重复判定（同 `type` + 同 `url` 拒绝）；名称留空时用清单顶层 `name` 兜底、再退到 URL host（顶层 `name` 依赖 002 的拉取，本片先做 host 兜底）。
- [x] UI 层：`app/src/mobile/res/layout/fragment_setting.xml` 在 `wallUrl` 之后加「订阅」一行；`SubscriptionActivity`（mobile）三段分组骨架（点播/直播/壁纸）+ 空态 + 「添加」；`SubscriptionEditDialog` 填名称与地址。`AndroidManifest` mobile 段注册 Activity。
- [x] 测试：Robolectric 覆盖 `SubscriptionStore` 增删查、重复 `type`+`url` 拒绝、重启后持久化；`Backup.include("subscription", settings)` 为 true，且取消「配置」选项时不导出。

## 验收标准
- 添加一条点播订阅后，管理页点播分组立即出现该条（US-01、US-02、US-03）。
- 同 `type` 同 `url` 再次添加被拒并提示，列表不出现两条（US-05）。
- 名称留空时显示订阅地址的 host（US-04 的 host 分支）。
- 杀进程重开 app，订阅列表原样还在（US-08）。
- 全量备份包含订阅、恢复后仍在；选择性同步关闭「配置」时不含（US-09）。
- leanback 端设置界面无任何变化。
