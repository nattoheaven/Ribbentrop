/*
 * Copyright 2008-2013 Ryohei NISHIMURA
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


public class RibbentropData {

	private static final Pattern[] PATTERNS1 = new Pattern[] {
		Pattern.compile("<a href=\"/app/leaf/songKaraokeLeaf\\.do\\?requestNo=\\d\\d\\d\\d-\\d\\d\">([^<]*)</a>"),
		Pattern.compile("<p class=\"artist\">／([^<]*)</p>"),
		Pattern.compile("<span class=\"(?:rankin|outofrank)\">(\\d*位|ランク外)</span>"),
		Pattern.compile("\\((\\d*\\.\\d*)点\\)"),
	};

	private static final Pattern[] PATTERNS2 = new Pattern[] {
		Pattern.compile("<a href=\"http://www\\.clubdam\\.com/app/damtomo/leaf/SongLeaf\\.do\\?requestNo=\\d\\d\\d\\d-\\d\\d\">([^<]*)</a>"),
		Pattern.compile("<p class=\"artist\">/([^<]*)</p>"),
		Pattern.compile("<td class=\"score\">(\\d*位|ランク外)"),
		Pattern.compile("\\((\\d*\\.\\d*)点\\)"),
	};

	private RibbentropData() {
	}

	public static List<RibbentropElement> parseHTML(Reader in)
	throws IOException {
		ArrayList<RibbentropElement> data =
			new ArrayList<RibbentropElement>();
		BufferedReader br = new BufferedReader(in);
		int state = 0;
		String[] matched = new String[4];
		String line;
		while ((line = br.readLine()) != null) {
			Matcher matcher1 = PATTERNS1[state].matcher(line);
			if (matcher1.find()) {
				try {
					matched[state] = matcher1.group(1);
					if (state == 3) {
						SongID songArtist =
							new SongID(matched[0], matched[1]);
						int rank;
						if (matched[2].equals("ランク外")) {
							rank = Integer.MAX_VALUE;
						} else {
							String rankStr =
								matched[2].substring(0, matched[2].length() - 1);
							rank = Integer.parseInt(rankStr);
						}
						BigDecimal point = new BigDecimal(matched[3]);
						RibbentropElement rbe =
							new RibbentropElement(songArtist, rank, point);
						data.add(rbe);
						state = 0;
					} else {
						state = state + 1;
					}
				} catch (NumberFormatException e) {
				}
			}
			Matcher matcher2 = PATTERNS2[state].matcher(line);
			if (matcher2.find()) {
				try {
					matched[state] = matcher2.group(1);
					if (state == 3) {
						SongID songArtist =
							new SongID(matched[0], matched[1]);
						int rank;
						if (matched[2].equals("ランク外")) {
							rank = Integer.MAX_VALUE;
						} else {
							String rankStr =
								matched[2].substring(0, matched[2].length() - 1);
							rank = Integer.parseInt(rankStr);
						}
						BigDecimal point = new BigDecimal(matched[3]);
						RibbentropElement rbe =
							new RibbentropElement(songArtist, rank, point);
						data.add(rbe);
						state = 0;
					} else {
						state = state + 1;
					}
				} catch (NumberFormatException e) {
				}
			}
		}
		return data;
	}

	public static RibbentropMergedElement[] getData(List<RibbentropElement> data) {
		TreeMap<SongID, RibbentropMergedElement> map =
			new TreeMap<SongID, RibbentropMergedElement>();
		for (RibbentropElement rbe : data) {
			SongID songArtist = rbe.getSongID();
			RibbentropMergedElement rbme = map.get(songArtist);
			if (rbme == null) {
				map.put(songArtist, new RibbentropMergedElement(rbe));
			} else {
				rbme.addPoint(rbe);
			}
		}
		RibbentropMergedElement[] ret =
			new RibbentropMergedElement[map.size()];
		map.values().toArray(ret);
		return ret;
	}

	public static void parseXML(List<RibbentropElement> data, InputStream in)
	throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document document = db.parse(in);
		ArrayList<Node> stack = new ArrayList<Node>();
		stack.add(document.getDocumentElement());
		while (!stack.isEmpty()) {
			Node node = stack.remove(stack.size() - 1);
			if (node.getNodeType() == Node.ELEMENT_NODE &&
					node.getNodeName().equals("ranking")) {
				NamedNodeMap attributes = node.getAttributes();
				String requestNo =
					attributes.getNamedItem("requestNo").getNodeValue();
				String contents =
					attributes.getNamedItem("contents").getNodeValue();
				String artist =
					attributes.getNamedItem("artist").getNodeValue();
				String date =
					attributes.getNamedItem("date").getNodeValue();
				String point =
					attributes.getNamedItem("point").getNodeValue();
				String ranking = node.getTextContent();
				RibbentropElement element =
					new RibbentropElement(requestNo, contents, artist,
							date, point, ranking);
				if (!data.contains(element)) {
					data.add(element);
				}
			} else {
				NodeList list = node.getChildNodes();
				for (int i = 0; i < list.getLength(); i++) {
					stack.add(list.item(i));
				}
			}
		}
	}

}
