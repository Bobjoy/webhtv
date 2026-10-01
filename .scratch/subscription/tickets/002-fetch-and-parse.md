# 002 点播订阅拉出条目列表

**Blocked by**: 001

## 目标
用户在管理页对一条点播订阅点「拉取」，能看到解析出来的候选资源地址列表（含社区多仓 JSON 和纯文本行清单）。

## 涉及层
- [x] 逻辑层（主接缝）：`app/src/main/java/com/fongmi/android/tv/api/subscription/SubscriptionParser.java` — 纯函数 `parse(String content) → List<Item>`。规则（ADR-0001 / ADR-0005）：首个非空白字符是 `{`/`[` 走 JSON（条目数组键名 `list` 与 `urls` 都认，`setLenient(true)` 吃掉 `//` 注释与 JSON 之前的注释头）；否则按纯文本行解析（首 token 为 `url`，其后 `#`/`//` 之后为 `name`，`name` 缺省取 host，空行/整行注释/非 URL 行跳过）。条目字段 `name`/`url`/`logo`/`remark`，仅前两项必填，`url` 为空的条目丢弃。**不做格式失败后的换路径重试**：JSON 解析失败即报错，不退回按行解析。
- [x] 逻辑层：`SubscriptionLoader` 用既有 `OkHttp.string(url)` 拉取，无缓存、无重试；失败抛出后由 UI 转成提示。中文域名（`tv.菜妮丝.top`）交给 `HttpUrl` 做 IDN 编码。
- [x] UI 层：`SubscriptionItemActivity`（mobile）展示条目列表（名称 + `remark` + 有 `logo` 显示图标）+ 加载态 + 失败 Toast。
- [x] 测试：`SubscriptionParser` 纯 JUnit（无 Android、无网络）覆盖：标准 JSON、带注释的多仓、注释头+脏空白、纯文本行、名称缺省取 host、跳过标题行、`url` 空丢弃、HTML 输入判为失败；断言条目顺序与五个字段。fixture 直接取自社区收集页真实形态。

## 验收标准
- 贴一个社区多仓地址（`{"urls":[{"name","url"}]}`，正文含 `//`）点拉取 → 列出条目（US-12）。
- 贴一行一条的纯文本清单 → 列出条目，`#` 后为名称；无名称时取 host（US-13、US-14）。
- 清单里的「单仓：」这类标题行与空行不出现为条目、不报错（US-15）。
- 填 `https://www.iyouhun.com/tv/dc`（返回 HTML）→ 看到明确失败提示，**不**显示空列表（US-17）。
- 网络失败/超时/非 200 → 一条 Toast，不留任何状态、不禁用订阅（US-19）。
- 再点一次拉取 → 重新发请求，不给缓存结果（US-20）。
- 标准自建清单 `{"name","list":[...]}` 正常解析，`logo`/`remark` 读到位（US-10、US-11）。
