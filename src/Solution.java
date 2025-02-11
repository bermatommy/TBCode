public class Solution {
    private int solutionID;
    private String codeSolution;
    private String description;
    private int solutionScore;
    private int scoreTimes;
    private double relevance;

    public Solution(int solutionID, String codeSolution, String description, int solutionScore, int scoreTimes, double relevance) {
        this.solutionID = solutionID;
        this.codeSolution = codeSolution;
        this.description = description;
        this.solutionScore = solutionScore;
        this.scoreTimes = scoreTimes;
        this.relevance = relevance;
    }

    public int getSolutionID() { return solutionID; }
    public String getCodeSolution() { return codeSolution; }
    public String getDescription() { return description; }
    public int getSolutionScore() { return solutionScore; }
    public int getScoreTimes() { return scoreTimes; }
    public double getRelevance() { return relevance; }

    public void setRelevance(double relevance) {
        this.relevance = relevance;
    }
}