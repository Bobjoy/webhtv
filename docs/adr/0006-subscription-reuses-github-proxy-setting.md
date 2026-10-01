# 订阅拉取复用现有的 GitHub 代理设置

订阅地址（以及条目里的资源地址不做处理）拉取时，若 host 属于 `raw.githubusercontent.com` / `github.com` / `gist.githubusercontent.com`，走项目已有的 `update_github_proxy` / `update_github_proxy_url` / `update_github_proxy_mode` 三键与 `GithubProxy.rewrite()`；其他域名原样直连。不新增订阅专用的代理设置项。

**理由**：社区收集页里近半订阅地址是 `raw.githubusercontent.com`，国内直连基本失败，而项目已经有一套带 presets（github.chenc.dev / gh.acmsz.top / ghfast.top / gh.monlor.com / 自定义）且已进备份白名单的 GitHub 代理机制。再开一份"订阅专用代理"等于同一件事两个旋钮，设置页、备份过滤、测试矩阵全部翻倍。

**后果**：一项名字带 `update_` 前缀的设置开始影响订阅，界面文案需改为"GitHub 代理（更新与订阅）"以消除歧义；偏好键名本身不改（改名将牵动备份兼容）。如果日后订阅需要与更新不同的镜像，再拆分为独立键，届时要做一次旧值迁移。
