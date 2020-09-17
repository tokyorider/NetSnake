package View;

import Model.GameModel;
import View.observers.*;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import util.Observable;

import java.io.IOException;

public class MainFX extends Application {
    private Stage stage;

    private Scene mainMenuScene, settingsScene, sessionsListScene, errorScene;

    private Observable mainMenuController, settingsController, sessionListController;

    public void start(Stage primaryStage) throws IOException {
        stage = primaryStage;
        initScenes();
        registerObservers();

        stage.setScene(mainMenuScene);
        stage.setTitle("NetSnakes");
        stage.setResizable(false);
        stage.show();
    }

    private void initScenes() throws IOException {
        FXMLLoader mainMenuLoader = new FXMLLoader(getClass().getResource("scenes/MainMenu.fxml")),
                settingsLoader = new FXMLLoader(getClass().getResource("scenes/Settings.fxml")),
                sessionsListLoader = new FXMLLoader(getClass().getResource("scenes/SessionsList.fxml")),
                errorSceneLoader = new FXMLLoader(getClass().getResource("scenes/Error.fxml"));

        mainMenuScene = new Scene(mainMenuLoader.load());
        settingsScene = new Scene(settingsLoader.load());
        sessionsListScene = new Scene(sessionsListLoader.load());
        errorScene = new Scene(errorSceneLoader.load());

        mainMenuController = mainMenuLoader.getController();
        settingsController = settingsLoader.getController();
        sessionListController = sessionsListLoader.getController();
    }

    private void registerObservers() {
        mainMenuController.addObserver(new SettingsShowObserver(stage, settingsScene));
        mainMenuController.addObserver(new SessionsListShowObserver(stage, sessionsListScene));

        settingsController.addObserver(new GoBackObserver(stage, mainMenuScene));

        sessionListController.addObserver(new GoBackObserver(stage, mainMenuScene));
        sessionListController.addObserver(new SessionsListUpdateObserver());

        GameModel.getInstance().addObserver(new SessionStartObserver(stage, mainMenuScene));
        GameModel.getInstance().addObserver(new ErrorObserver(stage, errorScene));
    }
}
