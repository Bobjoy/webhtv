# 009 解锁状态不进备份

**Blocked by**: 008

## 目标
验证"解锁是设备本地状态"这条口径：导出备份里找不到 `subscription_gate`，在另一台机器导入这份备份后「订阅」行仍然隐藏，需要重新连击输码。

## 涉及层
- [x] 逻辑层：`bean/Backup.java` 的 `include(String key, SyncOptions options)`（L169）里加一行 `if ("subscription_gate".equals(key)) return false;`，写法与位置紧跟已有的 `remote_trust_` 排除分支；**不要**把该键加进 `APP_PREFS`（`subscription`（订阅列表本体）留在 `APP_PREFS` 不动）。
- [x] 测试：`app/src/test/.../bean/BackupPreferenceFilterTest.java` 加一条断言：`subscription_gate` 对全部 `SyncOptions` 组合返回 false；同时保留并确认 `subscription` 仍走 `isSettings()` 的原行为。

## 验收标准
- `BackupPreferenceFilterTest` 全绿（US-52）。
- 真机：解锁 → 全量导出备份 → `grep subscription_gate` 在导出的 JSON 里无命中（US-52）。
- 真机：解锁后清数据 → 导入这份备份 → 设置页看不到「订阅」行（恢复不会带入解锁状态）。
- 一期行为不回归：备份里的 `subscription` 键（订阅列表）照常导出导入，恢复后进订阅页列表还在。

## 验收记录（2026-10-01）

- 已过：`BackupPreferenceFilterTest.subscriptionGateStaysDeviceLocal` 断言全量 `SyncOptions` 下 `subscription_gate` 返回 false；`subscription`（订阅列表本体）分支未改动。
- 未做（如实记录）：真机全量导出备份后 `grep subscription_gate` 无命中这一步没执行——导出会写用户外部存储并要过一遍同步 UI，收益不超过已有的 `Backup.include` 接缝断言；导入恢复同理需要清应用数据，用户明确禁止清数据（会毁掉他的真实配置与订阅）。
