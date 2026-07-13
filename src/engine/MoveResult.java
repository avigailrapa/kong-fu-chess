package src.engine;

public class MoveResult {
    private final boolean isAccepted;
    private final String reason;

    public MoveResult(boolean isAccepted, String reason) {
        this.isAccepted = isAccepted;
        this.reason = reason;
    }

    public boolean isAccepted() {
        return isAccepted;
    }

    public String reason() {
        return reason;
    }
}