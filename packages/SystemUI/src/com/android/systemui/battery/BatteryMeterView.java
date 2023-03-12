/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.systemui.battery;

import static android.provider.Settings.System.SHOW_BATTERY_PERCENT;

import static com.android.systemui.DejankUtils.whitelistIpcs;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.annotation.IntDef;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.systemui.tuner.TunerService;

import com.evillium.prjct.utils.EvlUtils;

import androidx.annotation.StyleRes;
import androidx.annotation.VisibleForTesting;

import com.android.settingslib.graph.CircleBatteryDrawable;
import com.android.settingslib.graph.ThemedBatteryDrawable;
import com.android.settingslib.graph.LandscapeBatteryA;
import com.android.settingslib.graph.LandscapeBatteryB;
import com.android.settingslib.graph.LandscapeBatteryC;
import com.android.settingslib.graph.LandscapeBatteryD;
import com.android.settingslib.graph.LandscapeBatteryE;
import com.android.settingslib.graph.LandscapeBatteryF;
import com.android.settingslib.graph.LandscapeBatteryG;
import com.android.settingslib.graph.LandscapeBatteryH;
import com.android.settingslib.graph.LandscapeBatteryI;
import com.android.settingslib.graph.LandscapeBatteryJ;
import com.android.settingslib.graph.LandscapeBatteryK;
import com.android.settingslib.graph.LandscapeBatteryL;
import com.android.settingslib.graph.LandscapeBatteryM;
import com.android.settingslib.graph.LandscapeBatteryN;
import com.android.settingslib.graph.LandscapeBatteryO;
import com.android.systemui.Dependency;
import com.android.systemui.DualToneHandler;
import com.android.systemui.R;
import com.android.systemui.animation.Interpolators;
import com.android.systemui.plugins.DarkIconDispatcher;
import com.android.systemui.plugins.DarkIconDispatcher.DarkReceiver;
import com.android.systemui.statusbar.policy.BatteryController;

import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.text.NumberFormat;
import java.util.ArrayList;

public class BatteryMeterView extends LinearLayout implements DarkReceiver ,TunerService.Tunable {

    private static final int BATTERY_STYLE_PORTRAIT = 0;
    private static final int BATTERY_STYLE_CIRCLE = 1;
    private static final int BATTERY_STYLE_DOTTED_CIRCLE = 2;
    private static final int BATTERY_STYLE_TEXT = 3;
    protected static final int BATTERY_STYLE_LANDSCAPEA = 4;
    protected static final int BATTERY_STYLE_LANDSCAPEB = 5;
    protected static final int BATTERY_STYLE_LANDSCAPEC = 6;
    protected static final int BATTERY_STYLE_LANDSCAPED = 7;
    protected static final int BATTERY_STYLE_LANDSCAPEE = 8;
    protected static final int BATTERY_STYLE_LANDSCAPEF = 9;
    protected static final int BATTERY_STYLE_LANDSCAPEG = 10;
    protected static final int BATTERY_STYLE_LANDSCAPEH = 11;
    protected static final int BATTERY_STYLE_LANDSCAPEI = 12;
    protected static final int BATTERY_STYLE_LANDSCAPEJ = 13;
    protected static final int BATTERY_STYLE_LANDSCAPEK = 14;
    protected static final int BATTERY_STYLE_LANDSCAPEL = 15;
    protected static final int BATTERY_STYLE_LANDSCAPEM = 16;
    protected static final int BATTERY_STYLE_LANDSCAPEN = 17;
    protected static final int BATTERY_STYLE_LANDSCAPEO = 18;

    @Retention(SOURCE)
    @IntDef({MODE_DEFAULT, MODE_ON, MODE_OFF, MODE_ESTIMATE})
    public @interface BatteryPercentMode {}
    public static final int MODE_DEFAULT = 0;
    public static final int MODE_ON = 1;
    public static final int MODE_OFF = 2;
    public static final int MODE_ESTIMATE = 3; // Not to be used
    private static final String EVL_BATTERY_IMAGE_ROTATION = "system:evl_battery_image_rotation";
    private static final String EVL_BATTERY_CUSTOM_DIMENSION = "system:evl_battery_custom_dimension";
    private static final String EVL_BATTERY_CUSTOM_MARGIN_LEFT = "system:evl_battery_custom_margin_left";
    private static final String EVL_BATTERY_CUSTOM_MARGIN_TOP = "system:evl_battery_custom_margin_top";
    private static final String EVL_BATTERY_CUSTOM_MARGIN_RIGHT = "system:evl_battery_custom_margin_right";
    private static final String EVL_BATTERY_CUSTOM_MARGIN_BOTTOM = "system:evl_battery_custom_margin_bottom";
    private static final String EVL_BATTERY_CUSTOM_SCALE_HEIGHT = "system:evl_battery_custom_scale_height";
    private static final String EVL_BATTERY_CUSTOM_SCALE_WIDTH = "system:evl_battery_custom_scale_width";
    
    private static final String EVL_BATTERY_SCALED_PERIMETER_ALPHA = "system:evl_battery_scaled_perimeter_alpha";
    private static final String EVL_BATTERY_SCALED_FILL_ALPHA = "system:evl_battery_scaled_fill_alpha";
    private static final String EVL_BATTERY_RAINBOW_FILL_COLOR = "system:evl_battery_rainbow_fill_color";
    private static final String EVL_BATTERY_CUSTOM_COLOR = "system:evl_battery_custom_color";
    private static final String EVL_BATTERY_CHARGING_COLOR = "system:evl_battery_charging_color";
    private static final String EVL_BATTERY_FILL_COLOR = "system:evl_battery_fill_color";
    private static final String EVL_BATTERY_FILL_GRADIENT_COLOR = "system:evl_battery_fill_gradient_color";
    private static final String EVL_BATTERY_POWERSAVE_COLOR = "system:evl_battery_powersave_color";
    private static final String EVL_BATTERY_POWERSAVEFILL_COLOR = "system:evl_battery_powersavefill_color"; 
    
    private static final String FLIPLAYOUTBATRE =
            "system:" + "FLIPLAYOUTBATRE";
    private static final String CUSTOM_CHARGE_SWITCH =
            "system:" + "CUSTOM_CHARGE_SWITCH";
    private static final String CUSTOM_CHARGE_SYMBOL =
            "system:" + "CUSTOM_CHARGE_SYMBOL";
            
    private static final String CUSTOM_CHARGING_ICON_SWITCH = "system:custom_charging_icon_switch";
    private static final String CUSTOM_CHARGING_ICON_STYLE = "system:custom_charging_icon_style";
    private static final String CUSTOM_CHARGING_ICON_ML = "system:custom_charging_icon_ml";
    private static final String CUSTOM_CHARGING_ICON_MR = "system:custom_charging_icon_mr";
    private static final String CUSTOM_CHARGING_ICON_WH = "system:custom_charging_icon_wh";

    private int ChargeSymbol;
    private boolean idcSwitch; 
    
    private boolean mChargingIconSwitch;
    private int mChargingIconStyle;
    private int mChargingIconML;
    private int mChargingIconMR;
    private int mChargingIconWH;
    
    private boolean mBatteryLayoutReverse;
    private boolean mBatteryCustomDimension;
    private int mBatteryMarginLeft;
    private int mBatteryMarginTop;
    private int mBatteryMarginRight;
    private int mBatteryMarginBottom;
    private int mBatteryScaleWidth;
    private int mBatteryScaleHeight;

    private boolean mScaledPerimeterAlpha;
    private boolean mScaledFillAlpha;
    private boolean mRainbowFillColor;
    private boolean mCustomBlendColor;
    private int mCustomChargingColor;
    private int mCustomFillColor;
    private int mCustomFillGradColor;
    private int mCustomPowerSaveColor;
    private int mCustomPowerSaveFillColor;

    private final ThemedBatteryDrawable mThemedDrawable;
    private final AccessorizedBatteryDrawable mAccessorizedDrawable;
    private final CircleBatteryDrawable mCircleDrawable;
    private final LandscapeBatteryA mLandscapeBatteryA;
    private final LandscapeBatteryB mLandscapeBatteryB;
    private final LandscapeBatteryC mLandscapeBatteryC;
    private final LandscapeBatteryD mLandscapeBatteryD;
    private final LandscapeBatteryE mLandscapeBatteryE;
    private final LandscapeBatteryF mLandscapeBatteryF;
    private final LandscapeBatteryG mLandscapeBatteryG;
    private final LandscapeBatteryH mLandscapeBatteryH;
    private final LandscapeBatteryI mLandscapeBatteryI;
    private final LandscapeBatteryJ mLandscapeBatteryJ;
    private final LandscapeBatteryK mLandscapeBatteryK;
    private final LandscapeBatteryL mLandscapeBatteryL;
    private final LandscapeBatteryM mLandscapeBatteryM;
    private final LandscapeBatteryN mLandscapeBatteryN;
    private final LandscapeBatteryO mLandscapeBatteryO;
    private final ImageView mBatteryIconView;
    private final ImageView mChargingIconView;
    private TextView mBatteryPercentView;

