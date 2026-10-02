# 006 删订阅链路与门禁：只留 Parser + Loader

**Blocked by**: 005

## 目标
简易版里不存在任何"订阅"与"解锁"概念：代码搜不到门禁、订阅列表、条目页；自动生效只依赖 `SubscriptionParser` + `SubscriptionLoader` 这两个保留文件。

## 涉及层
- [x] 删（UI）：`SubscriptionActivity`、`SubscriptionItemActivity`、`SubscriptionEditDialog`、`SubscriptionGateDialog` 及其 layout/strings/manifest 声明
- [x] 删（数据与逻辑）：`SubscriptionStore`（含 `seedDefaults`、内置 vod/live 订阅种子、active/cache 语义）、`bean.Subscription`、`SubscriptionCodes`、`Setting.isSubscriptionUnlocked`/`putSubscriptionUnlocked` 及解锁连击判定
- [x] 删（备份）：`Backup` 里订阅相关字段与 include 白名单条目（若有）
- [x] 保留：`api/subscription/SubscriptionParser.java`、`api/subscription/SubscriptionLoader.java`（003 依赖），GitHub 降级链与 good 键自愈原样可用
- [x] 测试：`SubscriptionStoreTest` 等失效测试删除；`app/src/test` 全量绿

## 验收标准
- `grep -rniE "subscription(store|activity|codes|gate|edit)|isSubscriptionUnlocked" app/src` 无结果
- 设置页依旧只有「清除缓存」；连击任何区域不会出现对话框（门禁彻底不可达）
- 003 的首启自动生效仍然成功（真机重测一次），证明 Parser/Loader 未被误删
- `SubscriptionLoader` 的 GitHub 代理链在简易版仍会逐跳降级（可用抓日志或 mock 代理验证一跳）
- 全量 `app/src/test` 绿 + `assembleMobileArm64_v8aRelease` 通过

## 约束
- 完整版分支（`feat-subscription`）的订阅与门禁能力不受影响，只在本分支删
- 默认不提交
