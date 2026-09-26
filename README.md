# DanmakuColorHook

一个 LSPosed 模块骨架：Hook 目标 app 的弹幕颜色赋值逻辑，替换成随机颜色。

## 使用前必须做的事

打开 `app/src/main/java/com/yourname/danmakucolor/MainHook.java`，
把顶部"配置区"的几个占位符改成你反编译目标 app 后确认的真实值：

| 变量 | 含义 | 怎么找 |
|---|---|---|
| `TARGET_PACKAGE` | 目标 app 包名 | `pm list packages` / `pm path 包名` |
| `TARGET_CLASS` | 弹幕颜色所在类的完整路径 | 反编译后 grep `textColor` / `setTextColor` |
| `SETTER_METHOD_NAME` | 如果颜色是通过 setter 方法设置的，填方法名 | 同上，看方法签名 |
| `PARSER_CLASS` / `PARSER_METHOD_NAME` | 如果颜色是构造/解析时直接赋值字段的，填负责创建弹幕对象的类和方法 | 反编译后找 `new BaseDanmaku(...)` 或类似工厂方法 |
| `COLOR_FIELD_NAME` | 字段名（多数情况下就是 `textColor`，不一定要改） | 反编译代码里的字段声明 |

两种方案（setter / 字段赋值）都实现好了，用不到的那个把对应变量留空字符串即可，
代码会自动跳过。

## 用 GitHub Actions 构建（推荐，仓库已配好）

推送到任意分支就会自动触发 `.github/workflows/build.yml`：

1. 装 JDK 17（AGP 8.1 要求）
2. 装 Android SDK
3. 用官方 `gradle/actions/setup-gradle` 直接管理 Gradle 版本并执行构建
   （没有走仓库自带 `gradlew`，因为仓库里没有 `gradle-wrapper.jar` 这个二进制文件，
   这个 action 会自己下载并缓存指定版本的 Gradle 来跑，效果等价）
4. 构建产物 `app-release-unsigned.apk` 会作为 **Artifact** 附加在这次 workflow 运行的页面上，
   直接在 GitHub 仓库的 Actions 标签页里下载

如果想手动触发（不推送代码也能跑一次），在 Actions 页面里用
`workflow_dispatch` 手动运行即可，workflow 里已经加了这个触发方式。

## 本地构建（如果不想依赖 CI）

本地需要自己装好 Gradle（版本建议 8.4+）和 JDK 17，然后：

```bash
gradle assembleRelease
```

产物在 `app/build/outputs/apk/release/app-release-unsigned.apk`。

如果想用 `./gradlew` 而不是本地装的 `gradle`，需要自己生成 wrapper：

```bash
gradle wrapper --gradle-version 8.4
```

这会补上 `gradlew`、`gradlew.bat`、`gradle/wrapper/gradle-wrapper.properties`
和 `gradle-wrapper.jar`，生成后提交进仓库，以后就能用 `./gradlew` 了。

## 签名安装

CI/本地产出的都是**未签名**的 APK，装到手机前需要签名：

```bash
apksigner sign --ks debug.keystore app-release-unsigned.apk
```

（LSPosed 模块不需要正式签名，调试签名即可，随便生成一个 keystore 都行）

签名后 `adb install` 或者手机端 `pm install -r` 装入。

## 安装后

1. 打开 LSPosed 管理器
2. 模块列表里找到"弹幕随机颜色"，勾选启用
3. 展开"作用范围"，勾选目标 app
4. 完全重启目标 app（不是切后台，是杀掉进程重新打开）生效

## 排查问题

`XposedBridge.log(...)` 的输出可以在 LSPosed 管理器的"日志"里看到，
Hook 有没有成功、目标类找没找到，日志里都会有对应的成功/失败提示，
方便你确认占位符填得对不对。

如果 CI 构建失败，先看 Actions 页面里具体是哪一步报错：
- SDK/JDK 安装步骤失败 → 一般是 action 版本问题，不用改代码
- 依赖下载失败 → 大概率是 `https://api.xposed.info/` 这个源偶尔不稳定，重新跑一次 workflow 通常能过
- 编译报错（找不到类/方法） → 说明 `MainHook.java` 里的占位符还没填或者填错了
