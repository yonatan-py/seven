package app.seven.meditation;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

public final class MeditationService extends Service {
    private static final String CHANNEL = "meditation";
    private static final String STOP = "app.seven.meditation.STOP";
    private static final int NOTIFICATION_ID = 7;
    // 3-second generated sample; keep the player alive after the final gong.
    private static final long TAIL_MS = 3_100L;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SoundPool sound;
    private int sample;
    private PowerManager.WakeLock wake;
    private boolean began;
    private final Runnable loadTimeout = this::stopSelf;
    public static volatile boolean active;
    private final AudioAttributes audio = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();

    @Override public void onCreate() {
        super.onCreate();
        NotificationManager nm = getSystemService(NotificationManager.class);
        NotificationChannel channel = new NotificationChannel(CHANNEL, "Meditation session", NotificationManager.IMPORTANCE_LOW);
        channel.setSound(null, null);
        channel.enableVibration(false);
        nm.createNotificationChannel(channel);
    }
    @Override public int onStartCommand(Intent intent, int flags, int id) {
        if (intent != null && STOP.equals(intent.getAction())) { stopSelf(); return START_NOT_STICKY; }
        if (active) return START_NOT_STICKY;
        active = true;
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent stop = PendingIntent.getService(this, 1, new Intent(this, MeditationService.class).setAction(STOP), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_seven).setContentTitle("Seven")
                .setContentText("Seven-minute meditation in progress")
                .setContentIntent(open).setOngoing(true).setCategory(Notification.CATEGORY_ALARM)
                .addAction(new Notification.Action.Builder(null, "Stop", stop).build()).build();
        if (Build.VERSION.SDK_INT >= 34) startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        else startForeground(NOTIFICATION_ID, notification);

        PowerManager pm = getSystemService(PowerManager.class);
        wake = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Seven:session");
        wake.acquire(SessionPlan.DURATION_MS + 15_000L);
        // Bound loading failure and the session even if no audio callback arrives.
        handler.postDelayed(loadTimeout, 10_000L);
        sound = new SoundPool.Builder().setMaxStreams(1).setAudioAttributes(audio).build();
        sound.setOnLoadCompleteListener((pool, loaded, result) -> handler.post(() -> {
            if (result != 0) { Log.e("Seven", "Gong load failed: " + result); stopSelf(); return; }
            if (!active || began) return;
            began = true;
            handler.removeCallbacks(loadTimeout);
            handler.postDelayed(this::stopSelf, SessionPlan.DURATION_MS + TAIL_MS + 5_000L);
            long epoch = SystemClock.elapsedRealtime();
            long uptimeEpoch = SystemClock.uptimeMillis();
            for (int i = 0; i < SessionPlan.count(); i++) {
                final int cue = i;
                handler.postAtTime(() -> gong(epoch, cue), uptimeEpoch + SessionPlan.offset(cue));
            }
        }));
        sample = sound.load(this, R.raw.gong, 1);
        return START_NOT_STICKY;
    }
    private void gong(long epoch, int cue) {
        if (!active || sound == null) return;
        long elapsed = SystemClock.elapsedRealtime() - epoch;
        Log.i("Seven", "cue=" + cue + " elapsedMs=" + elapsed + " targetMs=" + SessionPlan.offset(cue));
        int stream = sound.play(sample, 1f, 1f, 1, 0, 1f);
        if (stream == 0) Log.e("Seven", "Gong playback failed");
        Vibrator vibrator = getSystemService(Vibrator.class);
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK), audio);
        }
        if (cue == SessionPlan.count() - 1) handler.postDelayed(this::stopSelf, TAIL_MS);
    }
    @Override public void onDestroy() {
        active = false;
        handler.removeCallbacksAndMessages(null);
        if (sound != null) { sound.release(); sound = null; }
        Vibrator vibrator = getSystemService(Vibrator.class);
        if (vibrator != null) vibrator.cancel();
        if (wake != null && wake.isHeld()) wake.release();
        stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
