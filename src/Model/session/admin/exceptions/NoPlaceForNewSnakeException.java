package Model.session.admin.exceptions;

public class NoPlaceForNewSnakeException extends Exception {
    private static final String message = "Error: cant place new snake.";

    @Override
    public String getMessage() {
        return message;
    }
}
