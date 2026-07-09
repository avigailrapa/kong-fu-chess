package rules;

public record MoveValidation(boolean isValid, String reason) {

    public static MoveValidation ok() {
        return new MoveValidation(true, "ok");
    }

    public static MoveValidation invalid(String reason) {
        return new MoveValidation(false, reason);
    }
}
