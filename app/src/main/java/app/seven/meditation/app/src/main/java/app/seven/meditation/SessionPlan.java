package app.seven.meditation;

/** Pure timing contract: all cues are offsets from the same start, never chained delays. */
public final class SessionPlan {
    public static final long DURATION_MS = 420_000L;
    private static final long[] OFFSETS_MS = {0L, 17_000L, DURATION_MS};
    private SessionPlan() {}
    public static int count() { return OFFSETS_MS.length; }
    public static long offset(int cue) { return OFFSETS_MS[cue]; }
    public static long deadline(long start, int cue) { return start + offset(cue); }
}
