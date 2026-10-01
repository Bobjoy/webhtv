# 规格说明书：订阅门禁（Subscription Gate）

日期：2026-10-01 · 术语见 `CONTEXT.md`「配置与订阅」表 · 决策见 `docs/adr/0007`，上游规格见 `docs/specs/2026-09-30-subscription.md`

## 1. 问题陈述

一期把订阅做成了默认可见的功能：装机即有「订阅」行、进页面即种入两条内置订阅。但订阅面向的是会自己找源的高级用户，普通用户点进去只会看见一堆看不懂的 URL 和"验证失败"式的网络报错，且装机立刻开始向 `webhtv-sub` 拉取。需要把它降级为**按需开启的隐藏功能**：默认不存在，知道手势的人用授权码打开。

定位必须先钉死：4 位码只有 10000 种组合，校验发生在客户端，服务端存哈希与存明文等价（ADR-0007）。这层门禁是**防误入的障眼法，不是访问控制**——本规格不承诺任何安全性，只承诺"不知道的人找不到入口，知道的人能开"。

## 2. 方案概述

- **一个 pref 布尔键 + 一个手势 + 一个对话框。** 设置页「订阅」行的可见性由 `subscription_gate` 决定；连击设置页标题栏 5 次（2 秒滑动窗口）弹解锁对话框；输 4 位纯数字，app 直连拉 `webhtv-sub` 仓库的明文 `codes.txt` 逐行比对，任一命中即写标记，提示"重启生效"并自动重启进程。
- **未解锁 = 零副作用。** 一期的 `seedDefaults()` 只在进入订阅页时触发（`SubscriptionActivity:67`），入口被隐藏后装机既不写 `subscription` 键、也不产生任何对 `webhtv-sub` 的请求。门禁不需要新增任何"隐藏期间的数据清理"逻辑。
- **取码复用订阅已有的通路**：`SubscriptionLoader.githubProxied()` 是包内可见的纯函数，直接拿它拼代理地址，再用 `OkHttp.string()` 取原文；不新增设置项、不新增网络层（ADR-0006）。
- **重启复用项目已依赖的 `CustomActivityOnCrash`**，与 `CrashActivity:41` 同一条 API；不引入第二个重启实现。
- **码表内容与 app 代码同批交付**：在 `webhtv-sub` 仓库（本地 `~/study/github/webhtv-sub`）新增 `codes.txt`，随机 4 位码 8 条。

## 3. 用户故事

每条可独立验证。`type` 沿用 0=点播 / 1=直播 / 2=壁纸。

### 3.1 默认隐藏

- **US-31** 作为新装用户，设置页看不到「订阅」这一行，也不看得到任何与订阅有关的入口；订阅相关的全部界面仍只在 mobile flavor，leanback 端不变。
- **US-32** 作为新装用户，我从没打开过订阅，杀掉进程重开、导出备份、恢复备份之后，`subscription` 与 `subscription_seeded` 这些键依然不存在（入口隐藏期间不写入）。
- **US-33** 作为旁观者，我无法从 app 外部直接拉起 `SubscriptionActivity`：两个订阅 Activity 在 manifest 里没有 intent-filter 也未声明 `exported="true"`，保持现状即可。

### 3.2 连击与弹窗

- **US-34** 作为知道手势的用户，我在设置页标题栏上 2 秒内连续点第 5 下，立即弹出解锁对话框。
- **US-35** 作为用户，点击区域是整个标题栏（含标题文字与空白处），不需要精确点到某个字。
- **US-36** 作为用户，我点 4 下、停顿 3 秒、再点 4 下，什么都不会弹——窗口内无新点击计数归零。
- **US-37** 作为用户，点别的地方（列表行、返回键、其他控件）不计入连击；离开设置页再回来，计数从零开始。
- **US-38** 作为已解锁的用户，我再怎么连击标题栏都不弹窗、不重复验证。
- **US-39** 作为用户，对话框里有一句说明（这是开启订阅功能的入口 + 需要授权码）和一个 4 位数字输入框（输入内容不明文显示，键盘为数字键盘，长度锁 4），按钮是「开启」和「取消」。
- **US-40** 作为用户，我点「取消」或直接关掉对话框，解锁状态不变（仍隐藏），计数归零。
- **US-41** 作为用户，我输入不足 4 位就点「开启」，app 不发请求，只提示需要 4 位。

### 3.3 验证

