package model;

import data.Hero;


public class Edge implements Comparable<Edge> {
	
	private Node from;
	private Node to;
	private Double weight;
	boolean withOrAgainst;
	
	public Edge(Node from, Node to, Double weight, boolean withOrAgainst) {
		this.from = from;
		this.to = to;
		this.weight = weight;
		this.withOrAgainst = withOrAgainst;
	}
	
	public Node from() { return from; }
	
	public Node to() { return to; }
	
	public Double weight() { return weight; }
	
	public boolean withOrAgainst() { return withOrAgainst; }

	public boolean touches(Hero hero) {
		return from.hero() == hero || to.hero() == hero;
	}

	@Override
	public int compareTo(Edge o) {
		// higher values first
		return o.weight.compareTo(this.weight);
	}

}