    private final @StyleRes int mPercentageStyleId;
    private int mTextColor;
    private int mLevel;
    private int mShowPercentMode = MODE_DEFAULT;
    private String mEstimateText = null;
    private boolean mCharging;
    private boolean mIsOverheated;
    private boolean mDisplayShieldEnabled;
    private boolean mPCharging;
    // Error state where we know nothing about the current battery state
    private boolean mBatteryStateUnknown;
    // Lazily-loaded since this is expected to be a rare-if-ever state
    private Drawable mUnknownStateDrawable;

    private boolean mBatteryHidden;
    private int mBatteryStyle = BATTERY_STYLE_PORTRAIT;

    private DualToneHandler mDualToneHandler;

    private int mNonAdaptedSingleToneColor;
    private int mNonAdaptedForegroundColor;
    private int mNonAdaptedBackgroundColor;
    
    private boolean plip;

    private BatteryEstimateFetcher mBatteryEstimateFetcher;

    public BatteryMeterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BatteryMeterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        setOrientation(LinearLayout.HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL | Gravity.START);

        TypedArray atts = context.obtainStyledAttributes(attrs, R.styleable.BatteryMeterView,
                defStyle, 0);
        final int frameColor = atts.getColor(R.styleable.BatteryMeterView_frameColor,
                context.getColor(R.color.meter_background_color));
        mPercentageStyleId = atts.getResourceId(R.styleable.BatteryMeterView_textAppearance, 0);
        mAccessorizedDrawable = new AccessorizedBatteryDrawable(context, frameColor);
        mCircleDrawable = new CircleBatteryDrawable(context, frameColor);
        mThemedDrawable = new ThemedBatteryDrawable(context, frameColor);
        mLandscapeBatteryA = new LandscapeBatteryA(context, frameColor);
        mLandscapeBatteryB = new LandscapeBatteryB(context, frameColor);
        mLandscapeBatteryC = new LandscapeBatteryC(context, frameColor);
        mLandscapeBatteryD = new LandscapeBatteryD(context, frameColor);
        mLandscapeBatteryE = new LandscapeBatteryE(context, frameColor);
        mLandscapeBatteryF = new LandscapeBatteryF(context, frameColor);
        mLandscapeBatteryG = new LandscapeBatteryG(context, frameColor);
        mLandscapeBatteryH = new LandscapeBatteryH(context, frameColor);
        mLandscapeBatteryI = new LandscapeBatteryI(context, frameColor);
        mLandscapeBatteryJ = new LandscapeBatteryJ(context, frameColor);
        mLandscapeBatteryK = new LandscapeBatteryK(context, frameColor);
        mLandscapeBatteryL = new LandscapeBatteryL(context, frameColor);
        mLandscapeBatteryM = new LandscapeBatteryM(context, frameColor);
        mLandscapeBatteryN = new LandscapeBatteryN(context, frameColor);
        mLandscapeBatteryO = new LandscapeBatteryO(context, frameColor);
        atts.recycle();

        setupLayoutTransition();

        mBatteryIconView = new ImageView(context);
        updateDrawable();
        final MarginLayoutParams mlp = new MarginLayoutParams(
                getBatteryStyle() == BATTERY_STYLE_PORTRAIT ? getResources().getDimensionPixelSize(
                R.dimen.status_bar_battery_icon_width) : getResources().getDimensionPixelSize(
                R.dimen.status_bar_battery_icon_circle_width),
                getResources().getDimensionPixelSize(R.dimen.status_bar_battery_icon_height));
        mlp.setMargins(0, 0, 0,
                getResources().getDimensionPixelOffset(R.dimen.battery_margin_bottom));
        addView(mBatteryIconView, mlp);
        
        // Charging icon
        mChargingIconView = new ImageView(context);
        addView(mChargingIconView, mlp);
        updateChargingIconView();

        updateShowPercent();
        mDualToneHandler = new DualToneHandler(context);
        // Init to not dark at all.
        if (isNightMode()) {
            onDarkChanged(new ArrayList<Rect>(), 0, DarkIconDispatcher.DEFAULT_ICON_TINT);
        }

