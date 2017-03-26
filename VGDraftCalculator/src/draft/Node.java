package draft;

import java.util.HashSet;
import java.util.Set;

public class Node implements Comparable<Node> {
	
	private Hero hero;
	private Set<Edge> synergies;
	private Set<Edge> versus;
	
	public Node(Hero hero) {
		this.hero = hero;
	}
	
	public Hero hero() { return hero; }
	public Set<Edge> synergies() { return new HashSet<>(synergies); }
	public Set<Edge> versus() { return new HashSet<>(versus); }
	
	public Set<Edge> edges() {
		Set<Edge> edges = new HashSet<>();
		edges.addAll(synergies);
		edges.addAll(versus);
		return edges;
	}
	
	public Double score(Hero hero, boolean withOrAgainst) {
		Set<Edge> edges;
		if (withOrAgainst)
			edges = synergies;
		else
			edges = versus;
		for (Edge edge : edges) {
			if (edge.touches(hero)) // danger: assume well constructed graph
				return edge.weight();
		}
		throw new IllegalArgumentException(String.format(
				"Graph not well constructed. No %s edge from %s to %s.", 
				withOrAgainst ? "synergy" : "versus", this.hero, hero));
	}
	
	public Double score() {
		Double score = 1.0;
		for (Edge syn : synergies)
			score *= syn.weight();
		for (Edge vs : versus)
			score *= vs.weight();
		return Math.pow(score, 1 / (double) (synergies.size() + versus.size()));
	}
	
	public void addEdge(Edge edge, boolean withOrAgainst) {
		if (withOrAgainst)
			synergies.add(edge);
		else
			versus.add(edge);
	}
	
	public void absorb(Node other) {
		if (other.hero != this.hero)
			throw new IllegalArgumentException("Cannot blend with a different hero Node");
		synergies.addAll(other.synergies);
		versus.addAll(other.versus);
	}

	@Override
	public int compareTo(Node o) {
		// higher scores first
		return o.score().compareTo(this.score());
	}
	
}
