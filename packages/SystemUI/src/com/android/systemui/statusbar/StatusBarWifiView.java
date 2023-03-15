/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.systemui.statusbar;

import static com.android.systemui.plugins.DarkIconDispatcher.getTint;
import static com.android.systemui.statusbar.StatusBarIconView.STATE_DOT;
import static com.android.systemui.statusbar.StatusBarIconView.STATE_HIDDEN;
import static com.android.systemui.statusbar.StatusBarIconView.STATE_ICON;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Rect;
import android.provider.Settings;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.plugins.DarkIconDispatcher.DarkReceiver;
import com.android.systemui.statusbar.phone.StatusBarSignalPolicy.WifiIconState;
import com.android.systemui.tuner.TunerService;

import java.util.ArrayList;

/**
 * Start small: StatusBarWifiView will be able to layout from a WifiIconState
 */
public class StatusBarWifiView extends BaseStatusBarFrameLayout implements DarkReceiver,TunerService.Tunable {
    private static final String TAG = "StatusBarWifiView";
    
    private static final String IDC_WIFI_HEIGHT =
            "system:" + "IDC_WIFI_HEIGHT";     
    private static final String IDC_WIFI_WIDTH =
            "system:" + "IDC_WIFI_WIDTH";
    private static final String IDC_WIFI_TOP =
            "system:" + "IDC_WIFI_TOP";            
    private static final String IDC_WIFI_CUSTOM_DIMENSION =
            "system:" + "IDC_WIFI_CUSTOM_DIMENSION";

    /// Used to show etc dots
    private StatusBarIconView mDotView;
    /// Contains the main icon layout
    private LinearLayout mWifiGroup;
    private ImageView mWifiIcon;
    private ImageView mWifiStandard;
    private ImageView mIn;
    private ImageView mOut;
    private View mInoutContainer;
    private View mSignalSpacer;
    private View mAirplaneSpacer;
    private WifiIconState mState;
    private String mSlot;
    @StatusBarIconView.VisibleState
    private int mVisibleState = STATE_HIDDEN;
    private boolean mShowWifiStandard;
    private WifiManager mWifiManager;
    
    private boolean useIdcCustomDimension;           
    private int idcWifiHeight;
    private int idcWifiWidth; 
    private int idcWifiTop;

    public static StatusBarWifiView fromContext(Context context, String slot) {
        LayoutInflater inflater = LayoutInflater.from(context);
        StatusBarWifiView v = (StatusBarWifiView) inflater.inflate(R.layout.status_bar_wifi_group, null);
        v.setSlot(slot);
        v.init();
        v.setVisibleState(STATE_ICON);
        return v;
    }

    public StatusBarWifiView(Context context) {
        this(context, null);
    }

