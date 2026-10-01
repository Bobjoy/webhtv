# 008 连击解锁订阅门禁（核心示踪弹）

**Blocked by**: 无

## 目标
新装手机上默认看不到「订阅」行；在设置页标题栏 2 秒内连点 5 下弹出对话框，输 4 位授权码，命中 `webhtv-sub/codes.txt` 任一条即提示"已开启，重启生效"并自动重启，重启后「订阅」行出现、点进去一期功能照旧。

## 涉及层
- [x] 数据层：解锁标记 `subscription_gate`（`Prefers` 里的 `"1"`），读 = `Prefers.getString("subscription_gate").equals("1")`，写必须用 `Prefers.getPrefers().edit().putString(...).commit()`（`Prefers.put` 是 `apply()`，会被重启的 `killProcess` 杀掉）。
- [x] 逻辑层：`api/subscription/SubscriptionCodes.java` —— 纯函数 `matches(String content, String input)`：按行切（`\R`）、跳过空行与 `#` 开头行、比对前 `trim`、任一相等即 true；`content` 为空/HTML 视为不匹配并由调用方判为"取不到码"。取码 = `SubscriptionLoader.text(CODES_URL)`（新增静态方法，3 行：`OkHttp.string(githubProxied(url, githubProxy()))`，空响应抛异常，好让上层把"取不到"与"码不对"分开）；`CODES_URL` 是 `https://raw.githubusercontent.com/Bobjoy/webhtv-sub/main/codes.txt`。不新增网络层、不新增设置项、不改 `githubProxied` 本身（ADR-0006）。
- [x] UI层：`fragment_setting.xml` 的 `MaterialToolbar` 补 `@+id/toolbar`；`SettingFragment.initView()` 里 `mBinding.subscription.setVisibility(unlocked ? VISIBLE : GONE)`，并给 toolbar 挂连击监听（Fragment 字段计数 + `System.currentTimeMillis()` 滑动窗口，窗口 2 秒、阈值 5 次；已解锁直接不计数）；新增 `ui/dialog/SubscriptionGateDialog.java`（照 `SubscriptionEditDialog` 的 `BaseAlertDialog` + 窗口宽度写法）与 `dialog_subscription_gate.xml`（一句说明 + `EditText` inputType `numberPassword`、`maxLength=4`、`digits=0123456789` + 开启/取消）；「开启」不足 4 位只提示不发请求，够 4 位走后台线程（`Task.largeExecutor()` + `App.post`）；成功 `Notify.show` + `App.postDelayed(~1500ms)` 调 `CustomActivityOnCrash.restartApplication(requireActivity(), CustomActivityOnCrash.getConfig())`；失败按两类分别提示（`授权码错误` / `无法连接验证服务，请检查网络或 GitHub 代理设置`），对话框保持打开。
- [x] UI层：`app/src/mobile/res/values{,-zh-rCN,-zh-rTW}/strings.xml` 新增 `subscription_gate_*`（说明、输入提示、开启、已开启待重启、码错、取不到码）——三个目录都要有。
- [x] 测试：`app/src/test/.../api/subscription/SubscriptionCodesTest.java` 纯 JUnit（多行命中、`#` 注释、空行、首尾空白、重复码、空内容、HTML 内容、前导零不同、输入长度不足）；不加 Robolectric 依赖。

## 验收标准
- 装机后 uiautomator dump 设置页：无 `subscription` 行节点；连击别处 5 下不弹窗；点 4 下停 3 秒再点 4 下不弹窗（US-31、34、36、37）。
- 连击标题栏第 5 下弹出对话框，含密码输入框；点取消状态不变（US-34、39、40）。
- 已解锁后继续连击不弹窗（US-38）。
- 输错码 → Toast 是「授权码错误」且对话框仍在；飞行模式下输任意码 → Toast 是「无法连接验证服务…」而不是"授权码错误"（US-45、46）。
- 真机金路：临时把 `CODES_URL` 指向本机 `:8899` fixture（绕过墙内 `raw.githubusercontent.com` 不可达）→ 输对码 → 约 1.5 秒后进程 pid 变化、`shared_prefs` 里 `subscription_gate` 已落盘、设置页出现「订阅」行、点进去种入两条内置订阅并可拉取；验完把 URL 改回真实地址（US-49 至 51）。
- `bash ./gradlew --console=plain :app:assembleMobileArm64_v8aDebug :app:compileLeanbackArm64_v8aDebugJavaWithJavac :app:testMobileArm64_v8aDebugUnitTest` 一次通过。

## 备注
- 隐藏期间零副作用由既有代码保证：`seedDefaults()` 只在 `SubscriptionActivity:67` 调用，入口不显示就不会写 `subscription` / `subscription_seeded`（US-32）。本任务只需在验收里确认，不写清理逻辑。
- leanback 不动；不加反向关闭入口；不做码表缓存与兜底码（ADR-0007）。

## 验收记录（2026-10-01，USB 连接的真机）

已实测通过：
- 未解锁时设置页 dump 无「订阅」节点；连击标题栏第 5 下（2 秒内）弹出对话框，含说明 + 4 位密码框 + 开启/取消。
- 窗口过期不累计：4 下 → 停顿 → 4 下 → 1 下不弹窗（该现象在调测中真实复现过）。
- 三类提示分支各一张截图：不足 4 位「请输入 4 位授权码」（不发请求）、拉不到码「无法连接验证服务，请检查网络或 GitHub 代理设置」、码不对「授权码错误」，失败后对话框保持打开。
- 金路：输对码 → 提示「已开启，重启生效」→ 进程 pid 25480→26227、`shared_prefs` 落 `subscription_gate=1`、重启落在 HomeActivity、设置页出现「订阅」行、点进去两条内置订阅在列。
- 已解锁后再连击 5 下：dump 命中 0，不弹窗。
- 改回真实 `CODES_URL` 并重装后：解锁状态仍在、订阅页正常、连击不弹窗。
- 构建：`assembleMobileArm64_v8aDebug` + `compileLeanbackArm64_v8aDebugJavaWithJavac` + `testMobileArm64_v8aDebugUnitTest --tests "*Subscription*" --tests "*BackupPreferenceFilter*"` 一次通过（`SubscriptionCodesTest` 9 例全绿）。

未做（如实记录）：
- 「拉不到码」用本机 fixture 端口断开复现，未真开飞行模式；异常分支同一条代码路径。
- 改回真实 URL 后**没有**跑通一次真实解锁：`codes.txt` 尚未 push（010 的交付边界是不 push），且墙内直连 `raw.githubusercontent.com` 不通，需要用户 push 后配 GitHub 代理再验一次。
