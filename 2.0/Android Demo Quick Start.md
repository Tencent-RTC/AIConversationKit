本文主要介绍如何快速运行腾讯云 TRTC AIConversation Demo。
<table>
<tr>
<td rowspan="2" colspan="1" ><img src="https://write-document-release-1258344699.cos.ap-guangzhou.tencentcos.cn/100027984178/838b4029e97d11f1b89c525400ecee81.jpeg" alt="" /></td>

<td rowspan="2" colspan="1" ><img src="https://write-document-release-1258344699.cos.ap-guangzhou.tencentcos.cn/100027984178/838c8b61e97d11f19b16525400370dda.jpeg" alt="" /></td>

<td rowspan="2" colspan="1" ><img src="https://write-document-release-1258344699.cos.ap-guangzhou.tencentcos.cn/100027984178/838a2fa5e97d11f18e8352540073fd3b.jpeg" alt="" /></td>

<td rowspan="2" colspan="1" ><img src="https://write-document-release-1258344699.cos.ap-guangzhou.tencentcos.cn/100027984178/838aea73e97d11f1837a52540008be5a.jpeg" alt="" /></td>
</tr>

<tr></tr>
</table>

## 环境准备

|项目|要求|说明|
|---------|---------|---------|
|操作系统|Windows / macOS / Linux|本文档以 Windows 为例|
|JDK|**17**（必须）|AGP 8.5.2 要求 Java 17，用 Java 11 会报版本错误|
|Android Studio|建议 2024.1 或更高|用于打开工程、安装 SDK、运行到真机|
|Android SDK|Platform 34、Build Tools|Gradle 会自动下载缺失组件|
|Gradle|8.9（工程自带 wrapper）|无需单独安装，用 `gradlew` 即可|
|测试设备|**Android 6.0 / API 24+ 真机**|模拟器无法真实验证麦克风与摄像头|

## 下载 Demo

【ZIP 下载】
1. 下载 Demo 的 [ZIP 包](https://codeload.github.com/Tencent-RTC/AIConversationKit/zip/refs/heads/main)。

2. 解压到一个**不含中文和空格**的路径，例如：

``` bash
D:\workspace\AIConversationKit-2.0-Android
```
3. 确认解压后的目录结构中有 `gradlew.bat`、`settings.gradle.kts`、`app/`、`aiconversation/`、`libs/`。

> **说明：**
> 
> 路径含中文或空格时，Gradle 和 NDK 工具链可能出问题。
> 

【Github 下载】

**克隆仓库**：
``` bash
git clone https://github.com/Tencent-RTC/AIConversationKit.git
cd AIConversationKit/2.0/Android
```

**或直接下载 ZIP**：
1. 打开 <https://github.com/Tencent-RTC/AIConversationKit>

2. 点击绿色按钮 `Code` → `Download ZIP`

3. 解压后进入 `AIConversationKit-main/2.0/Android/`

## 配置 Demo
1. 获取 TRTC 应用信息，请根据 [开通服务](https://cloud.tencent.com/document/product/647/140388) 创建应用（如果已有 TRTC 应用，跳过此步骤）。

2. Demo 的账号信息集中在 `AppConfig.java`：

   ``` bash
   app/src/main/java/com/tencent/trtc/aiconversation/app/config/AppConfig.java
   ```

   用文本编辑器打开，填写以下 7 项：

   |字段|获取位置|
   |---------|---------|
   |`SDK_APP_ID`|[TRTC 控制台](https://console.cloud.tencent.com/trtc/app) → 应用管理，创建应用后得到|
   |`SDK_APP_KEY`|同一页面的"密钥"字段，用于本地计算 UserSig|
   |`CLOUD_SECRET_ID`|[腾讯云 CAM 控制台](https://console.cloud.tencent.com/cam/capi) → API 密钥|
   |`CLOUD_SECRET_KEY`|同上|
   |`LLM_API_URL`|LLM 服务商提供的 chat/completions 完整地址|
   |`LLM_API_KEY`|LLM 服务商的 API Key|
   |`LLM_MODEL`|实际使用的模型名称|

   其余字段（`CLOUD_REGION`、`LLM_TYPE`、场景提示词、TTS 音色等）保持默认即可。

   > **警告：**
   > 
   > **安全提醒**：`SDK_APP_KEY` 和 `CLOUD_SECRET_KEY` 写在客户端会被打进 APK，容易被反编译获取。仓库里这些字段是**空的**，仅本地调试时填写。正式环境必须把 UserSig 计算和云 API 调用放到业务服务端，参考 [UserSig 生成与校验](https://cloud.tencent.com/document/product/647/17275)。
   > 

## 编译与运行
1. 打开 Android Studio，选择 `File > Open`，选中 `2.0/Android` 目录，等待 Gradle Sync 完成。

2. 手机开启"开发者选项"和"USB 调试"，用数据线连接电脑，在顶部设备栏选中该设备。

3. 点击工具栏的 `Run`（或按 `Shift + F10`），Android Studio 会自动编译并安装到手机。

## 常见问题
1. **Gradle Sync 失败，提示 JDK 版本不对**

   确认 `JAVA_HOME` 指向 JDK 17，或在 Android Studio 的 `Project Structure > SDK Location > JDK` 中指定。

2. **找不到 TRTC SDK**

   确认 `libs/LiteAVSDK_Professional_13.6.0.20757.aar` 存在，且 `settings.gradle.kts` 中的 `flatDir { dirs(File(settingsDir, "libs").absolutePath) }` 未被改动。

3. **能进房但 AI 不说话**

   打开 logcat，过滤 `AIConversationViewModel` 和 `AIConversationRequest`：

- 看到"AI 对话任务创建失败"：检查 `AppConfig.java` 里的云 API 密钥和 LLM 配置是否填对。

- 看到"进房失败"：检查 `SDK_APP_ID` 和 `SDK_APP_KEY`，以及设备网络。

4. **字幕不显示**

   字幕依赖 AI 对话任务创建成功后下发的订阅指令。确认 logcat 中有"AI 对话任务创建成功"，再检查页面上的"字"按钮是否处于开启状态。

5. **房间号冲突**

   Demo 用 `Build.DEVICE` 拼接房间号和用户 ID。多台同型号设备同时跑可能进同一房间，此时修改 `AppConfig.java` 中的 `DEFAULT_ROOM_ID` / `DEFAULT_USER_ID` 即可。

