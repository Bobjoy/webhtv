# 010 codes.txt 码表与 README 同步（跨仓库）

**Blocked by**: 无（可与 008 并行；008 的真机金路先用本机 `:8899` fixture 打通，本任务完成后换成真实 URL 复验一次）

## 目标
`Bobjoy/webhtv-sub` 仓库根目录出现一份可分发的 `codes.txt`（一行一个 4 位随机码），app 端连击解锁能拿到真实码表；README 里说清"订阅入口默认隐藏 + 授权码从这个文件分发"。

## 涉及层
- [x] 数据层：`~/study/github/webhtv-sub/codes.txt` —— 首行 `# webhtv 订阅功能授权码，一行一个，4 位数字；# 开头为注释。`，随后 8 条互不重复的 4 位码，用 `openssl rand -hex 4 | tr -d 'abcdef'` 不稳（可能不足 4 位），改用 `od -An -N4 -tu4 /dev/urandom` 或 `awk` 生成 `0000`–`9999` 范围内的随机数并补前导零到 4 位；结尾留一个空行。
- [x] 文档层：`webhtv-sub/README.md` 加一节「授权码（codes.txt）」：说明它是 app 内部门禁数据、明文公开、不构成访问控制（引 ADR-0007 的口径），以及 app 侧「设置 → 连击标题栏 5 次 → 输入 4 位码」的开启方式；同时把现有「在 app 的『设置 → 订阅』里新增订阅」这句话改成"先在设置页连击标题栏开启订阅功能，再进入订阅页"。
- [x] 测试：无自动化测试（纯静态文件）。验证方式是 `gh api repos/Bobjoy/webhtv-sub/contents/codes.txt` 或本地 `git` 查看内容，并确认 8 条码彼此不重复、都是 4 位数字。
- [x] 交付边界：**只在本地写文件，不 push**。推送由用户自己执行（该仓库是独立 git 仓库，本任务不越权发布）。

## 验收标准
- `codes.txt` 里恰好 8 条码，每行一个，全部匹配 `^[0-9]{4}$`，无重复；`#` 注释行不会被 app 当成码（008 的 `SubscriptionCodesTest` 已覆盖该解析规则）。
- 用户 push 之后，`https://raw.githubusercontent.com/Bobjoy/webhtv-sub/main/codes.txt` 可取到该内容（本机直连 `raw.githubusercontent.com` 不通，走 `gh api` 校验仓库侧内容即可，并注明这一步）。
- README 不再让人以为订阅入口默认可见；README 与 `codes.txt` 里都不出现真实 IP、绝对路径、其他凭据。
- 008 的金路把 `CODES_URL` 改回真实地址后仍能解锁（用 `codes.txt` 里某条码 + 配好的 GitHub 代理实测一次）。

## 验收记录（2026-10-01）

- `~/study/github/webhtv-sub/codes.txt`：恰好 8 行匹配 `^[0-9]{4}$`，`sort -u` 仍 8 条（无重复），另有首行注释与结尾空行；文件与 README 均无真实路径/IP。
- README：订阅地址一节已改为「先连击标题栏开启订阅功能，再进入订阅页」，新增「授权码（codes.txt）」一节写明格式、实时拉取、不构成访问控制。
- 未做：不 push（交付边界）；因此「push 后 raw 地址可取到」和「真实 URL 端到端解锁」两步都留给用户 push 之后。
