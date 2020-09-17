package Controller.session;

import Model.GameModel;
import events.Event;
import events.GoBackEvent;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.input.KeyEvent;
import util.Observable;
import util.Observer;

import java.util.concurrent.ConcurrentLinkedDeque;

public class SessionController implements Observable {
    @FXML
    private Parent root;

    private ConcurrentLinkedDeque<Observer> observers = new ConcurrentLinkedDeque<>();

    public void initialize() {
        //Adding keyboard handler
        root.addEventHandler(KeyEvent.KEY_PRESSED,
                (keyEvent) -> KeyActionFactory.getActionByKey(keyEvent).apply());
    }

    @FXML
    public void backToMainMenu(ActionEvent event) {
        GameModel.getInstance().exitSession();

        notifyObservers(new GoBackEvent());
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