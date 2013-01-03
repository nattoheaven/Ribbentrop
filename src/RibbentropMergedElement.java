/*
 * Copyright 2008-2013 Ryohei NISHIMURA
 */

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;


public class RibbentropMergedElement {

	public static final Map<String, SortType> SORT_TYPE_MAP;
	static {
		HashMap<String, SortType> map = new HashMap<String, SortType>();
		map.put("max", SortType.MAX);
		map.put("min", SortType.MIN);
		map.put("average", SortType.AVERAGE);
		SORT_TYPE_MAP = Collections.unmodifiableMap(map);
	}

	private static final Comparator<RibbentropMergedElement>
	COMPARATOR_MAX = new Comparator<RibbentropMergedElement>() {
		@Override
		public int compare(RibbentropMergedElement o1,
				RibbentropMergedElement o2) {
			int ret0 = -o1.points.last().getPoint().
			compareTo(o2.points.last().getPoint());
			if (ret0 != 0) {
				return ret0;
			}
			return o1.songID.compareTo(o2.songID);
		}
	};

	private static final Comparator<RibbentropMergedElement>
	COMPARATOR_MIN = new Comparator<RibbentropMergedElement>() {
		@Override
		public int compare(RibbentropMergedElement o1,
				RibbentropMergedElement o2) {
			int ret0 = -o1.points.first().getPoint().
			compareTo(o2.points.first().getPoint());
			if (ret0 != 0) {
				return ret0;
			}
			return o1.songID.compareTo(o2.songID);
		}
	};

	private static final Comparator<RibbentropMergedElement>
	COMPARATOR_AVERAGE = new Comparator<RibbentropMergedElement>() {
		@Override
		public int compare(RibbentropMergedElement o1,
				RibbentropMergedElement o2) {
			int ret0 = -o1.getAverage().compareTo(o2.getAverage());
			if (ret0 != 0) {
				return ret0;
			}
			return o1.songID.compareTo(o2.songID);
		}
	};

	private static final Comparator<RibbentropElement> COMPARATOR_POINT =
		new Comparator<RibbentropElement>() {
		@Override
		public int compare(RibbentropElement o1,
				RibbentropElement o2) {
			return o1.getPoint().compareTo(o2.getPoint());
		}
	};

	public static enum SortType {
		MAX {
			@Override
			public Comparator<RibbentropMergedElement> getComparator() {
				return COMPARATOR_MAX;
			}
			@Override
			public BigDecimal getPoint(RibbentropMergedElement rbe) {
				return rbe.points.last().getPoint();
			}
		},
		MIN {
			@Override
			public Comparator<RibbentropMergedElement> getComparator() {
				return COMPARATOR_MIN;
			}
			@Override
			public BigDecimal getPoint(RibbentropMergedElement rbe) {
				return rbe.points.first().getPoint();
			}
		},
		AVERAGE {
			@Override
			public Comparator<RibbentropMergedElement> getComparator() {
				return COMPARATOR_AVERAGE;
			}
			@Override
			public BigDecimal getPoint(RibbentropMergedElement rbe) {
				return rbe.getAverage();
			}
		};
		abstract public Comparator<RibbentropMergedElement> getComparator();
		abstract public BigDecimal getPoint(RibbentropMergedElement rbe);
	}

	private final SongID songID;
	private int ranking;
	private final TreeSet<RibbentropElement> points =
		new TreeSet<RibbentropElement>(COMPARATOR_POINT);
	private BigDecimal average = null;

	public RibbentropMergedElement(RibbentropElement rbe)
	throws NullPointerException {
		if (rbe == null) {
			throw new NullPointerException();
		}
		songID = rbe.getSongID();
		ranking = rbe.getRanking();
		points.add(rbe);
	}

	public SongID getSongArtist() {
		return songID;
	}

	public int getRank() {
		return ranking;
	}

	public SortedSet<RibbentropElement> getElements() {
		return Collections.unmodifiableSortedSet(points);
	}

	public SortedSet<RibbentropElement> getDescendingPoints() {
		return Collections.unmodifiableSortedSet(points.descendingSet());
	}

	public void addPoint(RibbentropElement rbe)
	throws NullPointerException {
		if (rbe == null) {
			throw new NullPointerException();
		}
		ranking = Math.min(rbe.getRanking(), ranking);
		points.add(rbe);
	}

	public BigDecimal getMax() {
		return points.last().getPoint();
	}

	public BigDecimal getMin() {
		return points.first().getPoint();
	}

	public BigDecimal getAverage() {
		if (average == null) {
			computeAverage();
		}
		return average;
	}

	private void computeAverage() {
		BigDecimal average = new BigDecimal("0.000");
		for (RibbentropElement element : points) {
			average = average.add(element.getPoint());
		}
		average = average.divide(new BigDecimal(points.size()),
				BigDecimal.ROUND_HALF_UP);
		this.average = average;
	}

}
