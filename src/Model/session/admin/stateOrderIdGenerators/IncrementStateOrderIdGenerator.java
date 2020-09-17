package Model.session.admin.stateOrderIdGenerators;

public class IncrementStateOrderIdGenerator implements IStateOrderIdGenerator {
    private int value;

    public IncrementStateOrderIdGenerator(int startValue) {
        value = startValue;
    }

    @Override
    public int generateId() {
        return value++;
    }
}
