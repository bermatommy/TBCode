import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class TicketWindow {
    private static Stage reportStage; // Singleton instance of the stage

    public static void open(String errorMessage, String outputConsoleContent, String codeEditorContent) {
        if (reportStage != null && reportStage.isShowing()) {
            reportStage.toFront();
            return;
        }

        reportStage = new Stage();
        reportStage.setTitle("File Solution Report");

        Label header = new Label("File Solution Report");
        header.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        // Encountered Error/Exception Field
        Label errorLabel = new Label("Encountered Error/Exception:");
        errorLabel.setStyle("-fx-text-fill: white;");
        TextField errorField = new TextField(errorMessage); // Set default value

        // Output Console Field (Full error details)
        Label outputLabel = new Label("Output Console:");
        outputLabel.setStyle("-fx-text-fill: white;");
        TextArea outputConsoleReport = new TextArea();
        outputConsoleReport.setPromptText("Detailed error output...");
        outputConsoleReport.setPrefHeight(150);
        outputConsoleReport.setText(outputConsoleContent);
        outputConsoleReport.setEditable(false);

        // Code Solution Field
        Label codeSolutionLabel = new Label("Code Solution:");
        codeSolutionLabel.setStyle("-fx-text-fill: white;");
        TextArea codeSolutionArea = new TextArea();
        codeSolutionArea.setPromptText("Enter the code that fixed the error...");
        codeSolutionArea.setPrefHeight(150);
        codeSolutionArea.setText(codeEditorContent);

        Label solutionScoreLabel = new Label("Solution Score:");
        solutionScoreLabel.setStyle("-fx-text-fill: white;");
        TextField solutionScoreField = new TextField("0");  // Default score is 0
        solutionScoreField.setEditable(false);

        // Description Field (How the issue was solved)
        Label descriptionLabel = new Label("Solution Description:");
        descriptionLabel.setStyle("-fx-text-fill: white;");
        TextArea descriptionArea = new TextArea();
        descriptionArea.setPromptText("Describe how you fixed the issue...");
        descriptionArea.setPrefHeight(150);

        // Submit Button
        Button submitButton = new Button("Submit Solution");
        submitButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        submitButton.setOnAction(e -> {
            String errorType = errorField.getText().trim();
            String codeSolution = codeSolutionArea.getText().trim();
            String description = descriptionArea.getText().trim();

            if (errorType.isEmpty() || codeSolution.isEmpty() || description.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Error Type, Code Solution, and Description cannot be empty.");
                alert.showAndWait();
                return;
            }

            // Store solution in the database
            if (LoginRegister.storeSolution(errorType, description, codeSolution)) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "✅ Solution Submitted Successfully!");
                alert.showAndWait();
                reportStage.close();
                reportStage = null;
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR, "❌ Failed to submit solution.");
                alert.showAndWait();
            }
        });

        // Back Button
        Button backButton = new Button("Back");
        backButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
        backButton.setOnAction(e -> {
            reportStage.close();
            reportStage = null;
        });

        VBox layout = new VBox(15, header, errorLabel, errorField, outputLabel, outputConsoleReport, 
                codeSolutionLabel, codeSolutionArea, descriptionLabel, descriptionArea, submitButton, backButton);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: #1E1E1E;");

        Scene scene = new Scene(layout, 600, 700);
        reportStage.setScene(scene);

        reportStage.setOnCloseRequest(e -> reportStage = null);

        reportStage.show();
    }
}