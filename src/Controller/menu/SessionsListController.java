package Controller.menu;

import Model.GameModel;
import events.Event;
import events.GoBackEvent;
import events.SessionsListUpdateEvent;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.layout.GridPane;
import util.Observable;
import util.Observer;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedDeque;

public class SessionsListController implements Observable {
    private ConcurrentLinkedDeque<Observer> observers = new ConcurrentLinkedDeque<>();

    private Timer timer = new Timer(true);

    @FXML
    private GridPane sessionsListPane;

    public void initialize() {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                notifyObservers(new SessionsListUpdateEvent(
                        GameModel.getInstance().getLastSessionsInfos(), sessionsListPane));
            }
        }, 1000, 1000);
    }

    @FXML
    public void backToMainMenu(ActionEvent event) {
        timer.cancel();
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
