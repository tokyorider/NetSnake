package Model.network.messageSequenceGenerator;

import java.util.Random;

public class RandomSequenceGenerator implements IMessageSequenceGenerator {
    private Random random = new Random();

    @Override
    public long generateSequence() {
        return random.nextLong();
    }
}
