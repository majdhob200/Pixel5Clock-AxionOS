package com.majd.pixel5clock;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.format.DateFormat;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class InitHook implements IXposedHookLoadPackage {
    private static final String SYSTEMUI = "com.android.systemui";
    private static final String AX_CLOCK_VIEW = "com.android.systemui.shared.clocks.view.AxClockView";
    private static final String BITMAP_CLOCK_VIEW = "com.android.systemui.shared.clocks.view.BitmapDigitComposeClockView";
    private static final String TAG = "Pixel5ClockAxion";

    private static Typeface clockTypeface;
    private static Typeface dateTypeface;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!SYSTEMUI.equals(lpparam.packageName)) return;

        Class<?> axClockView = XposedHelpers.findClassIfExists(AX_CLOCK_VIEW, lpparam.classLoader);
        if (axClockView == null) {
            XposedBridge.log(TAG + ": AxClockView not found");
            return;
        }

        XposedBridge.log(TAG + ": v0.3 hooked " + AX_CLOCK_VIEW + ".draw(Canvas)");

        XposedBridge.hookAllMethods(axClockView, "draw", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                try {
                    Object object = param.thisObject;
                    if (!(object instanceof View)) return;
                    if (!BITMAP_CLOCK_VIEW.equals(object.getClass().getName())) return;

                    boolean large;
                    try {
                        large = XposedHelpers.getBooleanField(object, "isLargeClock");
                    } catch (Throwable t) {
                        return;
                    }
                    if (!large) return;

                    if (param.args == null || param.args.length == 0 || !(param.args[0] instanceof Canvas)) return;

                    View view = (View) object;
                    Canvas canvas = (Canvas) param.args[0];
                    drawPixelClock(view, canvas);

                    // Skip Axion's original BitmapDigit/Compose drawing only for the
                    // large clock. Small clock and all other clock styles stay stock.
                    param.setResult(null);
                } catch (Throwable t) {
                    XposedBridge.log(TAG + ": draw error: " + t);
                }
            }
        });
    }

    private static void ensureTypefaces() {
        if (clockTypeface == null) {
            try {
                clockTypeface = Typeface.createFromFile("/product/fonts/GoogleSansClock-Regular.ttf");
            } catch (Throwable ignored) {
                clockTypeface = Typeface.create("google-sans-clock", Typeface.NORMAL);
            }
        }
        if (dateTypeface == null) {
            try {
                dateTypeface = Typeface.createFromFile("/product/fonts/GoogleSans-Regular.ttf");
            } catch (Throwable ignored) {
                dateTypeface = Typeface.create("sans-serif", Typeface.NORMAL);
            }
        }
    }

    private static void drawPixelClock(View view, Canvas canvas) {
        ensureTypefaces();

        final int width = view.getWidth() > 0 ? view.getWidth() : canvas.getWidth();
        final int height = view.getHeight() > 0 ? view.getHeight() : canvas.getHeight();
        if (width <= 0 || height <= 0) return;

        final Locale locale = Locale.getDefault();
        final Date now = new Date();
        final boolean is24h = DateFormat.is24HourFormat(view.getContext());
        final String hourPattern = is24h ? "HH" : "hh";
        final String hour = new SimpleDateFormat(hourPattern, locale).format(now);
        final String minute = new SimpleDateFormat("mm", locale).format(now);

        Paint clock = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        clock.setColor(0xFFFFFFFF);
        clock.setTextAlign(Paint.Align.CENTER);
        clock.setTypeface(clockTypeface);

        // Fit a true two-row Pixel-style clock to Axion's existing large-clock box.
        // Width controls the visual Pixel proportions while height prevents clipping.
        float sizeByWidth = width * 0.43f;
        float sizeByHeight = height * 0.34f;
        float textSize = Math.min(sizeByWidth, sizeByHeight);
        textSize = Math.max(textSize, 72f * view.getResources().getDisplayMetrics().scaledDensity / 3f);
        clock.setTextSize(textSize);

        Paint.FontMetrics fm = clock.getFontMetrics();
        float lineAdvance = textSize * 0.86f;
        float clockBlockHeight = lineAdvance + (fm.descent - fm.ascent);

        // Reserve a small strip below the clock for the date, like the Pixel AOD.
        float dateStrip = Math.max(34f * view.getResources().getDisplayMetrics().density, height * 0.12f);
        float centerY = (height - dateStrip) * 0.50f;
        float firstBaseline = centerY - (clockBlockHeight * 0.5f) - fm.ascent;
        float secondBaseline = firstBaseline + lineAdvance;
        float centerX = width * 0.5f;

        canvas.drawText(hour, centerX, firstBaseline, clock);
        canvas.drawText(minute, centerX, secondBaseline, clock);

        Paint datePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        datePaint.setColor(0xFFFFFFFF);
        datePaint.setTextAlign(Paint.Align.CENTER);
        datePaint.setTypeface(dateTypeface);
        datePaint.setTextSize(Math.max(13f * view.getResources().getDisplayMetrics().scaledDensity, width * 0.031f));

        String datePattern;
        try {
            datePattern = DateFormat.getBestDateTimePattern(locale, "EEE, d MMM");
        } catch (Throwable ignored) {
            datePattern = "EEE, d MMM";
        }
        String date = new SimpleDateFormat(datePattern, locale).format(now);
        Paint.FontMetrics dfm = datePaint.getFontMetrics();
        float dateBaseline = height - (dateStrip * 0.40f) - dfm.descent;
        canvas.drawText(date, centerX, dateBaseline, datePaint);
    }
}
