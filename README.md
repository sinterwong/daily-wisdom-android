# 一日一句 · Daily Wisdom

一个自用的离线安卓桌面小组件。每天从《宝贵的人生建议》的 500 条书摘里随机显示一条。

## 使用

1. 从 GitHub Actions 最新成功构建的 **daily-wisdom-apk** 下载压缩包，解压并安装 `daily-wisdom.apk`。
2. 打开「一日一句」，点击「放到手机桌面」；或者长按桌面空白处 → 小组件 → 一日一句。
3. 推荐横向 4×2；长句可放大到 4×3，或点「展开全文」。
4. 每天换一句，随机读完 500 条再进入下一轮；支持手动换句、收藏、让当前句子停留。

无需账号、联网或广告。收藏及阅读进度保存在本机；卸载会删除这些数据。当前 CI 为试用调试包，使用运行器临时生成的调试签名。不同构建的签名可能不同，不能保证覆盖安装；如提示签名冲突，需要卸载旧版，收藏与进度将被清空。固定发布签名尚未配置。

### 小米手机

首次安装后先打开应用，再添加小组件。如果跨天后仍显示旧内容，打开应用即可刷新。必要时在系统应用设置中允许后台自启动，并将省电限制调为「无限制」（菜单名称依系统版本而异）。

更新采用系统小组件周期回调及非精确日期闹钟，允许省电策略造成延迟，不保证零点准时换句。多个卡片共享当天句子和收藏状态。

## 开发与 CI

- Java、Android 原生 RemoteViews；最低 Android 8.0，编译及目标 API 35。
- JDK 17、Gradle Wrapper 8.13、Android Gradle Plugin 8.11.1。
- 本地开发：用 Android Studio 打开项目，或运行 `./gradlew testDebugUnitTest lintDebug assembleDebug`。
- 推送 `main` 或手动运行 Actions：校验书摘、单元测试、Lint、编译调试签名 APK；另在 Android 15 模拟器执行安装、页面启动、真实存储与小组件布局测试。
- CI 使用 Android 工具自动生成的临时调试签名。现有私人签名密钥未配置到 CI。个人访问令牌不进入源码。

未来如需固定发布签名，可在用户明确授权后配置四个 Actions Secrets：`SIGNING_KEYSTORE_BASE64`、`SIGNING_STORE_PASSWORD`、`SIGNING_KEY_ALIAS`、`SIGNING_KEY_PASSWORD`。

本地签名 release 时配置对应密码变量及 `SIGNING_KEYSTORE_PATH`。未配置时本地 release 为未签名包；个人调试直接使用 debug 包。

## 内容与范围

原句来自用户提供的 PDF，凯文·凯利著、刘波译。`quotes.json` 按文件顺序编号，`page` 与 `endPage` 为 PDF 页序，不是纸书页码。已去除电子书杂字并修复第 74 页缺失分隔符，保留 40 条中文版独家建议及 460 条正文建议。

本仓库为私有个人用途。当前版本不包含自定义句库导入、云同步、备份恢复或应用商店发布。
