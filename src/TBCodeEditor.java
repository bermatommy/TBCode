import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.awt.Desktop;

public class TBCodeEditor extends Application {
    private TextArea outputConsole;
    private TextArea codeEditor;
    private TextArea lineNumbers;
    private Scene homeScene;
    private ErrorAnalyzer errorAnalyzer = new ErrorAnalyzer();
    private ListView<String> errorList;
    private ListView<String> solutionsList;
    private Button runButton;
    private Button submitFixButton;
    private MenuBar menuBar;
    private String currentTheme = "Dark Mode"; // Default theme
    private Process flaskProcess;

    @Override
    public void start(Stage primaryStage) {
        LoginRegister.connect("root", "1234");
        homeScene = buildHomeScene(primaryStage);
        applyTheme(getTheme());
        Scene loginScene = LoginRegister.setupLoginScene(primaryStage, homeScene);
        primaryStage.setScene(loginScene);
        primaryStage.setTitle("TBCode - Login");
        primaryStage.setWidth(800);
        primaryStage.setHeight(600);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> stopPythonServer()));
        primaryStage.setOnCloseRequest(event -> {
            saveCode();
            LoginRegister.disconnect();
        });
        primaryStage.show();
    }

    private Scene buildHomeScene(Stage primaryStage) {       
        MenuBar menuBar = new MenuBar();
        Menu homeMenu = new Menu("Home");
        Menu forumMenu = new Menu("Forum");

        Menu settingsMenu = new Menu("Settings");
        MenuItem changeTheme = new MenuItem("Change UI Theme");

        Button runButton = new Button("Run Code");
        Button submitFixButton = new Button("Submit a Fix");


        changeTheme.setOnAction(e -> openSettingsWindow());
        settingsMenu.getItems().add(changeTheme);
        menuBar.getMenus().addAll(homeMenu, forumMenu, settingsMenu); // ✅ Ensure Settings is added

        // Inside `buildHomeScene` method
        MenuItem openForum = new MenuItem("Open Forum");
        openForum.setOnAction(e -> {
            startPythonServer();  // Start the Flask server automatically
            openForumPage();      // Open the forum webpage
        });

        forumMenu.getItems().add(openForum); // Add to the menu


        codeEditor = new TextArea();
        codeEditor.setWrapText(true);
        codeEditor.setPrefHeight(400);
        codeEditor.setPadding(new Insets(10));

        lineNumbers = new TextArea();
        lineNumbers.setEditable(false);
        lineNumbers.setPrefWidth(40);
        lineNumbers.setPrefHeight(codeEditor.getPrefHeight());

        HBox codeEditorBox = new HBox(5, lineNumbers, codeEditor);
        HBox.setHgrow(codeEditor, Priority.ALWAYS);

        codeEditor.setOnKeyReleased(this::updateLineNumbers);
        updateLineNumbers(null);
        loadLastCode();

        Button openSolutionButton = new Button("Open Solution");
        openSolutionButton.setOnAction(e -> openSolutionWindow());


        runButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: black;");
        runButton.setOnAction(e -> runCodeInBackground());

        outputConsole = new TextArea();
        outputConsole.setEditable(false);
        outputConsole.setPrefHeight(150);
        outputConsole.setPromptText("Output console...");

        Label errorLabel = new Label("Errors:");
        errorList = new ListView<>();
        populateErrors();

        Label solutionLabel = new Label("Solutions:");
        solutionsList = new ListView<>();

        errorList.setOnMouseClicked(event -> {
            String selectedError = errorList.getSelectionModel().getSelectedItem();
            if (selectedError != null) {
                populateSolutions(selectedError);
            }
        });
        
        submitFixButton.setOnAction(e -> {
            String selectedError = errorList.getSelectionModel().getSelectedItem();
            if (selectedError != null) {
                TicketWindow.open(selectedError, outputConsole.getText(), codeEditor.getText()); 
            } else {
                showAlert("No Error Selected", "Please select an error to submit a fix.");
            }
        });       

        VBox solutionBox = new VBox(10, errorLabel, errorList, solutionLabel, solutionsList, submitFixButton, openSolutionButton);
        solutionBox.setPadding(new Insets(10));


        VBox editorBox = new VBox(10, menuBar, runButton, codeEditorBox, outputConsole);
        editorBox.setPadding(new Insets(10));

        HBox mainBox = new HBox(15, editorBox, solutionBox);
        mainBox.setPadding(new Insets(10));
        solutionBox.setPrefWidth(300);
        HBox.setHgrow(editorBox, Priority.ALWAYS);

        return new Scene(mainBox, 800, 600);
    }

    private void startPythonServer() {
        try {
            if (flaskProcess == null || !flaskProcess.isAlive()) {
                // ✅ Set working directory to the folder containing app.py
                File workingDir = new File("C:\\Users\\berma\\School Ort Bialik\\Final Project\\TBCode - Copy\\TBCode\\src\\HTML");
    
                ProcessBuilder processBuilder = new ProcessBuilder("python", "app.py"); // ✅ Run app.py
                processBuilder.directory(workingDir); // ✅ Set working directory to the folder
    
                processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
                flaskProcess = processBuilder.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Could not start the Flask server.");
        }
    }
    
    
    

    private void stopPythonServer() {
        if (flaskProcess != null && flaskProcess.isAlive()) {
            flaskProcess.destroy(); // Stop Flask when Java app closes
        }
    }
    

    private void openForumPage() {
        try {
            Desktop.getDesktop().browse(new URI("http://127.0.0.1:5000/")); // Open local Flask webpage
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Could not open the forum webpage.");
        }
    }

    private void populateErrors() {
        errorList.getItems().clear();
        Set<String> uniqueErrors = new HashSet<>(LoginRegister.getAllErrors());
        if (!uniqueErrors.isEmpty()) {
            errorList.getItems().addAll(uniqueErrors);
        }
    }

    private void addErrorIfNotExists(String errorMessage) {
        Platform.runLater(() -> {
            // ✅ Check if the message contains "error" or "exception" (case-insensitive)
            if (errorMessage.toLowerCase().contains("error") || errorMessage.toLowerCase().contains("exception")) {
                // ✅ Only add if the error doesn't already exist
                if (!errorList.getItems().contains(errorMessage)) {
                    errorList.getItems().add(errorMessage);
                    LoginRegister.insertError(1, errorMessage, "Auto-detected error", "", "");
                }
            }
        });
    }    

    private void populateSolutions(String errorHeader) {
        solutionsList.getItems().clear();
        int errorId = LoginRegister.getErrorIdByHeader(errorHeader);
        List<String> solutions = errorAnalyzer.orderSolutions(errorHeader, LoginRegister.getSolutionsForError(errorId));
        Platform.runLater(() -> {
            if (solutions.isEmpty()) {
                solutionsList.getItems().add("No solutions available.");
            } else {
                solutionsList.getItems().addAll(solutions);
            }
        });
    }      

    private void runCodeInBackground() {
        outputConsole.clear();
        Task<String> task = new Task<String>() {
            @Override
            protected String call() {
                // ✅ Get full output from Compile class
                String result = Compile.runCodeAndHandleExceptions(codeEditor.getText());
                // ✅ Split the result into lines
                String[] lines = result.split("\n");
                // ✅ Extract the first line as the error message
                String errorHeader = lines.length > 0 ? lines[0] : "Unknown error";
                // ✅ Ensure full exception/error details are printed in the console
                Platform.runLater(() -> outputConsole.setText(result));
                // ✅ Only add errors that contain "error" or "exception"
                addErrorIfNotExists(errorHeader);
                return result;
            }
        };
        task.setOnSucceeded(event -> outputConsole.setText(task.getValue()));
        new Thread(task).start();
    }    
    

    private void saveCode() {
        try {
            Files.write(Paths.get("last_code.txt"), codeEditor.getText().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadLastCode() {
        try {
            File file = new File("last_code.txt");
            if (file.exists()) {
                codeEditor.setText(new String(Files.readAllBytes(file.toPath())));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateLineNumbers(KeyEvent event) {
        StringBuilder lineNumbersText = new StringBuilder();
        int lineCount = codeEditor.getText().split("").length;
        for (int i = 1; i <= lineCount; i++) {
            lineNumbersText.append(i).append("");
        }
        lineNumbers.setText(lineNumbersText.toString());
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private void openSettingsWindow() {
        Stage settingsStage = new Stage();
        settingsStage.setTitle("Settings");

        Label colorLabel = new Label("Choose UI Theme:");
        ChoiceBox<String> colorChoice = new ChoiceBox<>();
        colorChoice.setValue(getTheme());
        colorChoice.getItems().addAll("Light Mode", "Dark Mode", "Blue Theme");

        Button applyButton = new Button("Apply");
        applyButton.setOnAction(e -> applyTheme(colorChoice.getValue()));

        VBox layout = new VBox(10, colorLabel, colorChoice, applyButton);
        layout.setPadding(new Insets(20));
        Scene scene = new Scene(layout, 300, 200);
        settingsStage.setScene(scene);
        settingsStage.show();
    }

    private void openSolutionWindow() {
        String selectedError = errorList.getSelectionModel().getSelectedItem();
        
        if (selectedError == null) {
            showAlert("No Error Selected", "Please select an error to view a solution.");
            return;
        }
    
        // Get the best solution from the database
        Solution bestSolution = LoginRegister.getBestSolution(selectedError);
    
        if (bestSolution == null) {
            showAlert("No Solution Found", "No solutions available for this error.");
            return;
        }
    
        // Create the solution window
        Stage solutionStage = new Stage();
        solutionStage.setTitle("Solution for: " + selectedError);
    
        Label solutionLabel = new Label("Solution Code:");
        TextArea solutionCodeArea = new TextArea(bestSolution.getCodeSolution());
        solutionCodeArea.setEditable(false);
    
        Label descriptionLabel = new Label("Description:");
        TextArea solutionDescriptionArea = new TextArea(bestSolution.getDescription());
        solutionDescriptionArea.setEditable(false);
    
        Label scoreLabel = new Label("Current Score: " + bestSolution.getSolutionScore());
    
        Label rateLabel = new Label("Rate this solution (1-100):");
        TextField rateField = new TextField();
        rateField.setPromptText("Enter a rating...");
    
        Button rateButton = new Button("Submit Rating");
        rateButton.setOnAction(e -> {
            try {
                int rating = Integer.parseInt(rateField.getText().trim());
                if (rating < 1 || rating > 100) {
                    showAlert("Invalid Rating", "Please enter a number between 1 and 100.");
                    return;
                }
    
                // Update solution score in database
                LoginRegister.updateSolutionScore(bestSolution.getSolutionID(), rating);

                Solution updatedSolution = LoginRegister.getBestSolution(selectedError);
    
                // Refresh score display
                scoreLabel.setText("Current Score: " + updatedSolution.getSolutionScore() +
                           " (Based on " + updatedSolution.getScoreTimes() + " reviews)");
    
                showAlert("Success", "Your rating has been submitted!");
            } catch (NumberFormatException ex) {
                showAlert("Invalid Input", "Please enter a valid number between 1 and 100.");
            }
        });
    
        VBox layout = new VBox(10, solutionLabel, solutionCodeArea, descriptionLabel, solutionDescriptionArea, scoreLabel, rateLabel, rateField, rateButton);
        layout.setPadding(new Insets(20));
    
        Scene scene = new Scene(layout, 600, 400);
        solutionStage.setScene(scene);
        solutionStage.show();
    }    

    private void applyTheme(String theme) {
        currentTheme = theme; // ✅ Store the selected theme
    
        String backgroundColor = "";
        String textColor = "";
        String buttonColor = "";
        String outputBackground = "";
        String outputText = "";
        
        if ("Light Mode".equals(theme)) {
            backgroundColor = "white";
            textColor = "black";
            buttonColor = "#4CAF50";
            outputBackground = "white";
            outputText = "black";
        } else if ("Dark Mode".equals(theme)) {
            backgroundColor = "#1E1E1E";
            textColor = "white";
            buttonColor = "#4CAF50";
            outputBackground = "#1E1E1E";
            outputText = "white";
        } else if ("Blue Theme".equals(theme)) {
            backgroundColor = "#2D2D30";
            textColor = "white";
            buttonColor = "#4CAF50";
            outputBackground = "#35355E";
            outputText = "white";
        }    
    
        // ✅ Apply theme to scene root
        if (homeScene != null && homeScene.getRoot() != null) {
            homeScene.getRoot().setStyle("-fx-background-color: " + backgroundColor + "; -fx-text-fill: " + textColor + ";");
        }
    
        if (menuBar != null) {
            menuBar.setStyle("-fx-background-color: " + backgroundColor + "; -fx-text-fill: " + textColor + ";");
        }
    
        if (runButton != null) {
            runButton.setStyle("-fx-background-color: " + buttonColor + "; -fx-text-fill: " + textColor + ";");
        }
    
        if (submitFixButton != null) {
            submitFixButton.setStyle("-fx-background-color: " + buttonColor + "; -fx-text-fill: " + textColor + ";");
        }
    
        if (outputConsole != null) {
            outputConsole.setStyle("-fx-control-inner-background: " + outputBackground + "; -fx-text-fill: " + outputText + ";");
        }
    
        if (codeEditor != null) {
            codeEditor.setStyle("-fx-control-inner-background: " + outputBackground + "; -fx-text-fill: " + outputText + ";");
        }
    
        if (lineNumbers != null) {
            lineNumbers.setStyle("-fx-control-inner-background: " + outputBackground + "; -fx-text-fill: " + outputText + ";");
        }
    
        if (errorList != null) {
            errorList.setStyle("-fx-control-inner-background: " + outputBackground + "; -fx-text-fill: " + outputText + ";");
        }
    
        if (solutionsList != null) {
            solutionsList.setStyle("-fx-control-inner-background: " + outputBackground + "; -fx-text-fill: " + outputText + ";");
        }
    }

    public String getTheme() {
        return currentTheme;
    }
}