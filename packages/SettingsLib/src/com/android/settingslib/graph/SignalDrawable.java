/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settingslib.graph;

import android.animation.ArgbEvaluator;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.Path.FillType;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.DrawableWrapper;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.CellSignalStrength;
import android.util.LayoutDirection;
import android.util.PathParser;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.TypedValue;

import com.android.settingslib.R;
import com.android.settingslib.Utils;

/**
 * Drawable displaying a mobile cell signal indicator.
 */
public class SignalDrawable extends DrawableWrapper {

    private static final String TAG = "SignalDrawable";

    private static final int NUM_DOTS = 3;

    private static final float VIEWPORT = 24f;
    private static final float PAD = 2f / VIEWPORT;

    private static final float DOT_SIZE = 3f / VIEWPORT;
    private static final float DOT_PADDING = 1.5f / VIEWPORT;

    // All of these are masks to push all of the drawable state into one int for easy callbacks
    // and flow through sysui.
    private static final int LEVEL_MASK = 0xff;
    private static final int NUM_LEVEL_SHIFT = 8;
    private static final int NUM_LEVEL_MASK = 0xff << NUM_LEVEL_SHIFT;
    private static final int STATE_SHIFT = 16;
    private static final int STATE_MASK = 0xff << STATE_SHIFT;
    private static final int STATE_CUT = 2;
    private static final int STATE_CARRIER_CHANGE = 3;

    private static final long DOT_DELAY = 1000;
   
   private SettingObserver mSettingObserver;

    private final Paint mForegroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mTransparentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int mDarkModeFillColor;
    private final int mLightModeFillColor;
    private final Path mCutoutPath = new Path();
    private final Path mForegroundPath = new Path();
    private final Path mAttributionPath = new Path();
    private final Matrix mAttributionScaleMatrix = new Matrix();
    private final Path mScaledAttributionPath = new Path();
    private final Handler mHandler;
    private final float mCutoutWidthFraction;
    private final float mCutoutHeightFraction;
    private float mDarkIntensity = -1;
    private int mIntrinsicSize;
    private int mIntrinsicSizex;
    private boolean ifUseCustom;
    private int mIntrinsicSizeAy;
    private int mIntrinsicSizeAx;
    private Context mcontext;
    private boolean mAnimating;
    private int mCurrentDot;

    public SignalDrawable(Context context) {
        super(context.getDrawable(com.android.internal.R.drawable.ic_signal_cellular));
        final String attributionPathString = context.getString(
                com.android.internal.R.string.config_signalAttributionPath);
        mAttributionPath.set(PathParser.createPathFromPathData(attributionPathString));
        updateScaledAttributionPath();
        mCutoutWidthFraction = context.getResources().getFloat(
                com.android.internal.R.dimen.config_signalCutoutWidthFraction);
        mCutoutHeightFraction = context.getResources().getFloat(
                com.android.internal.R.dimen.config_signalCutoutHeightFraction);
        mDarkModeFillColor = Utils.getColorStateListDefaultColor(context,
                R.color.dark_mode_icon_color_single_tone);
        mLightModeFillColor = Utils.getColorStateListDefaultColor(context,
                R.color.light_mode_icon_color_single_tone);
        mSettingObserver = new SettingObserver(new Handler(context.getMainLooper()));
        mTransparentPaint.setColor(context.getColor(android.R.color.transparent));
        mTransparentPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        mHandler = new Handler();
        mcontext = context;
        setDarkIntensity(0);
        mSettingObserver.observe();
    }

    private void updateScaledAttributionPath() {
        if (getBounds().isEmpty()) {
            mAttributionScaleMatrix.setScale(1f, 1f);
        } else {
            mAttributionScaleMatrix.setScale(
                    getBounds().width() / VIEWPORT, getBounds().height() / VIEWPORT);
        }
        mAttributionPath.transform(mAttributionScaleMatrix, mScaledAttributionPath);
    }

