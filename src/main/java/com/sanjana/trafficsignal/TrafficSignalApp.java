package com.sanjana.trafficsignal;

import java.util.List;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

public class TrafficSignalApp extends Application {

    // Wait-time thresholds (seconds) that control car color coding
    private static final int WAIT_LOW = 20;   // below this: green (fine)
    private static final int WAIT_MEDIUM = 50; // below this: yellow (getting long); above: red
    private static final int MAX_CARS_SHOWN = 8; // cap how many car icons are drawn per side

    private SimulationEngine engine;
    private Timeline timeline;

    private final java.util.Map<Side, Rectangle> signalBoxes = new java.util.EnumMap<>(Side.class);
    private final java.util.Map<Side, Label> queueLabels = new java.util.EnumMap<>(Side.class);
    private final java.util.Map<Side, HBox> carContainers = new java.util.EnumMap<>(Side.class);
    private Label clockLabel;
    private Label metricsLabel;

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));

        GridPane grid = buildIntersectionGrid();
        root.setCenter(grid);

        HBox controls = buildControls();
        root.setBottom(controls);

        clockLabel = new Label("t = 0s");
        metricsLabel = new Label("Avg wait: -- | Cleared: --");
        VBox topBox = new VBox(5, clockLabel, metricsLabel);
        topBox.setAlignment(Pos.CENTER);
        root.setTop(topBox);

        Scene scene = new Scene(root, 550, 600);
        stage.setTitle("Traffic Signal Simulation");
        stage.setScene(scene);
        stage.show();
    }

    private GridPane buildIntersectionGrid() {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(20);
        grid.setVgap(20);

        Rectangle center = new Rectangle(80, 80, Color.LIGHTGRAY);
        Label centerLabel = new Label("Intersection");

        grid.add(buildSideBox(Side.NORTH), 1, 0);
        grid.add(buildSideBox(Side.WEST), 0, 1);
        grid.add(new StackPane(center, centerLabel), 1, 1);
        grid.add(buildSideBox(Side.EAST), 2, 1);
        grid.add(buildSideBox(Side.SOUTH), 1, 2);

        return grid;
    }

    private VBox buildSideBox(Side side) {
        Rectangle box = new Rectangle(70, 70, Color.GRAY);
        Label sideName = new Label(side.toString());
        Label queueLabel = new Label("0 waiting");

        HBox carRow = new HBox(3);
        carRow.setAlignment(Pos.CENTER);
        carRow.setPrefWidth(160);
        carRow.setPrefHeight(20);

        signalBoxes.put(side, box);
        queueLabels.put(side, queueLabel);
        carContainers.put(side, carRow);

        VBox box1 = new VBox(5, sideName, box, carRow, queueLabel);
        box1.setAlignment(Pos.CENTER);
        return box1;
    }

    private HBox buildControls() {
        ComboBox<String> algoSelector = new ComboBox<>();
        algoSelector.getItems().addAll("Hybrid", "Priority + Aging");
        algoSelector.setValue("Hybrid");

        Button startButton = new Button("Start");
        startButton.setOnAction(e -> startSimulation(algoSelector.getValue()));

        Button stopButton = new Button("Stop");
        stopButton.setOnAction(e -> { if (timeline != null) timeline.stop(); });

        HBox box = new HBox(15, algoSelector, startButton, stopButton);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(15, 0, 0, 0));
        return box;
    }

    private void startSimulation(String algorithmName) {
        if (timeline != null) timeline.stop();

        SignalController controller = algorithmName.equals("Hybrid")
                ? new HybridController()
                : new PriorityAgingController(0.2);

        List<Vehicle> traffic = TrafficGenerator.generate(SimulationConfig.SIMULATION_DURATION, 4, 42);
        engine = new SimulationEngine(controller, traffic, SimulationConfig.SIMULATION_DURATION);

        timeline = new Timeline(new KeyFrame(Duration.millis(150), e -> tick()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void tick() {
        if (engine.isFinished()) {
            timeline.stop();
            return;
        }
        engine.step();
        updateUI();
    }

    private void updateUI() {
        Intersection intersection = engine.getIntersection();
        Side greenSide = intersection.getCurrentGreenSide();
        int currentTime = intersection.getCurrentTime();

        for (Side side : Side.values()) {
            boolean isGreen = side == greenSide;
            Lane lane = intersection.getLane(side);

            signalBoxes.get(side).setFill(isGreen ? Color.LIMEGREEN : Color.GRAY);
            queueLabels.get(side).setText(lane.getQueueLength() + " waiting");
            updateCarRow(side, lane, currentTime);
        }

        metricsLabel.setText(String.format("Avg wait: %.1f sec | Cleared: %d",
                engine.getMetrics().getAverageWaitTime(), engine.getMetrics().getTotalVehiclesCleared()));
    }

    // Draws one small rectangle per waiting vehicle, colored by how long that specific vehicle has waited
    private void updateCarRow(Side side, Lane lane, int currentTime) {
        HBox row = carContainers.get(side);
        row.getChildren().clear();

        List<Vehicle> vehicles = lane.getVehiclesSnapshot();
        int carsToShow = Math.min(vehicles.size(), MAX_CARS_SHOWN);

        for (int i = 0; i < carsToShow; i++) {
            Vehicle v = vehicles.get(i);
            int waitTime = v.getWaitTime(currentTime);

            Rectangle car = new Rectangle(12, 8, waitColor(waitTime));
            car.setArcWidth(4);
            car.setArcHeight(4);
            row.getChildren().add(car);
        }

        if (vehicles.size() > MAX_CARS_SHOWN) {
            Label overflow = new Label("+" + (vehicles.size() - MAX_CARS_SHOWN));
            overflow.setStyle("-fx-font-size: 10px;");
            row.getChildren().add(overflow);
        }
    }

    // Green = recently arrived, Yellow = waiting a while, Red = waiting long enough to be a concern
    private Color waitColor(int waitTime) {
        if (waitTime < WAIT_LOW) {
            return Color.LIMEGREEN;
        } else if (waitTime < WAIT_MEDIUM) {
            return Color.GOLD;
        } else {
            return Color.CRIMSON;
        }
    }

    // Helper just for the top metrics label — not part of MetricsCollector's own summary
    

    public static void main(String[] args) {
        launch(args);
    }
}