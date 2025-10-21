package io.github.shio2077.shingen;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Path;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public abstract class MyAccessibilityService extends AccessibilityService {

    private static final String TAG = "MyAccessibilityService";
    private BroadcastReceiver clickReceiver;

    //@Override
    //protected void onServiceConnected() {
    //    super.onServiceConnected();
    //    Log.d(TAG, "Accessibility Service Connected and ready.");

    //    clickReceiver = new BroadcastReceiver() {
    //        @Override
    //        public void onReceive(Context context, Intent intent) {
    //            if (intent != null && ScreenCaptureHelper.ACTION_PERFORM_CLICK.equals(intent.getAction())) {
    //                int x = intent.getIntExtra(ScreenCaptureHelper.EXTRA_CLICK_X, -1);
    //                int y = intent.getIntExtra(ScreenCaptureHelper.EXTRA_CLICK_Y, -1);

    //                if (x != -1 && y != -1) {
    //                    Log.d(TAG, "Received click request. Performing click at: (" + x + ", " + y + ")");
    //                    //performScreenClick(x, y);
    //                }
    //            }
    //        }
    //    };

    //    LocalBroadcastManager.getInstance(this).registerReceiver(
    //            clickReceiver,
    //            new IntentFilter(ScreenCaptureHelper.ACTION_PERFORM_CLICK)
    //    );
    //}

    //private void performScreenClick(int x, int y) {
    //    Path clickPath = new Path();
    //    clickPath.moveTo(x, y);
    //    GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
    //    // --- FIX: Increased gesture duration to 50ms ---
    //    gestureBuilder.addStroke(new GestureDescription.StrokeDescription(clickPath, 0, 10L));

    //    dispatchGesture(gestureBuilder.build(), new GestureResultCallback() {
    //        @Override
    //        public void onCompleted(GestureDescription gestureDescription) {
    //            super.onCompleted(gestureDescription);
    //            Log.d(TAG, "Click gesture completed successfully.");
    //        }

    //        @Override
    //        public void onCancelled(GestureDescription gestureDescription) {
    //            super.onCancelled(gestureDescription);
    //            Log.e(TAG, "Click gesture was cancelled.");
    //        }
    //    }, null);
    //}

    //@Override
    //public void onAccessibilityEvent(AccessibilityEvent event) {
    //    // Not used for this task
    //}

    //@Override
    //public void onInterrupt() {
    //    Log.w(TAG, "Accessibility Service Interrupted.");
    //}

    //@Override
    //public boolean onUnbind(Intent intent) {
    //    Log.d(TAG, "Accessibility Service is being unbound.");
    //    if (clickReceiver != null) {
    //        LocalBroadcastManager.getInstance(this).unregisterReceiver(clickReceiver);
    //        clickReceiver = null;
    //    }
    //    return super.onUnbind(intent);
    //}
}