    @Override
    public int getIntrinsicWidth() {
        if (!ifUseCustom) {
            return mIntrinsicSizex = Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 15,
                mcontext.getResources().getDisplayMetrics())); 
        } else { 
           return mIntrinsicSizex = (int) mIntrinsicSizeAx;
        }
    }

    @Override
    public int getIntrinsicHeight() {
        if (!ifUseCustom) {
            return mIntrinsicSize = Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 15,
                mcontext.getResources().getDisplayMetrics())); 
        } else { 
           return mIntrinsicSize = (int) mIntrinsicSizeAy;
        }
    }

    private void updateAnimation() {
        boolean shouldAnimate = isInState(STATE_CARRIER_CHANGE) && isVisible();
        if (shouldAnimate == mAnimating) return;
        mAnimating = shouldAnimate;
        if (shouldAnimate) {
            mChangeDot.run();
        } else {
            mHandler.removeCallbacks(mChangeDot);
        }
    }

    @Override
    protected boolean onLevelChange(int packedState) {
        super.onLevelChange(unpackLevel(packedState));
        updateAnimation();
        setTintList(ColorStateList.valueOf(mForegroundPaint.getColor()));
        invalidateSelf();
        updateSettings();
        return true;
    }

    private int unpackLevel(int packedState) {
        int numBins = (packedState & NUM_LEVEL_MASK) >> NUM_LEVEL_SHIFT;
        int levelOffset = numBins == (CellSignalStrength.getNumSignalStrengthLevels() + 1) ? 10 : 0;
        int level = (packedState & LEVEL_MASK);
        return level + levelOffset;
    }

    public void setDarkIntensity(float darkIntensity) {
        if (darkIntensity == mDarkIntensity) {
            return;
        }
        setTintList(ColorStateList.valueOf(getFillColor(darkIntensity)));
    }

    @Override
    public void setTintList(ColorStateList tint) {
        super.setTintList(tint);
        int colorForeground = mForegroundPaint.getColor();
        mForegroundPaint.setColor(tint.getDefaultColor());
        if (colorForeground != mForegroundPaint.getColor()) invalidateSelf();
    }

    private int getFillColor(float darkIntensity) {
        return getColorForDarkIntensity(
                darkIntensity, mLightModeFillColor, mDarkModeFillColor);
    }

    private int getColorForDarkIntensity(float darkIntensity, int lightColor, int darkColor) {
        return (int) ArgbEvaluator.getInstance().evaluate(darkIntensity, lightColor, darkColor);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        updateScaledAttributionPath();
        invalidateSelf();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        canvas.saveLayer(null, null);
        final float width = getBounds().width();
        final float height = getBounds().height();

        boolean isRtl = getLayoutDirection() == LayoutDirection.RTL;
        if (isRtl) {
            canvas.save();
            // Mirror the drawable
            canvas.translate(width, 0);
            canvas.scale(-1.0f, 1.0f);
        }
        super.draw(canvas);
        mCutoutPath.reset();
        mCutoutPath.setFillType(FillType.WINDING);

        final float padding = Math.round(PAD * width);

        if (isInState(STATE_CARRIER_CHANGE)) {
            float dotSize = (DOT_SIZE * height);
            float dotPadding = (DOT_PADDING * height);
            float dotSpacing = dotPadding + dotSize;
            float x = width - padding - dotSize;
            float y = height - padding - dotSize;
            mForegroundPath.reset();
            drawDotAndPadding(x, y, dotPadding, dotSize, 2);
            drawDotAndPadding(x - dotSpacing, y, dotPadding, dotSize, 1);
            drawDotAndPadding(x - dotSpacing * 2, y, dotPadding, dotSize, 0);
            canvas.drawPath(mCutoutPath, mTransparentPaint);
            canvas.drawPath(mForegroundPath, mForegroundPaint);
        } else if (isInState(STATE_CUT)) {
            float cutX = (mCutoutWidthFraction * width / VIEWPORT);
            float cutY = (mCutoutHeightFraction * height / VIEWPORT);
            mCutoutPath.moveTo(width, height);
            mCutoutPath.rLineTo(-cutX, 0);
            mCutoutPath.rLineTo(0, -cutY);
            mCutoutPath.rLineTo(cutX, 0);
            mCutoutPath.rLineTo(0, cutY);
            canvas.drawPath(mCutoutPath, mTransparentPaint);
            canvas.drawPath(mScaledAttributionPath, mForegroundPaint);
        }
        if (isRtl) {
            canvas.restore();
        }
        canvas.restore();
    }

    private void drawDotAndPadding(float x, float y,
            float dotPadding, float dotSize, int i) {
        if (i == mCurrentDot) {
            // Draw dot
            mForegroundPath.addRect(x, y, x + dotSize, y + dotSize, Direction.CW);
            // Draw dot padding
            mCutoutPath.addRect(x - dotPadding, y - dotPadding, x + dotSize + dotPadding,
                    y + dotSize + dotPadding, Direction.CW);
        }
    }

    @Override
    public void setAlpha(@IntRange(from = 0, to = 255) int alpha) {
        super.setAlpha(alpha);
        mForegroundPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        super.setColorFilter(colorFilter);
        mForegroundPaint.setColorFilter(colorFilter);
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        boolean changed = super.setVisible(visible, restart);
        updateAnimation();
        return changed;
    }
    
    private void updateSettings() {
        ifUseCustom = Settings.System.getIntForUser(mcontext.getContentResolver(),
                "IDC_SIGNAL_CUSTOM_DIMENSION", 0,
                UserHandle.USER_CURRENT) == 1;
        int mIntrinsicSizeAye = Settings.System.getIntForUser(mcontext.getContentResolver(),
                "IDC_SIGNAL_HEIGHT", 15,
                UserHandle.USER_CURRENT);
            mIntrinsicSizeAy = Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, mIntrinsicSizeAye,
                mcontext.getResources().getDisplayMetrics()));
        int mIntrinsicSizeAex = Settings.System.getIntForUser(mcontext.getContentResolver(),
                "IDC_SIGNAL_WIDTH", 15,
                UserHandle.USER_CURRENT);
            mIntrinsicSizeAx = Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, mIntrinsicSizeAex,
                mcontext.getResources().getDisplayMetrics()));   
        if (!ifUseCustom) {
            mIntrinsicSize = Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 15,
                mcontext.getResources().getDisplayMetrics())); 
            mIntrinsicSizex = Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 15,
                mcontext.getResources().getDisplayMetrics())); 
        } else { 
           mIntrinsicSize = (int) mIntrinsicSizeAy;
           mIntrinsicSizex = (int) mIntrinsicSizeAx;
        }
        invalidateSelf();
    }

    private void updateCustomDimen() {
     //unused
    }

    private final Runnable mChangeDot = new Runnable() {
        @Override
        public void run() {
            if (++mCurrentDot == NUM_DOTS) {
                mCurrentDot = 0;
            }
            invalidateSelf();
            mHandler.postDelayed(mChangeDot, DOT_DELAY);
        }
    };

    /**
     * Returns whether this drawable is in the specified state.
     *
     * @param state must be one of {@link #STATE_CARRIER_CHANGE} or {@link #STATE_CUT}
     */
    private boolean isInState(int state) {
        return getState(getLevel()) == state;
    }

    public static int getState(int fullState) {
        return (fullState & STATE_MASK) >> STATE_SHIFT;
    }

    public static int getState(int level, int numLevels, boolean cutOut) {
        return ((cutOut ? STATE_CUT : 0) << STATE_SHIFT)
                | (numLevels << NUM_LEVEL_SHIFT)
                | level;
    }

    /** Returns the state representing empty mobile signal with the given number of levels. */
    public static int getEmptyState(int numLevels) {
        return getState(0, numLevels, true);
    }

    /** Returns the state representing carrier change with the given number of levels. */
    public static int getCarrierChangeState(int numLevels) {
        return (STATE_CARRIER_CHANGE << STATE_SHIFT) | (numLevels << NUM_LEVEL_SHIFT);
    }
    
    private final class SettingObserver extends ContentObserver {
        public SettingObserver(Handler handler) {
            super(handler);
        }    

        void observe() {
            ContentResolver resolver = mcontext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    "IDC_SIGNAL_CUSTOM_DIMENSION"),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    "IDC_SIGNAL_HEIGHT"),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    "IDC_SIGNAL_WIDTH"),
                    false, this, UserHandle.USER_ALL);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            updateSettings();
        }
    }
}
