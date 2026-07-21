package src.net;

public record RatingChanged(int newRating) implements WireMessage {
}
