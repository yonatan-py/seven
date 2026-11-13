package app.seven.meditation;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;

public final class MainActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Button start;
    private TextView status;
    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            boolean active = MeditationService.active;
            start.setEnabled(!active);
            start.setText(active ? "Started" : "Start");
            status.setText(active ? "Meditating" : "7 minutes");
            handler.postDelayed(this, 500);
        }
    };
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        start = findViewById(R.id.start);
        status = findViewById(R.id.status);
        start.setOnClickListener(v -> {
            start.setEnabled(false);
            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 7);
            } else begin();
        });
    }
    private void begin() {
        if (!MeditationService.active) {
            startForegroundService(new Intent(this, MeditationService.class));
        }
    }
    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(request, permissions, grants);
        // Notification denial doesn't prevent a user-started foreground service.
        if (request == 7) begin();
    }
    @Override public void onResume() { super.onResume(); handler.post(refresh); }
    @Override public void onPause() { handler.removeCallbacks(refresh); super.onPause(); }
}