        setClipChildren(false);
        setClipToPadding(false);
    }
    
    private void updateFlipper() {
        setOrientation(LinearLayout.HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        if (plip) {
            setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        } else {
            setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        }
    }

    private boolean isNightMode() {
        return (mContext.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    private void setupLayoutTransition() {
        LayoutTransition transition = new LayoutTransition();
        transition.setDuration(200);

        // Animates appearing/disappearing of the battery percentage text using fade-in/fade-out
        // and disables all other animation types
        ObjectAnimator appearAnimator = ObjectAnimator.ofFloat(null, "alpha", 0f, 1f);
        transition.setAnimator(LayoutTransition.APPEARING, appearAnimator);
        transition.setInterpolator(LayoutTransition.APPEARING, Interpolators.ALPHA_IN);

        ObjectAnimator disappearAnimator = ObjectAnimator.ofFloat(null, "alpha", 1f, 0f);
        transition.setInterpolator(LayoutTransition.DISAPPEARING, Interpolators.ALPHA_OUT);
        transition.setAnimator(LayoutTransition.DISAPPEARING, disappearAnimator);

        transition.setAnimator(LayoutTransition.CHANGE_APPEARING, null);
        transition.setAnimator(LayoutTransition.CHANGE_DISAPPEARING, null);
        transition.setAnimator(LayoutTransition.CHANGING, null);

        setLayoutTransition(transition);
    }

    protected void updateBatteryStyle() {
        updateDrawable();
        scaleBatteryMeterViews();
        updatePercentView();
    }

    public void setForceShowPercent(boolean show) {
        setPercentShowMode(show ? MODE_ON : MODE_DEFAULT);
    }

    /**
     * Force a particular mode of showing percent
     *
     * 0 - No preference
     * 1 - Force on
     * 2 - Force off
     * 3 - Estimate
     * @param mode desired mode (none, on, off)
     */
    public void setPercentShowMode(@BatteryPercentMode int mode) {
        if (mode == mShowPercentMode) return;
        mShowPercentMode = mode;
        updateShowPercent();
        updatePercentText();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateBatteryStyle();
        mAccessorizedDrawable.notifyDensityChanged();
    }

    public void setColorsFromContext(Context context) {
        if (context == null) {
            return;
        }

        mDualToneHandler.setColorsFromContext(context);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }
    
    @Override
    public void onTuningChanged(String key, String newValue) {
        if (EVL_BATTERY_IMAGE_ROTATION.equals(key)) {
            mBatteryLayoutReverse = TunerService.parseIntegerSwitch(newValue, false);
        } else if (EVL_BATTERY_CUSTOM_DIMENSION.equals(key)) {
            mBatteryCustomDimension = TunerService.parseIntegerSwitch(newValue, false);
        } else if (EVL_BATTERY_CUSTOM_MARGIN_LEFT.equals(key)) {
            mBatteryMarginLeft = TunerService.parseInteger(newValue, 0);
        } else if (EVL_BATTERY_CUSTOM_MARGIN_TOP.equals(key)) {
            mBatteryMarginTop = TunerService.parseInteger(newValue, 0);
        } else if (EVL_BATTERY_CUSTOM_MARGIN_RIGHT.equals(key)) {
            mBatteryMarginRight = TunerService.parseInteger(newValue, 0);
        } else if (EVL_BATTERY_CUSTOM_MARGIN_BOTTOM.equals(key)) {
            mBatteryMarginBottom = TunerService.parseInteger(newValue, 0);
        } else if (EVL_BATTERY_CUSTOM_SCALE_HEIGHT.equals(key)) {
            mBatteryScaleHeight = TunerService.parseInteger(newValue, 20);
        } else if (EVL_BATTERY_CUSTOM_SCALE_WIDTH.equals(key)) {
            mBatteryScaleWidth = TunerService.parseInteger(newValue, 28);
        } else if (EVL_BATTERY_SCALED_PERIMETER_ALPHA.equals(key)) {
            mScaledPerimeterAlpha = TunerService.parseIntegerSwitch(newValue, false);
        } else if (EVL_BATTERY_SCALED_FILL_ALPHA.equals(key)) {
            mScaledFillAlpha = TunerService.parseIntegerSwitch(newValue, false);
        } else if (EVL_BATTERY_RAINBOW_FILL_COLOR.equals(key)) {
            mRainbowFillColor = TunerService.parseIntegerSwitch(newValue, false);
        } else if (EVL_BATTERY_CUSTOM_COLOR.equals(key)) {
            mCustomBlendColor = TunerService.parseIntegerSwitch(newValue, false);
        } else if (EVL_BATTERY_CHARGING_COLOR.equals(key)) {
            mCustomChargingColor = TunerService.parseInteger(newValue, Color.BLACK);
        } else if (EVL_BATTERY_FILL_COLOR.equals(key)) {
            mCustomFillColor = TunerService.parseInteger(newValue, Color.BLACK);
        } else if (EVL_BATTERY_FILL_GRADIENT_COLOR.equals(key)) {
            mCustomFillGradColor = TunerService.parseInteger(newValue, Color.BLACK);
        } else if (EVL_BATTERY_POWERSAVE_COLOR.equals(key)) {
            mCustomPowerSaveColor = TunerService.parseInteger(newValue, Color.BLACK);
        } else if (EVL_BATTERY_POWERSAVEFILL_COLOR.equals(key)) {
            mCustomPowerSaveFillColor = TunerService.parseInteger(newValue, Color.BLACK);
        } else if (FLIPLAYOUTBATRE.equals(key)) {
            plip = TunerService.parseIntegerSwitch(newValue, false);
        } else if (CUSTOM_CHARGE_SYMBOL.equals(key)) {
            ChargeSymbol = TunerService.parseInteger(newValue, 0);
            setPercentTextAtCurrentLevel();
        } else if (CUSTOM_CHARGE_SWITCH.equals(key)) {
            idcSwitch = TunerService.parseIntegerSwitch(newValue, false);
            setPercentTextAtCurrentLevel();
        } else if (CUSTOM_CHARGING_ICON_SWITCH.equals(key)) {
            mChargingIconSwitch = TunerService.parseIntegerSwitch(newValue, false);
        } else if (CUSTOM_CHARGING_ICON_STYLE.equals(key)) {
            mChargingIconStyle = TunerService.parseInteger(newValue, 0);
        } else if (CUSTOM_CHARGING_ICON_ML.equals(key)) {
            mChargingIconML = TunerService.parseInteger(newValue, 1);
        } else if (CUSTOM_CHARGING_ICON_MR.equals(key)) {
            mChargingIconMR = TunerService.parseInteger(newValue, 0);
        } else if (CUSTOM_CHARGING_ICON_WH.equals(key)) {
            mChargingIconWH = TunerService.parseInteger(newValue, 14);
        }
        updateSettings();
    }
    
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Dependency.get(TunerService.class)
            .addTunable(this, new String[] {
                            EVL_BATTERY_IMAGE_ROTATION,
                            EVL_BATTERY_CUSTOM_DIMENSION,
                            EVL_BATTERY_CUSTOM_MARGIN_LEFT,
                            EVL_BATTERY_CUSTOM_MARGIN_TOP,
                            EVL_BATTERY_CUSTOM_MARGIN_RIGHT,
                            EVL_BATTERY_CUSTOM_MARGIN_BOTTOM,
                            EVL_BATTERY_CUSTOM_SCALE_HEIGHT,
                            EVL_BATTERY_CUSTOM_SCALE_WIDTH,
                            EVL_BATTERY_SCALED_PERIMETER_ALPHA,
                            EVL_BATTERY_SCALED_FILL_ALPHA,
                            EVL_BATTERY_RAINBOW_FILL_COLOR,
                            EVL_BATTERY_CUSTOM_COLOR,
                            EVL_BATTERY_CHARGING_COLOR,
                            EVL_BATTERY_FILL_COLOR,
                            EVL_BATTERY_FILL_GRADIENT_COLOR,
                            EVL_BATTERY_POWERSAVE_COLOR,
                            EVL_BATTERY_POWERSAVEFILL_COLOR,
                            CUSTOM_CHARGE_SWITCH, 
                            CUSTOM_CHARGE_SYMBOL,
                            CUSTOM_CHARGING_ICON_SWITCH,
                            CUSTOM_CHARGING_ICON_STYLE,
                            CUSTOM_CHARGING_ICON_ML,
                            CUSTOM_CHARGING_ICON_MR,
                            CUSTOM_CHARGING_ICON_WH,
                            FLIPLAYOUTBATRE
                        });
    }
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Dependency.get(TunerService.class).removeTunable(this);
    }

    private void updateRotationLandscape() {
        if (mBatteryLayoutReverse) {
            if (getBatteryStyle() == BATTERY_STYLE_LANDSCAPEA
                || getBatteryStyle() == BATTERY_STYLE_LANDSCAPEB
                || getBatteryStyle() == BATTERY_STYLE_LANDSCAPEC
                || getBatteryStyle() == BATTERY_STYLE_LANDSCAPEF
                || getBatteryStyle() == BATTERY_STYLE_LANDSCAPEG
                || getBatteryStyle() == BATTERY_STYLE_LANDSCAPEH
                || getBatteryStyle() == BATTERY_STYLE_LANDSCAPEI
                || getBatteryStyle() == BATTERY_STYLE_LANDSCAPEJ
                || getBatteryStyle() == BATTERY_STYLE_LANDSCAPEK
                || getBatteryStyle() == BATTERY_STYLE_LANDSCAPEL
                || getBatteryStyle() == BATTERY_STYLE_LANDSCAPEM
                || getBatteryStyle() == BATTERY_STYLE_LANDSCAPEN
                || getBatteryStyle() == BATTERY_STYLE_LANDSCAPEO) {
                mBatteryIconView.setRotation(180f);
            } else {
                mBatteryIconView.setRotation(0f);
            }
        } else {
            mBatteryIconView.setRotation(0f);
        }
    }
    
    public void setIsQsPercent(boolean isQs) {
        mLandscapeBatteryL.setQsPercent(isQs);
    }

    private void updateChargingIconView() {
        final Context c = mContext;

        mLandscapeBatteryA.setCustomChargingIcon(mChargingIconSwitch);
        mLandscapeBatteryB.setCustomChargingIcon(mChargingIconSwitch);
        mLandscapeBatteryC.setCustomChargingIcon(mChargingIconSwitch);
        mLandscapeBatteryD.setCustomChargingIcon(mChargingIconSwitch);
        mLandscapeBatteryE.setCustomChargingIcon(mChargingIconSwitch);
        mLandscapeBatteryF.setCustomChargingIcon(mChargingIconSwitch);
        mLandscapeBatteryG.setCustomChargingIcon(mChargingIconSwitch);
        mLandscapeBatteryH.setCustomChargingIcon(mChargingIconSwitch);
        mLandscapeBatteryJ.setCustomChargingIcon(mChargingIconSwitch);
        mLandscapeBatteryK.setCustomChargingIcon(mChargingIconSwitch);
        mLandscapeBatteryL.setCustomChargingIcon(mChargingIconSwitch);
        mLandscapeBatteryM.setCustomChargingIcon(mChargingIconSwitch);
        mLandscapeBatteryN.setCustomChargingIcon(mChargingIconSwitch);
        mLandscapeBatteryO.setCustomChargingIcon(mChargingIconSwitch);

        Drawable d = null;
        switch (mChargingIconStyle) {
            case 0:
                d = c.getDrawable(R.drawable.ic_charging_bold);
                break;
            case 1:
                d = c.getDrawable(R.drawable.ic_charging_asus);
                break;
            case 2:
                d = c.getDrawable(R.drawable.ic_charging_buddy);
                break;
            case 3:
                d = c.getDrawable(R.drawable.ic_charging_evplug);
                break;
            case 4:
                d = c.getDrawable(R.drawable.ic_charging_idc);
                break;
            case 5:
                d = c.getDrawable(R.drawable.ic_charging_ios);
                break;
            case 6:
                d = c.getDrawable(R.drawable.ic_charging_koplak);
                break;
            case 7:
                d = c.getDrawable(R.drawable.ic_charging_miui);
                break;
            case 8:
                d = c.getDrawable(R.drawable.ic_charging_mmk);
                break;
            case 9:
                d = c.getDrawable(R.drawable.ic_charging_moto);
                break;
            case 10:
                d = c.getDrawable(R.drawable.ic_charging_nokia);
                break;
            case 11:
                d = c.getDrawable(R.drawable.ic_charging_plug);
                break;
            case 12:
                d = c.getDrawable(R.drawable.ic_charging_powercable);
                break;
            case 13:
                d = c.getDrawable(R.drawable.ic_charging_powercord);
                break;
            case 14:
                d = c.getDrawable(R.drawable.ic_charging_powerstation);
                break;
            case 15:
                d = c.getDrawable(R.drawable.ic_charging_realme);
                break;
            case 16:
                d = c.getDrawable(R.drawable.ic_charging_soak);
                break;
            case 17:
                d = c.getDrawable(R.drawable.ic_charging_stres);
                break;
            case 18:
                d = c.getDrawable(R.drawable.ic_charging_strip);
                break;
            case 19:
                d = c.getDrawable(R.drawable.ic_charging_usbcable);
                break;
            case 20:
                d = c.getDrawable(R.drawable.ic_charging_xiaomi);
                break;
            default:
                d = null;
        }

        if (d != null)
            mChargingIconView.setImageDrawable(d);

        int l = EvlUtils.dpToPx(mChargingIconML);
        int r = EvlUtils.dpToPx(mChargingIconMR);
        int wh = EvlUtils.dpToPx(mChargingIconWH);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(wh, wh);
        lp.setMargins(l, 0, r, getResources().getDimensionPixelSize(R.dimen.battery_margin_bottom));
        mChargingIconView.setLayoutParams(lp);

        mChargingIconView.setVisibility(
            mCharging && mChargingIconSwitch ? View.VISIBLE : View.GONE
        );
    }
    
    private void updateCustomizeBatteryDrawable() {
        
        mThemedDrawable.customizeBatteryDrawable(
            mBatteryLayoutReverse,
            mScaledPerimeterAlpha,
            mScaledFillAlpha,
            mCustomBlendColor,
            mCustomFillColor,
            mCustomFillGradColor,
            mCustomChargingColor,
            mCustomPowerSaveColor,
            mCustomPowerSaveFillColor);
        
        mLandscapeBatteryA.customizeBatteryDrawable(
            mBatteryLayoutReverse,
            mScaledPerimeterAlpha,
            mScaledFillAlpha,
            mCustomBlendColor,
            mCustomFillColor,
            mCustomFillGradColor,
            mCustomChargingColor,
            mCustomPowerSaveColor,
            mCustomPowerSaveFillColor);

        mLandscapeBatteryB.customizeBatteryDrawable(
            mBatteryLayoutReverse,
            mScaledPerimeterAlpha,
            mScaledFillAlpha,
            mCustomBlendColor,
            mCustomFillColor,
            mCustomFillGradColor,
            mCustomChargingColor,
            mCustomPowerSaveColor,
            mCustomPowerSaveFillColor);

        mLandscapeBatteryC.customizeBatteryDrawable(
            mBatteryLayoutReverse,
            mScaledPerimeterAlpha,
            mScaledFillAlpha,
            mCustomBlendColor,
            mCustomFillColor,
            mCustomFillGradColor,
            mCustomChargingColor,
            mCustomPowerSaveColor,
            mCustomPowerSaveFillColor);

        mLandscapeBatteryD.customizeBatteryDrawable(
            mScaledPerimeterAlpha,
            mScaledFillAlpha,
            mCustomBlendColor,
            mCustomFillColor,
            mCustomFillGradColor,
            mCustomChargingColor,
            mCustomPowerSaveColor,
            mCustomPowerSaveFillColor);

        mLandscapeBatteryE.customizeBatteryDrawable(
            mScaledPerimeterAlpha,
            mScaledFillAlpha,
            mCustomBlendColor,
            mCustomFillColor,
            mCustomFillGradColor,
            mCustomChargingColor,
            mCustomPowerSaveColor,
            mCustomPowerSaveFillColor);

        mLandscapeBatteryF.customizeBatteryDrawable(
            mScaledPerimeterAlpha,
            mScaledFillAlpha,
            mCustomBlendColor,
            mCustomFillColor,
            mCustomFillGradColor,
            mCustomChargingColor,
            mCustomPowerSaveColor,
            mCustomPowerSaveFillColor);

        mLandscapeBatteryG.customizeBatteryDrawable(
            mBatteryLayoutReverse,
            mScaledPerimeterAlpha,
            mScaledFillAlpha,
            mCustomBlendColor,
            mCustomFillColor,
            mCustomFillGradColor,
            mCustomChargingColor,
            mCustomPowerSaveColor,
            mCustomPowerSaveFillColor);

        mLandscapeBatteryH.customizeBatteryDrawable(
            mBatteryLayoutReverse,
            mScaledPerimeterAlpha,
            mScaledFillAlpha,
            mCustomBlendColor,
            mCustomFillColor,
            mCustomFillGradColor,
            mCustomChargingColor,
            mCustomPowerSaveColor,
            mCustomPowerSaveFillColor);

        mLandscapeBatteryI.customizeBatteryDrawable(
            mBatteryLayoutReverse,
            mScaledPerimeterAlpha,
            mScaledFillAlpha,
            mCustomBlendColor,
            mRainbowFillColor,
            mCustomFillColor,
            mCustomFillGradColor,
            mCustomChargingColor,
            mCustomPowerSaveColor,
            mCustomPowerSaveFillColor);

        mLandscapeBatteryJ.customizeBatteryDrawable(
            mScaledPerimeterAlpha,
            mScaledFillAlpha,
            mCustomBlendColor,
            mRainbowFillColor,
            mCustomFillColor,
            mCustomFillGradColor,
            mCustomChargingColor,
            mCustomPowerSaveColor,
            mCustomPowerSaveFillColor);

        mLandscapeBatteryK.customizeBatteryDrawable(
            mBatteryLayoutReverse,
            mScaledPerimeterAlpha,
            mScaledFillAlpha,
            mCustomBlendColor,
            mCustomFillColor,
            mCustomFillGradColor,
            mCustomChargingColor,
            mCustomPowerSaveColor,
            mCustomPowerSaveFillColor);

        mLandscapeBatteryL.customizeBatteryDrawable(
            mBatteryLayoutReverse,
            mCustomBlendColor,
            mCustomFillColor,
            mCustomFillGradColor,
            mCustomChargingColor,
            mCustomPowerSaveColor,
            mCustomPowerSaveFillColor);

        mLandscapeBatteryM.customizeBatteryDrawable(
            mBatteryLayoutReverse,
            mCustomBlendColor,
            mCustomFillColor,
            mCustomFillGradColor,
            mCustomChargingColor,
            mCustomPowerSaveColor,
            mCustomPowerSaveFillColor);

        mLandscapeBatteryN.customizeBatteryDrawable(
            mBatteryLayoutReverse,
            mScaledPerimeterAlpha,
            mScaledFillAlpha,
            mCustomBlendColor,
            mCustomFillColor,
            mCustomFillGradColor,
            mCustomChargingColor,
            mCustomPowerSaveColor,
            mCustomPowerSaveFillColor);

        mLandscapeBatteryO.customizeBatteryDrawable(
            mBatteryLayoutReverse,
            mScaledPerimeterAlpha,
            mScaledFillAlpha,
            mCustomBlendColor,
            mCustomFillColor,
            mCustomFillGradColor,
            mCustomChargingColor,
            mCustomPowerSaveColor,
            mCustomPowerSaveFillColor);
    }
    
    private void updateSettings() {
        updateCustomizeBatteryDrawable();
        updateChargingIconView();
        updateRotationLandscape();
        updateShowPercent();
        scaleBatteryMeterViews();
        updateFlipper();
    }

    void onBatteryLevelChanged(int level, boolean pluggedIn) {
        if (mLevel != level) {
            mLevel = level;
            mCircleDrawable.setBatteryLevel(level);
            mThemedDrawable.setBatteryLevel(mLevel);
            mLandscapeBatteryA.setBatteryLevel(level);
            mLandscapeBatteryB.setBatteryLevel(level);
            mLandscapeBatteryC.setBatteryLevel(level);
            mLandscapeBatteryD.setBatteryLevel(level);
            mLandscapeBatteryE.setBatteryLevel(level);
            mLandscapeBatteryF.setBatteryLevel(level);
            mLandscapeBatteryG.setBatteryLevel(level);
            mLandscapeBatteryH.setBatteryLevel(level);
            mLandscapeBatteryI.setBatteryLevel(level);
            mLandscapeBatteryJ.setBatteryLevel(level);
            mLandscapeBatteryK.setBatteryLevel(level);
            mLandscapeBatteryL.setBatteryLevel(level);
            mLandscapeBatteryM.setBatteryLevel(level);
            mLandscapeBatteryN.setBatteryLevel(level);
            mLandscapeBatteryO.setBatteryLevel(level);
            updatePercentText();
        }
        if (mCharging != pluggedIn) {
            mCharging = pluggedIn;
            mCircleDrawable.setCharging(pluggedIn);
            mThemedDrawable.setCharging(mCharging);
            mLandscapeBatteryA.setCharging(pluggedIn);
            mLandscapeBatteryB.setCharging(pluggedIn);
            mLandscapeBatteryC.setCharging(pluggedIn);
            mLandscapeBatteryD.setCharging(pluggedIn);
            mLandscapeBatteryE.setCharging(pluggedIn);
            mLandscapeBatteryF.setCharging(pluggedIn);
            mLandscapeBatteryG.setCharging(pluggedIn);
            mLandscapeBatteryH.setCharging(pluggedIn);
            mLandscapeBatteryI.setCharging(pluggedIn);
            mLandscapeBatteryJ.setCharging(pluggedIn);
            mLandscapeBatteryK.setCharging(pluggedIn);
            mLandscapeBatteryL.setCharging(pluggedIn);
            mLandscapeBatteryM.setCharging(pluggedIn);
            mLandscapeBatteryN.setCharging(pluggedIn);
            mLandscapeBatteryO.setCharging(pluggedIn);
            updateShowPercent();
            updatePercentText();
            updateChargingIconView();
        }
    }

    void onPowerSaveChanged(boolean isPowerSave) {
        mAccessorizedDrawable.setPowerSaveEnabled(isPowerSave);
        mCircleDrawable.setPowerSaveEnabled(isPowerSave);
        mThemedDrawable.setPowerSaveEnabled(isPowerSave);
        mLandscapeBatteryA.setPowerSaveEnabled(isPowerSave);
        mLandscapeBatteryB.setPowerSaveEnabled(isPowerSave);
        mLandscapeBatteryC.setPowerSaveEnabled(isPowerSave);
        mLandscapeBatteryD.setPowerSaveEnabled(isPowerSave);
        mLandscapeBatteryE.setPowerSaveEnabled(isPowerSave);
        mLandscapeBatteryF.setPowerSaveEnabled(isPowerSave);
        mLandscapeBatteryG.setPowerSaveEnabled(isPowerSave);
        mLandscapeBatteryH.setPowerSaveEnabled(isPowerSave);
        mLandscapeBatteryI.setPowerSaveEnabled(isPowerSave);
        mLandscapeBatteryJ.setPowerSaveEnabled(isPowerSave);
        mLandscapeBatteryK.setPowerSaveEnabled(isPowerSave);
        mLandscapeBatteryL.setPowerSaveEnabled(isPowerSave);
        mLandscapeBatteryM.setPowerSaveEnabled(isPowerSave);
        mLandscapeBatteryN.setPowerSaveEnabled(isPowerSave);
        mLandscapeBatteryO.setPowerSaveEnabled(isPowerSave);
    }

    void onIsOverheatedChanged(boolean isOverheated) {
        boolean valueChanged = mIsOverheated != isOverheated;
        mIsOverheated = isOverheated;
        if (valueChanged) {
            updateContentDescription();
            // The battery drawable is a different size depending on whether it's currently
            // overheated or not, so we need to re-scale the view when overheated changes.
            scaleBatteryMeterViews();
        }
    }

    private TextView loadPercentView() {
        return (TextView) LayoutInflater.from(getContext())
                .inflate(R.layout.battery_percentage_view, null);
    }

    /**
     * Updates percent view by removing old one and reinflating if necessary
     */
    public void updatePercentView() {
        if (mBatteryPercentView != null) {
            removeView(mBatteryPercentView);
            mBatteryPercentView = null;
        }
        updateShowPercent();
    }

    /**
     * Sets the fetcher that should be used to get the estimated time remaining for the user's
     * battery.
     */
    void setBatteryEstimateFetcher(BatteryEstimateFetcher fetcher) {
        mBatteryEstimateFetcher = fetcher;
    }

    void setDisplayShieldEnabled(boolean displayShieldEnabled) {
        mDisplayShieldEnabled = displayShieldEnabled;
    }

    void updatePercentText() {
        if (mBatteryStateUnknown) {
            return;
        }

        if (mBatteryEstimateFetcher == null) {
            setPercentTextAtCurrentLevel();
            return;
        }
        
        if (mBatteryPercentView != null) {
            if (mShowPercentMode == MODE_ESTIMATE && !mCharging) {
                mBatteryEstimateFetcher.fetchBatteryTimeRemainingEstimate(
                        (String estimate) -> {
                    if (mBatteryPercentView == null) {
                        return;
                    }
                    if (estimate != null && mShowPercentMode == MODE_ESTIMATE) {
                        mEstimateText = estimate;
                        mBatteryPercentView.setText(estimate);
                        updateContentDescription();
                    } else {
                        setPercentTextAtCurrentLevel();
                    }
                });
            } else {
                setPercentTextAtCurrentLevel();
            }
        } else {
            updateContentDescription();
        }
    }

    private void setPercentTextAtCurrentLevel() {
        if (mBatteryPercentView == null) return;

        String PercentText = NumberFormat.getPercentInstance().format(mLevel / 100f);
        // Setting text actually triggers a layout pass (because the text view is set to
        // wrap_content width and TextView always relayouts for this). Avoid needless
        // relayout if the text didn't actually change.
        if (!TextUtils.equals(mBatteryPercentView.getText(), PercentText) || mPCharging != mCharging) {
            mPCharging = mCharging;
            // Use the high voltage symbol ⚡ (u26A1 unicode) but prevent the system
            // to load its emoji colored variant with the uFE0E flag
            // only use it when there is no batt icon showing
            String indication = mCharging && (getBatteryStyle() == BATTERY_STYLE_TEXT)
                    ? "\u26A1\uFE0E " : "";
            String indication0 = mCharging && (getBatteryStyle() == BATTERY_STYLE_TEXT)
                    ? "" : "";
            String indication1 = mCharging && (getBatteryStyle() == BATTERY_STYLE_TEXT)
                    ? "\u2623\uFE0E " : "";
            String indication2 = mCharging && (getBatteryStyle() == BATTERY_STYLE_TEXT)
                    ? "\u2605\uFE0E " : "";
            String indication3 = mCharging && (getBatteryStyle() == BATTERY_STYLE_TEXT)
                    ? "\u263a\uFE0E " : "";
            String indication4 = mCharging && (getBatteryStyle() == BATTERY_STYLE_TEXT)
                    ? "\u267d\uFE0E " : "";
            String indication5 = mCharging && (getBatteryStyle() == BATTERY_STYLE_TEXT)
                    ? "\u199f\uFE0E " : "";
            String indication6 = mCharging && (getBatteryStyle() == BATTERY_STYLE_TEXT)
                    ? "\u2741\uFE0E " : "";
            String indication7 = mCharging && (getBatteryStyle() == BATTERY_STYLE_TEXT)
                    ? "\u274a\uFE0E " : "";
            String indication8 = mCharging && (getBatteryStyle() == BATTERY_STYLE_TEXT)
                    ? "\u2746\uFE0E " : "";
            String indication9 = mCharging && (getBatteryStyle() == BATTERY_STYLE_TEXT)
                    ? "\u224b\uFE0E " : "";
            if (ChargeSymbol == 1) {
                     mBatteryPercentView.setText(indication1 + PercentText);
                  } else if (ChargeSymbol == 2) {
                     mBatteryPercentView.setText(indication2 + PercentText);
                  } else if (ChargeSymbol == 3) {
                     mBatteryPercentView.setText(indication3 + PercentText);
                  } else if (ChargeSymbol == 4) {
                     mBatteryPercentView.setText(indication4 + PercentText);
                  } else if (ChargeSymbol == 5) {
                     mBatteryPercentView.setText(indication5 + PercentText);
                  } else if (ChargeSymbol == 6) {
                     mBatteryPercentView.setText(indication6 + PercentText);
                  } else if (ChargeSymbol == 7) {
                     mBatteryPercentView.setText(indication7 + PercentText);
                  } else if (ChargeSymbol == 8) {
                     mBatteryPercentView.setText(indication8 + PercentText);
                  } else if (ChargeSymbol == 9) {
                     mBatteryPercentView.setText(indication9 + PercentText);
                  } else {
                    mBatteryPercentView.setText(indication + PercentText);       
             }
       	}
        setContentDescription(
                getContext().getString(mCharging ? R.string.accessibility_battery_level_charging
                        : R.string.accessibility_battery_level, mLevel));
    }

    private void updateContentDescription() {
        Context context = getContext();

        String contentDescription;
        if (mBatteryStateUnknown) {
            contentDescription = context.getString(R.string.accessibility_battery_unknown);
        } else if (mShowPercentMode == MODE_ESTIMATE && !TextUtils.isEmpty(mEstimateText)) {
            contentDescription = context.getString(
                    mIsOverheated
                            ? R.string.accessibility_battery_level_charging_paused_with_estimate
                            : R.string.accessibility_battery_level_with_estimate,
                    mLevel,
                    mEstimateText);
        } else if (mIsOverheated) {
            contentDescription =
                    context.getString(R.string.accessibility_battery_level_charging_paused, mLevel);
        } else if (mCharging) {
            contentDescription =
                    context.getString(R.string.accessibility_battery_level_charging, mLevel);
        } else {
            contentDescription = context.getString(R.string.accessibility_battery_level, mLevel);
        }

        setContentDescription(contentDescription);
    }
    
    void updateShowPercent() {
        final boolean showing = mBatteryPercentView != null;
        // TODO(b/140051051)
        final int showBatteryPercent = Settings.System.getIntForUser(
                getContext().getContentResolver(), SHOW_BATTERY_PERCENT, 0,
                UserHandle.USER_CURRENT);
        final boolean drawPercentInside = mShowPercentMode == MODE_DEFAULT &&
                showBatteryPercent == 1;
        final boolean drawPercentOnly = mShowPercentMode == MODE_ESTIMATE ||
                showBatteryPercent == 2;
        boolean shouldShow =
                (drawPercentOnly && (!drawPercentInside || mCharging) ||
                getBatteryStyle() == BATTERY_STYLE_TEXT);
        shouldShow = shouldShow && !mBatteryStateUnknown;
        
        mCircleDrawable.setShowPercent(drawPercentInside);
        mThemedDrawable.setShowPercent(drawPercentInside);
        mLandscapeBatteryA.setShowPercent(drawPercentInside);
        mLandscapeBatteryB.setShowPercent(drawPercentInside);
        mLandscapeBatteryC.setShowPercent(drawPercentInside);
        mLandscapeBatteryD.setShowPercent(drawPercentInside);
        mLandscapeBatteryE.setShowPercent(drawPercentInside);
        mLandscapeBatteryF.setShowPercent(drawPercentInside);
        mLandscapeBatteryG.setShowPercent(drawPercentInside);
        mLandscapeBatteryH.setShowPercent(drawPercentInside);
        mLandscapeBatteryI.setShowPercent(drawPercentInside);
        mLandscapeBatteryJ.setShowPercent(drawPercentInside);
        mLandscapeBatteryK.setShowPercent(drawPercentInside);
        mLandscapeBatteryL.setShowPercent(drawPercentInside);
        mLandscapeBatteryM.setShowPercent(drawPercentInside);
        mLandscapeBatteryN.setShowPercent(drawPercentInside);
        mLandscapeBatteryO.setShowPercent(drawPercentInside);

        if (shouldShow) {
            mAccessorizedDrawable.showPercent(false);
            mCircleDrawable.setShowPercent(false);
            if (!showing) {
                mBatteryPercentView = loadPercentView();
                if (mPercentageStyleId != 0) { // Only set if specified as attribute
                    mBatteryPercentView.setTextAppearance(mPercentageStyleId);
                }
                if (mTextColor != 0) mBatteryPercentView.setTextColor(mTextColor);
                updatePercentText();
                addView(mBatteryPercentView,
                        new ViewGroup.LayoutParams(
                                LayoutParams.WRAP_CONTENT,
                                LayoutParams.MATCH_PARENT));
            }
            if (getBatteryStyle() == BATTERY_STYLE_TEXT) {
                mBatteryPercentView.setPaddingRelative(0, 0, 0, 0);
            } else {
                Resources res = getContext().getResources();
                mBatteryPercentView.setPaddingRelative(
                        res.getDimensionPixelSize(R.dimen.battery_level_padding_start), 0, 0, 0);
            }

        } else {
            mAccessorizedDrawable.showPercent(drawPercentInside);
            mCircleDrawable.setShowPercent(drawPercentInside);
            if (showing) {
                removeView(mBatteryPercentView);
                mBatteryPercentView = null;
            }
        }
    }

    private Drawable getUnknownStateDrawable() {
        if (mUnknownStateDrawable == null) {
            mUnknownStateDrawable = mContext.getDrawable(R.drawable.ic_battery_unknown);
            mUnknownStateDrawable.setTint(mTextColor);
        }

        return mUnknownStateDrawable;
    }

    void onBatteryUnknownStateChanged(boolean isUnknown) {
        if (mBatteryStateUnknown == isUnknown) {
            return;
        }

        mBatteryStateUnknown = isUnknown;
        updateContentDescription();

        if (mBatteryStateUnknown) {
            mBatteryIconView.setImageDrawable(getUnknownStateDrawable());
        } else {
            updateDrawable();
        }
    }

    /**
     * Looks up the scale factor for status bar icons and scales the battery view by that amount.
     */
    void scaleBatteryMeterViews() {
        if (mBatteryIconView == null) {
            return;
        }
        Resources res = getContext().getResources();
        TypedValue typedValue = new TypedValue();

        res.getValue(R.dimen.status_bar_icon_scale_factor, typedValue, true);
        float iconScaleFactor = typedValue.getFloat();
        
        int defaultHeight = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height);
        int defaultWidth = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width);
        int defaultMarginBottom = res.getDimensionPixelSize(R.dimen.battery_margin_bottom);
        int marginLeft = EvlUtils.dpToPx(mBatteryMarginLeft);
        int marginTop = EvlUtils.dpToPx(mBatteryMarginTop);
        int marginRight = EvlUtils.dpToPx(mBatteryMarginRight);
        int marginBottom = EvlUtils.dpToPx(mBatteryMarginBottom);
        int scaleHeight = EvlUtils.dpToPx(mBatteryScaleHeight);
        int scaleWidth = EvlUtils.dpToPx(mBatteryScaleWidth);
        float mainBatteryHeight = defaultHeight * iconScaleFactor;
        float mainBatteryWidth = defaultWidth * iconScaleFactor;

        int mBatteryStyle = getBatteryStyle();
        
        if (mBatteryCustomDimension) {
            if (mBatteryStyle == BATTERY_STYLE_LANDSCAPEA ||
                mBatteryStyle == BATTERY_STYLE_LANDSCAPEB ||
                mBatteryStyle == BATTERY_STYLE_LANDSCAPEC ||
                mBatteryStyle == BATTERY_STYLE_LANDSCAPED ||
                mBatteryStyle == BATTERY_STYLE_LANDSCAPEE ||
                mBatteryStyle == BATTERY_STYLE_LANDSCAPEF ||
                mBatteryStyle == BATTERY_STYLE_LANDSCAPEG ||
                mBatteryStyle == BATTERY_STYLE_LANDSCAPEH ||
                mBatteryStyle == BATTERY_STYLE_LANDSCAPEI ||
                mBatteryStyle == BATTERY_STYLE_LANDSCAPEJ ||
                mBatteryStyle == BATTERY_STYLE_LANDSCAPEK ||
                mBatteryStyle == BATTERY_STYLE_LANDSCAPEL ||
                mBatteryStyle == BATTERY_STYLE_LANDSCAPEM ||
                mBatteryStyle == BATTERY_STYLE_LANDSCAPEN ||
                mBatteryStyle == BATTERY_STYLE_LANDSCAPEO ||
                mBatteryStyle == BATTERY_STYLE_PORTRAIT   ||
                mBatteryStyle == BATTERY_STYLE_CIRCLE     ||
                mBatteryStyle == BATTERY_STYLE_DOTTED_CIRCLE) {
                LinearLayout.LayoutParams scaledLayoutParams = new LinearLayout.LayoutParams(
                    (scaleWidth), (scaleHeight));
                scaledLayoutParams.setMargins(marginLeft,
                                              marginTop,
                                              marginRight,
                                              marginBottom);
                mBatteryIconView.setLayoutParams(scaledLayoutParams);
            } else {
                LinearLayout.LayoutParams scaledLayoutParams = new LinearLayout.LayoutParams(
                    (defaultWidth), (defaultHeight));
                scaledLayoutParams.setMargins(0, 0, 0, defaultMarginBottom);
                mBatteryIconView.setLayoutParams(scaledLayoutParams);
            }
        } else {
            if (mBatteryStyle == BATTERY_STYLE_LANDSCAPEA) {
                int batteryWidth = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape_a);
                int batteryHeight = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape_a);
                LinearLayout.LayoutParams scaledLayoutParams = new LinearLayout.LayoutParams(
                    (int) (batteryWidth * iconScaleFactor), (int) (batteryHeight * iconScaleFactor));
                scaledLayoutParams.setMargins(0, 0, 0, defaultMarginBottom);
                mBatteryIconView.setLayoutParams(scaledLayoutParams);
            } else if (mBatteryStyle == BATTERY_STYLE_LANDSCAPEB) {
                int batteryWidth = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape_b);
                int batteryHeight = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape_b);
                LinearLayout.LayoutParams scaledLayoutParams = new LinearLayout.LayoutParams(
                    (int) (batteryWidth * iconScaleFactor), (int) (batteryHeight * iconScaleFactor));
                scaledLayoutParams.setMargins(0, 0, 0, defaultMarginBottom);
                mBatteryIconView.setLayoutParams(scaledLayoutParams);
            } else if (mBatteryStyle == BATTERY_STYLE_LANDSCAPEC) {
                int batteryWidth = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape_c);
                int batteryHeight = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape_c);
                LinearLayout.LayoutParams scaledLayoutParams = new LinearLayout.LayoutParams(
                    (int) (batteryWidth * iconScaleFactor), (int) (batteryHeight * iconScaleFactor));
                scaledLayoutParams.setMargins(0, 0, 0, defaultMarginBottom);
                mBatteryIconView.setLayoutParams(scaledLayoutParams);
            } else if (mBatteryStyle == BATTERY_STYLE_LANDSCAPED) {
                int batteryWidth = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape_d);
                int batteryHeight = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape_d);
                LinearLayout.LayoutParams scaledLayoutParams = new LinearLayout.LayoutParams(
                    (int) (batteryWidth * iconScaleFactor), (int) (batteryHeight * iconScaleFactor));
                scaledLayoutParams.setMargins(0, 0, 0, defaultMarginBottom);
                mBatteryIconView.setLayoutParams(scaledLayoutParams);
            } else if (mBatteryStyle == BATTERY_STYLE_LANDSCAPEE) {
                int batteryWidth = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape_e);
                int batteryHeight = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape_e);
                LinearLayout.LayoutParams scaledLayoutParams = new LinearLayout.LayoutParams(
                    (int) (batteryWidth * iconScaleFactor), (int) (batteryHeight * iconScaleFactor));
                scaledLayoutParams.setMargins(0, 0, 0, defaultMarginBottom);
                mBatteryIconView.setLayoutParams(scaledLayoutParams);
            } else if (mBatteryStyle == BATTERY_STYLE_LANDSCAPEF) {
                int batteryWidth = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape_f);
                int batteryHeight = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape_f);
                LinearLayout.LayoutParams scaledLayoutParams = new LinearLayout.LayoutParams(
                    (int) (batteryWidth * iconScaleFactor), (int) (batteryHeight * iconScaleFactor));
                scaledLayoutParams.setMargins(0, 0, 0, defaultMarginBottom);
                mBatteryIconView.setLayoutParams(scaledLayoutParams);
            } else if (mBatteryStyle == BATTERY_STYLE_LANDSCAPEG) {
                int batteryWidth = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape_g);
                int batteryHeight = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape_g);
                LinearLayout.LayoutParams scaledLayoutParams = new LinearLayout.LayoutParams(
                    (int) (batteryWidth * iconScaleFactor), (int) (batteryHeight * iconScaleFactor));
                scaledLayoutParams.setMargins(0, 0, 0, defaultMarginBottom);
                mBatteryIconView.setLayoutParams(scaledLayoutParams);
            } else if (mBatteryStyle == BATTERY_STYLE_LANDSCAPEH) {
                int batteryWidth = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape_h);
                int batteryHeight = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape_h);
                LinearLayout.LayoutParams scaledLayoutParams = new LinearLayout.LayoutParams(
                    (int) (batteryWidth * iconScaleFactor), (int) (batteryHeight * iconScaleFactor));
                scaledLayoutParams.setMargins(0, 0, 0, defaultMarginBottom);
                mBatteryIconView.setLayoutParams(scaledLayoutParams);
            } else if (mBatteryStyle == BATTERY_STYLE_LANDSCAPEI) {
                int batteryWidth = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape_i);
                int batteryHeight = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape_i);
                LinearLayout.LayoutParams scaledLayoutParams = new LinearLayout.LayoutParams(
                    (int) (batteryWidth * iconScaleFactor), (int) (batteryHeight * iconScaleFactor));
                scaledLayoutParams.setMargins(0, 0, 0, defaultMarginBottom);
                mBatteryIconView.setLayoutParams(scaledLayoutParams);
            } else if (mBatteryStyle == BATTERY_STYLE_LANDSCAPEJ) {
                int batteryWidth = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape_j);
                int batteryHeight = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape_j);
                LinearLayout.LayoutParams scaledLayoutParams = new LinearLayout.LayoutParams(
                    (int) (batteryWidth * iconScaleFactor), (int) (batteryHeight * iconScaleFactor));
                scaledLayoutParams.setMargins(0, 0, 0, defaultMarginBottom);
                mBatteryIconView.setLayoutParams(scaledLayoutParams);
            } else if (mBatteryStyle == BATTERY_STYLE_LANDSCAPEK) {
                int batteryWidth = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape_k);
                int batteryHeight = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape_k);
                LinearLayout.LayoutParams scaledLayoutParams = new LinearLayout.LayoutParams(
                    (int) (batteryWidth * iconScaleFactor), (int) (batteryHeight * iconScaleFactor));
                scaledLayoutParams.setMargins(0, 0, 0, defaultMarginBottom);
                mBatteryIconView.setLayoutParams(scaledLayoutParams);
            } else if (mBatteryStyle == BATTERY_STYLE_LANDSCAPEL) {
                int batteryWidth = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape_l);
                int batteryHeight = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape_l);
                LinearLayout.LayoutParams scaledLayoutParams = new LinearLayout.LayoutParams(
                    (int) (batteryWidth * iconScaleFactor), (int) (batteryHeight * iconScaleFactor));
                scaledLayoutParams.setMargins(0, 0, 0, defaultMarginBottom);
                mBatteryIconView.setLayoutParams(scaledLayoutParams);
            } else if (mBatteryStyle == BATTERY_STYLE_LANDSCAPEM) {
                int batteryWidth = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape_m);
                int batteryHeight = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape_m);
                LinearLayout.LayoutParams scaledLayoutParams = new LinearLayout.LayoutParams(
                    (int) (batteryWidth * iconScaleFactor), (int) (batteryHeight * iconScaleFactor));
                scaledLayoutParams.setMargins(0, 0, 0, defaultMarginBottom);
                mBatteryIconView.setLayoutParams(scaledLayoutParams);
            } else if (mBatteryStyle == BATTERY_STYLE_LANDSCAPEN) {
                int batteryWidth = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape_n);
                int batteryHeight = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape_n);
                LinearLayout.LayoutParams scaledLayoutParams = new LinearLayout.LayoutParams(
                    (int) (batteryWidth * iconScaleFactor), (int) (batteryHeight * iconScaleFactor));
                scaledLayoutParams.setMargins(0, 0, 0, defaultMarginBottom);
                mBatteryIconView.setLayoutParams(scaledLayoutParams);
            } else if (mBatteryStyle == BATTERY_STYLE_LANDSCAPEO) {
                int batteryWidth = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape_o);
                int batteryHeight = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape_o);
                LinearLayout.LayoutParams scaledLayoutParams = new LinearLayout.LayoutParams(
                    (int) (batteryWidth * iconScaleFactor), (int) (batteryHeight * iconScaleFactor));
                scaledLayoutParams.setMargins(0, 0, 0, defaultMarginBottom);
                mBatteryIconView.setLayoutParams(scaledLayoutParams);
            } else if (mBatteryStyle == BATTERY_STYLE_CIRCLE || mBatteryStyle == BATTERY_STYLE_DOTTED_CIRCLE) {
                int batteryWidth = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_circle_width);
                int batteryHeight = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height);
                LinearLayout.LayoutParams scaledLayoutParams = new LinearLayout.LayoutParams(
                    (int) (batteryWidth * iconScaleFactor), (int) (batteryHeight * iconScaleFactor));
                scaledLayoutParams.setMargins(0, 0, 0, defaultMarginBottom);
                mBatteryIconView.setLayoutParams(scaledLayoutParams);
            }
        }

        // If the battery is marked as overheated, we should display a shield indicating that the
        // battery is being "defended".
        boolean displayShield = mDisplayShieldEnabled && mIsOverheated;
        float fullBatteryIconHeight =
                BatterySpecs.getFullBatteryHeight(mainBatteryHeight, displayShield);
        float fullBatteryIconWidth =
                BatterySpecs.getFullBatteryWidth(mainBatteryWidth, displayShield);

        if (displayShield) {
            // If the shield is displayed, we need some extra marginTop so that the bottom of the
            // main icon is still aligned with the bottom of all the other system icons.
            int shieldHeightAddition = Math.round(fullBatteryIconHeight - mainBatteryHeight);
            // However, the other system icons have some embedded bottom padding that the battery
            // doesn't have, so we shouldn't move the battery icon down by the full amount.
            // See b/258672854.
            marginTop = shieldHeightAddition
                    - res.getDimensionPixelSize(R.dimen.status_bar_battery_extra_vertical_spacing);
        } else {
            marginTop = 0;
        }
    }

    private void updateDrawable() {
        switch (getBatteryStyle()) {
            case BATTERY_STYLE_PORTRAIT:
                mBatteryIconView.setImageDrawable(mThemedDrawable);
                mBatteryIconView.setVisibility(View.VISIBLE);
                break;
            case BATTERY_STYLE_CIRCLE:
            case BATTERY_STYLE_DOTTED_CIRCLE:
                mCircleDrawable.setMeterStyle(getBatteryStyle());
                mBatteryIconView.setImageDrawable(mCircleDrawable);
                break;
            case BATTERY_STYLE_LANDSCAPEA:
                mBatteryIconView.setImageDrawable(mLandscapeBatteryA);
                mBatteryIconView.setVisibility(View.VISIBLE);
                break;
            case BATTERY_STYLE_LANDSCAPEB:
                mBatteryIconView.setImageDrawable(mLandscapeBatteryB);
                mBatteryIconView.setVisibility(View.VISIBLE);
                break;
            case BATTERY_STYLE_LANDSCAPEC:
                mBatteryIconView.setImageDrawable(mLandscapeBatteryC);
                mBatteryIconView.setVisibility(View.VISIBLE);
                break;
            case BATTERY_STYLE_LANDSCAPED:
                mBatteryIconView.setImageDrawable(mLandscapeBatteryD);
                mBatteryIconView.setVisibility(View.VISIBLE);
                break;
            case BATTERY_STYLE_LANDSCAPEE:
                mBatteryIconView.setImageDrawable(mLandscapeBatteryE);
                mBatteryIconView.setVisibility(View.VISIBLE);
                break;
            case BATTERY_STYLE_LANDSCAPEF:
                mBatteryIconView.setImageDrawable(mLandscapeBatteryF);
                mBatteryIconView.setVisibility(View.VISIBLE);
                break;
            case BATTERY_STYLE_LANDSCAPEG:
                mBatteryIconView.setImageDrawable(mLandscapeBatteryG);
                mBatteryIconView.setVisibility(View.VISIBLE);
                break;
            case BATTERY_STYLE_LANDSCAPEH:
                mBatteryIconView.setImageDrawable(mLandscapeBatteryH);
                mBatteryIconView.setVisibility(View.VISIBLE);
                break;
            case BATTERY_STYLE_LANDSCAPEI:
                mBatteryIconView.setImageDrawable(mLandscapeBatteryI);
                mBatteryIconView.setVisibility(View.VISIBLE);
                break;
            case BATTERY_STYLE_LANDSCAPEJ:
                mBatteryIconView.setImageDrawable(mLandscapeBatteryJ);
                mBatteryIconView.setVisibility(View.VISIBLE);
                break;
            case BATTERY_STYLE_LANDSCAPEK:
                mBatteryIconView.setImageDrawable(mLandscapeBatteryK);
                break;
            case BATTERY_STYLE_LANDSCAPEL:
                mBatteryIconView.setImageDrawable(mLandscapeBatteryL);
                break;
            case BATTERY_STYLE_LANDSCAPEM:
                mBatteryIconView.setImageDrawable(mLandscapeBatteryM);
                break;
            case BATTERY_STYLE_LANDSCAPEN:
                mBatteryIconView.setImageDrawable(mLandscapeBatteryN);
                break;
            case BATTERY_STYLE_LANDSCAPEO:
                mBatteryIconView.setImageDrawable(mLandscapeBatteryO);
                break;
            case BATTERY_STYLE_TEXT:
                mBatteryIconView.setVisibility(View.GONE);
                mBatteryIconView.setImageDrawable(null);
                break;
        }
    }

    private int getBatteryStyle() {
        return Settings.Secure.getIntForUser(getContext().getContentResolver(),
                Settings.Secure.STATUS_BAR_BATTERY_STYLE, BATTERY_STYLE_PORTRAIT,
                UserHandle.USER_CURRENT);
    }

    @Override
    public void onDarkChanged(ArrayList<Rect> areas, float darkIntensity, int tint) {
        float intensity = DarkIconDispatcher.isInAreas(areas, this) ? darkIntensity : 0;
        mNonAdaptedSingleToneColor = mDualToneHandler.getSingleColor(intensity);
        mNonAdaptedForegroundColor = mDualToneHandler.getFillColor(intensity);
        mNonAdaptedBackgroundColor = mDualToneHandler.getBackgroundColor(intensity);

        updateColors(mNonAdaptedForegroundColor, mNonAdaptedBackgroundColor,
                mNonAdaptedSingleToneColor);
    }

    /**
     * Sets icon and text colors. This will be overridden by {@code onDarkChanged} events,
     * if registered.
     *
     * @param foregroundColor
     * @param backgroundColor
     * @param singleToneColor
     */
    public void updateColors(int foregroundColor, int backgroundColor, int singleToneColor) {
        mAccessorizedDrawable.setColors(foregroundColor, backgroundColor, singleToneColor);
        mCircleDrawable.setColors(foregroundColor, backgroundColor, singleToneColor);
        mThemedDrawable.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeBatteryA.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeBatteryB.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeBatteryC.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeBatteryD.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeBatteryE.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeBatteryF.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeBatteryG.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeBatteryH.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeBatteryI.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeBatteryJ.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeBatteryK.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeBatteryL.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeBatteryM.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeBatteryN.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeBatteryO.setColors(foregroundColor, backgroundColor, singleToneColor);
        mTextColor = singleToneColor;
        if (mBatteryPercentView != null) {
            mBatteryPercentView.setTextColor(singleToneColor);
        }

        if (mUnknownStateDrawable != null) {
            mUnknownStateDrawable.setTint(singleToneColor);
        }
        
        if (mChargingIconView != null) {
            mChargingIconView.setImageTintList(android.content.res.ColorStateList.valueOf(singleToneColor));
        }
    }

    public void dump(PrintWriter pw, String[] args) {
        String powerSave = mThemedDrawable == null ?
                null : mThemedDrawable.getPowerSaveEnabled() + "";
        CharSequence percent = mBatteryPercentView == null ? null : mBatteryPercentView.getText();
        pw.println("  BatteryMeterView:");
        pw.println("    getPowerSave: " + powerSave);
        pw.println("    mBatteryPercentView.getText(): " + percent);
        pw.println("    mTextColor: #" + Integer.toHexString(mTextColor));
        pw.println("    mBatteryStateUnknown: " + mBatteryStateUnknown);
        pw.println("    mLevel: " + mLevel);
    }

    @VisibleForTesting
    CharSequence getBatteryPercentViewText() {
        return mBatteryPercentView.getText();
    }

    /** An interface that will fetch the estimated time remaining for the user's battery. */
    public interface BatteryEstimateFetcher {
        void fetchBatteryTimeRemainingEstimate(
                BatteryController.EstimateFetchCompletion completion);
    }
}

