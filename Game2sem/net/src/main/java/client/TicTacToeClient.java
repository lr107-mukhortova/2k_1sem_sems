package client;

import java.io.*;
import java.net.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class TicTacToeClient extends Application
        implements TicTacToeConstants {
    private boolean myTurn = false;
    private char myToken = ' ';
    private char otherToken = ' ';
    private Cell[][] cell =  new Cell[3][3];
    private Label lblTitle = new Label();
    private Label lblStatus = new Label();
    private int rowSelected;
    private int columnSelected;
    private DataInputStream fromServer;
    private DataOutputStream toServer;
    private boolean continueToPlay = true;
    private boolean waiting = true;

    private String host = "localhost";
    private final int PORT = 1234;

    @Override
    public void start(Stage primaryStage) {
        GridPane pane = new GridPane();
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                pane.add(cell[i][j] = new Cell(i, j), j, i);

        BorderPane borderPane = new BorderPane();
        borderPane.setTop(lblTitle);
        borderPane.setCenter(pane);
        borderPane.setBottom(lblStatus);

        Scene scene = new Scene(borderPane, 320, 350);
        primaryStage.setTitle("Крестики-нолики");
        primaryStage.setScene(scene);
        primaryStage.show();

        connectToServer();
    }

    private void connectToServer() {
        try {
            Socket socket = new Socket(host, PORT);
            fromServer = new DataInputStream(socket.getInputStream());
            toServer = new DataOutputStream(socket.getOutputStream());
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        new Thread(() -> {
            try {
                int player = fromServer.readInt();
                if (player == TicTacToeConstants.PLAYER1) {
                    myToken = 'X';
                    otherToken = 'O';
                    Platform.runLater(() -> {
                        lblTitle.setText("Игрок 1 за 'X'");
                        lblStatus.setText("Ожидаем второго игрока");
                    });

                    fromServer.readInt();

                    Platform.runLater(() ->
                            lblStatus.setText("Второй игрок присоединился. Первый начинает"));

                    myTurn = true;
                }
                else if (player == TicTacToeConstants.PLAYER2) {
                    myToken = 'O';
                    otherToken = 'X';
                    Platform.runLater(() -> {
                        lblTitle.setText("Игрок 2 за 'О'");
                        lblStatus.setText("Ждём ход первого игрока.");
                    });
                }

                while (continueToPlay) {
                    if (player == TicTacToeConstants.PLAYER1) {
                        waitForPlayerAction();
                        sendMove();
                        receiveInfoFromServer();
                    }
                    else if (player == TicTacToeConstants.PLAYER2) {
                        receiveInfoFromServer();
                        waitForPlayerAction();
                        sendMove();
                    }
                }
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    private void waitForPlayerAction() throws InterruptedException {
        while (waiting) {
            Thread.sleep(100);
        }

        waiting = true;
    }

    private void sendMove() throws IOException {
        toServer.writeInt(rowSelected);
        toServer.writeInt(columnSelected);
    }

    private void receiveInfoFromServer() throws IOException {
        int status = fromServer.readInt();

        if (status == TicTacToeConstants.PLAYER1_WON) {
            continueToPlay = false;
            if (myToken == 'X') {
                Platform.runLater(() -> lblStatus.setText("Вы победили!"));
            }
            else if (myToken == 'O') {
                Platform.runLater(() ->
                        lblStatus.setText("Победа первого игрока!"));
                receiveMove();
            }
        }
        else if (status == TicTacToeConstants.PLAYER2_WON) {
            continueToPlay = false;
            if (myToken == 'O') {
                Platform.runLater(() -> lblStatus.setText("Вы победили!"));
            }
            else if (myToken == 'X') {
                Platform.runLater(() ->
                        lblStatus.setText("Победа второго игрока!"));
                receiveMove();
            }
        }
        else if (status == TicTacToeConstants.DRAW) {
            continueToPlay = false;
            Platform.runLater(() ->
                    lblStatus.setText("Ничья!"));

            if (myToken == 'O') {
                receiveMove();
            }
        }
        else {
            receiveMove();
            Platform.runLater(() -> lblStatus.setText("Ваша очередь"));
            myTurn = true;
        }
    }

    private void receiveMove() throws IOException {
        int row = fromServer.readInt();
        int column = fromServer.readInt();
        Platform.runLater(() -> {
            try {
                cell[row][column].setToken(otherToken);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        });
    }

    public class Cell extends Pane {
        private int row;
        private int column;

        private char token = ' ';

        public Cell(int row, int column) {
            this.row = row;
            this.column = column;
            this.setPrefSize(2000, 2000);
            setStyle("-fx-border-color: darkred");
            this.setOnMouseClicked(e -> {
                try {
                    handleMouseClick();
                } catch (MalformedURLException e1) {
                    e1.printStackTrace();
                }
            });
        }

        public char getToken() {
            return token;
        }

        public void setToken(char c) throws MalformedURLException {
            token = c;
            repaint();
        }

        protected void repaint() throws MalformedURLException {
            if (token == 'X') {
                File file1 = new File("C:\\Users\\555\\IdeaProjects\\Game2sem\\net\\src\\main\\java\\client\\X.png");
                Image image1 = new Image(file1.toURI().toURL().toString());
                ImageView imageView1 = new ImageView(image1);
                this.getChildren().addAll(imageView1);
            }
            else if (token == 'O') {
                File file2 = new File("C:\\Users\\555\\IdeaProjects\\Game2sem\\net\\src\\main\\java\\client\\O.png");
                Image image2 = new Image(file2.toURI().toURL().toString());
                ImageView imageView2 = new ImageView(image2);
                getChildren().add(imageView2);
            }
        }

        private void handleMouseClick() throws MalformedURLException {
            if (token == ' ' && myTurn) {
                setToken(myToken);
                myTurn = false;
                rowSelected = row;
                columnSelected = column;
                lblStatus.setText("Ждём хода другого игрока");
                waiting = false;
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}