package draft;

public enum Phase {
	BLUE_PICK(true,true), RED_PICK(true,false), BLUE_BAN(false,true), RED_BAN(false,false);

	private final boolean isPick;
	private final boolean isBlue;
	
	private Phase(boolean isPick, boolean isBlue) {
		this.isPick = isPick;
		this.isBlue = isBlue;
	}
	public boolean isPick() { return isPick; }
	public boolean isBlue() { return isBlue; }
}