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
		// "higher" scores come "first" -> (.70, .65, .55, ...)
		return o.score.compareTo(score);
	}
	
	/**
	 * @return a {@code Pick} of this hero with a combined score of this and the given picks
	 */
	public Pick blend(Pick other) {
		if (this.candidate != other.candidate)
			throw new IllegalArgumentException("What does it mean to blend scores with a different Hero?");
		return new Pick(candidate, Math.pow((this.score * other.score), .5));
	}

	@Override
	public String toString() {
		return "(" + candidate.getName() + ": " + String.format("%.3f", score) + ")";
	}
}
