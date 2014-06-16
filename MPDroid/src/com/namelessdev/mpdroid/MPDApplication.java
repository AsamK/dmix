/*
 * Copyright (C) 2010-2014 The MPDroid Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.namelessdev.mpdroid;

import com.namelessdev.mpdroid.helpers.MPDAsyncHelper;
import com.namelessdev.mpdroid.helpers.MPDAsyncHelper.ConnectionListener;
import com.namelessdev.mpdroid.helpers.UpdateTrackInfo;
import com.namelessdev.mpdroid.tools.SettingsHelper;

import org.a0z.mpd.MPD;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager.BadTokenException;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import static android.util.Log.w;

public class MPDApplication extends Application implements ConnectionListener {

    public class ApplicationState {
        public boolean streamingMode = false;
        public boolean notificationMode = false;
        public boolean persistentNotification = false;
    }

    class DialogClickListener implements OnClickListener {
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case AlertDialog.BUTTON_NEUTRAL:
                    // Show Settings
                    currentActivity.startActivityForResult(new Intent(currentActivity,
                            WifiConnectionSettings.class), SETTINGS);
                    break;
                case AlertDialog.BUTTON_NEGATIVE:
                    currentActivity.finish();
                    break;
                case AlertDialog.BUTTON_POSITIVE:
                    connectMPD();
                    break;

            }
        }
    }

    private static MPDApplication instance;

    public static final String TAG = "MPDroid";
    private static final long DISCONNECT_TIMER = 15000;
    public MPDAsyncHelper oMPDAsyncHelper = null;
    public UpdateTrackInfo updateTrackInfo = null;

    private SettingsHelper settingsHelper = null;
    private ApplicationState state = new ApplicationState();
    private Collection<Object> connectionLocks = new LinkedList<Object>();
    private AlertDialog ad;

    private boolean settingsShown = false;
    private boolean warningShown = false;

    private Activity currentActivity;

    private Timer disconnectSheduler;

    public static final int SETTINGS = 5;

    public static MPDApplication getInstance() {
        return instance;
    }

    private void cancelDisconnectSheduler() {
        disconnectSheduler.cancel();
        disconnectSheduler.purge();
        disconnectSheduler = new Timer();
    }

    private void checkConnectionNeeded() {
        if (connectionLocks.size() > 0) {
            if (!oMPDAsyncHelper.isMonitorAlive()) {
                oMPDAsyncHelper.startMonitor();
            }
            if (!oMPDAsyncHelper.oMPD.isConnected()
                    && (currentActivity == null || !currentActivity.getClass().equals(
                            WifiConnectionSettings.class))) {
                connect();
            }
        } else {
            disconnect();
        }
    }

    public void connect() {
        if (!settingsHelper.updateSettings()) {
            // Absolutely no settings defined! Open Settings!
            if (currentActivity != null && !settingsShown) {
                currentActivity.startActivityForResult(new Intent(currentActivity,
                        WifiConnectionSettings.class), SETTINGS);
                settingsShown = true;
            }
        }

        if (currentActivity != null && !settingsHelper.warningShown() && !warningShown) {
            currentActivity.startActivity(new Intent(currentActivity, WarningActivity.class));
            warningShown = true;
        }
        connectMPD();
    }

    public synchronized void connectionFailed(String message) {

        if (ad != null && !(ad instanceof ProgressDialog) && ad.isShowing()) {
            return;
        }

        // dismiss possible dialog
        dismissAlertDialog();

        oMPDAsyncHelper.disconnect();

        if (currentActivity == null)
            return;

        if (currentActivity != null && connectionLocks.size() > 0) {
            // are we in the settings activity?
            if (currentActivity.getClass() == SettingsActivity.class) {
                AlertDialog.Builder builder = new AlertDialog.Builder(currentActivity);
                builder.setCancelable(false);
                builder.setMessage(
                        getResources().getString(R.string.connectionFailedMessageSetting, message));
                builder.setPositiveButton("OK", new OnClickListener() {
                    public void onClick(DialogInterface arg0, int arg1) {
                    }
                });
                ad = builder.show();
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(currentActivity);
                builder.setTitle(R.string.connectionFailed);
                builder.setMessage(
                        getResources().getString(R.string.connectionFailedMessage, message));
                builder.setCancelable(false);

                DialogClickListener oDialogClickListener = new DialogClickListener();
                builder.setNegativeButton(R.string.quit, oDialogClickListener);
                builder.setNeutralButton(R.string.settings, oDialogClickListener);
                builder.setPositiveButton(R.string.retry, oDialogClickListener);

                try {
                    ad = builder.show();
                } catch (BadTokenException e) {
                    // Can't display it. Don't care.
                }
            }
        }

    }

    public synchronized void connectionSucceeded(String message) {
        dismissAlertDialog();
        // checkMonitorNeeded();
    }

    private void connectMPD() {
        // dismiss possible dialog
        dismissAlertDialog();

        // show connecting to server dialog
        if (currentActivity != null) {
            ad = new ProgressDialog(currentActivity);
            ad.setTitle(R.string.connecting);
            ad.setMessage(getResources().getString(R.string.connectingToServer));
            ad.setCancelable(false);
            ad.setOnKeyListener(new OnKeyListener() {
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    // Handle all keys!
                    return true;
                }
            });
            try {
                ad.show();
            } catch (BadTokenException e) {
                // Can't display it. Don't care.
            }
        }

        cancelDisconnectSheduler();

        // really connect
        oMPDAsyncHelper.connect();
    }

    public void disconnect() {
        cancelDisconnectSheduler();
        startDisconnectSheduler();
    }

    private void dismissAlertDialog() {
        if (ad != null) {
            if (ad.isShowing()) {
                try {
                    ad.dismiss();
                } catch (IllegalArgumentException e) {
                    // We don't care, it has already been destroyed
                }
            }
        }
    }

    public ApplicationState getApplicationState() {
        return state;
    }

    public boolean isLightThemeSelected() {
        return PreferenceManager.getDefaultSharedPreferences(this).getBoolean("lightTheme", false);
    }

    /**
     * isLocalAudible()
     *
     * @return Returns whether it is probable that the local audio
     * system will be playing audio controlled by this application.
     */
    public final boolean isLocalAudible() {
        return state.streamingMode ||
                "127.0.0.1".equals(oMPDAsyncHelper.getConnectionSettings().sServer);
    }

    public boolean isTabletUiEnabled() {
        return getResources().getBoolean(R.bool.isTablet)
                && PreferenceManager.getDefaultSharedPreferences(this).getBoolean("tabletUI", true);
    }

    public boolean isInSimpleMode() {
        return PreferenceManager.getDefaultSharedPreferences(this).getBoolean("simpleMode", false);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Log.d(MPDApplication.TAG, "onCreate Application");
        init(this);
    }

    public void init(Context context) {
        MPD.setApplicationContext(context);

        StrictMode.VmPolicy vmpolicy = new StrictMode.VmPolicy.Builder().penaltyLog().build();
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        StrictMode.setVmPolicy(vmpolicy);

        // Init the default preferences (meaning we won't have different defaults between code/xml)
        PreferenceManager.setDefaultValues(this, R.xml.settings, false);

        oMPDAsyncHelper = new MPDAsyncHelper();
        oMPDAsyncHelper.addConnectionListener(this);

        settingsHelper = new SettingsHelper(oMPDAsyncHelper);

        disconnectSheduler = new Timer();

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        if (!settings.contains("albumTrackSort"))
            settings.edit().putBoolean("albumTrackSort", true).commit();
    }

    public void setActivity(Object activity) {
        if (activity instanceof Activity)
            currentActivity = (Activity) activity;

        addConnectionLock(activity);
    }

    private void startDisconnectSheduler() {
        disconnectSheduler.schedule(new TimerTask() {
            @Override
            public void run() {
                w(TAG, "Disconnecting (" + DISCONNECT_TIMER + " ms timeout)");
                oMPDAsyncHelper.stopMonitor();
                oMPDAsyncHelper.disconnect();
            }
        }, DISCONNECT_TIMER);

    }

    public void terminateApplication() {
        this.currentActivity.finish();
    }

    public void unsetActivity(Object activity) {
        removeConnectionLock(activity);

        if (currentActivity == activity)
            currentActivity = null;
    }

    public void addConnectionLock(Object lockOwner) {
        connectionLocks.add(lockOwner);
        checkConnectionNeeded();
        cancelDisconnectSheduler();
    }

    public void removeConnectionLock(Object lockOwner) {
        connectionLocks.remove(lockOwner);
        checkConnectionNeeded();
    }
}
