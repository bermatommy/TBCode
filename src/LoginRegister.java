import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LoginRegister {
    private static Connection con;
    private static Stage registerStage;

    public static Connection getConnection() {
        return con; // Ensure connection is managed correctly
    }

    public static void connect(String username, String password) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/tbcode?useSSL=false&allowPublicKeyRetrieval=true",
                username, password
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void disconnect() {
        try {
            if (con != null && !con.isClosed()) {
                con.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    

    public static boolean isUserValid(String username, String password) {
        if (!isConnected()) return false;
        
        String sql = "SELECT COUNT(*) FROM Users WHERE Username = ? AND Password = ?";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean registerUser(String username, String password) {
        if (!isConnected()) return false;

        String sql = "INSERT INTO Users (Username, Password) VALUES (?, ?)";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private static void openRegisterWindow() {
        if (registerStage != null && registerStage.isShowing()) {
            registerStage.toFront();
            return;
        }

        registerStage = new Stage();
        registerStage.setTitle("Register");

        VBox registerBox = new VBox(10);
        registerBox.setPadding(new Insets(20));

        Label usernameLabel = new Label("New Username:");
        TextField usernameField = new TextField();

        Label passwordLabel = new Label("New Password:");
        PasswordField passwordField = new PasswordField();

        Button submitButton = new Button("Register");
        submitButton.setOnAction(e -> {
            if (registerUser(usernameField.getText(), passwordField.getText())) {
                showAlert("Registration Successful", "You can now log in.");
                registerStage.close();
            } else {
                showAlert("Registration Failed", "Username may already exist.");
            }
        });

        registerBox.getChildren().addAll(usernameLabel, usernameField, passwordLabel, passwordField, submitButton);
        Scene registerScene = new Scene(registerBox, 400, 250);
        registerStage.setScene(registerScene);
        registerStage.show();
    }

    private static void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static boolean isConnected() {
        try {
            if (con == null || con.isClosed()) {
                System.out.println("Database is NOT connected!");
                return false;
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    

    public static Scene setupLoginScene(Stage primaryStage, Scene homeScene) {
        VBox loginBox = new VBox(10);
        loginBox.setPadding(new Insets(20));
    
        Label usernameLabel = new Label("Username:");
        TextField usernameField = new TextField();
    
        Label passwordLabel = new Label("Password:");
        PasswordField passwordField = new PasswordField();
    
        Button loginButton = new Button("Login");
        loginButton.setOnAction(e -> {
            if (isUserValid(usernameField.getText(), passwordField.getText())) {
                primaryStage.setScene(homeScene);
            } else {
                showAlert("Login Failed", "Invalid username or password.");
            }
        });
    
        Button registerButton = new Button("Register");
        registerButton.setOnAction(e -> openRegisterWindow());
    
        loginBox.getChildren().addAll(usernameLabel, usernameField, passwordLabel, passwordField, loginButton, registerButton);
        return new Scene(loginBox, 400, 300);
    }
    
    public static List<String> getAllErrors() {
        List<String> errors = new ArrayList<>();
        String sql = "SELECT Header FROM Errors";
        try (Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                errors.add(rs.getString("Header"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return errors;
    }

    public static boolean storeSolution(String errorType, String description, String codeSolution) {
        if (!isConnected()) return false;
        String sql = "INSERT INTO Solutions (ErrorID, UserID, CodeSolution, Description, SolutionScore) " +
                     "SELECT ErrorID, 1, ?, ?, 0 FROM Errors WHERE Header = ? LIMIT 1";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, codeSolution);
            pstmt.setString(2, description);
            pstmt.setString(3, errorType);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static Solution getBestMatchingSolution(String errorHeader) {
        if (!isConnected()) return null;
    
        String sql = "SELECT s.SolutionID, s.CodeSolution, s.Description, s.SolutionScore, s.ScoreTimes, s.Relevance " +
                 "FROM Solutions s " +
                 "JOIN Errors e ON s.ErrorID = e.ErrorID " +
                 "WHERE e.Header LIKE ? " +
                 "ORDER BY s.SolutionScore DESC " +
                 "LIMIT 1";
    
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, "%" + errorHeader + "%"); // ✅ Find similar errors
            ResultSet rs = pstmt.executeQuery();
    
            if (rs.next()) {
                return new Solution(
                    rs.getInt("SolutionID"),
                    rs.getString("CodeSolution"),
                    rs.getString("Description"),
                    rs.getInt("SolutionScore"),
                    rs.getInt("ScoreTimes"),
                    rs.getInt("Relevance")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
      
    
    
    public static boolean insertError(int userId, String header, String description, String code, String tags) {
        if (!isConnected()) return false;

        String sql = "INSERT INTO Errors (UserID, Header, Description, Code, Tags) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, header);
            pstmt.setString(3, description);
            pstmt.setString(4, code);
            pstmt.setString(5, tags);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static Solution getSolutionByID(int solutionID) {
        if (!isConnected()) return null;
    
        String sql = "SELECT SolutionID, CodeSolution, Description, SolutionScore, ScoreTimes, Relevance " +
                     "FROM Solutions WHERE SolutionID = ?";
    
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setInt(1, solutionID);
            ResultSet rs = pstmt.executeQuery();
    
            if (rs.next()) {
                return new Solution(
                    rs.getInt("SolutionID"),
                    rs.getString("CodeSolution"),
                    rs.getString("Description"),
                    rs.getInt("SolutionScore"),
                    rs.getInt("ScoreTimes"),
                    rs.getDouble("Relevance")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    

    public static Solution getSolution(int solutionID) {
        if (!isConnected()) return null;
    
        String sql = "SELECT SolutionID, CodeSolution, Description, SolutionScore, ScoreTimes, Relevance " +
                     "FROM Solutions WHERE SolutionID = ?";
    
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setInt(1, solutionID);
            ResultSet rs = pstmt.executeQuery();
    
            if (rs.next()) {
                return new Solution(
                    rs.getInt("SolutionID"),
                    rs.getString("CodeSolution"),
                    rs.getString("Description"),
                    rs.getInt("SolutionScore"),
                    rs.getInt("ScoreTimes"),
                    rs.getDouble("Relevance")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    

    // Fetch the best solution for a given error
    public static Solution getBestSolution(String errorHeader) {
        if (!isConnected()) return null;

        String sql = "SELECT s.SolutionID, s.CodeSolution, s.Description, s.SolutionScore, s.ScoreTimes, s.Relevance " +
                    "FROM Solutions s " +
                    "JOIN Errors e ON s.ErrorID = e.ErrorID " +
                    "WHERE e.Header LIKE ? " +
                    "ORDER BY s.SolutionScore DESC " +
                    "LIMIT 1";

        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, "%" + errorHeader + "%");
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new Solution(
                    rs.getInt("SolutionID"),
                    rs.getString("CodeSolution"),
                    rs.getString("Description"),
                    rs.getInt("SolutionScore"),
                    rs.getInt("ScoreTimes"),
                    rs.getDouble("Relevance")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Update the solution score
    public static void updateSolutionScore(int solutionID, int newRating) {
        if (!isConnected()) return;
    
        String sql = "UPDATE Solutions " +
                     "SET SolutionScore = ((SolutionScore * ScoreTimes) + ?) / (ScoreTimes + 1), " +
                     "ScoreTimes = ScoreTimes + 1 " +
                     "WHERE SolutionID = ?";
    
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setInt(1, newRating);
            pstmt.setInt(2, solutionID);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }    

    // Get the updated score of a solution
    public static int getSolutionScore(int solutionID) {
        if (!isConnected()) return 0;

        String sql = "SELECT SolutionScore FROM Solutions WHERE SolutionID = ?";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setInt(1, solutionID);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("SolutionScore");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }


    public static List<Solution> getSolutionsForError(int errorId) {
        List<Solution> solutions = new ArrayList<>();
        if (!isConnected()) return solutions;
    
        String sql = "SELECT SolutionID, CodeSolution, Description, SolutionScore, ScoreTimes, Relevance FROM Solutions WHERE ErrorID = ?";
    
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setInt(1, errorId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                solutions.add(new Solution(
                    rs.getInt("SolutionID"),
                    rs.getString("CodeSolution"),
                    rs.getString("Description"),
                    rs.getInt("SolutionScore"),
                    rs.getInt("ScoreTimes"),
                    rs.getDouble("Relevance")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return solutions;
    }
    
    
    

    public static int getErrorIdByHeader(String header) {
        int errorId = -1;
        String sql = "SELECT ErrorID FROM Errors WHERE Header = ?";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, header);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                errorId = rs.getInt("ErrorID");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return errorId;
    }

    /**
     * Fetches the average score and review count for each solution.
     * return List of results with SolutionID, AvgScore, and ReviewCount
     */
    public static List<String> getSolutionScoresAndReviews() {
        List<String> results = new ArrayList<>();
        String sql = "SELECT SolutionID, AVG(SolutionScore) AS AvgSolutionScore, " +
                     "COUNT(SolutionScore) AS ReviewCount FROM Solutions GROUP BY SolutionID";

        try (PreparedStatement pstmt = con.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                int solutionId = rs.getInt("SolutionID");
                double avgScore = rs.getDouble("AvgSolutionScore");
                int reviewCount = rs.getInt("ReviewCount");

                results.add("SolutionID: " + solutionId + ", Avg Score: " + avgScore + ", Reviews: " + reviewCount);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return results;
    }

    // Example method to call this function from other parts of the code
    public static void printSolutionScores() {
        List<String> scores = getSolutionScoresAndReviews();
        for (String score : scores) {
            System.out.println(score);
        }
    }
}
