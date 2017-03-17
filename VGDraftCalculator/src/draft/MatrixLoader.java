package draft;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class MatrixLoader {

	/**
	 * @param vsXmlFile
	 * @param synergyXmlFile
	 * @return
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public static HeroMatrix load(File vsXmlFile, File synergyXmlFile) throws ParserConfigurationException, SAXException, IOException {
		
		if (!vsXmlFile.exists())
			throw new IllegalArgumentException("Invalid file input: " + vsXmlFile);
		if (!synergyXmlFile.exists())
			throw new IllegalArgumentException("Invalid file input: " + synergyXmlFile);

		HashMap<Hero, HashMap<Hero, Integer>> vsMap = new HashMap<>();
		HashMap<Hero, HashMap<Hero, Integer>> synMap = new HashMap<>();
		HashMap<Hero, HashMap<Hero, Integer>> withMap = new HashMap<>();
		for (Hero h : Hero.values()) {
			vsMap.put(h, new HashMap<Hero,Integer>());
			synMap.put(h, new HashMap<Hero,Integer>());
			withMap.put(h, new HashMap<Hero,Integer>());
		}

		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		DocumentBuilder dBuilder2 = dbFactory.newDocumentBuilder();
		Document vsDoc = dBuilder.parse(vsXmlFile);
		Document synDoc = dBuilder2.parse(synergyXmlFile);

		//optional, but recommended
		//read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
		// doc.getDocumentElement().normalize();

		// System.out.println("Root element :" + doc.getDocumentElement().getNodeName());

		NodeList nList = vsDoc.getElementsByTagName("ROW");
		NodeList nList2 = synDoc.getElementsByTagName("ROW");

		System.out.println("Loading data from: " + vsXmlFile.getPath() + ", " + synergyXmlFile);

		for (int i = 0; i < nList.getLength(); i++) {

			Node rowNode = nList.item(i);

			// System.out.println("\nNode: " + rowNode.getNodeName());

			if (rowNode.getNodeType() == Node.ELEMENT_NODE) {

				Element eElement = (Element) rowNode;

				NodeList elements = eElement.getElementsByTagName("*");
				String heroName = elements.item(0).getTextContent();
				Hero hero = Hero.fromName(heroName);
				// System.out.println("Current Hero: " + heroName);
				for (int j=0; j<elements.getLength(); j++) {
					Node column = elements.item(j);
					String columnName = column.getNodeName();
					if (columnName.startsWith("beat")) {
						Hero opponent = Hero.fromName(columnName.substring(4));
						String textContent = column.getTextContent();
						int value = textContent.equals("NULL") ? 0 : Integer.parseInt(textContent);;
						// System.out.println(opponent + ":\t" + value);
						// WEIGHTING WIN-RATES:
						value += 10;
						vsMap.get(hero).put(opponent, value);
					}
				}

			}
		}
		
		for (int i = 0; i < nList2.getLength(); i++) {

			Node rowNode = nList2.item(i);

			// System.out.println("\nNode: " + rowNode.getNodeName());

			if (rowNode.getNodeType() == Node.ELEMENT_NODE) {

				Element eElement = (Element) rowNode;

				NodeList elements = eElement.getElementsByTagName("*");
				String heroName = elements.item(0).getTextContent();
				Hero hero = Hero.fromName(heroName);
//				 System.out.println("Current Hero: " + heroName);
				for (int j=0; j<elements.getLength(); j++) {
					Node column = elements.item(j);
					String columnName = column.getNodeName();
					if (columnName.startsWith("winswith")) {
						Hero partner = Hero.fromName(columnName.substring(8));
						String textContent = column.getTextContent();
						int value = textContent.equals("NULL") ? 0 : Integer.parseInt(textContent);;
//						 System.out.println(partner + " wins:\t" + value);
						// WEIGHTING WIN-RATES:
						value += 5;
						synMap.get(hero).put(partner, value);
					} else if (columnName.startsWith("playswith")) {
						Hero partner = Hero.fromName(columnName.substring(9));
						String textContent = column.getTextContent();
						int value = textContent.equals("NULL") ? 0 : Integer.parseInt(textContent);;
//						 System.out.println(partner + " plays:\t" + value);
						// WEIGHTING WIN-RATES:
						value += 10;
						withMap.get(hero).put(partner, value);
					}
				}

			}
		}

		// convert values into ratio matrix
		HeroMatrix matrix = new HeroMatrix();
		for (Hero hero : Hero.values()) {
			for (Hero other : Hero.values()) {
				Double wins = new Double(vsMap.get(hero).get(other));
				Double losses = new Double(vsMap.get(other).get(hero));
				Double winsWith = new Double(synMap.get(hero).get(other));
				Double playsWith = new Double(withMap.get(hero).get(other));
				matrix.put(hero, other, wins/((double) wins+losses), false);
				matrix.put(hero, other, winsWith/((double) winsWith+playsWith), true);
			}
		}
		return matrix;
	}

	public static HeroMatrix loadRandom() {
		HeroMatrix matrix = new HeroMatrix();

		// generate random win rates
		// (.5) (1/x) (1/y)
		// (x)  (.5)  (1/z)
		// (y)   (z)   (.5) etc...
		double standardDeviation = .15;
		ArrayList<Hero> axis2 = new ArrayList<>();
		Random rand = new Random();
		for (Hero one : Hero.values()) {
			for (Hero two : axis2) {
				double score = rand.nextGaussian();
				double synergy = rand.nextGaussian();
				// scale and shift:
				score *= standardDeviation;
				synergy *= standardDeviation;
				score += .5;
				synergy += .5;
				// bound outliers within probability:
				score = score > 1 ? 1 : score;
				synergy = synergy > 1 ? 1 : synergy;
				score = score < 0 ? 0 : score;
				synergy = synergy < 0 ? 0 : synergy;
				matrix.put(one, two, score, false);
				matrix.put(two, one, 1 - score, false);
				matrix.put(one, two, synergy, true);
				matrix.put(two, one, synergy, true);
			}
			matrix.put(one, one, .5, false);
			matrix.put(one, one, .5, true);
			axis2.add(one);
		}
		return matrix;
	}
}
