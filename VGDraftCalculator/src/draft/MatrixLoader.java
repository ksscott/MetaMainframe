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

	public static HeroMatrix load(File xmlFile) throws ParserConfigurationException, SAXException, IOException {
		
		if (!xmlFile.exists())
			throw new IllegalArgumentException("Invalid file input: " + xmlFile);

		HashMap<Hero, HashMap<Hero, Integer>> map = new HashMap<>();
		for (Hero h : Hero.values()) {
			map.put(h, new HashMap<Hero,Integer>());
		}

		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(xmlFile);

		//optional, but recommended
		//read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
		// doc.getDocumentElement().normalize();

		// System.out.println("Root element :" + doc.getDocumentElement().getNodeName());

		NodeList nList = doc.getElementsByTagName("ROW");

		System.out.println("Loading data from: " + xmlFile.getPath());

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
						map.get(hero).put(opponent, value);
					}
				}

			}
		}

		// convert values into ratio matrix
		HeroMatrix matrix = new HeroMatrix();
		for (Hero hero : Hero.values()) {
			for (Hero opponent : Hero.values()) {
				Double wins = new Double(map.get(hero).get(opponent));
				Double losses = new Double(map.get(opponent).get(hero));
				matrix.put(hero, opponent, wins/((double) wins+losses));
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
				// scale and shift:
				score *= standardDeviation;
				score += .5;
				// bound outliers within probability:
				score = score > 1 ? 1 : score;
				score = score < 0 ? 0 : score;
				matrix.put(one, two, score);
				matrix.put(two, one, 1 - score);
			}
			matrix.put(one, one, .5);
			axis2.add(one);
		}
		return matrix;
	}
}
