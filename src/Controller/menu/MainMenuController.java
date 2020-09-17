package Controller.menu;

import Model.GameModel;
import events.Event;
import events.SessionsListShowEvent;
import events.SettingsShowEvent;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import util.Observable;
import util.Observer;

import java.util.concurrent.ConcurrentLinkedDeque;

public class MainMenuController implements Observable {
    private ConcurrentLinkedDeque<Observer> observers = new ConcurrentLinkedDeque<>();

    @FXML
    public void createGame(ActionEvent event) {
        notifyObservers(new SettingsShowEvent());
    }

    @FXML
    public void showSessionsList(ActionEvent event) {
        notifyObservers(new SessionsListShowEvent());
    }

    @FXML
    public void exitGame(ActionEvent event) {
        Platform.exit();
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