- **US-42** 作为用户，app 请求 `https://raw.githubusercontent.com/Bobjoy/webhtv-sub/main/codes.txt`，命中其中任意一条即通过。
- **US-43** 作为分发者，我在 `codes.txt` 里一行写一个码，`#` 开头的行当注释，空行跳过，比对前去掉首尾空白——我的码是 ` 0473 ` 这样带空格的写法也能命中。
- **US-44** 作为分发者，`codes.txt` 里出现重复码不会造成任何异常，只要有一条命中就通过。
- **US-45** 作为用户，码取回来了但都不匹配 → 提示「授权码错误」，对话框保持打开，我改一个数字再试即可（重新发一次请求）。
- **US-46** 作为用户，`codes.txt` 拉不回来（断网 / 被墙 / 404 / 返回 HTML 页面）→ 提示「无法连接验证服务，请检查网络或 GitHub 代理设置」，对话框保持打开，绝不显示成"授权码错误"。
- **US-47** 作为墙内用户，我在设置页的 GitHub 代理里选好预设之后，取码走该代理（与订阅拉取同一套改写规则）；代理设置为空时原样直连。
- **US-48** 作为用户，app 不缓存码表：上一次网络失败的记录不会让我这次免密通过，也不会拿旧码表比对。

### 3.4 生效与重启

- **US-49** 作为验证通过的用户，app 先提示「已开启，重启生效」，约 1.5 秒后进程自动重启并回到 app 首页。
- **US-50** 作为用户，重启后设置页出现「订阅」行，点进去是订阅管理页——一期所有订阅能力（001–007）从这里开始完全照旧工作，包括进入页面时种入两条内置订阅。
- **US-51** 作为用户，解锁标记必须已经落盘：重启走的是 `killProcess`，所以写入必须同步提交，不允许出现"重启后仍是隐藏"。
- **US-52** 作为用户，我在另一台机器上导入这台机器的备份，「订阅」行仍然不显示，需要重新连击输码（解锁状态是设备本地状态，不进备份）。
- **US-53** 作为用户，我不做任何反向关闭操作：解锁后入口常驻可见。只有清除应用数据或卸载重装才回到隐藏。

## 4. 已定决策

| # | 决策 | 出处 |
| --- | --- | --- |
| 1 | 明文 `codes.txt` + 客户端比对；服务端校验方案否决 | 拷问第 1 问、ADR-0007 |
| 2 | 取不到码只提示失败，必须联网；不内置兜底码、不缓存码表 | 拷问第 2 问 |
| 3 | 连击判定 = 2 秒滑动窗口内第 5 次点击，整个标题栏区域 | 拷问第 3 问 |
| 4 | 解锁标记独立 pref 键 `subscription_gate`，在 `Backup.include()` 显式排除，不进备份 | 拷问第 4 问 |
| 5 | 重启走 `CustomActivityOnCrash.restartApplication(activity, CustomActivityOnCrash.getConfig())`；写入用 `commit()` | 拷问第 5 问 |
| 6 | 码为 4 位纯数字，输入框 `numberPassword` + 长度 4；`codes.txt` 一行一码、`#` 注释、比对前 trim | 拷问第 6 问 |
| 7 | 不提供反向关闭入口 | 拷问第 7 问 |
| 8 | 单个对话框：说明 + 输入框 + 开启/取消；不做两步确认 | 拷问第 8 问 |
| 9 | 失败提示区分「网络/取不到码」与「授权码错误」两类 | 拷问第 9 问 |
| 10 | 本次一并生成随机码表（`codes.txt`，8 条 + 注释头） | 拷问第 10 问 |

## 5. 测试决策

**主接缝（新建的唯一纯函数接缝）**：`SubscriptionCodes.matches(String content, String input) → boolean`，放在 `app/src/main/java/com/fongmi/android/tv/api/subscription/`，与 `SubscriptionParser` 同族、同样零 Android 依赖。US-42 至 US-48 全部在这一层用纯 JUnit 测：多行命中、`#` 注释行、空行、首尾空白、重复码、输入不足位数、内容为空、内容是 HTML、码存在于文件中但输入大小写/前导零不同。**不测网络**——取原文只在 `SubscriptionLoader` 上加一个 3 行的 `text(url)`（`OkHttp.string(githubProxied(url, githubProxy()))`，空响应抛异常以区分"取不到"与"码不对"），代理改写本身复用已有的包内函数 `githubProxied()`，只断言它对 `raw.githubusercontent.com` 返回改写地址、对非 GitHub 域名原样返回。

