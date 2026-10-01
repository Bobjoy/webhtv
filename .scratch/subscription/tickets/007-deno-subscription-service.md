# 007 订阅清单服务（Deno）

**Blocked by**: 无（可与 001 并行；只依赖 ADR-0005 的字段契约）

## 目标
部署者改一个常量文件、跑一条命令，就能对外提供一个固定路径的标准格式订阅清单。

## 涉及层
- [ ] 服务端：`serverless/webhtv-sub-deno/` — `sources.js`（部署者写死的 `{name, url, type}` 数组，**不接受请求参数指定目标地址**，ADR-0003）；`main.js` 固定路径 handler；`normalize.js` 拉取来源 → 解析（含第三方配置形态）→ 去重 → 补齐 `logo`/`remark` → 输出 `{"name"?, "list":[...]}`，`Content-Type: application/json`。
- [ ] 逻辑层护栏：仅 http/https；响应大小与超时上限；重定向次数限制；单个来源失败跳过并记日志，其余仍返回；中文域名按 IDN 处理。
- [ ] 文档：`README.md` 写清 `deno run --allow-net` 本地跑、Deno Deploy 部署、改源即重新部署，并明确"app 已能直接解析社区多仓 JSON 与纯文本行清单，本服务只用于合并多源与补 `logo`/`remark`"。沿用 `serverless/webhtv-remote-deno/` 的目录与 README 风格。
- [ ] 测试：`serverless/webhtv-sub-deno/test/` 用 fixture + 注入的假 HTTP 客户端断言：去重（同 `url` 只出一条）、失败来源跳过不影响整体、输出结构符合 ADR-0005 契约、非白名单协议被拒。**不发真实网络请求。**

## 验收标准
- `deno test` 全绿；`deno run` 后 GET 固定路径返回合法标准格式 JSON（US-27、US-30）。
- `sources.js` 里塞一个不可达 URL：服务仍返回其余来源的条目并打印跳过日志（US-28）。
- 两个来源含同一 `url`：输出只有一条（US-29）。
- 用 app 侧（002 的解析器）读这份输出能正常列出条目——把服务 fixture 直接喂给 `SubscriptionParser` 的测试断言一次。
- Cloudflare / Go / Rust / Vercel 形态本期不新增（明确不做）。

## 撤销记录（2026-10-01）

用户判定该服务在当前链路里没有调用点，要求删除。`serverless/webhtv-sub-deno/` 已整目录删除（未提交过，无历史残留）；其中 4 份真实形态 fixture（`standard-list.txt` / `multi-store.txt` / `plain-lines.txt` / `html-page.txt`）搬到 `app/src/test/resources/subscription/`，`SubscriptionParserTest` 改为 classpath 读取，16 条解析断言一条没丢，也不再依赖工作目录或 `Assume` 跳过。ADR-0003 标为已撤销，一期 spec 顶部加了作废说明。
