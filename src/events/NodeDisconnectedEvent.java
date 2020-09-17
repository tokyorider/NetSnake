package events;

public class NodeDisconnectedEvent extends Event {
    private int id;

    public NodeDisconnectedEvent(int id) {
        super(EventType.NODE_DISCONNECTED);

        this.id = id;
    }

    public int getNodeId() {
        return id;
    }
}
