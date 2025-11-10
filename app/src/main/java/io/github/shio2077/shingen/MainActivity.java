package io.github.shio2077.shingen;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.opencv.android.OpenCVLoader;

import rikka.shizuku.Shizuku;

public class MainActivity extends AppCompatActivity implements ScreenCaptureHelper.OnScreenshotListener {

    private static final String TAG = "MainActivity";
    private static final int SHIZUKU_PERMISSION_REQUEST_CODE = 101;

    private TextView tvCaptureStatus;
    private Button btnStartCapture;
    private TextView tvShizukuStatus;
    private Button btnRequestShizuku;

    private static final int REQUEST_CODE_SCREEN_CAPTURE = 1001;
    private ScreenCaptureHelper screenCaptureHelper;
    private ImageView ivScreenshotPreview;

    private BroadcastReceiver captureStatusReceiver;
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

    private final Shizuku.OnRequestPermissionResultListener requestPermissionResultListener = (requestCode, grantResult) -> {
        if (requestCode == SHIZUKU_PERMISSION_REQUEST_CODE) {
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                bindAdbClickService();
            } else {
                updateShizukuStatus();
            }
        }
    };

    private final Shizuku.OnBinderReceivedListener binderReceivedListener = () -> {
        Log.d(TAG, "Shizuku binder received.");
        runOnUiThread(() -> {
            updateShizukuStatus();
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                bindAdbClickService();
            }
        });
    };

    private final Shizuku.OnBinderDeadListener binderDeadListener = () -> {
        Log.w(TAG, "Shizuku binder has died.");
        runOnUiThread(this::updateShizukuStatus);
    };

    private void requestNotificationPermission(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 102);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "FATAL: Unable to load OpenCV!");
            Toast.makeText(this, "FATAL: OpenCV 加载失败", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        NotificationUtils.createNotificationChannel(this);
        requestNotificationPermission();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvCaptureStatus = findViewById(R.id.tv_capture_status);
        btnStartCapture = findViewById(R.id.btn_start_capture);
        ivScreenshotPreview = findViewById(R.id.iv_screenshot_preview);
        tvShizukuStatus = findViewById(R.id.tv_shizuku_status);
        btnRequestShizuku = findViewById(R.id.btn_request_shizuku);

        screenCaptureHelper = new ScreenCaptureHelper(this);
        screenCaptureHelper.setOnScreenshotListener(this);

        btnStartCapture.setOnClickListener(v -> {
            if (screenCaptureHelper != null && screenCaptureHelper.isCapturing()) {
                Intent serviceIntent = new Intent(this, MediaProjectionService.class);
                stopService(serviceIntent);
            } else {
                if (adbClickService == null) {
                    Toast.makeText(this, "Shizuku service is not connected.", Toast.LENGTH_LONG).show();
                    return;
                }
                Intent intent = screenCaptureHelper.createScreenCaptureIntent();
                startActivityForResult(intent, REQUEST_CODE_SCREEN_CAPTURE);
            }
        });

        btnRequestShizuku.setOnClickListener(v -> {
            if (Shizuku.pingBinder()) {
                if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                    Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE);
                } else {
                    bindAdbClickService();
                }
            } else {
                Toast.makeText(this, "Please start the Shizuku service first.", Toast.LENGTH_SHORT).show();
            }
        });

        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener);
        Shizuku.addBinderDeadListener(binderDeadListener);
        Shizuku.addRequestPermissionResultListener(requestPermissionResultListener);

        captureStatusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent != null && ScreenCaptureHelper.ACTION_CAPTURE_STATUS_CHANGED.equals(intent.getAction())) {
                    boolean isCapturingNow = intent.getBooleanExtra(ScreenCaptureHelper.EXTRA_CAPTURE_STATUS, false);
                    updateCaptureStatusText(isCapturingNow);
                }
            }
        };

        updateShizukuStatus();
        updateCaptureStatus();
    }

    private void bindAdbClickService() {
        if (adbClickService != null) return;
        userServiceArgs = new Shizuku.UserServiceArgs(new ComponentName(this, ClickHelperService.class))
                .daemon(false)
                .debuggable(BuildConfig.DEBUG)
                .processNameSuffix(":adb_click")
                .version(BuildConfig.VERSION_CODE);
        Shizuku.bindUserService(userServiceArgs, serviceConnection);
    }

    private void unbindAdbClickService() {
        if (userServiceArgs != null) {
            Shizuku.unbindUserService(userServiceArgs, serviceConnection, true);
            adbClickService = null;
            userServiceArgs = null;
        }
    }

    private void updateShizukuStatus() {
        String statusText;
        if (Shizuku.isPreV11()) {
            statusText = "Shizuku service not running or version too old.";
        } else if (Shizuku.pingBinder()) {
            try {
                if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                    statusText = "✅ Shizuku Status: Authorized (Version " + Shizuku.getVersion() + ")";
                    if(adbClickService == null) {
                        bindAdbClickService();
                    }
                } else {
                    statusText = "❌ Shizuku Status: Not Authorized";
                }
            } catch (Exception e) {
                Log.e(TAG, "Unable to get Shizuku status", e);
                statusText = "Shizuku Status: Error";
            }
        } else {
            statusText = "Shizuku Status: Not running or connecting...";
        }
        tvShizukuStatus.setText(statusText);
    }

    @Override
    public void onBitmapAvailable(Bitmap bitmap) {
        if (ivScreenshotPreview != null && bitmap != null) {
            Drawable oldDrawable = ivScreenshotPreview.getDrawable();
            if (oldDrawable instanceof BitmapDrawable) {
                Bitmap oldBitmap = ((BitmapDrawable) oldDrawable).getBitmap();
                if (oldBitmap != null && !oldBitmap.isRecycled()) {
                    oldBitmap.recycle();
                }
            }
            ivScreenshotPreview.setImageBitmap(bitmap);
        }
    }

    private void updateCaptureStatusText(boolean isCapturing) {
        tvCaptureStatus.setText(isCapturing ? "📷 Capture Status: Running" : "📷 Capture Status: Stopped");
        btnStartCapture.setText(isCapturing ? "Stop Service" : "Start Screen Capture");
    }

    private void updateCaptureStatus() {
        if (screenCaptureHelper != null) {
            boolean isCapturing = screenCaptureHelper.isCapturing();
            updateCaptureStatusText(isCapturing);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateShizukuStatus();
        updateCaptureStatus();
        LocalBroadcastManager.getInstance(this).registerReceiver(
                captureStatusReceiver,
                new IntentFilter(ScreenCaptureHelper.ACTION_CAPTURE_STATUS_CHANGED)
        );
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindAdbClickService();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(captureStatusReceiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE) {
            if (resultCode == RESULT_OK) {
                Intent serviceIntent = new Intent(this, MediaProjectionService.class);
                serviceIntent.putExtra(MediaProjectionService.EXTRA_RESULT_CODE, resultCode);
                serviceIntent.putExtra(MediaProjectionService.EXTRA_RESULT_DATA, data);
                ContextCompat.startForegroundService(this, serviceIntent);
            } else {
                updateCaptureStatusText(false);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Shizuku.removeRequestPermissionResultListener(requestPermissionResultListener);
        Shizuku.removeBinderReceivedListener(binderReceivedListener);
        Shizuku.removeBinderDeadListener(binderDeadListener);

        if (ivScreenshotPreview != null) {
            Drawable drawable = ivScreenshotPreview.getDrawable();
            if (drawable instanceof BitmapDrawable) {
                Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
                if (bitmap != null && !bitmap.isRecycled()) {
                    bitmap.recycle();
                }
            }
        }
    }
}
