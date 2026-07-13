package src.rules;

public class MoveValidation {
    private final boolean isValid;
    private final String reason;

    public MoveValidation(boolean isValid, String reason) {
        this.isValid = isValid;
        this.reason = reason;
    }

    public boolean isValid() {
        return isValid;
    }

    public String reason() {
        return reason;
    }

    public static MoveValidation ok() {
        return new MoveValidation(true, "ok");
    }

    public static MoveValidation invalid(String reason) {
        return new MoveValidation(false, reason);
    }
}