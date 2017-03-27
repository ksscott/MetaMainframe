package draft;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import data.Hero;
import data.Pick;

public class Roster implements Iterable<Hero>,Cloneable {

	private int finalSize;
	private Set<Hero> picked;
	
	public Roster(int finalSize) {
		this.finalSize = finalSize;
		this.picked = new HashSet<Hero>();
	}
	
	@Override
	public Roster clone() {
		Roster clone = new Roster(finalSize);
		clone.picked = new HashSet<Hero>(picked);
//		System.out.println("CLONE " + picked + " => " + clone.picked);
		return clone;
	}
	
	public Roster whatIf(Hero hero) {
		Roster hypothetical = this.clone();
		hypothetical.add(hero);
//		System.out.println(toString() + " => whatIf(" + hero + ") => " + hypothetical.toString());
		return hypothetical;
	}
	
	public int size() { return picked.size(); }
	public int fullSize() { return finalSize; }
	public int room() { return fullSize() - size(); }
	public boolean isEmpty() { return picked.isEmpty(); }
	public boolean isFull() { return room() == 0; }
	public Set<Hero> getPicked() { return picked; }
	
	public Roster add(Hero newHero) {
		if (picked.size() < finalSize)
			picked.add(newHero);
		else
			throw new IllegalStateException("Roster is already full. (Nice try, though!) " + toString());
		return this;
	}
	
	/**
	 * Adds heroes from the given list in order until this roster is full
	 */
	public Roster fill(List<Pick> recruits) {
		for (Pick pick : recruits) {
			if (isFull())
				break;
			add(pick.getCandidate());
		}
		return this;
	}
	
	@Override
	public Iterator<Hero> iterator() {
		return picked.iterator();
	}
	
	@Override
	public String toString() {
		String heroes = "";
		for (Hero hero : picked)
			heroes += hero + " ";
		return "Roster: { " + heroes + "}";
	}
}
