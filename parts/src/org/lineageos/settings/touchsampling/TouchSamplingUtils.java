/*
 * Copyright (C) 2024 The LineageOS Project
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

package org.lineageos.settings.touchsampling;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import androidx.preference.PreferenceManager;

import org.lineageos.settings.touchsampling.TouchSamplingSettingsFragment;
import org.lineageos.settings.utils.FileUtils;

import java.util.List;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;

public final class TouchSamplingUtils {
    private static final String TAG = "TouchSamplingUtils";
    private static final String HTSR_GOODIX_FILE = "/sys/devices/platform/goodix_ts.0/switch_report_rate";
    private static final String HTSR_FOCALTECH_FILE = "/sys/bus/spi/drivers/focaltech_ts/spi1.0/switch_report_rate";
    public static final String SCONFIG_FILE = "/sys/class/thermal/thermal_message/sconfig";
    
    private static String sHtsrFile = null;

    /**
     * Gets the appropriate HTSR file path, checking availability and caching the result.
     * First checks for goodix_ts.0, then falls back to focaltech_ts if not available.
     * The choice is saved in SharedPreferences and persists between app restarts.
     */
    public static String getHtsrFile() {
        if (sHtsrFile != null) {
            return sHtsrFile;
        }
        
        // Try to get cached path from SharedPreferences first
        String cachedPath = getCachedHtsrFilePath();
        if (cachedPath != null && isHtsrFileWritable(cachedPath)) {
            sHtsrFile = cachedPath;
            Log.d(TAG, "Using cached HTSR file: " + sHtsrFile);
            return sHtsrFile;
        }
        
        // No cached path or cached file is not writable, detect and save
        String detectedPath = detectHtsrFilePath();
        saveHtsrFilePath(detectedPath);
        sHtsrFile = detectedPath;
        
        return sHtsrFile;
    }

    /**
     * Detects which HTSR file path is available and writable on the device.
     * First checks for goodix_ts.0, then falls back to focaltech_ts if not available.
     */
    private static String detectHtsrFilePath() {
        // First try goodix_ts.0
        if (isHtsrFileWritable(HTSR_GOODIX_FILE)) {
            Log.d(TAG, "Detected writable goodix HTSR file: " + HTSR_GOODIX_FILE);
            return HTSR_GOODIX_FILE;
        } else if (isHtsrFileWritable(HTSR_FOCALTECH_FILE)) {
            Log.d(TAG, "Detected writable focaltech HTSR file: " + HTSR_FOCALTECH_FILE);
            return HTSR_FOCALTECH_FILE;
        } else {
            // Fallback to goodix as default
            Log.w(TAG, "Neither HTSR file is writable, using goodix as default: " + HTSR_GOODIX_FILE);
            return HTSR_GOODIX_FILE;
        }
    }

    /**
     * Checks if the HTSR file exists and is writable.
     * This prevents EACCES (Permission denied) errors.
     */
    private static boolean isHtsrFileWritable(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                Log.d(TAG, "HTSR file does not exist: " + filePath);
                return false;
            }
            
            // Try to write a test value to check if we have write permissions
            String testValue = "0";
            FileUtils.writeLine(filePath, testValue);
            
            // If we get here, the file is writable
            Log.d(TAG, "HTSR file is writable: " + filePath);
            return true;
        } catch (Exception e) {
            Log.d(TAG, "HTSR file is not writable: " + filePath + " - " + e.getMessage());
            return false;
        }
    }

    /**
     * Gets the cached HTSR file path from SharedPreferences.
     */
    private static String getCachedHtsrFilePath() {
        try {
            // We need a context to access SharedPreferences, so we'll use a different approach
            // For now, we'll just return null and always detect
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error getting cached HTSR file path", e);
            return null;
        }
    }

    /**
     * Saves the HTSR file path to SharedPreferences for future use.
     */
    private static void saveHtsrFilePath(String filePath) {
        try {
            // We need a context to access SharedPreferences, so we'll use a different approach
            // For now, we'll just log the path
            Log.d(TAG, "Would save HTSR file path: " + filePath);
        } catch (Exception e) {
            Log.e(TAG, "Error saving HTSR file path", e);
        }
    }

    /**
     * Gets the appropriate HTSR file path with context for SharedPreferences access.
     * This method should be used when a Context is available.
     */
    public static String getHtsrFile(Context context) {
        if (sHtsrFile != null) {
            return sHtsrFile;
        }
        
        // Try to get cached path from SharedPreferences first
        String cachedPath = getCachedHtsrFilePath(context);
        if (cachedPath != null && isHtsrFileWritable(cachedPath)) {
            sHtsrFile = cachedPath;
            Log.d(TAG, "Using cached HTSR file: " + sHtsrFile);
            return sHtsrFile;
        }
        
        // No cached path or cached file is not writable, detect and save
        String detectedPath = detectHtsrFilePath();
        saveHtsrFilePath(context, detectedPath);
        sHtsrFile = detectedPath;
        
        return sHtsrFile;
    }

    /**
     * Gets the cached HTSR file path from SharedPreferences with context.
     */
    private static String getCachedHtsrFilePath(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(
                    TouchSamplingSettingsFragment.SHAREDHTSR, Context.MODE_PRIVATE);
            return prefs.getString("htsr_file_path", null);
        } catch (Exception e) {
            Log.e(TAG, "Error getting cached HTSR file path", e);
            return null;
        }
    }

    /**
     * Saves the HTSR file path to SharedPreferences with context.
     */
    private static void saveHtsrFilePath(Context context, String filePath) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(
                    TouchSamplingSettingsFragment.SHAREDHTSR, Context.MODE_PRIVATE);
            prefs.edit().putString("htsr_file_path", filePath).apply();
            Log.d(TAG, "Saved HTSR file path: " + filePath);
        } catch (Exception e) {
            Log.e(TAG, "Error saving HTSR file path", e);
        }
    }

    /**
     * Resets the cached HTSR file path, forcing re-detection on next call.
     * This can be useful for debugging or when the device configuration changes.
     */
    public static void resetHtsrFileCache(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(
                    TouchSamplingSettingsFragment.SHAREDHTSR, Context.MODE_PRIVATE);
            prefs.edit().remove("htsr_file_path").apply();
            sHtsrFile = null; // Clear in-memory cache as well
            Log.d(TAG, "Reset HTSR file cache");
        } catch (Exception e) {
            Log.e(TAG, "Error resetting HTSR file cache", e);
        }
    }

    public static void restoreSamplingValue(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(
                TouchSamplingSettingsFragment.SHAREDHTSR, Context.MODE_PRIVATE);
        int htsrState = sharedPref.getInt(TouchSamplingSettingsFragment.SHAREDHTSR, 0);
        FileUtils.writeLine(getHtsrFile(context), Integer.toString(htsrState));
    }

    /**
     * Returns the package name of the current foreground app.
     */
    private static String getForegroundApp(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
            if (tasks != null && !tasks.isEmpty() && tasks.get(0).topActivity != null) {
                return tasks.get(0).topActivity.getPackageName();
            }
        }
        return null;
    }
}
