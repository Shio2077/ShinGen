// File: ScreenCaptureHelper.java

package io.github.shio2077.shingen;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Random;

public class ScreenCaptureHelper {

    // --- Actions for Broadcasts ---
    public static final String ACTION_CAPTURE_STATUS_CHANGED = "io.github.shio2077.shingen.ACTION_CAPTURE_STATUS_CHANGED";
    public static final String EXTRA_CAPTURE_STATUS = "io.github.shio2077.shingen.EXTRA_CAPTURE_STATUS";
    public static final String ACTION_PERFORM_CLICK = "io.github.shio2077.shingen.ACTION_PERFORM_CLICK";
    public static final String EXTRA_CLICK_X = "io.github.shio2077.shingen.EXTRA_CLICK_X";
    public static final String EXTRA_CLICK_Y = "io.github.shio2077.shingen.EXTRA_CLICK_Y";

    private static final String TAG = "ScreenCaptureHelper";
    private static final String OPENCV_TAG = "OpenCV";
    private static final String OPENCV_DEBUG_TAG = "OpenCVDebug";

    private MediaProjectionManager projectionManager;
    private MediaProjection mediaProjection;
    private ImageReader imageReader;
    private HandlerThread handlerThread;
    private Handler backgroundHandler;
    private final Random random = new Random();

    private Mat eyeTemplate;
    private Mat chestTemplate;
    private Mat bubbleTemplate;

    public MediaProjection getMediaProjection() {
        return instance != null ? instance.mediaProjection : null;
    }

    public interface OnScreenshotListener {
        void onBitmapAvailable(Bitmap bitmap);
    }

    private OnScreenshotListener listener;

    public void setOnScreenshotListener(OnScreenshotListener listener) {
        this.listener = listener;
    }

    private static ScreenCaptureHelper instance;
    private Context context;

    public static ScreenCaptureHelper getInstance() {
        return instance;
    }

    private boolean isCapturing = false;

    public void setContext(Context context) {
        if (this.context == null) {
            this.context = context.getApplicationContext();
            loadTemplateImages();
        }
    }

    public ScreenCaptureHelper(Activity activity) {
        instance = this;
        this.context = activity.getApplicationContext();
        projectionManager = (MediaProjectionManager) activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        loadTemplateImages();
    }

    private void loadTemplateImages() {
        if (context == null) {
            Log.e(TAG, "Context is null, cannot load templates.");
            return;
        }
        eyeTemplate = loadTemplateFromAssets("eye.jpg");
        chestTemplate = loadTemplateFromAssets("chest.png");
        Log.d(OPENCV_DEBUG_TAG, "chestTemplate: channels=" + chestTemplate.channels() + ", type=" + chestTemplate.type());
        bubbleTemplate = loadTemplateFromAssets("bubble.jpg");
    }

    private Mat loadTemplateFromAssets(String fileName) {
        AssetManager assetManager = context.getAssets();
        Mat template = new Mat();
        try (InputStream is = assetManager.open(fileName)) {
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            if (bitmap == null) {
                Log.e(OPENCV_TAG, "Failed to decode asset: " + fileName);
                return template;
            }

            Utils.bitmapToMat(bitmap, template);

            // ⭐ 强制转成 8 位无符号（防止CV_32S）
            if (template.depth() != CvType.CV_8U) {
                template.convertTo(template, CvType.CV_8U);
                Log.w(OPENCV_TAG, fileName + " depth corrected to CV_8U");
            }

            // ⭐ 再转成BGR三通道
            if (template.channels() == 4) {
                Imgproc.cvtColor(template, template, Imgproc.COLOR_BGRA2BGR);
            } else if (template.channels() == 1) {
                Imgproc.cvtColor(template, template, Imgproc.COLOR_GRAY2BGR);
            }

            Log.d(OPENCV_TAG, "Loaded template from assets: " + fileName +
                    ", channels=" + template.channels() + ", type=" + template.type());

            if (!bitmap.isRecycled()) bitmap.recycle();

        } catch (IOException e) {
            Log.e(OPENCV_TAG, "Error loading template: " + fileName, e);
        }

        return template;
    }




    public boolean isCapturing() {
        return isCapturing;
    }

    private void setCapturing(boolean capturing) {
        isCapturing = capturing;
        if (this.context != null) {
            Intent intent = new Intent(ACTION_CAPTURE_STATUS_CHANGED);
            intent.putExtra(EXTRA_CAPTURE_STATUS, capturing);
            LocalBroadcastManager.getInstance(this.context).sendBroadcast(intent);
        }
    }

    public Intent createScreenCaptureIntent() {
        return projectionManager.createScreenCaptureIntent();
    }

    public void startScreenCapture(int resultCode, Intent data) {
        Log.d(TAG, "startScreenCapture called");
        if (context == null || data == null) return;

        mediaProjection = projectionManager.getMediaProjection(resultCode, data);
        if (mediaProjection == null) return;

        mediaProjection.registerCallback(new MediaProjection.Callback() {
            @Override
            public void onStop() { stopScreenCapture(); }
        }, null);

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(metrics);

        int width = Math.max(metrics.widthPixels, metrics.heightPixels);
        int height = Math.min(metrics.widthPixels, metrics.heightPixels);
        int density = metrics.densityDpi;

        handlerThread = new HandlerThread("ScreenCapture");
        handlerThread.start();
        backgroundHandler = new Handler(handlerThread.getLooper());

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
        imageReader.setOnImageAvailableListener(new ImageAvailableListener(), backgroundHandler);

        mediaProjection.createVirtualDisplay("ScreenCapture", width, height, density, android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, imageReader.getSurface(), null, backgroundHandler);
        setCapturing(true);
    }

