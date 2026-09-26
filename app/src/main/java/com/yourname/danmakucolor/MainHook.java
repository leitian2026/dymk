package com.yourname.danmakucolor;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.Random;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

public class MainHook implements IXposedHookLoadPackage {

    // ===================== 配置区:改这里 =====================

    // TODO 1: 改成目标 app 的真实包名(用 `pm list packages` 或 `pm path` 确认)
    private static final String TARGET_PACKAGE = "com.example.targetapp";

    // TODO 2: 改成反编译后确认的、负责弹幕颜色的完整类名
    private static final String TARGET_CLASS = "com.example.app.danmaku.BaseDanmaku";

    // TODO 3a: 如果是 setter 方法形式 (danmaku.setTextColor(color))，填方法名
    private static final String SETTER_METHOD_NAME = "setTextColor";

    // TODO 3b: 如果是字段直接赋值形式 (danmaku.textColor = color)，
    //          填"创建/解析弹幕对象"的类名、方法名，以及字段名。
    //          没用到就留空，代码会自动跳过这条 Hook。
    private static final String PARSER_CLASS = ""; // 例如 "com.example.app.danmaku.DanmakuParser"
    private static final String PARSER_METHOD_NAME = ""; // 例如 "parseDanmaku"
    private static final String COLOR_FIELD_NAME = "textColor";

    // ===========================================================

    private static final Random random = new Random();

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(TARGET_PACKAGE)) {
            return; // 只对目标 app 生效，避免影响其他 app 甚至系统进程
        }

        XposedBridge.log("[DanmakuColorHook] 已进入目标进程: " + lpparam.packageName);

        // ---- 方案 A: 目标是一个 setter 方法 ----
        if (SETTER_METHOD_NAME != null && !SETTER_METHOD_NAME.isEmpty()) {
            try {
                findAndHookMethod(
                        TARGET_CLASS,
                        lpparam.classLoader,
                        SETTER_METHOD_NAME,
                        int.class,
                        new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) {
                                param.args[0] = getRandomColor();
                            }
                        }
                );
                XposedBridge.log("[DanmakuColorHook] setter 方法 Hook 成功: "
                        + TARGET_CLASS + "#" + SETTER_METHOD_NAME);
            } catch (Throwable t) {
                XposedBridge.log("[DanmakuColorHook] setter 方法 Hook 失败: " + t);
            }
        }

        // ---- 方案 B: 目标是字段直接赋值，Hook 创建/解析方法后用反射改字段 ----
        if (PARSER_CLASS != null && !PARSER_CLASS.isEmpty()
                && PARSER_METHOD_NAME != null && !PARSER_METHOD_NAME.isEmpty()) {
            try {
                findAndHookMethod(
                        PARSER_CLASS,
                        lpparam.classLoader,
                        PARSER_METHOD_NAME,
                        new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                Object result = param.getResult();
                                if (result != null) {
                                    try {
                                        XposedHelpers.setIntField(result, COLOR_FIELD_NAME, getRandomColor());
                                    } catch (Throwable inner) {
                                        XposedBridge.log("[DanmakuColorHook] 字段赋值失败: " + inner);
                                    }
                                }
                            }
                        }
                );
                XposedBridge.log("[DanmakuColorHook] 字段方式 Hook 成功: "
                        + PARSER_CLASS + "#" + PARSER_METHOD_NAME);
            } catch (Throwable t) {
                XposedBridge.log("[DanmakuColorHook] 字段方式 Hook 失败: " + t);
            }
        }
    }

    /**
     * 生成随机但不过暗/过亮的颜色，避免弹幕在背景下不可读。
     * alpha 固定为不透明 (0xFF)。
     */
    private static int getRandomColor() {
        int r = random.nextInt(156) + 100; // 100-255
        int g = random.nextInt(156) + 100;
        int b = random.nextInt(156) + 100;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
