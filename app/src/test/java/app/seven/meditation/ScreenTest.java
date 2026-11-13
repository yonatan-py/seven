package app.seven.meditation;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import android.widget.Button;
import java.io.FileOutputStream;
import java.io.File;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34, qualifiers="w192dp-h192dp-round-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class ScreenTest {
    @Test public void idleScreenFitsAndRenders() throws Exception {
        try (var controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            View root = activity.getWindow().getDecorView();
            int exact = View.MeasureSpec.makeMeasureSpec(384, View.MeasureSpec.EXACTLY);
            root.measure(exact, exact);
            root.layout(0, 0, 384, 384);
            Button start = activity.findViewById(R.id.start);
            assertEquals("Start", start.getText().toString());
            assertTrue(start.isEnabled());
            root.invalidate();
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
            Bitmap image = Bitmap.createBitmap(384,384,Bitmap.Config.ARGB_8888);
            root.draw(new Canvas(image));
            File previews = new File("build/previews");
            previews.mkdirs();
            try(var out = new FileOutputStream(new File(previews, "seven.png"))) {
                image.compress(Bitmap.CompressFormat.PNG,100,out);
            }
        }
    }
    @Test public void cueContractIsExact() {
        assertEquals(3,SessionPlan.count());
        assertEquals(0,SessionPlan.offset(0));
        assertEquals(17_000,SessionPlan.offset(1));
        assertEquals(420_000,SessionPlan.offset(2));
        assertEquals(403_000,SessionPlan.offset(2)-SessionPlan.offset(1));
    }
}
