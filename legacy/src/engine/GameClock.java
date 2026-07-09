public class GameClock {
    private long currentTime;

    public GameClock() {
        this.currentTime = 0;
    }

    public void advance(long milliseconds) {
        if (milliseconds < 0) {
            throw new IllegalArgumentException("Cannot advance time backwards");
        }
        this.currentTime += milliseconds;
    }

    public long getCurrentTime() {
        return currentTime;
    }

    public void reset() {
        this.currentTime = 0;
    }
}
