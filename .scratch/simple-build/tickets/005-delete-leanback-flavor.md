# 005 删 leanback flavor：mode 维度只留 mobile

**Blocked by**: 004

## 目标
简易版分支只出手机版，`assembleLeanback*` 系列任务不复存在，mobile 两个 ABI 出包正常。

## 涉及层
- [x] 构建层：`app/build.gradle` 的 `mode` flavor 移除 `leanback`；`leanbackImplementation`/`testLeanbackImplementation`（含 `app/build.gradle:205` 的 Robolectric）等依赖声明同步移除
- [x] 源集（删）：`app/src/leanback/**` 整目录（含上一期移植的订阅/门禁/加速 UI）
- [x] 清理：CI/脚本/文档里对 leanback 任务名的引用（本票**不改** `android-release.yml`、`apk-build.yml` 的触发与签名逻辑，只处理会直接报错的任务名）
- [x] 验证：`./gradlew tasks --all` 里无 leanback 变体；两个 mobile release 任务出包

## 验收标准
- `ls app/src` 只剩 `main` 与 `mobile`（外加 `test` 等）
- `assembleMobileArm64_v8aRelease` 与 `assembleMobileArmeabi_v7aRelease` 均成功，产出 2 个 APK
- 全仓 `grep -rn "leanback" app/build.gradle` 无残留
- `app/src/test` 全量绿（`app/src/testLeanback*` 若存在则一并移除，且不影响共享测试）
- 明确记录：UI 层自动化测试通道在本票之后消失（Robolectric 原只挂 leanback），后续验证一律走真机

## 约束
- 不动 `fongmi-sync`/`main` 的 leanback 能力，只在本分支删
- 默认不提交
