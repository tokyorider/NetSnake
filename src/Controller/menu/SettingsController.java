package Controller.menu;

import Model.GameModel;
import events.Event;
import events.GoBackEvent;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import me.ippolitov.fit.snakes.SnakesProto;
import util.Observable;
import util.Observer;

import java.util.concurrent.ConcurrentLinkedDeque;

public class SettingsController implements Observable {
    private ConcurrentLinkedDeque<Observer> observers = new ConcurrentLinkedDeque<>();

    @FXML
    private Slider widthSlider, heightSlider, stateDelaySlider, foodStaticSlider, foodPerPlayerSlider,
            deadFoodProbSlider, pingDelaySlider, nodeTimeoutSlider;

    @FXML
    private Label widthLabel, heightLabel, stateDelayLabel, foodStaticLabel, foodPerPlayerLabel,
            deadFoodProbLabel, pingDelayLabel, nodeTimeoutLabel;

    public void initialize() {
        setSliderIntValueShower(widthSlider, widthLabel, "Field width");
        setSliderIntValueShower(heightSlider, heightLabel, "Field height");
        setSliderIntValueShower(stateDelaySlider, stateDelayLabel, "State change delay(ms)");
        setSliderIntValueShower(foodStaticSlider, foodStaticLabel, "Static food");
        setSliderDoubleValueShower(foodPerPlayerSlider, foodPerPlayerLabel, "Food per player");
        setSliderDoubleValueShower(deadFoodProbSlider, deadFoodProbLabel, "Dead food probability");
        setSliderIntValueShower(pingDelaySlider, pingDelayLabel, "Ping delay(ms)");
        setSliderIntValueShower(nodeTimeoutSlider, nodeTimeoutLabel, "Node timeout(ms)");
    }

    private void setSliderIntValueShower(Slider slider, Label label, String name) {
        label.setText(name + ": " + (int)slider.getValue());
        slider.valueProperty().addListener((changed, oldValue, newValue) ->
                label.setText(name + ": " + newValue.intValue()));
    }

    private void setSliderDoubleValueShower(Slider slider, Label label, String name) {
        label.setText(name + ": " + slider.getValue());
        slider.valueProperty().addListener((changed, oldValue, newValue) ->
                label.setText(name + ": " + newValue.doubleValue()));
    }

    @FXML
    public void backToMainMenu(ActionEvent event) {
        notifyObservers(new GoBackEvent());
    }

    @FXML
    public void createGame(ActionEvent event) {
        SnakesProto.GameConfig config = SnakesProto.GameConfig.newBuilder().
                                        setWidth((int)widthSlider.getValue()).
                                        setHeight((int)heightSlider.getValue()).
                                        setStateDelayMs((int)stateDelaySlider.getValue()).
                                        setFoodStatic((int)foodStaticSlider.getValue()).
                                        setFoodPerPlayer((float)foodPerPlayerSlider.getValue()).
                                        setDeadFoodProb((float)deadFoodProbSlider.getValue()).
                                        setPingDelayMs((int)pingDelaySlider.getValue()).
                                        setNodeTimeoutMs((int)nodeTimeoutSlider.getValue()).
                                        build();
        GameModel.getInstance().createSession(config);
    }

    @Override
    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(Event event) {
        observers.forEach(observer -> observer.handleEvent(event));
    }
}
