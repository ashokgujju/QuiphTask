package com.quiphtask;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

/**
 * Created by ashok on 19/12/17.
 */

public class QuiphTaskService extends AccessibilityService {
    public static final int NOTIFICATION_ID = 112;
    static final String TAG = QuiphTaskService.class.getSimpleName();

    private Boolean flag = false;
    private boolean isShowingNotification = false;

    private boolean hasWebView(AccessibilityNodeInfo info, int depth) {
        if (info == null) return false;
        if (info.getClassName().equals("android.webkit.WebView")) return true;

        for (int i = 0; i < info.getChildCount(); i++) {
            if (hasWebView(info.getChild(i), depth + 1)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasEditTextWithUrl() {
        try {
            List<AccessibilityNodeInfo> nodes = getRootInActiveWindow()
                    .findAccessibilityNodeInfosByText("www.quiph.com");
            if (nodes != null && nodes.size() > 0) {
                for (AccessibilityNodeInfo info : nodes) {
                    if (info.getClassName().equals("android.widget.EditText")) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return false;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        //Ignore notification ui events
        if (event.getPackageName().equals("com.android.systemui"))
            return;

        try {
            if (hasWebView(getRootInActiveWindow(), 0)) {
                if (hasEditTextWithUrl()) {
                    showNotification();
                    flag = true;
                } else {
                    if (flag) {
                        showNotification();
                    } else {
                        hideNotification();
                    }
                }
            } else {
                flag = hasEditTextWithUrl();
                if (flag) {
                    showNotification();
                } else {
                    hideNotification();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            flag = false;
            hideNotification();
        }
    }

    @Override
    public void onInterrupt() {
        Log.v(TAG, "onInterrupt");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        Log.v(TAG, "onServiceConnected");
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.flags = AccessibilityServiceInfo.DEFAULT;
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        setServiceInfo(info);
    }

    private void showNotification() {
        if (isShowingNotification) return;
        isShowingNotification = true;

        Notification.Builder mBuilder =
                new Notification.Builder(this)
                        .setSmallIcon(R.drawable.ic_stat_name)
                        .setOngoing(true)
                        .setContentIntent(getPendingIntent())
                        .setContentTitle("Quiph Task");
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (mNotificationManager != null) {
            mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
        }
    }

    private PendingIntent getPendingIntent() {
        //TODO change package name and create pending intent
        if (isPackageInstalled("com.quiph.package")) {
            return null;
        } else {
            Intent intent;
            try {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.ashokgujju.newsonair"));
            } catch (android.content.ActivityNotFoundException e) {
                intent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=com.ashokgujju.newsonair"));
            }

            return PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            );
        }
    }

    private boolean isPackageInstalled(String packageName) {
        try {
            getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void hideNotification() {
        if (!isShowingNotification) return;
        isShowingNotification = false;

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (mNotificationManager != null) {
            mNotificationManager.cancel(NOTIFICATION_ID);
        }
    }
}
