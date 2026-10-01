# 005 直播与壁纸订阅接通

**Blocked by**: 003

## 目标
用户在直播分组和壁纸分组各走一遍「拉取 → 选一条 → 生效」，与点播同路。

## 涉及层
- [x] 逻辑层（前置小重构，本片内完成）：把 `SettingFragment.load(Config)` 里的 `switch (type)` 抽到 `app/src/main` 的公共激活入口（如 `ConfigActivator`），供 mobile 设置页与订阅条目页共用；`SettingFragment.setConfig()` 保留 `file://` 权限申请分支，其余行为不变。壁纸分支要保留现有 `Setting.putWall(0)` 这一句。
- [x] 数据层：`Config` 的 `type=1`/`type=2` 与 `config_1`/`config_2` 键沿用现有语义，无新增。
- [x] UI 层：订阅管理页的直播、壁纸分组接入同一条拉取与条目通路（002/003 的页面按 `type` 参数化，不复制页面）。
- [ ] 测试：Robolectric 断言 `type=1` 命中 `LiveConfig.load()`、`type=2` 命中 `WallConfig.load()` 且「使用中」比对用的是各自 `config_<type>`；抽取 `ConfigActivator` 后原有设置页行为一条回归。

## 验收标准
- 直播条目提交后 `config_1` 更新且 `LiveConfig` 加载成功，设置页 `liveUrl` 显示条目名（US-26）。
- 壁纸条目提交后 `config_2` 更新且 `WallConfig` 加载成功，`wallUrl` 文案跟着变（US-26）。
- 直播条目列表的「使用中」标记读 `config_1`，不串到点播。
- 抽取公共激活入口后，手填资源地址的既有流程（`ConfigDialog`）行为零变化。
