package src.net.messages;

public record RatingChanged(int newRating) implements WireMessage {
}
