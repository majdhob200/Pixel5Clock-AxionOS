package com.majd.pixel5clock;

import android.content.Context;
import android.graphics.Typeface;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextClock;

import java.util.Map;
import java.util.WeakHashMap;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class InitHook implements IXposedHookLoadPackage {
    private static final String SYSTEMUI = "com.android.systemui";
    private static final String AX_CLOCK_VIEW = "com.android.systemui.shared.clocks.view.AxClockView";
    private static final String GENERAL_CLOCK_VIEW = "com.android.systemui.shared.clocks.view.GeneralClockView";
    private static final String TAG = "Pixel5ClockAxion";
    private static final Map<Object, TextClock> CLOCKS = new WeakHashMap<>();

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!SYSTEMUI.equals(lpparam.packageName)) return;

        Class<?> axClockView = XposedHelpers.findClassIfExists(AX_CLOCK_VIEW, lpparam.classLoader);
        if (axClockView == null) {
            XposedBridge.log(TAG + ": AxClockView not found");
            return;
        }

        XposedBridge.log(TAG + ": hooked " + AX_CLOCK_VIEW);

        XposedBridge.hookAllMethods(axClockView, "onAttachedToWindow", new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) { installPixelClock(param.thisObject); }
        });

        XposedBridge.hookAllMethods(axClockView, "onLayout", new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) { layoutPixelClock(param.thisObject); }
        });

        XposedBridge.hookAllMethods(axClockView, "onConfigurationChanged", new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) { updatePixelClock(param.thisObject); }
        });

        XposedBridge.hookAllMethods(axClockView, "onDetachedFromWindow", new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                synchronized (CLOCKS) { CLOCKS.remove(param.thisObject); }
            }
        });
    }

    private static boolean shouldReplace(Object object) {
        if (!(object instanceof ViewGroup)) return false;
        if (!GENERAL_CLOCK_VIEW.equals(object.getClass().getName())) return false;
        try {
            return XposedHelpers.getBooleanField(object, "isLargeClock");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": cannot read isLargeClock: " + t);
            return false;
        }
    }

    private static void installPixelClock(Object object) {
        if (!shouldReplace(object)) return;
        ViewGroup host = (ViewGroup) object;

        synchronized (CLOCKS) {
            if (CLOCKS.containsKey(object)) {
                updatePixelClock(object);
                return;
            }

            Context context = host.getContext();
            TextClock clock = new TextClock(context);
            clock.setFormat24Hour("HH\nmm");
            clock.setFormat12Hour("h\nmm");
            clock.setGravity(Gravity.CENTER);
            clock.setIncludeFontPadding(false);
            clock.setTextSize(TypedValue.COMPLEX_UNIT_SP, 112f);
            clock.setLineSpacing(-18f, 0.86f);
            clock.setSingleLine(false);
            clock.setTextColor(0xFFFFFFFF);
            clock.setElegantTextHeight(false);

            try {
                clock.setTypeface(Typeface.createFromFile("/product/fonts/GoogleSansClock-Regular.ttf"), Typeface.NORMAL);
            } catch (Throwable first) {
                try {
                    clock.setTypeface(Typeface.create("google-sans-clock", Typeface.NORMAL), Typeface.NORMAL);
                } catch (Throwable ignored) { }
            }

            for (int i = 0; i < host.getChildCount(); i++) {
                host.getChildAt(i).setAlpha(0f);
            }

            host.addView(clock, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            CLOCKS.put(object, clock);
            XposedBridge.log(TAG + ": Pixel clock installed");
        }

        updatePixelClock(object);
        host.requestLayout();
        host.invalidate();
    }

    private static void updatePixelClock(Object object) {
        if (!shouldReplace(object)) return;
        TextClock clock;
        synchronized (CLOCKS) { clock = CLOCKS.get(object); }
        if (clock == null) return;

        Context context = ((View) object).getContext();
        if (DateFormat.is24HourFormat(context)) {
            clock.setFormat24Hour("HH\nmm");
        } else {
            clock.setFormat12Hour("h\nmm");
        }
        clock.setVisibility(View.VISIBLE);
        clock.invalidate();
    }

    private static void layoutPixelClock(Object object) {
        if (!shouldReplace(object)) return;
        ViewGroup host = (ViewGroup) object;
        TextClock clock;
        synchronized (CLOCKS) { clock = CLOCKS.get(object); }

        if (clock == null) {
            installPixelClock(object);
            synchronized (CLOCKS) { clock = CLOCKS.get(object); }
            if (clock == null) return;
        }

        int width = host.getWidth();
        int height = host.getHeight();
        if (width <= 0 || height <= 0) return;

        int wSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
        int hSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY);
        clock.measure(wSpec, hSpec);
        clock.layout(0, 0, width, height);
        clock.bringToFront();
    }
}