    public StatusBarWifiView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StatusBarWifiView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mWifiManager = context.getSystemService(WifiManager.class);
    }

    public void setSlot(String slot) {
        mSlot = slot;
    }

    @Override
    public void setStaticDrawableColor(int color) {
        ColorStateList list = ColorStateList.valueOf(color);
        mWifiIcon.setImageTintList(list);
        mWifiStandard.setImageTintList(list);
        mIn.setImageTintList(list);
        mOut.setImageTintList(list);
        mDotView.setDecorColor(color);
    }

    @Override
    public void setDecorColor(int color) {
        mDotView.setDecorColor(color);
    }

    @Override
    public String getSlot() {
        return mSlot;
    }

    @Override
    public boolean isIconVisible() {
        return mState != null && mState.visible;
    }

    @Override
    public void setVisibleState(@StatusBarIconView.VisibleState int state, boolean animate) {
        if (state == mVisibleState) {
            return;
        }
        mVisibleState = state;

        switch (state) {
            case STATE_ICON:
                mWifiGroup.setVisibility(View.VISIBLE);
                mDotView.setVisibility(View.GONE);
                break;
            case STATE_DOT:
                mWifiGroup.setVisibility(View.GONE);
                mDotView.setVisibility(View.VISIBLE);
                break;
            case STATE_HIDDEN:
            default:
                mWifiGroup.setVisibility(View.GONE);
                mDotView.setVisibility(View.GONE);
                break;
        }
    }

    @Override
    @StatusBarIconView.VisibleState
    public int getVisibleState() {
        return mVisibleState;
    }

    @Override
    public void getDrawingRect(Rect outRect) {
        super.getDrawingRect(outRect);
        float translationX = getTranslationX();
        float translationY = getTranslationY();
        outRect.left += translationX;
        outRect.right += translationX;
        outRect.top += translationY;
        outRect.bottom += translationY;
    }

    private void init() {
        mWifiGroup = findViewById(R.id.wifi_group);
        mWifiIcon = findViewById(R.id.wifi_signal);
        mWifiStandard = findViewById(R.id.wifi_standard);
        mIn = findViewById(R.id.wifi_in);
        mOut = findViewById(R.id.wifi_out);
        mSignalSpacer = findViewById(R.id.wifi_signal_spacer);
        mAirplaneSpacer = findViewById(R.id.wifi_airplane_spacer);
        mInoutContainer = findViewById(R.id.inout_container);

        initDotView();
        
        Dependency.get(TunerService.class).addTunable(this,
                IDC_WIFI_HEIGHT, IDC_WIFI_WIDTH, IDC_WIFI_CUSTOM_DIMENSION, IDC_WIFI_TOP);   
    }

    private void setMobileWifiDimension(boolean idcganteng) {
        ViewGroup.LayoutParams p = mWifiIcon.getLayoutParams();
        if (!idcganteng) {    
            if (!useIdcCustomDimension) {
                if (p != null) {       
                p.width = mContext.getResources().getDimensionPixelSize(
                      R.dimen.status_bar_wifi_signal_size);
                p.height = mContext.getResources().getDimensionPixelSize(
                      R.dimen.status_bar_wifi_signal_size);
                mWifiIcon.setPadding(0, 0, 0, 0);               
               } else {
                p = new ViewGroup.LayoutParams(mContext.getResources().getDimensionPixelSize(
                      R.dimen.status_bar_wifi_signal_size), mContext.getResources().getDimensionPixelSize(
                      R.dimen.status_bar_wifi_signal_size));  
                mWifiIcon.setPadding(0, 0, 0, 0);  
               }         
            } else {
               if (p != null) {        
                p.height = (int) idcWifiHeight;        
                p.width = (int) idcWifiWidth;
                int paddingtop =  (int) idcWifiTop;
                mWifiIcon.setPadding(0, paddingtop, 0, 0);       
               } else {
                mWifiIcon.setPadding(0, 0, 0, 0);              
                p = new ViewGroup.LayoutParams((int) idcWifiWidth, (int) idcWifiHeight);
               }         
            }
        } else {  
            if (p != null) {           
            p.width = mContext.getResources().getDimensionPixelSize(
                      R.dimen.status_bar_icon_size);
            p.height = mContext.getResources().getDimensionPixelSize(
                      R.dimen.status_bar_icon_size);
            mWifiIcon.setPadding(0, 0, 0, 0);          
            } else {
                mWifiIcon.setPadding(0, 0, 0, 0);      
                p = new ViewGroup.LayoutParams(mContext.getResources().getDimensionPixelSize(
                      R.dimen.status_bar_wifi_signal_size), mContext.getResources().getDimensionPixelSize(
                      R.dimen.status_bar_wifi_signal_size));
            } 
        }        
        mWifiIcon.setLayoutParams(p);
    }

    private void initDotView() {
        mDotView = new StatusBarIconView(mContext, mSlot, null);
        mDotView.setVisibleState(STATE_DOT);

        int width = mContext.getResources().getDimensionPixelSize(R.dimen.status_bar_icon_size);
        LayoutParams lp = new LayoutParams(width, width);
        lp.gravity = Gravity.CENTER_VERTICAL | Gravity.START;
        addView(mDotView, lp);
    }

    public void applyWifiState(WifiIconState state) {
        boolean requestLayout = false;

        if (state == null) {
            requestLayout = getVisibility() != View.GONE;
            setVisibility(View.GONE);
            mState = null;
        } else if (mState == null) {
            requestLayout = true;
            mState = state.copy();
            initViewState();
        } else if (!mState.equals(state)) {
            requestLayout = updateState(state.copy());
        }

        if (requestLayout) {
            requestLayout();
        }
    }

    private boolean updateState(WifiIconState state) {
        setContentDescription(state.contentDescription);
        if (mShowWifiStandard) {
            mWifiStandard.setVisibility(View.VISIBLE);
            setWifiStandard();
        } else {
            mWifiStandard.setVisibility(View.GONE);
        }
        if (mState.resId != state.resId && state.resId >= 0) {
            mWifiIcon.setImageDrawable(mContext.getDrawable(state.resId));
        }

        mIn.setVisibility(state.activityIn ? View.VISIBLE : View.GONE);
        mOut.setVisibility(state.activityOut ? View.VISIBLE : View.GONE);
        mInoutContainer.setVisibility(
                (state.activityIn || state.activityOut) ? View.VISIBLE : View.GONE);
        mAirplaneSpacer.setVisibility(state.airplaneSpacerVisible ? View.VISIBLE : View.GONE);
        mSignalSpacer.setVisibility(state.signalSpacerVisible ? View.VISIBLE : View.GONE);

        boolean needsLayout = state.activityIn != mState.activityIn
                ||state.activityOut != mState.activityOut;

        if (mState.visible != state.visible) {
            needsLayout |= true;
            setVisibility(state.visible ? View.VISIBLE : View.GONE);
        }

        mState = state;
        return needsLayout;
    }

    private void initViewState() {
        setContentDescription(mState.contentDescription);
        if (mShowWifiStandard) {
            mWifiStandard.setVisibility(View.VISIBLE);
            setWifiStandard();
        } else {
            mWifiStandard.setVisibility(View.GONE);
        }
        if (mState.resId >= 0) {
            mWifiIcon.setImageDrawable(mContext.getDrawable(mState.resId));
        }

        mIn.setVisibility(mState.activityIn ? View.VISIBLE : View.GONE);
        mOut.setVisibility(mState.activityOut ? View.VISIBLE : View.GONE);
        mInoutContainer.setVisibility(
                (mState.activityIn || mState.activityOut) ? View.VISIBLE : View.GONE);
        mAirplaneSpacer.setVisibility(mState.airplaneSpacerVisible ? View.VISIBLE : View.GONE);
        mSignalSpacer.setVisibility(mState.signalSpacerVisible ? View.VISIBLE : View.GONE);
        setVisibility(mState.visible ? View.VISIBLE : View.GONE);
    }

    private void setWifiStandard() {
        int wifiStandard = getWifiStandard(mState);
        if (wifiStandard >= 4) {
            int identifier = getResources().getIdentifier("ic_wifi_standard_" + wifiStandard,
                    "drawable", getContext().getPackageName());
            if (identifier > 0) {
                mWifiStandard.setImageDrawable(mContext.getDrawable(identifier));
            }
        }
    }

    private int getWifiStandard(WifiIconState state) {
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        return state.visible ? wifiInfo.getWifiStandard() : -1;
    }

    @Override
    public void onDarkChanged(ArrayList<Rect> areas, float darkIntensity, int tint) {
        int areaTint = getTint(areas, this, tint);
        ColorStateList color = ColorStateList.valueOf(areaTint);
        mWifiIcon.setImageTintList(color);
        mWifiStandard.setImageTintList(color);
        mIn.setImageTintList(color);
        mOut.setImageTintList(color);
        mDotView.setDecorColor(areaTint);
        mDotView.setIconColor(areaTint, false);
    }

    @Override
    public String toString() {
        return "StatusBarWifiView(slot=" + mSlot + " state=" + mState + ")";
    }
    
    @Override
    public void onTuningChanged(String key, String newValue) {
        if (IDC_WIFI_HEIGHT.equals(key)) {
            int midcWifiHeight = TunerService.parseInteger(newValue, 15);
            idcWifiHeight = Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, midcWifiHeight,
                getResources().getDisplayMetrics()));        
            setMobileWifiDimension(false); 
        } else if (IDC_WIFI_WIDTH.equals(key)) {
            int midcWifiWidth = TunerService.parseInteger(newValue, 15);
            idcWifiWidth = Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, midcWifiWidth,
                getResources().getDisplayMetrics()));   
            setMobileWifiDimension(false); 
        } else if (IDC_WIFI_TOP.equals(key)) {
            int midcWifiTop = TunerService.parseInteger(newValue, 0);
            idcWifiTop = Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, midcWifiTop,
                getResources().getDisplayMetrics()));   
            setMobileWifiDimension(false); 
        } else if (IDC_WIFI_CUSTOM_DIMENSION.equals(key)) {
            useIdcCustomDimension = TunerService.parseIntegerSwitch(newValue, false);
            setMobileWifiDimension(false); 
        }
    }

    public void updateWifiState(boolean showWifiStandard) {
        boolean needsLayout = false;
        if (mShowWifiStandard != showWifiStandard) {
            if (showWifiStandard) {
                mWifiStandard.setVisibility(View.VISIBLE);
                setWifiStandard();
            } else {
                mWifiStandard.setVisibility(View.GONE);
            }
        }

        mIn.setVisibility(mState.activityIn ? View.VISIBLE : View.GONE);
        mOut.setVisibility(mState.activityOut ? View.VISIBLE : View.GONE);
        mInoutContainer.setVisibility(
                (mState.activityIn || mState.activityOut) ? View.VISIBLE : View.GONE);
        mAirplaneSpacer.setVisibility(mState.airplaneSpacerVisible ? View.VISIBLE : View.GONE);
        mSignalSpacer.setVisibility(mState.signalSpacerVisible ? View.VISIBLE : View.GONE);
        setVisibility(mState.visible ? View.VISIBLE : View.GONE);

        needsLayout = mShowWifiStandard != showWifiStandard;
        mShowWifiStandard = showWifiStandard;

        if (needsLayout) {
            requestLayout();
        }
    }
}
