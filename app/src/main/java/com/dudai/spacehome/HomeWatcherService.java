package com.dudai.spacehome;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;

/**
 * On Google TV the Home button always opens Google's own home screen.
 * This service notices when that screen comes up and immediately opens our space home screen instead.
 *
 * It only listens for "a new window opened" events and only reacts to Google's launcher.
 */
public class HomeWatcherService extends AccessibilityService {

    /** Google TV's home screen app. */
    private static final String GOOGLE_HOME_PACKAGE = "com.google.android.apps.tv.launcherx";

    /** Avoids opening our screen several times in a row for one button press. */
    private static final long MIN_GAP_MS = 800;

    private long lastLaunch;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        if (!GOOGLE_HOME_PACKAGE.contentEquals(String.valueOf(event.getPackageName()))) return;

        // Only the home screen itself, not other screens Google's launcher may show
        CharSequence cls = event.getClassName();
        if (cls != null && !cls.toString().contains("HomeActivity")) return;

        long now = SystemClock.elapsedRealtime();
        if (now - lastLaunch < MIN_GAP_MS) return;
        lastLaunch = now;

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }

    @Override
    public void onInterrupt() {
        // nothing to stop
    }
}
