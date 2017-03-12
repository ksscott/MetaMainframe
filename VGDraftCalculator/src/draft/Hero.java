package draft;

public enum Hero {

	ADAGIO("Adagio", "*Adagio*"),
	ALPHA("Alpha", "*Alpha*"),
	ARDAN("Ardan", "*Ardan*"),
	BARON("Baron", "*Baron"),
	BLACKFEATHER("Blackfeather", "*Blackfeather*"),
	CATHERINE("Catherine", "*Catherine*"),
	CELESTE("Celeste", "*Celeste*"),
	FLICKER("Flicker", "*Flicker*"),
	FORTRESS("Fortress", "*Fortress*"),
	GLAIVE("Glaive", "*Glaive*"),
	GRUMPJAW("Grumpjaw", "*Grumpjaw*"),
	GWEN("Gwen", "*Gwen*"),
	IDRIS("Idris", "*Idris*"),
	JOULE("Joule", "*Joule*"),
	KESTREL("Kestrel", "*Kestrel*"),
	KOSHKA("Koshka", "*Koshka*"),
	KRUL("Krul", "Hero009"),
	LANCE("Lance", "*Lance*"),
	LYRA("Lyra", "*Lyra*"),
	OZO("Ozo", "*Ozo*"),
	PETAL("Petal", "*Petal*"),
	PHINN("Phinn", "*Phinn*"),
	REIM("Reim", "*Reim*"),
	RINGO("Ringo", "*Ringo*"),
	RONA("Rona", "Hero016"),
	SAMUEL("Samuel", "*Samuel*"),
	SAW("SAW", "*SAW*"),
	SKAARF("Skaarf", "*Skaarf*"),
	SKYE("Skye", "*Skye*"),
	TAKA("Taka", "*Sayoc*"),
	VOX("Vox", "*Vox*");
	
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
	
	public static Hero fromName(String name) {
		for (Hero h : Hero.values()) {
			if (h.name.equalsIgnoreCase(name))
				return h;
		}
		throw new IllegalArgumentException("Not a hero name: " + name);
	}
}
