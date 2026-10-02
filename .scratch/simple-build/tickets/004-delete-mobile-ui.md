# 004 删 mobile UI 面：去掉底部导航、删直播/壁纸页面、首页右上角下拉菜单收敛为五项

**Blocked by**: 003

> 2026-10-02 追加需求（用户）：① **底部导航栏去掉，删除缓存放到首页右上角**；② **下拉菜单只保留 刷新 / 清缓存 / GitHub 加速 / 检查更新**；③ **新增「更新资源」项，手动拉 `sub.txt` 并自动生效；菜单 label 前面要带 icon**。原「设置页只留一行」的口径作废——底部导航一去掉，设置/播放器/增强/弹幕四个 tab 页整体消失，不再存在任何设置页。

## 目标
首页只有点播一个页面，顶栏右上角的下拉菜单恰好五项且每项都有真实反应：刷新、更新资源（手动拉清单并生效）、清除缓存、GitHub 加速、检查更新；直播、壁纸在 UI 面上完全不可达。

## 涉及层
- [x] UI 层（删）：`activity_home.xml` 的 `BottomNavigationView`；`HomeActivity` 的 `FragmentStateManager` 多 tab 切换、`setNavigation`、`onNavigationItemSelected`、live 入口与 `openLive`/`loadLive`
- [x] UI 层（删）：`SettingFragment`、`SettingPlayerFragment`、`SettingEnhanceFragment`、`SettingDanmakuFragment` 及 `fragment_setting*.xml`（含 `menu_setting_enhance.xml`）
- [x] UI 层（删）：`LiveActivity`、`LiveAdapter`、`LiveDialog`、`LiveControlDialog` 及对应 layout/res；`ConfigDialog`/`HistoryDialog` 的直播/壁纸分支
- [x] UI 层（改）：`menu_vod.xml` 顶栏只留 搜索/最近观看/⋮；⋮ 子菜单五项带 icon —— `refresh`(ic_popup_refresh)、`update_source`(ic_popup_sync)、`clear_cache`(ic_popup_delete)、`github_proxy`(ic_git_cloud_download)、`check_update`(ic_popup_apk)；收藏/一键同步/推送apk/推送播放/增强功能入口随设置页删除
- [x] UI 层（接回）：`GithubProxyDialog`、`Updater.force()` 从设置页迁到首页菜单；`update_source` 走 `BuiltinSource.refresh(Consumer<Boolean>)`，成功/失败各一条 toast（`source_updating`/`source_updated`/`source_update_failed`）
- [x] 保留：`src/main` 的 `LiveConfig`/`WallConfig`（本票只断开 mobile 引用，数据层留给 007）
- [x] 测试：`app/src/test` 无新增失败；`compileMobileArm64_v8aReleaseJavaWithJavac` 收敛；UI 只能真机手测（leanback 已删，无 Robolectric 通道）

## 验收标准
- 首页无底部导航栏，`activity_home.xml` 里 `navigation` 节点不存在；容器铺满
- 首页没有直播、壁纸入口；返回键/滑动不会进入这两个页面
- ⋮ 下拉恰好五项、每项 label 前有 icon；点每项都有可见反馈（已真机验证：GitHub 加速弹窗、检查更新「已是最新版本」、清除缓存「缓存已清除」）
- 「更新资源」点击 → 后台拉 `sub.txt` → 取第一条走 `VodConfig.load` 生效 → `ConfigEvent.VOD` → `RefreshEvent.home()` 出内容；拉不到给「资源更新失败」
- `grep -rn "LiveActivity\|SettingFragment" app/src/mobile` 无结果；没有点了没反应的死控件
- 003 的自动生效路径在本票改动后仍正常（真机重测一次首启出内容）

## 约束
- 只删 UI 与入口，`src/main` 的类留到 007
- **执行顺序已调整为 003 → 005 → 004 → 006**：`src/main` 的 `HomeWebBridge` 同时引用两个 flavor 的 `LiveActivity`，先删 leanback 可省一轮跨 flavor 修复
- 默认不提交
