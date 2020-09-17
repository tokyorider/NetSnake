package events;

import me.ippolitov.fit.snakes.SnakesProto;


public class MessageReceiveEvent extends Event {
    private SnakesProto.GameMessage msg;

    public MessageReceiveEvent(SnakesProto.GameMessage msg) {
        super(EventType.MESSAGE_RECEIVE);

        this.msg = msg;
    }

    public SnakesProto.GameMessage getMsg() {
        return msg;
    }
}
