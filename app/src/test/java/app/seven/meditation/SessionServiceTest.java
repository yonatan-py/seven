package app.seven.meditation;

import static org.junit.Assert.*;
import android.content.Intent;
import android.media.SoundPool;
import android.os.Looper;
import android.os.PowerManager;
import android.os.Vibrator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowSoundPool;
import java.lang.reflect.Field;
import java.time.Duration;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
@LooperMode(LooperMode.Mode.PAUSED)
public class SessionServiceTest {
    private Object field(Object service, String name) throws Exception {
        Field f=service.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(service);
    }
    @Test public void playsExactlyAtOffsetsAndStopsAfterTail() throws Exception {
        var controller=Robolectric.buildService(MeditationService.class).create();
        MeditationService service=controller.get();
        try {
            service.onStartCommand(new Intent(),0,1);
            assertTrue(MeditationService.active);
            PowerManager.WakeLock wake=(PowerManager.WakeLock)field(service,"wake");
            assertTrue(wake.isHeld());
            ShadowSoundPool audio=Shadow.extract((SoundPool)field(service,"sound"));
            assertEquals(0,audio.getResourcePlaybacks(R.raw.gong).size());
            audio.notifyResourceLoaded(R.raw.gong,true);
            var clock=Shadows.shadowOf(Looper.getMainLooper());clock.idle();
            assertEquals(1,audio.getResourcePlaybacks(R.raw.gong).size());
            // Repeated Start must not schedule another session.
            service.onStartCommand(new Intent(),0,2);
            clock.idleFor(Duration.ofMillis(16_999));
            assertEquals(1,audio.getResourcePlaybacks(R.raw.gong).size());
            clock.idleFor(Duration.ofMillis(1));
            assertEquals(2,audio.getResourcePlaybacks(R.raw.gong).size());
            clock.idleFor(Duration.ofMillis(402_999));
            assertEquals(2,audio.getResourcePlaybacks(R.raw.gong).size());
            clock.idleFor(Duration.ofMillis(1));
            assertEquals(3,audio.getResourcePlaybacks(R.raw.gong).size());
            assertFalse(Shadows.shadowOf(service).isStoppedBySelf());
            clock.idleFor(Duration.ofMillis(3_100));
            assertTrue(Shadows.shadowOf(service).isStoppedBySelf());
            controller.destroy();
            assertFalse(MeditationService.active);
            assertFalse(wake.isHeld());
        } finally { if(MeditationService.active) controller.destroy(); }
    }
    @Test public void stoppingCancelsFutureCues() throws Exception {
        var controller=Robolectric.buildService(MeditationService.class).create();
        MeditationService service=controller.get();
        service.onStartCommand(new Intent(),0,1);
        ShadowSoundPool audio=Shadow.extract((SoundPool)field(service,"sound"));
        audio.notifyResourceLoaded(R.raw.gong,true);
        var clock=Shadows.shadowOf(Looper.getMainLooper());clock.idle();
        controller.destroy();
        clock.idleFor(Duration.ofSeconds(421));
        assertFalse(MeditationService.active);
        assertEquals(1,audio.getResourcePlaybacks(R.raw.gong).size());
    }
}
