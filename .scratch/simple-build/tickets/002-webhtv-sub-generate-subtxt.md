# 002 webhtv-sub：日巡检顺带生成 sub.txt

**Blocked by**: 无（改的是另一个仓库 `webhtv-sub`，可与 001 并行）

## 目标
`https://raw.githubusercontent.com/Bobjoy/webhtv-sub/main/sub.txt` 上存在一份非空、一行一条点播资源地址的清单，且每天由巡检自动刷新，人工只维护 `vod.json`。

## 涉及层
- [x] 脚本层：`scripts/check-resources.mjs` 在现有探测逻辑之后，把当天实测可用的点播条目按 vod.json 原顺序写成 `sub.txt`。**行格式必须是 `<url> #<名称>`（URL、一个空格、井号加名称）**——消费端 `SubscriptionParser.lines()` 按第一个空白切分 URL 与标签，写成 `url#名称`（无空格）会把名称粘进 URL。名称含空格时换成 `-` 保持单 token；名称为空则只写 URL。只处理点播（vod.json），不要把直播写进去
- [x] CI 层：`.github/workflows/check-resources.yml` 的 `git add` 列表补上 `sub.txt`
- [x] 文档层：`webhtv-sub/README.md` 增加 `sub.txt` 说明——行序即优先级、面向简易版 app、不得写入敏感信息
- [x] 验证：本地 `node scripts/check-resources.mjs` 跑通并产出 `sub.txt`

## 验收标准
- 本地运行脚本后 `sub.txt` 存在，每行以 `http(s)://` 开头，无空行、无 HTML 片段
- `sub.txt` 的行数 = `vod.json` 中当天状态为可用的条目数（失效条目已被移入 `removed.json`）
- 把 `sub.txt` 内容喂给 `SubscriptionParser.parse`（app 侧既有 JVM 测试的 fixture 形态）能解析出同样数量的条目
- 文件内不含授权码、token、内网 IP、绝对路径等敏感信息（公开仓库口径）
- 推上去后一次 `curl` 能拿到非空正文

## 约束
- 不改 `vod.json`/`live.json`/`codes.txt` 的格式与语义
- 默认不提交，除非明确要求
