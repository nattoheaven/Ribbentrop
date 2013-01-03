/*
 * Copyright 2008-2013 Ryohei NISHIMURA
 */

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SongID implements Comparable<SongID> {

	private static class Replacement {
		private final int start;
		private final int end;
		private final String str;
		public Replacement(int start, int end, String str) {
			this.start = start;
			this.end = end;
			this.str = str;
		}
		public Replacement(int start, int end, char[] chars) {
			this.start = start;
			this.end = end;
			this.str = new String(chars);
		}
		public void replace(StringBuffer sb) {
			sb.replace(start, end, str);
		}
	}

	private static abstract class ReferenceConverter {
		public abstract String convert(String input);
	}

	private static class EntityReferenceConverter
	extends ReferenceConverter {
		private final Pattern pattern;
		private final String replacement;
		public EntityReferenceConverter(String pattern, String replacement) {
			this.pattern = Pattern.compile(pattern);
			this.replacement = replacement;
		}
		@Override
		public String convert(String input) {
			return pattern.matcher(input).replaceAll(replacement);
		}
	}

	private static class CharacterAndAmpReferenceConverter
	extends ReferenceConverter {
		private static final Pattern PATTERN =
			Pattern.compile("&(?:#(?:(\\d*)|x([0-9A-Fa-f]*))|(amp));");
		@Override
		public String convert(String input) {
			Matcher matcher = PATTERN.matcher(input);
			ArrayList<Replacement> list = new ArrayList<Replacement>();
			int start = 0;
			while (matcher.find(start)) {
				start = matcher.end();
				try {
					String group1 = matcher.group(1);
					if (group1 != null) {
						int codePoint = Integer.parseInt(group1);
						char[] chars = Character.toChars(codePoint);
						Replacement r =
							new Replacement(matcher.start(), start, chars);
						list.add(r);
					}
					String group2 = matcher.group(2);
					if (group2 != null) {
						int codePoint = Integer.parseInt(group2, 16);
						char[] chars = Character.toChars(codePoint);
						Replacement r =
							new Replacement(matcher.start(), start, chars);
						list.add(r);
					}
					String group3 = matcher.group(3);
					if (group3 != null) {
						Replacement r =
							new Replacement(matcher.start(), start, "&");
						list.add(r);
					}
				} catch (IllegalArgumentException e) {
				}
			}
			StringBuffer sb = new StringBuffer(input);
			for (int i = list.size() - 1; i >= 0; --i) {
				list.get(i).replace(sb);
			}
			return sb.toString();
		}
	}

	static final ReferenceConverter[] REFERENCE_CONVERTERS =
		new ReferenceConverter[] {
		new EntityReferenceConverter("&nbsp;", "\u00A0"),
		new EntityReferenceConverter("&iexcl;", "\u00A1"),
		new EntityReferenceConverter("&cent;", "\u00A2"),
		new EntityReferenceConverter("&pound;", "\u00A3"),
		new EntityReferenceConverter("&curren;", "\u00A4"),
		new EntityReferenceConverter("&yen;", "\u00A5"),
		new EntityReferenceConverter("&brvbar;", "\u00A6"),
		new EntityReferenceConverter("&sect;", "\u00A7"),
		new EntityReferenceConverter("&uml;", "\u00A8"),
		new EntityReferenceConverter("&copy;", "\u00A9"),
		new EntityReferenceConverter("&ordf;", "\u00AA"),
		new EntityReferenceConverter("&laquo;", "\u00AB"),
		new EntityReferenceConverter("&not;", "\u00AC"),
		new EntityReferenceConverter("&shy;", "\u00AD"),
		new EntityReferenceConverter("&reg;", "\u00AE"),
		new EntityReferenceConverter("&macr;", "\u00AF"),
		new EntityReferenceConverter("&deg;", "\u00B0"),
		new EntityReferenceConverter("&plusmn;", "\u00B1"),
		new EntityReferenceConverter("&sup2;", "\u00B2"),
		new EntityReferenceConverter("&sup3;", "\u00B3"),
		new EntityReferenceConverter("&acute;", "\u00B4"),
		new EntityReferenceConverter("&micro;", "\u00B5"),
		new EntityReferenceConverter("&para;", "\u00B6"),
		new EntityReferenceConverter("&middot;", "\u00B7"),
		new EntityReferenceConverter("&cedil;", "\u00B8"),
		new EntityReferenceConverter("&sup1;", "\u00B9"),
		new EntityReferenceConverter("&ordm;", "\u00BA"),
		new EntityReferenceConverter("&raquo;", "\u00BB"),
		new EntityReferenceConverter("&frac14;", "\u00BC"),
		new EntityReferenceConverter("&frac12;", "\u00BD"),
		new EntityReferenceConverter("&frac34;", "\u00BE"),
		new EntityReferenceConverter("&iquest;", "\u00BF"),
		new EntityReferenceConverter("&Agrave;", "\u00C0"),
		new EntityReferenceConverter("&Aacute;", "\u00C1"),
		new EntityReferenceConverter("&Acirc;", "\u00C2"),
		new EntityReferenceConverter("&Atilde;", "\u00C3"),
		new EntityReferenceConverter("&Auml;", "\u00C4"),
		new EntityReferenceConverter("&Aring;", "\u00C5"),
		new EntityReferenceConverter("&AElig;", "\u00C6"),
		new EntityReferenceConverter("&Ccedil;", "\u00C7"),
		new EntityReferenceConverter("&Egrave;", "\u00C8"),
		new EntityReferenceConverter("&Eacute;", "\u00C9"),
		new EntityReferenceConverter("&Ecirc;", "\u00CA"),
		new EntityReferenceConverter("&Euml;", "\u00CB"),
		new EntityReferenceConverter("&Igrave;", "\u00CC"),
		new EntityReferenceConverter("&Iacute;", "\u00CD"),
		new EntityReferenceConverter("&Icirc;", "\u00CE"),
		new EntityReferenceConverter("&Iuml;", "\u00CF"),
		new EntityReferenceConverter("&ETH;", "\u00D0"),
		new EntityReferenceConverter("&Ntilde;", "\u00D1"),
		new EntityReferenceConverter("&Ograve;", "\u00D2"),
		new EntityReferenceConverter("&Oacute;", "\u00D3"),
		new EntityReferenceConverter("&Ocirc;", "\u00D4"),
		new EntityReferenceConverter("&Otilde;", "\u00D5"),
		new EntityReferenceConverter("&Ouml;", "\u00D6"),
		new EntityReferenceConverter("&times;", "\u00D7"),
		new EntityReferenceConverter("&Oslash;", "\u00D8"),
		new EntityReferenceConverter("&Ugrave;", "\u00D9"),
		new EntityReferenceConverter("&Uacute;", "\u00DA"),
		new EntityReferenceConverter("&Ucirc;", "\u00DB"),
		new EntityReferenceConverter("&Uuml;", "\u00DC"),
		new EntityReferenceConverter("&Yacute;", "\u00DD"),
		new EntityReferenceConverter("&THORN;", "\u00DE"),
		new EntityReferenceConverter("&szlig;", "\u00DF"),
		new EntityReferenceConverter("&agrave;", "\u00E0"),
		new EntityReferenceConverter("&aacute;", "\u00E1"),
		new EntityReferenceConverter("&acirc;", "\u00E2"),
		new EntityReferenceConverter("&atilde;", "\u00E3"),
		new EntityReferenceConverter("&auml;", "\u00E4"),
		new EntityReferenceConverter("&aring;", "\u00E5"),
		new EntityReferenceConverter("&aelig;", "\u00E6"),
		new EntityReferenceConverter("&ccedil;", "\u00E7"),
		new EntityReferenceConverter("&egrave;", "\u00E8"),
		new EntityReferenceConverter("&eacute;", "\u00E9"),
		new EntityReferenceConverter("&ecirc;", "\u00EA"),
		new EntityReferenceConverter("&euml;", "\u00EB"),
		new EntityReferenceConverter("&igrave;", "\u00EC"),
		new EntityReferenceConverter("&iacute;", "\u00ED"),
		new EntityReferenceConverter("&icirc;", "\u00EE"),
		new EntityReferenceConverter("&iuml;", "\u00EF"),
		new EntityReferenceConverter("&eth;", "\u00F0"),
		new EntityReferenceConverter("&ntilde;", "\u00F1"),
		new EntityReferenceConverter("&ograve;", "\u00F2"),
		new EntityReferenceConverter("&oacute;", "\u00F3"),
		new EntityReferenceConverter("&ocirc;", "\u00F4"),
		new EntityReferenceConverter("&otilde;", "\u00F5"),
		new EntityReferenceConverter("&ouml;", "\u00F6"),
		new EntityReferenceConverter("&divide;", "\u00F7"),
		new EntityReferenceConverter("&oslash;", "\u00F8"),
		new EntityReferenceConverter("&ugrave;", "\u00F9"),
		new EntityReferenceConverter("&uacute;", "\u00FA"),
		new EntityReferenceConverter("&ucirc;", "\u00FB"),
		new EntityReferenceConverter("&uuml;", "\u00FC"),
		new EntityReferenceConverter("&yacute;", "\u00FD"),
		new EntityReferenceConverter("&thorn;", "\u00FE"),
		new EntityReferenceConverter("&yuml;", "\u00FF"),
		new EntityReferenceConverter("&fnof;", "\u0192"),
		new EntityReferenceConverter("&Alpha;", "\u0391"),
		new EntityReferenceConverter("&Beta;", "\u0392"),
		new EntityReferenceConverter("&Gamma;", "\u0393"),
		new EntityReferenceConverter("&Delta;", "\u0394"),
		new EntityReferenceConverter("&Epsilon;", "\u0395"),
		new EntityReferenceConverter("&Zeta;", "\u0396"),
		new EntityReferenceConverter("&Eta;", "\u0397"),
		new EntityReferenceConverter("&Theta;", "\u0398"),
		new EntityReferenceConverter("&Iota;", "\u0399"),
		new EntityReferenceConverter("&Kappa;", "\u039A"),
		new EntityReferenceConverter("&Lambda;", "\u039B"),
		new EntityReferenceConverter("&Mu;", "\u039C"),
		new EntityReferenceConverter("&Nu;", "\u039D"),
		new EntityReferenceConverter("&Xi;", "\u039E"),
		new EntityReferenceConverter("&Omicron;", "\u039F"),
		new EntityReferenceConverter("&Pi;", "\u03A0"),
		new EntityReferenceConverter("&Rho;", "\u03A1"),
		new EntityReferenceConverter("&Sigma;", "\u03A3"),
		new EntityReferenceConverter("&Tau;", "\u03A4"),
		new EntityReferenceConverter("&Upsilon;", "\u03A5"),
		new EntityReferenceConverter("&Phi;", "\u03A6"),
		new EntityReferenceConverter("&Chi;", "\u03A7"),
		new EntityReferenceConverter("&Psi;", "\u03A8"),
		new EntityReferenceConverter("&Omega;", "\u03A9"),
		new EntityReferenceConverter("&alpha;", "\u03B1"),
		new EntityReferenceConverter("&beta;", "\u03B2"),
		new EntityReferenceConverter("&gamma;", "\u03B3"),
		new EntityReferenceConverter("&delta;", "\u03B4"),
		new EntityReferenceConverter("&epsilon;", "\u03B5"),
		new EntityReferenceConverter("&zeta;", "\u03B6"),
		new EntityReferenceConverter("&eta;", "\u03B7"),
		new EntityReferenceConverter("&theta;", "\u03B8"),
		new EntityReferenceConverter("&iota;", "\u03B9"),
		new EntityReferenceConverter("&kappa;", "\u03BA"),
		new EntityReferenceConverter("&lambda;", "\u03BB"),
		new EntityReferenceConverter("&mu;", "\u03BC"),
		new EntityReferenceConverter("&nu;", "\u03BD"),
		new EntityReferenceConverter("&xi;", "\u03BE"),
		new EntityReferenceConverter("&omicron;", "\u03BF"),
		new EntityReferenceConverter("&pi;", "\u03C0"),
		new EntityReferenceConverter("&rho;", "\u03C1"),
		new EntityReferenceConverter("&sigmaf;", "\u03C2"),
		new EntityReferenceConverter("&sigma;", "\u03C3"),
		new EntityReferenceConverter("&tau;", "\u03C4"),
		new EntityReferenceConverter("&upsilon;", "\u03C5"),
		new EntityReferenceConverter("&phi;", "\u03C6"),
		new EntityReferenceConverter("&chi;", "\u03C7"),
		new EntityReferenceConverter("&psi;", "\u03C8"),
		new EntityReferenceConverter("&omega;", "\u03C9"),
		new EntityReferenceConverter("&thetasym;", "\u03D1"),
		new EntityReferenceConverter("&upsih;", "\u03D2"),
		new EntityReferenceConverter("&piv;", "\u03D6"),
		new EntityReferenceConverter("&bull;", "\u2022"),
		new EntityReferenceConverter("&hellip;", "\u2026"),
		new EntityReferenceConverter("&prime;", "\u2032"),
		new EntityReferenceConverter("&Prime;", "\u2033"),
		new EntityReferenceConverter("&oline;", "\u203E"),
		new EntityReferenceConverter("&frasl;", "\u2044"),
		new EntityReferenceConverter("&weierp;", "\u2118"),
		new EntityReferenceConverter("&image;", "\u2111"),
		new EntityReferenceConverter("&real;", "\u211C"),
		new EntityReferenceConverter("&trade;", "\u2122"),
		new EntityReferenceConverter("&alefsym;", "\u2135"),
		new EntityReferenceConverter("&larr;", "\u2190"),
		new EntityReferenceConverter("&rarr;", "\u2192"),
		new EntityReferenceConverter("&darr;", "\u2193"),
		new EntityReferenceConverter("&harr;", "\u2194"),
		new EntityReferenceConverter("&crarr;", "\u21B5"),
		new EntityReferenceConverter("&lArr;", "\u21D0"),
		new EntityReferenceConverter("&uArr;", "\u21D1"),
		new EntityReferenceConverter("&rArr;", "\u21D2"),
		new EntityReferenceConverter("&dArr;", "\u21D3"),
		new EntityReferenceConverter("&hArr;", "\u21D4"),
		new EntityReferenceConverter("&forall;", "\u2200"),
		new EntityReferenceConverter("&part;", "\u2202"),
		new EntityReferenceConverter("&exist;", "\u2203"),
		new EntityReferenceConverter("&empty;", "\u2205"),
		new EntityReferenceConverter("&nabla;", "\u2207"),
		new EntityReferenceConverter("&isin;", "\u2208"),
		new EntityReferenceConverter("&notin;", "\u2209"),
		new EntityReferenceConverter("&ni;", "\u220B"),
		new EntityReferenceConverter("&prod;", "\u220F"),
		new EntityReferenceConverter("&sum;", "\u2211"),
		new EntityReferenceConverter("&minus;", "\u2212"),
		new EntityReferenceConverter("&lowast;", "\u2217"),
		new EntityReferenceConverter("&radic;", "\u221A"),
		new EntityReferenceConverter("&prop;", "\u221D"),
		new EntityReferenceConverter("&infin;", "\u221E"),
		new EntityReferenceConverter("&ang;", "\u2220"),
		new EntityReferenceConverter("&and;", "\u2227"),
		new EntityReferenceConverter("&or;", "\u2228"),
		new EntityReferenceConverter("&cap;", "\u2229"),
		new EntityReferenceConverter("&cup;", "\u222A"),
		new EntityReferenceConverter("&int;", "\u222B"),
		new EntityReferenceConverter("&there4;", "\u2234"),
		new EntityReferenceConverter("&sim;", "\u223C"),
		new EntityReferenceConverter("&cong;", "\u2245"),
		new EntityReferenceConverter("&asymp;", "\u2248"),
		new EntityReferenceConverter("&ne;", "\u2260"),
		new EntityReferenceConverter("&equiv;", "\u2261"),
		new EntityReferenceConverter("&le;", "\u2264"),
		new EntityReferenceConverter("&ge;", "\u2265"),
		new EntityReferenceConverter("&sub;", "\u2282"),
		new EntityReferenceConverter("&sup;", "\u2283"),
		new EntityReferenceConverter("&nsub;", "\u2284"),
		new EntityReferenceConverter("&sube;", "\u2286"),
		new EntityReferenceConverter("&supe;", "\u2287"),
		new EntityReferenceConverter("&oplus;", "\u2295"),
		new EntityReferenceConverter("&otimes;", "\u2297"),
		new EntityReferenceConverter("&perp;", "\u22A5"),
		new EntityReferenceConverter("&sdot;", "\u22C5"),
		new EntityReferenceConverter("&lceil;", "\u2308"),
		new EntityReferenceConverter("&rceil;", "\u2309"),
		new EntityReferenceConverter("&lfloor;", "\u230A"),
		new EntityReferenceConverter("&rfloor;", "\u230B"),
		new EntityReferenceConverter("&lang;", "\u2329"),
		new EntityReferenceConverter("&rang;", "\u232A"),
		new EntityReferenceConverter("&loz;", "\u25CA"),
		new EntityReferenceConverter("&spades;", "\u2660"),
		new EntityReferenceConverter("&clubs;", "\u2663"),
		new EntityReferenceConverter("&hearts;", "\u2665"),
		new EntityReferenceConverter("&diams;", "\u2666"),
		new EntityReferenceConverter("&quot;", "\""),
		new EntityReferenceConverter("&lt;", "\u003C"),
		new EntityReferenceConverter("&gt;", "\u003E"),
		new EntityReferenceConverter("&OElig;", "\u0152"),
		new EntityReferenceConverter("&oelig;", "\u0153"),
		new EntityReferenceConverter("&Scaron;", "\u0160"),
		new EntityReferenceConverter("&scaron;", "\u0161"),
		new EntityReferenceConverter("&Yuml;", "\u0178"),
		new EntityReferenceConverter("&circ;", "\u02C6"),
		new EntityReferenceConverter("&tilde;", "\u02DC"),
		new EntityReferenceConverter("&ensp;", "\u2002"),
		new EntityReferenceConverter("&emsp;", "\u2003"),
		new EntityReferenceConverter("&thinsp;", "\u2009"),
		new EntityReferenceConverter("&zwnj;", "\u200C"),
		new EntityReferenceConverter("&zwj;", "\u200D"),
		new EntityReferenceConverter("&lrm;", "\u200E"),
		new EntityReferenceConverter("&rlm;", "\u200F"),
		new EntityReferenceConverter("&ndash;", "\u2013"),
		new EntityReferenceConverter("&mdash;", "\u2014"),
		new EntityReferenceConverter("&lsquo;", "\u2018"),
		new EntityReferenceConverter("&rsquo;", "\u2019"),
		new EntityReferenceConverter("&sbquo;", "\u201A"),
		new EntityReferenceConverter("&ldquo;", "\u201C"),
		new EntityReferenceConverter("&rdquo;", "\u201D"),
		new EntityReferenceConverter("&bdquo;", "\u201E"),
		new EntityReferenceConverter("&dagger;", "\u2020"),
		new EntityReferenceConverter("&Dagger;", "\u2021"),
		new EntityReferenceConverter("&permil;", "\u2030"),
		new EntityReferenceConverter("&lsaquo;", "\u2039"),
		new EntityReferenceConverter("&rsaquo;", "\u203A"),
		new EntityReferenceConverter("&euro;", "\u20AC"),
		new CharacterAndAmpReferenceConverter(),
	};

	private final String requestNo;
	private final String contents;
	private final String artist;
	private final String convertedContents;
	private final String convertedArtist;

	public SongID(String contents, String artist)
	throws NullPointerException {
		this.requestNo = "";
		if (contents == null || artist == null) {
			throw new NullPointerException();
		}
		this.contents = contents;
		this.artist = artist;
		this.convertedContents = convertEntityReference(contents);
		this.convertedArtist = convertEntityReference(artist);
	}

	public SongID(String requestNo, String contents, String artist) {
		if (requestNo == null) {
			this.requestNo = "";
		} else {
			this.requestNo = requestNo;
		}
		if (contents == null) {
			this.contents = "";
		} else {
			this.contents = contents;
		}
		if (artist == null) {
			this.artist = "";
		} else {
			this.artist = artist;
		}
		this.convertedContents = this.contents;
		this.convertedArtist = this.artist;
	}

	@Override
	public int compareTo(SongID o) {
		int ret0 = contents.compareTo(o.contents);
		if (ret0 != 0) {
			return ret0;
		}
		int ret1 = artist.compareTo(o.artist);
		if (ret1 != 1) {
			return ret1;
		}
		return requestNo.compareTo(o.requestNo);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj != null && obj instanceof SongID) {
			SongID sid = (SongID) obj;
			return requestNo.equals(sid.requestNo) &&
			contents.equals(sid.contents) && artist.equals(sid.artist);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return requestNo.hashCode() + contents.hashCode() + artist.hashCode();
	}

	@Override
	public String toString() {
		return requestNo + " " + convertedContents + "/" + convertedArtist;
	}

	public String toBlogLine() {
		return convertedContents + "/" + convertedArtist;
	}

	public String getRequestNo() {
		return requestNo;
	}

	public String getContents() {
		return contents;
	}

	public String getArtist() {
		return artist;
	}

	private static String convertEntityReference(String s) {
		for (ReferenceConverter rc : REFERENCE_CONVERTERS) {
			s = rc.convert(s);
		}
		return s;
	}

}
