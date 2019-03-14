package by.anegin.telegram_contests.core.utils;

/**
 * Thread-safe synchronized float range
 */
public class AtomicRange {

    private volatile float start = 0f;
    private volatile float end = 1f;

    public synchronized float getSize() {
        return end - start;
    }

    public synchronized float getStart() {
        return start;
    }

    public synchronized float getEnd() {
        return end;
    }

    public synchronized boolean checkNotEqualsAndSet(float start, float end) {
        if (start != this.start || end != this.end) {
            this.start = start;
            this.end = end;
            return true;
        }
        return false;
    }

}