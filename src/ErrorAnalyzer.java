import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

public class ErrorAnalyzer {
    public void updateSolutionRelevanceInDB(String errorHeader, String outputConsole, String userCode, String userDescription, List<Solution> solutions) {
        Connection con = null;
        PreparedStatement pstmt = null;
    
        try {
            con = LoginRegister.getConnection();
    
            // If connection is null or closed, reconnect and retrieve a new one
            if (con == null || con.isClosed()) {
                System.out.println("Database connection is closed! Reconnecting...");
                LoginRegister.connect("root", "1234");  // Connects to DB
                con = LoginRegister.getConnection();    // Retrieves the connection again
            }
    
            if (con == null) {
                System.out.println("Failed to establish database connection.");
                return; // Exit method if no connection is available
            }
    
            String updateQuery = "UPDATE Solutions SET Relevance = ? WHERE SolutionID = ?";
            pstmt = con.prepareStatement(updateQuery);
    
            for (Solution solution : solutions) {
                double relevance = computeRelevance(errorHeader, outputConsole, userCode, userDescription, solution);
                solution.setRelevance(relevance);
    
                pstmt.setDouble(1, relevance);
                pstmt.setInt(2, solution.getSolutionID());
                pstmt.addBatch();
            }
    
            pstmt.executeBatch();
    
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    

    private double computeRelevance(String errorHeader, String outputConsole, String userCode, String userDescription, Solution solution) {
        double errorSimilarity = cosineSimilarity(errorHeader, solution.getDescription());
        double outputSimilarity = cosineSimilarity(outputConsole, solution.getDescription());
        double codeSimilarity = cosineSimilarity(userCode, solution.getCodeSolution());
        double descriptionSimilarity = cosineSimilarity(userDescription, solution.getDescription());
    
        // Weighted sum: Error header (30%), Output console (20%), Code (25%), User description (25%)
        return (0.3 * errorSimilarity) + (0.2 * outputSimilarity) + (0.25 * codeSimilarity) + (0.25 * descriptionSimilarity);
    }
    

    private List<ErrorData> errorDatabase = new ArrayList<>();
    
    public void addError(String errorMessage, String solution) {
        errorDatabase.add(new ErrorData(errorMessage, solution));
    }

    public String findSimilarError(String newError) {
        if (errorDatabase.isEmpty()) return "No similar errors found.";

        ErrorData bestMatch = null;
        double bestScore = 0;

        for (ErrorData error : errorDatabase) {
            double similarity = cosineSimilarity(newError, error.errorMessage);
            if (similarity > bestScore) {
                bestScore = similarity;
                bestMatch = error;
            }
        }

        return (bestMatch != null) ? bestMatch.solution : "No similar errors found.";
    }

    public String classifyError(int lineNumber, int messageLength) {
        if (errorDatabase.isEmpty()) return "Unknown error classification.";

        ErrorData bestMatch = null;
        double bestDistance = Double.MAX_VALUE;

        for (ErrorData error : errorDatabase) {
            double distance = Math.sqrt(Math.pow(lineNumber - error.lineNumber, 2) +
                                        Math.pow(messageLength - error.messageLength, 2));
            if (distance < bestDistance) {
                bestDistance = distance;
                bestMatch = error;
            }
        }

        return (bestMatch != null) ? bestMatch.errorMessage : "Unknown error classification.";
    }

    private double cosineSimilarity(String text1, String text2) {
        Map<String, Integer> freq1 = getWordFrequency(text1);
        Map<String, Integer> freq2 = getWordFrequency(text2);

        Set<String> words = new HashSet<>(freq1.keySet());
        words.addAll(freq2.keySet());

        double dotProduct = 0, norm1 = 0, norm2 = 0;

        for (String word : words) {
            int count1 = freq1.getOrDefault(word, 0);
            int count2 = freq2.getOrDefault(word, 0);

            dotProduct += count1 * count2;
            norm1 += Math.pow(count1, 2);
            norm2 += Math.pow(count2, 2);
        }

        return (norm1 == 0 || norm2 == 0) ? 0 : (dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2)));
    }

    private Map<String, Integer> getWordFrequency(String text) {
        Map<String, Integer> freq = new HashMap<>();
        for (String word : text.toLowerCase().split("\\W+")) {
            freq.put(word, freq.getOrDefault(word, 0) + 1);
        }
        return freq;
    }

    private static class ErrorData {
        String errorMessage, solution;
        int lineNumber, messageLength;

        ErrorData(String errorMessage, String solution) {
            this.errorMessage = errorMessage;
            this.solution = solution;
            this.lineNumber = (int) (Math.random() * 100);  // Placeholder
            this.messageLength = errorMessage.length();
        }
    }

    public List<Solution> orderSolutions(String errorHeader, List<Solution> solutions) {
        // Calculate relevance scores
        for (Solution solution : solutions) {
            double relevance = cosineSimilarity(errorHeader, solution.getDescription());
            solution.setRelevance(relevance);
        }

        // Sort by relevance first
        solutions.sort((s1, s2) -> Double.compare(s2.getRelevance(), s1.getRelevance()));

        // If relevance > threshold (0.7), sort by score
        double threshold = 0.1;
        solutions.sort((s1, s2) -> {
            if (s1.getRelevance() >= threshold && s2.getRelevance() >= threshold) {
                return Integer.compare(s2.getSolutionScore(), s1.getSolutionScore()); // Sort by score
            }
            return Double.compare(s2.getRelevance(), s1.getRelevance()); // Sort by relevance
        });

        return solutions;
    }

    // 🔹 Computes a basic relevance score (more advanced NLP could be used)
    // private int relevanceScore(String error, String solution) {
    //     int score = 0;
    //     if (solution.toLowerCase().contains(error.toLowerCase())) {
    //         score += 10; // Higher score if the solution directly mentions the error
    //     }
    //     if (solution.length() < 150) {
    //         score += 5; // Prefer shorter solutions (more concise)
    //     }
    //     return score;
    // }
}