**次接缝（复用既有，不新建）**：`Backup.include(key, options)`。在 `app/src/test/.../bean/BackupPreferenceFilterTest.java` 里加一条断言：`subscription_gate` 对**所有**同步选项都返回 false，而 `subscription`（订阅列表本体）的行为保持不变。`SubscriptionStoreTest` 现有用例不动。

**不建测试接缝的部分**：连击计数（Fragment 字段 + 时间戳比较）、`EditText` 的 inputType、Toast、以及"点开启→后台线程→写标记→延迟重启"这条编排。它们是 Android 交互与一次性的胶水，用 Robolectric 测只能测到自己 mock 自己。验收方式是 USB 连接的真机 + uiautomator dump 断言节点存在性：
- 装机后 dump 设置页 → 无 `subscription` 行节点；
- 连击 5 下（坐标取标题栏中点，间隔 <400ms）→ dump 出现对话框节点且含输入框；
- 连击 4 下后等 3 秒再连击 4 下 → 无对话框；
- 输错码 → dump 对话框仍在、Toast 文案是「授权码错误」；把设备断网后输任意码 → Toast 文案是「无法连接验证服务…」；
- 输对码（临时把 `CODES_URL` 指向本机 `:8899` fixture 以绕过墙内不可达，验完改回）→ 约 1.5 秒后进程 pid 变化、`shared_prefs` 里 `subscription_gate` 已落盘、重新进设置页「订阅」行出现；
- 备份导出 JSON 里 grep 不到 `subscription_gate`，恢复后入口仍隐藏。

**"完成"的判定**：`SubscriptionCodesTest` 全绿 + `BackupPreferenceFilterTest` 全绿 + 上面真机 6 条走通 + `bash ./gradlew --console=plain :app:assembleMobileArm64_v8aDebug :app:compileLeanbackArm64_v8aDebugJavaWithJavac :app:testMobileArm64_v8aDebugUnitTest` 一次通过。

## 6. 明确不做

- 任何形式的真实访问控制：码加密存储、服务端校验、设备绑定、有效期、分发计数、撤回能力（ADR-0007 明写这不是安全边界）。
- 反向关闭入口、"重新锁定"、锁定倒计时、每次启动重新验证。
- 兜底码、离线校验、码表缓存、失败重试、断网时放行。
- leanback / TV 端任何改动（订阅 UI 全在 mobile flavor）。
- 新设置项、新网络层、新代理键、新备份分区；`Prefers` 键改名；`update_github_proxy*` 键名变更。
- 一期订阅功能（001–007）的任何行为改动——门禁只决定入口是否可见，不碰订阅本身的增删改查、拉取、解析、生效通路。
- 顺手修 `subscription_seeded` 与 `subscription_<type>_<url>` 缓存键目前落到 `Backup.include()` 末尾 `isSpider()` 分支这个一期遗留归属问题（记录但不改）。
- 历史装机迁移：一期从未发布，不存在"已解锁/已 seed 的存量装机"需要处理。

## 7. 补充说明

- **`fragment_setting.xml` 的 `MaterialToolbar` 目前没有 `android:id`**，加连击监听前必须补 `@+id/toolbar`；这是本期唯一动到一期布局的地方。
- **`Prefers.put()` 用的是 `apply()`**（`catvod/.../Prefers.java:75`），而 `restartApplication` 最终 `killProcess`。所以写标记必须直接 `Prefers.getPrefers().edit().putString("subscription_gate", "1").commit()`，不要图顺手用 `Prefers.put`。
- **Toast 必须早于重启可见**：`App.postDelayed` 约 1.5 秒是提示可见性与"用户来不及反应"之间的折中，真机验一次即可，不必做成可配置。
- **`Startup:31` 设了 `BACKGROUND_MODE_SILENT` 且未关 `killProcess`**，`CustomActivityOnCrash.getConfig()` 拿到的就是这个配置，重启语义成立；如果日后有人改这份 CaocConfig，需要重新验证重启是否真的杀进程。
- **码与订阅共用同一个仓库，但用途不同**：`vod.json`/`live.json` 是清单数据，`codes.txt` 是 app 内部门禁数据。两者都放 `webhtv-sub` 仓库根目录同一层，靠文件名区分，不需要子目录。
- **`codes.txt` 是公开可读的**，任何拿到 URL 的人都能读到全部码。这是 ADR-0007 的既定后果，不是缺陷；不要把真实个人信息或别的凭据放进那个仓库。
- **本期交付跨两个 git 仓库**：app 侧改动在本仓库（默认不提交），`codes.txt` 在 `~/study/github/webhtv-sub`（由用户自行推送）。
