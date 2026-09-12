# 一日一句 · Daily Wisdom

面向现代 Android 设备的离线每日语录小组件。把自己的书摘、笔记、学习卡片或提醒语导入为 JSON，每天在桌面看到一条。

**不绑定手机品牌、型号、书籍或作者。** 内置《宝贵的人生建议》仅作为示例句库；你可以导入和切换自己的内容。

## 功能

- 深色横向桌面卡片：左侧内容、右侧日期，点开查看全文。
- 每天随机一条，同一天保持不变；一轮读完再重复。
- 手动换句、收藏、暂停每日换句。
- 从系统文件选择器导入 JSON，校验并预览后保存。
- 多句库切换，每个句库分别保存收藏、阅读进度和停留状态。
- 同 ID 更新句库时，保留仍存在的内容 ID 对应的收藏。
- 导出当前句库内容，方便复用或作为格式模板。
- 离线运行，无账号、广告、服务器及联网权限。

## 安装与使用

1. 打开本仓库的 [Releases](https://github.com/sinterwong/daily-wisdom-android/releases)，在对应版本的 Assets 中直接下载 `daily-wisdom-v版本号.apk` 安装。开发中的 main 分支仍可从 Actions 下载 APK 压缩包。
2. 打开「一日一句」，点击「放到手机桌面」→「请求添加」，在桌面弹窗确认。如果没有弹窗，点击「从系统小组件添加」查看步骤：长按桌面空白处 → 小组件／窗口小工具 → 一日一句 → 长按拖到桌面。部分桌面需在组件列表进入「全部」或「Android 小部件」。
3. 推荐横向 4×2；长内容可放大到 4×3，或点击「展开全文」。
4. 点击「导入 JSON」选择自己的文件，确认预览；通过「切换句库」选择桌面内容。

[JSON 接口说明](docs/json-format.md) · [可直接导入的示例](examples/my-library.json) · [JSON Schema](docs/library.schema.json)

### 兼容范围

最低 Android 8.0（API 26），需要桌面支持标准 Android App Widgets。当前 CI 在 Android 15（API 35）模拟器验证；不代表已经逐一验证所有品牌、桌面或系统版本。手机、平板均可安装，显示尺寸可调整。

系统省电、强制停止或后台限制可能延迟每日更新；打开应用会检查日期并刷新。必要时可在设备设置中允许应用后台运行，具体入口由系统决定。不保证零点准时换句。

### 数据和当前构建类型

句库、收藏及进度保存在本机，卸载会删除这些数据。导出 JSON 只包含句库内容，不包含收藏及阅读进度。

当前 CI 生成**试用调试包**，使用运行器临时生成的调试签名。不同构建的签名可能不同，不能保证覆盖安装；如提示签名冲突，需要卸载旧版，收藏与进度会被清空。固定发布签名尚未配置。

## 开发与自动构建

- Java、原生 Android RemoteViews；编译及目标 API 35。
- JDK 17、Gradle Wrapper 8.13、Android Gradle Plugin 8.11.1。
- 用 Android Studio 打开项目，或运行 `./gradlew testDebugUnitTest lintDebug assembleDebug`。
- 推送 `main` 或手动运行 Actions：校验内置数据、单元测试、Lint、编译 APK、验证签名并上传构建产物。
- 另在 Android 15 模拟器检查启动、真实存储、句库导入及切换、小组件布局，并生成长短内容截图。
- 推送 `v*` tag 后，构建与模拟器测试全部通过才自动创建 GitHub Release，附加版本化 APK、校验文件、JSON 示例、Schema 和格式说明。
- 私人签名密钥和访问令牌不进入源码，也未配置到 CI。

未来如需固定发布签名，在明确授权后配置 `SIGNING_KEYSTORE_BASE64`、`SIGNING_STORE_PASSWORD`、`SIGNING_KEY_ALIAS`、`SIGNING_KEY_PASSWORD`。本地 release 构建可用 `SIGNING_KEYSTORE_PATH` 和对应密码变量指定证书；未配置时 release 为未签名包。

## 内置示例内容

内置示例的 500 条原句来自用户提供的《宝贵的人生建议》PDF，凯文·凯利著、刘波译。PDF 页码随条目保留。这部分内容用于私有个人示例，不代表应用只能使用该书。

当前版本不提供云同步、收藏备份或应用商店发布。

## 发布一个版本

1. 修改 `app/build.gradle` 中的 `versionName` 和递增的 `versionCode`，提交到 main。
2. 创建与版本号对应的 tag 并推送，例如：

```bash
git tag -a v0.3.0 -m "Release v0.3.0"
git push origin v0.3.0
```

3. 等待该 tag 的 Actions 成功，Release 与 APK 附件将自动出现。tag 必须等于 `v` 加 `versionName`，否则 CI 拒绝发布。

发布任务使用 GitHub 自动提供的 `GITHUB_TOKEN`，不需要保存个人 token。附件先上传到草稿 Release，上传成功后才公布。失败可重跑；已经公布的同版本附件不会被自动替换。修复已发布版本时请递增版本号并推送新 tag。
