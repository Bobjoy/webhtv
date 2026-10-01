# 004 编辑与删除订阅

**Blocked by**: 001

## 目标
用户能改正已有订阅的名称和地址，能删除一条订阅而不影响它曾经生效过的资源地址。

## 涉及层
- [x] 数据层：`SubscriptionStore` 的 update（按 `type`+原 `url` 定位）与 delete；更新地址后不保留旧地址的任何拉取结果（条目列表本就是会话级的，无需额外清理）。
- [x] 逻辑层：编辑时同样拒绝与他条重复的 `type`+`url`。
- [x] UI 层：管理页订阅行的编辑入口（沿用 mobile 现有长按/菜单惯例，与 `ConfigDialog.create().edit()` 的交互一致性优先）与删除确认。
- [x] 测试：Robolectric 覆盖 update 命中正确行、delete 只删订阅本身、编辑重复被拒。

## 验收标准
- 改名称后列表立即显示新名称；改地址后下次拉取用新地址（US-06）。
- 删除一条订阅后，由它提交生效的资源地址仍是生效状态，历史记录与收藏不受影响（US-07）。
- 编辑成与另一条同 `type` 同 `url` 时被拒（US-05 的编辑分支）。

## 变更记录（2026-10-01）
删除入口从订阅行的图标改到**编辑弹窗左下角**（`dialog_subscription.xml` 的 `@+id/delete`，`origin == null` 时 GONE，所以「新增」弹窗没有删除键）；行内只保留「拉取」图标 + 「使用中」标记。内置订阅点行体只弹「内置订阅不可编辑」，根本进不了弹窗，因此删除键对内置天然不可达，`subscription_delete_locked` 三条文案随之删除。真机已验：用户行弹窗有删除、新增弹窗无删除节点、删除后 `SubscriptionStore.clearCache` 把 `subscription_<type>_<url>` 写成空串（键留作墓碑，不 remove）。
