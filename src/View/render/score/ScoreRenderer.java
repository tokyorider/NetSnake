package View.render.score;

import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import me.ippolitov.fit.snakes.SnakesProto;

import java.util.ArrayList;
import java.util.List;

public class ScoreRenderer {
    private final static Font font = new Font("Book Antiqua", 30);

    private final static Color fontColor = Color.WHITE;

    private GridPane scoreTable;

    public ScoreRenderer(GridPane scoreTable) {
        this.scoreTable = scoreTable;
    }

    public void renderScores(List<SnakesProto.GamePlayer> players) {
        scoreTable.getChildren().clear();
        var playersSorted = new ArrayList<>(players);
        playersSorted.sort((p1, p2) -> -1 * Integer.compare(p1.getScore(), p2.getScore()));

        for (int i = 0; i < playersSorted.size(); ++i) {
            Label nameLabel = new Label(playersSorted.get(i).getName()),
                    scoreLabel = new Label(Integer.toString(playersSorted.get(i).getScore()));
            nameLabel.setFont(font);
            scoreLabel.setFont(font);
            nameLabel.setTextFill(fontColor);
            scoreLabel.setTextFill(fontColor);

            scoreTable.add(nameLabel, 0, i);
            scoreTable.add(scoreLabel, 1, i);
        }
    }
}
