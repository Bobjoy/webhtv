# 003 自动生效竖切：冷启动拉 sub.txt → 点播可用

**Blocked by**: 001, 002

## 目标
全新装机的用户不需要任何操作，冷启动后自动看到点播内容；拉不到时保持上次生效源且只显示空态提示。

## 涉及层
- [x] 逻辑层：新增挑源模块（常量 `https://raw.githubusercontent.com/Bobjoy/webhtv-sub/main/sub.txt`），复用 `SubscriptionLoader.load` + `SubscriptionParser`，产出「下一条该试哪个 url」的**纯函数**（输入条目列表 + 当前生效源，输出候选，无 Android 依赖）
- [x] 生效层：走既有接缝 `ConfigActivator.activate(Config, Callback)`（`type=0`），不新开第二条生效路径
- [x] 触发层：mobile 冷启动后台执行一次（`Task` 线程），不做前台轮询
- [x] UI 层：首页无生效源时显示「正在获取点播源」空态 + 退避自动重试；已有生效源时不显示、不打扰
- [x] 测试：`app/src/test` 新增 JVM 测试（fixture 复用 `app/src/test/resources/subscription` 的真实形态）

## 验收标准
- 真机（本机测试机）卸载重装后打开，无需任何输入即在数秒内出现点播内容
- 飞行模式首启：首页显示空态提示且不崩溃、无弹窗；恢复网络后重试自动生效
- 已有生效源时把 `sub.txt` 内容改坏（返回非法正文）：当前源保持不变，UI 无提示打断
- 挑源纯函数测试覆盖：空列表 / 首条与当前生效源相同（应跳过）/ 全部重复 URL / 非 URL 行混入
- 第一条源 `VodConfig.load` 失败时，自动顺位尝试第二行（真机或 mock 任一种方式验证到即可）
- `app/src/test` 全量绿；不依赖 Robolectric

## 约束
- 不内置兜底源；不在 app 内写死任何非 GitHub 清单地址
- 默认不提交

## 实现补记
- 真机 `dumpsys activity top` 定位到空态提示不显示的成因：`mPending` 原先在 `Task.submit` 的后台线程里才置位，`VodFragment.initView()` 的 `showProgress()` 早于它执行，读到 `false` ⇒ 转圈可见、`@id/hint` 被置 GONE。改为 `BuiltinSource.refresh(Config, Consumer<Boolean>)` 由调用方（`HomeActivity.initConfig` 已持有 `mStartupConfig`）同步传入当前源，pending 在首帧之前就位；后台重试走 `retry()` 自己读 `Config.vod()`。
- `sub.txt` 形态已核对：`SubscriptionParser.lines()` 按空白切分，`http://xhztv.top/4k.json #xhztv-4K` 解析为 url=`http://xhztv.top/4k.json`、name=`xhztv-4K`，首条即挑中。
- 端到端已打通（2026-10-02）：`sub.txt` 已提交并 push 到 `webhtv-sub` main，线上 HTTP 200（888 B，16 行）。真机全新安装首启实测：冷启动自动拉到清单并生效，分类与海报正常出内容。顺位实测发现清单第 1 行在该机上不可用，`BuiltinSource` 自动轮转到第 2 行才成功，并在 `Config` 表留下一条 `time=0` 的记录。
