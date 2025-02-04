import java.util.*;
import java.util.stream.Collectors;

public class ErrorAnalyzer {
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
        for (String word : text.toLowerCase().split("\\\\W+")) {
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

    public List<String> orderSolutions(String errorHeader, List<String> solutions) {
        return solutions.stream()
                .sorted((s1, s2) -> relevanceScore(errorHeader, s2) - relevanceScore(errorHeader, s1)) // Sort by score
                .collect(Collectors.toList());
    }

    // 🔹 Computes a basic relevance score (more advanced NLP could be used)
    private int relevanceScore(String error, String solution) {
        int score = 0;
        if (solution.toLowerCase().contains(error.toLowerCase())) {
            score += 10; // Higher score if the solution directly mentions the error
        }
        if (solution.length() < 150) {
            score += 5; // Prefer shorter solutions (more concise)
        }
        return score;
    }
}
