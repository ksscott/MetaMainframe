package draft;

public enum Hero {

	ADAGIO("Adagio", "adagio"),
	ALPHA("Alpha", "alpha"),
	ARDAN("Ardan", "ardan"),
	BARON("Baron", "baron"),
	BLACKFEATHER("Blackfeather", "blackfeather"),
	CATHERINE("Catherine", "catherine"),
	CELESTE("Celeste", "celeste"),
	FLICKER("Flicker", "flicker"),
	FORTRESS("Fortress", "fortress"),
	GWEN("Gwen", "gwen"),
	IDRIS("Idris", "idris"),
	JOULE("Joule", "joule"),
	KESTREL("Kestrel", "kestrel"),
	KOSHKA("Koshka", "koshka"),
	KRUL("Krul", "krul"),
	LANCE("Lance", "lance"),
	LYRA("Lyra", "lyra"),
	OZO("Ozo", "ozo"),
	PHINN("Phinn", "phinn"),
	REIM("Reim", "reim"),
	RINGO("Ringo", "ringo"),
	RONA("Rona", "rona"),
	SAMUEL("Samuel", "samuel"),
	SAW("SAW", "saw"),
	SKAARF("Skaarf", "skaarf"),
	SKYE("Skye", "skye"),
	TAKA("Taka", "taka"),
	VOX("Vox", "vox");
	
	private final String name;
	private final String code;
	
	private Hero(String name, String code) {
		this.name = name;
		this.code = code;
	}
	
	public String getName() {
		return name;
	}
	
	public String getCode() {
		return code;
	}
	
	public static Hero fromCode(String code) {
		for (Hero h : Hero.values()) {
			if (h.code.equals(code))
				return h;
		}
		throw new IllegalArgumentException("Not a hero code: " + code);
	}
	
}
