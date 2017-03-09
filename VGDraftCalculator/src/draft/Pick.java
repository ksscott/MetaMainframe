package draft;

public class Pick implements Comparable<Pick> {

	private final Hero candidate;
	private final Double score;
	
	public Pick(Hero candidate, double score) {
		this.candidate = candidate;
		this.score = new Double(score);
	}
	
	public Hero getCandidate() {
		return candidate;
	}
	
	public Double getScore() {
		return score;
	}
	
	@Override
	public int compareTo(Pick o) {
		return score.compareTo(o.score);
	}

	@Override
	public String toString() {
		return "(" + candidate.getName() + ": " + String.format("%.3f", score) + ")";
	}
}