    public void stopScreenCapture() {
        Log.d(TAG, "stopScreenCapture called.");
        if (isCapturing) setCapturing(false);
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
        if (handlerThread != null) {
            handlerThread.quitSafely();
            handlerThread = null;
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
    }

    private class ImageAvailableListener implements ImageReader.OnImageAvailableListener {
        private final Handler mainHandler = new Handler(Looper.getMainLooper());
        private long lastTime = 0;
        private static final long INTERVAL = 200; // 200ms interval

        @Override
        public void onImageAvailable(ImageReader reader) {
            try (Image image = reader.acquireLatestImage()) {
                if (image == null) return;

                long now = System.currentTimeMillis();
                if (now - lastTime < INTERVAL) return;
                lastTime = now;

                Bitmap bitmap = imageToBitmap(image);
                if (bitmap != null) {
                    processScreenshot(bitmap);
                    mainHandler.post(() -> {
                        if (listener != null) listener.onBitmapAvailable(bitmap);
                        else bitmap.recycle();
                    });
                } else {
                    Log.w(TAG, "Converted bitmap was null.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in onImageAvailable", e);
            }
        }
    }

    private void processScreenshot(Bitmap sceneBitmap) {
        Mat sceneGray = new Mat();
        Mat sceneBgr = new Mat();
        Utils.bitmapToMat(sceneBitmap, sceneGray);
        Utils.bitmapToMat(sceneBitmap, sceneBgr);
        if (sceneGray.depth() != CvType.CV_8U) {
            sceneGray.convertTo(sceneGray, CvType.CV_8U);
        }
        if (sceneBgr.depth() != CvType.CV_8U) {
            sceneBgr.convertTo(sceneBgr, CvType.CV_8U);
        }
        if (sceneGray.channels() == 4) {
            Imgproc.cvtColor(sceneGray, sceneGray, Imgproc.COLOR_BGRA2BGR);
        }
        if (sceneBgr.channels() == 4) {
            Imgproc.cvtColor(sceneBgr, sceneBgr, Imgproc.COLOR_BGRA2BGR);
        }
        try {
            // Condition 1: Check for eye
            Point eyeCenter = findTemplate(sceneGray, eyeTemplate, 0.80);

            // Additional condition: Check for chest at any cycle
            // Chest icon will never showup with conversation icon/bubble in same time
            Point chestCenter = findTemplate(sceneBgr, chestTemplate, 0.80);
            if(chestCenter != null && chestCenter.x > 600){
                Log.d(OPENCV_TAG, "Treasure chest found. Clicking chest icon bubble at (" + (int)chestCenter.x + ", " + (int)chestCenter.y + ").");
                broadcastClickRequest((int)chestCenter.x, (int)chestCenter.y);
            }

            if (eyeCenter != null) {
                // Branch 1, True: Eye found, now check for bubble
                Log.d(OPENCV_TAG, "Branch 1 TRUE: Eye found.");

                // Condition 2: Check for bubble
                Point bubbleCenter = findTemplate(sceneGray, bubbleTemplate, 0.75);

                if (bubbleCenter != null) {
                    // Branch 2, True: Bubble found
                    Log.d(OPENCV_TAG, "Branch 2 TRUE: Bubble found. Clicking bubble at (" + (int)bubbleCenter.x + ", " + (int)bubbleCenter.y + ").");
                    broadcastClickRequest((int) bubbleCenter.x, (int) bubbleCenter.y);
                } else {
                    // Branch 2, False: Bubble not found
                    int screenCenterX = sceneGray.cols() / 2;
                    int screenCenterY = sceneGray.rows() / 2;
                    int randomX = screenCenterX + random.nextInt(41) - 20; // -20 to +20
                    int randomY = screenCenterY + random.nextInt(41) - 20; // -20 to +20
                    Log.d(OPENCV_TAG, "Branch 2 FALSE: Bubble NOT found. Clicking random pos (" + randomX + ", " + randomY + ").");
                    broadcastClickRequest(randomX, randomY);
                }
            } else {
                // Branch 1, False: Eye not found
                // No action performed
            }
        } finally {
            sceneGray.release();
        }
    }

    private Point findTemplate(Mat sceneTemp, Mat template, double threshold) {
        if (template == null || template.empty() || sceneTemp.empty()) return null;
        if (sceneTemp.cols() < template.cols() || sceneTemp.rows() < template.rows()) return null;

        Mat result = new Mat();
        try {
            Imgproc.matchTemplate(sceneTemp, template, result, Imgproc.TM_CCOEFF_NORMED);
            Core.MinMaxLocResult mmr = Core.minMaxLoc(result);

            if (mmr.maxVal >= threshold) {
                Point matchLoc = mmr.maxLoc;
                return new Point(matchLoc.x + template.cols() / 2.0, matchLoc.y + template.rows() / 2.0);
            }
        } finally {
            result.release();
        }
        return null;
    }

    private void broadcastClickRequest(int x, int y) {
        if (context == null) return;
        Intent intent = new Intent(ACTION_PERFORM_CLICK);
        intent.putExtra(EXTRA_CLICK_X, x);
        intent.putExtra(EXTRA_CLICK_Y, y);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private Bitmap imageToBitmap(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * image.getWidth();

        Bitmap tempBitmap = Bitmap.createBitmap(image.getWidth() + rowPadding / pixelStride, image.getHeight(), Bitmap.Config.ARGB_8888);
        tempBitmap.copyPixelsFromBuffer(buffer);
        Bitmap croppedBitmap = Bitmap.createBitmap(tempBitmap, 0, 0, image.getWidth(), image.getHeight());
        tempBitmap.recycle();
        return croppedBitmap;
    }
}
