package io.github.shio2077.shingen;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import rikka.shizuku.Shizuku;

public class MediaProjectionService extends Service {

    public static final String EXTRA_RESULT_CODE = "result_code";
    public static final String EXTRA_RESULT_DATA = "result_data";

    private static final String TAG = "MediaProjectionService";
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = NotificationUtils.CHANNEL_ID;

    private BroadcastReceiver clickReceiver;
    private IAdbClickService adbClickService;
    private Shizuku.UserServiceArgs userServiceArgs;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            adbClickService = IAdbClickService.Stub.asInterface(service);
            Log.d(TAG, "AdbClickService connected.");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            adbClickService = null;
            Log.d(TAG, "AdbClickService disconnected.");
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationUtils.createNotificationChannel(this);
        startForeground(NOTIFICATION_ID, createNotification());
        bindAdbClickService();

        clickReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent != null && ScreenCaptureHelper.ACTION_PERFORM_CLICK.equals(intent.getAction())) {
                    int x = intent.getIntExtra(ScreenCaptureHelper.EXTRA_CLICK_X, -1);
                    int y = intent.getIntExtra(ScreenCaptureHelper.EXTRA_CLICK_Y, -1);

                    if (x != -1 && y != -1 && adbClickService != null) {
                        Log.d(TAG, "Perform ADB click request at: (" + x + ", " + y + ")");
                        try {
                            String[] command = {"input", "tap", String.valueOf(x), String.valueOf(y)};
                            String result = adbClickService.execArr(command);
                            Log.d(TAG, "ADB click result: " + result);
                        } catch (RemoteException e) {
                            Log.e(TAG, "Failed to execute ADB click", e);
                        }
                    }
                }
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(clickReceiver, new IntentFilter(ScreenCaptureHelper.ACTION_PERFORM_CLICK));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            Log.e(TAG, "Intent is null, stopping service.");
            stopSelf();
            return START_NOT_STICKY;
        }

        int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
        Intent resultData = intent.getParcelableExtra(EXTRA_RESULT_DATA);

        if (resultCode == 0 || resultData == null) {
            Log.e(TAG, "Invalid media projection result data.");
            stopSelf();
            return START_NOT_STICKY;
        }

        ScreenCaptureHelper.getInstance().setContext(this);

        new Handler(Looper.getMainLooper()).post(() -> {
            ScreenCaptureHelper.getInstance().startScreenCapture(resultCode, resultData);
        });

        return START_STICKY;
    }

    private void bindAdbClickService() {
        userServiceArgs = new Shizuku.UserServiceArgs(new ComponentName(this, ClickHelperService.class))
                .processNameSuffix(":adb_click");
        Shizuku.bindUserService(userServiceArgs, serviceConnection);
    }

    private void unbindAdbClickService() {
        if (adbClickService != null) {
            Shizuku.unbindUserService(userServiceArgs, serviceConnection, true);
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("OpenCVTrail Service")
                .setContentText("Screen capture and analysis is active.")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setOngoing(true)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (clickReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(clickReceiver);
        }
        unbindAdbClickService();
        ScreenCaptureHelper.getInstance().stopScreenCapture();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
