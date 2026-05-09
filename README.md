# BiliCut

一个轻量级的 Android 工具，用于将 **B站（Bilibili）手机版截图** 中的视频块分割出来，并按自定义顺序重新拼接。

## 这是什么

B站手机版首页或频道页会以网格形式展示多个视频封面。当你想把几个视频封面拼在一起分享时，传统的截图裁剪工具很难精确分割。**BiliCut** 就是为了解决这个问题而生——你只需要上传一张截图，手动画几条竖线/横线标记分割位置，就能自动按网格裁剪，再按你想要的顺序拼接成一张新图。

## 功能

- **上传截图** - 从相册选择 B站手机版截图
- **手动画线** - 在图片上添加竖线和横线，标记视频块的分割位置
- **撤销线条** - 支持撤销操作，灵活调整分割线
- **网格裁剪** - 根据线条自动将图片裁剪为多个视频块
- **按序选择** - 点击图片决定拼接顺序
- **无边框拼接** - 将选中的视频块无缝拼合成一张新图
- **预览保存** - 预览拼接结果，满意后再保存到相册

## 使用流程

1. **首页** → 点击"选择截图"按钮，从相册选取 B站截图
2. **标记裁剪线** → 在图片上点击添加竖线和横线，分割出每个视频块
3. **选择视频块** → 按想要的拼接顺序依次点击各个视频块
4. **拼接结果** → 预览拼接后的图片，点击保存

## 截图

> **首页**

**image**

> **标记裁剪线**

**image**

> **选择视频块**

**image**

> **拼接结果**

**image**

> **关于我**

**image**

## 技术栈

- **语言**: Kotlin
- **UI 框架**: Jetpack Compose + Material3
- **架构**: MVVM (ViewModel + State)
- **最低 SDK**: Android 9 (API 28)
- **目标 SDK**: Android 16 (API 36)

## 构建

```bash
# Clone 项目
git clone https://github.com/WalkerBaoZhi/BiliCut.git

# 使用 Gradle 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK（需配置签名）
./gradlew assembleRelease
```

构建产物位于 `app/build/outputs/apk/` 目录。

## 致谢

- 感谢 B站 提供的优秀内容平台（本工具仅用于截图后处理，与 B站官方无关）

## 关于作者

- **WalkerBaoZhi** - 喜欢开发小应用的大学生
- GitHub: [https://github.com/WalkerBaoZhi/](https://github.com/WalkerBaoZhi/)
