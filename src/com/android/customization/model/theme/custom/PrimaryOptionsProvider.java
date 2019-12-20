/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.customization.model.theme.custom;

import static com.android.customization.model.ResourceConstants.PRIMARY_COLOR_NAME;
import static com.android.customization.model.ResourceConstants.PRIMARY_COLOR_DEFAULT_DARK_NAME;
import static com.android.customization.model.ResourceConstants.PRIMARY_COLOR_DEFAULT_LIGHT_NAME;
import static com.android.customization.model.ResourceConstants.ANDROID_PACKAGE;
import static com.android.customization.model.ResourceConstants.OVERLAY_CATEGORY_ANDROID_THEME;
import static com.android.customization.model.ResourceConstants.OVERLAY_CATEGORY_PRIMARY;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.util.Log;

import com.android.customization.model.ResourceConstants;
import com.android.customization.model.theme.OverlayManagerCompat;
import com.android.customization.model.theme.custom.ThemeComponentOption.PrimaryOption;
import com.android.wallpaper.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link ThemeComponentOptionProvider} that reads {@link PrimaryOption}s from
 * UI Styles overlays.
 */
public class PrimaryOptionsProvider extends ThemeComponentOptionProvider<PrimaryOption> {

    private static final String TAG = "PrimaryOptionsProvider";
    private final CustomThemeManager mCustomThemeManager;
    private final String mDefaultThemePackage;

    public PrimaryOptionsProvider(Context context, OverlayManagerCompat manager,
            CustomThemeManager customThemeManager) {
        super(context, manager, OVERLAY_CATEGORY_PRIMARY);
        mCustomThemeManager = customThemeManager;
        List<String> themePackages = manager.getOverlayPackagesForCategory(
                OVERLAY_CATEGORY_ANDROID_THEME, UserHandle.myUserId(), ANDROID_PACKAGE);
        mDefaultThemePackage = themePackages.isEmpty() ? null : themePackages.get(0);
    }

    @Override
    protected void loadOptions() {
        int accentColor = mCustomThemeManager.resolveAccentColor(mContext.getResources());
        int defaultPrimaryColor = getSystemDefaultPrimary();
        addDefault();

        for (String overlayPackage : mOverlayPackages) {
            try {
                Resources overlayRes = getOverlayResources(overlayPackage);
                int primaryColor = overlayRes.getColor(
                        overlayRes.getIdentifier(PRIMARY_COLOR_NAME, "color", overlayPackage),
                        null);
                if (primaryColor == defaultPrimaryColor) {
                    continue;
                }
                PackageManager pm = mContext.getPackageManager();
                String label = pm.getApplicationInfo(overlayPackage, 0).loadLabel(pm).toString();
                PrimaryOption option = new PrimaryOption(overlayPackage, label, primaryColor, accentColor);
                mOptions.add(option);
            } catch (NameNotFoundException | NotFoundException e) {
                Log.w(TAG, String.format("Couldn't load UI style overlay %s, will skip it",
                        overlayPackage), e);
            }
        }
    }

    int getSystemDefaultPrimary() {
        Configuration configuration = mContext.getResources().getConfiguration();
        boolean nightMode = (configuration.uiMode & Configuration.UI_MODE_NIGHT_MASK)
                    == Configuration.UI_MODE_NIGHT_YES ? true : false;
        Resources system = Resources.getSystem();
        int colorPrimary = system.getColor(
                system.getIdentifier(nightMode ? ResourceConstants.PRIMARY_COLOR_DEFAULT_DARK_NAME : ResourceConstants.PRIMARY_COLOR_DEFAULT_LIGHT_NAME, "color",
                ResourceConstants.ANDROID_PACKAGE), null);
        return colorPrimary;
    }

    private void addDefault() {
        Resources system = Resources.getSystem();
        int accentColor = mCustomThemeManager.resolveAccentColor(mContext.getResources());
        int primaryColor = getSystemDefaultPrimary();
        PrimaryOption option = new PrimaryOption(null,
                mContext.getString(R.string.default_theme_title), primaryColor, accentColor);
        mOptions.add(option);
    }
}
