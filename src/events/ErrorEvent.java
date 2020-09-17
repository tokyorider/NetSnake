package events;

public class ErrorEvent extends Event {
    private String errMsg;

    public ErrorEvent(String errMsg) {
        super(EventType.ERROR);

        this.errMsg = errMsg;
    }

    @Override
    public String toString() {
        return errMsg;
    }
}
