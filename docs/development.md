# 开发说明

项目使用 Java 和原生 Android App Widgets，最低 API 26，编译与目标 API 35。构建需要 JDK 17，仓库包含 Gradle Wrapper。

本地开发：`./gradlew testDebugUnitTest lintDebug assembleDebug`。调试包用于开发，不能用于向正式版用户推送更新。

## 发布签名

CI 从仓库 Actions Secrets 读取 `SIGNING_KEYSTORE_BASE64` 和 `SIGNING_STORE_PASSWORD`，恢复固定发布密钥，别名为 `daily-wisdom`。密钥仅写入运行器临时目录，任务结束时删除，不进入源码、构建附件或缓存。缺少密钥时构建失败，不回退到临时签名。

公开证书指纹记录在 `signing-certificate-sha256.txt`。每次发布都会验证 APK 签名匹配此指纹。请保留发布密钥，不要重新生成或随意替换，否则已安装用户无法覆盖升级。

本地正式构建设置 `SIGNING_KEYSTORE_PATH`、`SIGNING_STORE_PASSWORD`、`SIGNING_KEY_PASSWORD`、`SIGNING_KEY_ALIAS` 后运行 `./gradlew assembleRelease`。

## 发布流程

递增 app/build.gradle 中的默认版本号与版本名称，提交 main，再推送与版本名称一致的 v 开头标签。不要移动已发布标签。

CI 检查数据、运行单元测试和 Lint，构建正式 APK；随后在 Android 15 模拟器运行功能测试，并分别构建低版本包、安装低版本包、用发布 APK 覆盖升级。所有检查通过后，自动创建 Release，附 APK、校验文件和 JSON 格式资料。

`-PappVersionCode=1` 仅用于构造升级测试的低版本包。对外分发的 APK 始终使用默认版本号。

内置示例来自用户提供的《宝贵的人生建议》，凯文·凯利著、刘波译，保留 PDF 页码，用于私有个人示例。
