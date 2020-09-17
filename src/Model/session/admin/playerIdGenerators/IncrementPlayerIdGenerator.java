package Model.session.admin.playerIdGenerators;

public class IncrementPlayerIdGenerator implements IPlayerIdGenerator{
    private int value;

    public IncrementPlayerIdGenerator(int startValue) {
        value = startValue;
    }

    @Override
    public int generateId() {
        return value++;
    }
}
