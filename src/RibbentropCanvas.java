/*
 * Copyright 2008-2013 Ryohei NISHIMURA
 */

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;

import javax.swing.JComponent;
import javax.swing.JPanel;


public class RibbentropCanvas extends JPanel implements Printable {

	private static final long serialVersionUID = -229728886060031312L;

	private static final Font FONT = new Font("SansSerif", Font.PLAIN, 12);
	private static final String MAX_POINT_STR = "100.000";
	private static final int MAX_POINT_COMMA = MAX_POINT_STR.indexOf('.');
	private static final BigDecimal MAX_POINT = new BigDecimal(MAX_POINT_STR);
	private static final BigDecimal MIN_POINT = new BigDecimal("0.000");
	private static final BigDecimal BIG_DECIMAL_4999 = new BigDecimal("4.999");

	private final FontMetrics fontMetrics = getFontMetrics(FONT);
	private final int fontAscent = fontMetrics.getAscent();
	private final int fontHeight = fontMetrics.getHeight();
	private RibbentropMergedElement[] rbes;
	private PageFormat pf;
	private boolean isPaintingRank;
	private int pointWidth;
	private int rankWidth;
	private int minx;
	private int maxx;
	private RibbentropMergedElement.SortType sortType;

	public RibbentropCanvas(List<RibbentropElement> data,
			PageFormat pf,
			boolean isPaintingRank,
			RibbentropMergedElement.SortType sortType) {
		super(null);
		this.pf = pf;
		this.isPaintingRank = isPaintingRank;
		this.sortType = sortType;
		RibbentropMergedElement[] mergedData =
			RibbentropData.getData(data);
		Arrays.sort(mergedData, sortType.getComparator());
		this.rbes = mergedData;
		pointWidth = fontMetrics.stringWidth("100.000");
		rankWidth = fontMetrics.stringWidth("（ランク外）");
		calculateMinxMaxx();
		int width;
		if (pf == null) {
			width = 640;
		} else {
			width = (int) pf.getImageableWidth();
		}
		int height = fontMetrics.getHeight() * (rbes.length + 1);
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				makeToolTipComponents();
			}
		});
		setPreferredSize(new Dimension(width, height));
		makeToolTipComponents();
	}

	@Override
	public int print(Graphics graphics, PageFormat pageFormat, int pageIndex)
			throws PrinterException {
		int pageHeight = ((int) pageFormat.getImageableHeight()) - fontHeight;
		if (pageHeight < fontHeight) {
			return NO_SUCH_PAGE;
		}
		int miny = (pageHeight / fontHeight) * pageIndex;
		if (miny < rbes.length) {
			int pointsWidth = ((int) pf.getImageableWidth());
			int maxy = Math.min(rbes.length,
					(pageHeight / fontHeight) * (pageIndex + 1));
			Graphics2D g2d = (Graphics2D) graphics;
			g2d.translate(pf.getImageableX(), pf.getImageableY());
			subPaint(g2d, pointsWidth, miny, maxy);
			return PAGE_EXISTS;
		} else {
			return NO_SUCH_PAGE;
		}
	}

	@Override
	protected void paintComponent(Graphics g) {
		g.setColor(getBackground());
		g.fillRect(0, 0, getWidth(), getHeight());
		if (rbes.length == 0) {
			return;
		}
		subPaint(g, getWidth(), 0, rbes.length);
	}

	public void paintForImageFile(Graphics g,
			int width,
			int height,
			int pageIndex) {
		int pageHeight = height - fontHeight;
		if (pageHeight < fontHeight) {
			return;
		}
		int miny = (pageHeight / fontHeight) * pageIndex;
		if (miny < rbes.length) {
			int maxy = Math.min(rbes.length,
					(pageHeight / fontHeight) * (pageIndex + 1));
			subPaint(g, width, miny, maxy);
			return;
		}
	}

	private void subPaint(Graphics g, int width, int miny, int maxy) {
		int pointsWidth = width - pointWidth;
		if (isPaintingRank) {
			pointsWidth -= rankWidth;
		}
		g.setFont(FONT);
		g.setColor(Color.YELLOW);
		for (int i = miny; i < maxy; i++) {
			int y = (i - miny + 1) * fontHeight;
			int nextx = 0;
			for (int j = 0; j < maxx - minx; j += 5) {
				int x = nextx;
				nextx = (j + 5) * pointsWidth / (maxx - minx);
				int cellWidth = nextx - x;
				if ((j / 5 + i) % 2 == 1) {
					g.fillRect(x, y, cellWidth, fontHeight);
				}
			}
			if (((maxx - minx) / 5 + i) % 2 == 1) {
				g.fillRect(pointsWidth, y, pointWidth, fontHeight);
			} else if (isPaintingRank) {
				int x = pointsWidth + pointWidth;
				g.fillRect(x, y, rankWidth, fontHeight);
			}
		}
		g.setColor(Color.BLACK);
		for (int i = minx; i < maxx; i += 5) {
			int x = (i - minx) * pointsWidth / (maxx - minx);
			g.drawString(Integer.toString(i), x, fontAscent);
		}
		g.drawString(Integer.toString(maxx), pointsWidth, fontAscent);
		for (int i = miny; i < maxy; i++) {
			RibbentropMergedElement rbe = rbes[i];
			int liney1 = (i - miny + 1) * fontHeight;
			int liney2 = liney1 + fontHeight;
			int stry = liney1 + fontAscent;
			g.setColor(Color.BLUE);
			String saStr = rbe.getSongArtist().toString();
			int sax = pointsWidth - fontMetrics.stringWidth(saStr);
			g.drawString(saStr, sax, stry);
			g.setColor(Color.BLACK);
			String highPointStr = sortType.getPoint(rbe).toString();
			StringBuffer sb = new StringBuffer();
			int startj = highPointStr.indexOf('.');
			for (int j = startj; j < MAX_POINT_COMMA; j++) {
				sb.append('0');
			}
			int highPointx =
				pointsWidth + fontMetrics.stringWidth(sb.toString());
			g.drawString(highPointStr, highPointx, stry);
			if (isPaintingRank) {
				StringBuilder rankSB = new StringBuilder();
				rankSB.append('（');
				int rank = rbe.getRank();
				if (rank == Integer.MAX_VALUE) {
					rankSB.append("ランク外");
				} else {
					rankSB.append(rank);
					rankSB.append('位');
				}
				rankSB.append('）');
				g.drawString(rankSB.toString(), pointsWidth + pointWidth, stry);
			}
			for (RibbentropElement element : rbe.getElements()) {
				double pointDouble = element.getPoint().doubleValue() - minx;
				int x1 =
					(int) (pointDouble * pointsWidth / (maxx - minx) -
							fontHeight / 2);
				int x2 = x1 + fontHeight;
				g.drawLine(x1, liney1, x2, liney2);
				g.drawLine(x2, liney1, x1, liney2);
			}
		}
	}

	public void dataChanged(List<RibbentropElement> data) {
		RibbentropMergedElement[] mergedData =
			RibbentropData.getData(data);
		Arrays.sort(mergedData, sortType.getComparator());
		this.rbes = mergedData;
		calculateMinxMaxx();
		int height = fontMetrics.getHeight() * (rbes.length + 1);
		setPreferredSize(new Dimension(getPreferredSize().width, height));
	}

	public void pageChanged(PageFormat pf) {
		this.pf = pf;
		int width = (int) pf.getImageableWidth();
		setPreferredSize(new Dimension(width, getPreferredSize().height));
	}

	public void rankChanged(boolean isPaintingRank) {
		this.isPaintingRank = isPaintingRank;
		repaint();
		makeToolTipComponents();
	}

	public void sortTypeChanged(RibbentropMergedElement.SortType sortType) {
		this.sortType = sortType;
		Arrays.sort(rbes, sortType.getComparator());
		makeToolTipComponents();
	}

	public int getNumberOfPages(int height) {
		if (height < fontHeight * 2) {
			return -1;
		}
		int pageHeight = height - fontHeight;
		int pageRows = pageHeight / fontHeight;
		return (rbes.length + pageRows - 1) / pageRows;
	}

	private void calculateMinxMaxx() {
		BigDecimal minPoint = MAX_POINT;
		BigDecimal maxPoint = MIN_POINT;
		for (RibbentropMergedElement rbe : rbes) {
			SortedSet<RibbentropElement> elements = rbe.getElements();
			minPoint = minPoint.min(elements.first().getPoint());
			maxPoint = maxPoint.max(elements.last().getPoint());
		}
		minx = (minPoint.intValue() / 5) * 5;
		maxx = (maxPoint.add(BIG_DECIMAL_4999).intValue() / 5) * 5;
	}

	private static class ToolTipComponent extends JComponent {
		private static final long serialVersionUID = -80362076792936669L;
		public ToolTipComponent(String text) {
			setToolTipText(text);
		}
	}

	private void makeToolTipComponents() {
		setVisible(false);
		removeAll();
		int width = getWidth();
		int pointsWidth = width - pointWidth;
		if (isPaintingRank) {
			pointsWidth -= rankWidth;
		}
		for (int i = 0; i < rbes.length; i++) {
			RibbentropMergedElement rbe = rbes[i];
			int y = (i + 1) * fontHeight;
			for (RibbentropElement element : rbe.getDescendingPoints()) {
				double pointDouble = element.getPoint().doubleValue() - minx;
				int x = (int) (pointDouble * pointsWidth / (maxx - minx) -
						fontHeight / 2);
				String toolTipString;
				if (element.getDate() == 0) {
					toolTipString = element.getPoint().toString() + "点";
				} else {
					toolTipString = element.getDateString() + " " +
					element.getPoint().toString() + "点";
				}
				ToolTipComponent ttc2 = new ToolTipComponent(toolTipString);
				add(ttc2);
				ttc2.setBounds(x, y, fontHeight, fontHeight);
			}
			ToolTipComponent ttc1 =
				new ToolTipComponent("最高点: " + rbe.getMax() +
						" 最低点: " + rbe.getMin() +
						" 平均点: " + rbe.getAverage());
			add(ttc1);
			ttc1.setBounds(0, y, width, fontHeight);
		}
		setVisible(true);
	}

}
