# 003 选中条目成为生效资源地址

**Blocked by**: 002

## 目标
用户在条目列表点一条，它立刻成为当前生效的点播资源地址，设置页显示它的名字且站点加载成功。

## 涉及层
- [x] 数据层：`Config.create(type).url(url).name(name).logo(...).update()` 写入现有 Room 表（ADR-0002）；`logo` 落 `Config.logo`，`remark` 提交时丢弃（`Config.notice` 是 `@Ignore`）。同 `url`+`type` 已存在则复用既有行（走 `AppDatabase.getConfigDao().find(url, type)`，与 `ConfigDialog.saveConfig()` 同逻辑）。
- [x] 逻辑层：不新建生效机制 —— 复用 `ConfigListener.setConfig(Config)` → `SettingFragment.load(config)` 这条既有通路；「使用中」判定读 `Prefers` 键 `config_<type>`。
- [x] UI 层：条目行点击 → 提交 → 回管理页并刷新「使用中」标记；`ConfigEvent` 已在生效时广播，设置页 `onConfigEvent` 会更新 `vodUrl` 文案。
- [ ] 测试：Robolectric 一条贯通断言（点条目 → `config_0` 等于该 url → `Config` 表新增/复用一行）；`Backup`/历史/远程管理不需要改动，只做一条回归确认。

## 验收标准
- 点一条条目 → 走 `VodConfig.load()` → 加载完成后设置页 `vodUrl` 显示条目名称（US-21）。
- 条目列表里与 `config_0` 相同的那条显示「使用中」（US-22）。
- 重复点同一条目不新增 `Config` 行（US-23）。
- `logo` 出现在 `Config.logo`；`remark` 不出现在任何持久化字段（US-24）。
- 由订阅生效的源，在历史、远程管理页、备份里与手填源表现一致（US-25）。
- 真机或 Robolectric 截图走完一次点播金路；本机是 Intel Mac 且产物为 arm ABI，**不承诺 AVD 验证**。
