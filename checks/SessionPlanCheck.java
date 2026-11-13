import app.seven.meditation.SessionPlan;
public final class SessionPlanCheck {
    public static void main(String[] args) {
        long[] expected = {0, 17_000, 420_000};
        if (SessionPlan.count() != 3) throw new AssertionError("Exactly three cues");
        for (int i = 0; i < 3; i++) {
            if (SessionPlan.offset(i) != expected[i]) throw new AssertionError("Wrong offset");
            if (SessionPlan.deadline(123_456, i) != 123_456 + expected[i]) throw new AssertionError("Wrong absolute deadline");
        }
        if (SessionPlan.offset(2) - SessionPlan.offset(1) != 403_000) throw new AssertionError("Final gap");
        System.out.println("PASS: three cues, offsets 0/17000/420000, absolute deadlines, final gap 403000 ms");
    }
}
