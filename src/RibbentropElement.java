/*
 * Copyright 2008-2013 Ryohei NISHIMURA
 */

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public class RibbentropElement {

	public static class IllegalCSVFormatException extends Exception {
		private static final long serialVersionUID = 1L;
		public IllegalCSVFormatException() {
			super("CSVの形式が不正です");
		}
	}

	private static enum TokenizeState {
		NOT_QUOTED,
		QUOTED,
		ESCAPED,
	}

	private static final SimpleDateFormat dateFormat =
		new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

	private final SongID songID;
	private final long date;
	private final BigDecimal point;
	private final int ranking;

	public RibbentropElement(SongID songID,
			int ranking,
			BigDecimal point)
	throws NullPointerException {
		if (songID == null || point == null) {
			throw new NullPointerException();
		}
		this.songID = songID;
		this.date = 0;
		this.point = point;
		this.ranking = ranking;
	}

	public RibbentropElement(String requestNo, String contents, String artist,
			String date, String point, String ranking) {
		if (requestNo == null) {
			requestNo = "";
		}
		if (contents == null) {
			contents = "";
		}
		if (artist == null) {
			artist = "";
		}
		this.songID = new SongID(requestNo, contents, artist);
		if (date == null) {
			this.date = 0;
		} else {
			long thisDate;
			try {
				thisDate = dateFormat.parse(date).getTime();
			} catch (ParseException e) {
				thisDate = 0;
			}
			this.date = thisDate;
		}
		if (point == null) {
			this.point = new BigDecimal("0.000");
		} else {
			this.point = new BigDecimal(point).setScale(3);
		}
		if (ranking == null || ranking.equals("")) {
			this.ranking = Integer.MAX_VALUE;
		} else {
			this.ranking = Integer.parseInt(ranking);
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj != null && obj instanceof RibbentropElement) {
			RibbentropElement rbe = (RibbentropElement) obj;
			return songID.equals(rbe.songID) &&
			date == rbe.date && point.equals(rbe.point) &&
			ranking == rbe.ranking;
		} else {
			return false;
		}
	}

	public SongID getSongID() {
		return songID;
	}

	public long getDate() {
		return date;
	}

	public BigDecimal getPoint() {
		return point;
	}

	public int getRanking() {
		return ranking;
	}

	public String getDateString() {
		return dateFormat.format(new Date(date));
	}

	public static RibbentropElement parseCSVLine(String line)
	throws IllegalCSVFormatException, NumberFormatException {
		char[] chars = line.toCharArray();
		ArrayList<String> tokens = new ArrayList<String>(4);
		StringBuffer token = new StringBuffer();
		TokenizeState state = TokenizeState.NOT_QUOTED;
		for (char c : chars) {
			switch (state) {
			case NOT_QUOTED:
				switch (c) {
				case '"':
					state = TokenizeState.QUOTED;
					break;
				case ',':
					tokens.add(token.toString());
					token = new StringBuffer();
					break;
				default:
					token.append(c);
				}
				break;
			case QUOTED:
				switch (c) {
				case '"':
					state = TokenizeState.ESCAPED;
					break;
				default:
					token.append(c);
				}
				break;
			case ESCAPED:
				switch (c) {
				case '"':
					token.append('"');
					state = TokenizeState.QUOTED;
					break;
				case ',':
					tokens.add(token.toString());
					token = new StringBuffer();
					state = TokenizeState.NOT_QUOTED;
					break;
				default:
					token.append(c);
					state = TokenizeState.NOT_QUOTED;
				}
				break;
			}
		}
		if (token.length() != 0) {
			tokens.add(token.toString());
		}
		if (tokens.size() != 4) {
			throw new IllegalCSVFormatException();
		}
		SongID songArtist =
			new SongID(tokens.get(0), tokens.get(1));
		String rankStr = tokens.get(2);
		int rank;
		if (rankStr.equals("ランク外")) {
			rank = Integer.MAX_VALUE;
		} else {
			rank = Integer.parseInt(rankStr);
		}
		return new RibbentropElement(songArtist,
				rank,
				new BigDecimal(tokens.get(3)));
	}

	public static RibbentropElement parseCSVLine2(String line)
	throws IllegalCSVFormatException, NumberFormatException {
		char[] chars = line.toCharArray();
		ArrayList<String> tokens = new ArrayList<String>(6);
		StringBuffer token = new StringBuffer();
		TokenizeState state = TokenizeState.NOT_QUOTED;
		for (char c : chars) {
			switch (state) {
			case NOT_QUOTED:
				switch (c) {
				case '"':
					state = TokenizeState.QUOTED;
					break;
				case ',':
					tokens.add(token.toString());
					token = new StringBuffer();
					break;
				default:
					token.append(c);
				}
				break;
			case QUOTED:
				switch (c) {
				case '"':
					state = TokenizeState.ESCAPED;
					break;
				default:
					token.append(c);
				}
				break;
			case ESCAPED:
				switch (c) {
				case '"':
					token.append('"');
					state = TokenizeState.QUOTED;
					break;
				case ',':
					tokens.add(token.toString());
					token = new StringBuffer();
					state = TokenizeState.NOT_QUOTED;
					break;
				default:
					token.append(c);
					state = TokenizeState.NOT_QUOTED;
				}
				break;
			}
		}
		tokens.add(token.toString());
		if (tokens.size() != 6) {
			throw new IllegalCSVFormatException();
		}
		return new RibbentropElement(tokens.get(0), tokens.get(1),
				tokens.get(2), tokens.get(3), tokens.get(4), tokens.get(5));
	}

	public String toCSVLine() {
		StringBuffer sb = new StringBuffer();
		sb.append('"');
		sb.append(songID.getContents().replaceAll("\"", "\"\""));
		sb.append("\",\"");
		sb.append(songID.getArtist().replaceAll("\"", "\"\""));
		sb.append("\",\"");
		if (ranking == Integer.MAX_VALUE) {
			sb.append("ランク外");
		} else {
			sb.append(ranking);
		}
		sb.append("\",\"");
		sb.append(point.toString());
		sb.append("\"\r\n");
		return sb.toString();
	}

	public String toCSVLine2() {
		StringBuffer sb = new StringBuffer();
		sb.append('"');
		sb.append(songID.getRequestNo().replaceAll("\"", "\"\""));
		sb.append("\",\"");
		sb.append(songID.getContents().replaceAll("\"", "\"\""));
		sb.append("\",\"");
		sb.append(songID.getArtist().replaceAll("\"", "\"\""));
		sb.append("\",\"");
		sb.append(getDateString().replaceAll("\"", "\"\""));
		sb.append("\",\"");
		sb.append(point.toString());
		sb.append("\",\"");
		if (ranking == Integer.MAX_VALUE) {
			sb.append("");
		} else {
			sb.append(ranking);
		}
		sb.append("\"\r\n");
		return sb.toString();
	}

	public String toBlogLine() {
		StringBuffer sb = new StringBuffer();
		sb.append(songID.toBlogLine());
		sb.append(' ');
		if (ranking == Integer.MAX_VALUE) {
			sb.append("ランク外");
		} else {
			sb.append(ranking);
			sb.append("位");
		}
		sb.append(" (");
		sb.append(point);
		sb.append("点)\n");
		return sb.toString();
	}

}
