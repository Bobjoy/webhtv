# 006 GitHub 订阅地址复用现有代理设置

**Blocked by**: 002

## 目标
订阅地址是 GitHub 链接时，走用户已在更新设置里配好的 GitHub 代理，国内能真正拉通这类清单。

## 涉及层
- [x] 逻辑层：域名判定函数（host ∈ `raw.githubusercontent.com` / `github.com` / `gist.githubusercontent.com` → 用 `GithubProxy.resolve(偏好三键).rewrite(url)`；其他域名原样返回），在 `SubscriptionLoader` 请求前调用（ADR-0006）。
- [x] UI 层：`app/src/main/res/values*/strings.xml` 的 `R.string.update_github_proxy` 文案改为「GitHub 代理（更新与订阅）」一类表述，消除 `update_` 前缀带来的歧义；**偏好键名 `update_github_proxy*` 不改**，避免破坏备份兼容。
- [x] 测试：一条域名判定断言（三个 GitHub 域名改写、其他域名原样、`DIRECT`  preset 时原样）；沿用 `app/src/test/java/com/fongmi/android/tv/update/GithubProxyTest.java` 的写法；`BackupPreferenceFilterTest` 里三个键的既有断言不受影响。

## 验收标准
- 配了 `ghfast.top` 后，`https://raw.githubusercontent.com/.../x.json` 的订阅请求实际带上代理前缀并能拉通（US-18）。
- 同一设置下，`http://www.饭太硬.net/tv` 这类非 GitHub 地址请求原样发出、不被改写。
- 代理设置设为「GitHub（直连）」时，GitHub 地址也原样请求。
- 更新设置页文案不再让人误以为该开关只影响检查更新。
