/*
 * Copyright 2008-2013 Ryohei NISHIMURA
 */

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.InvalidPropertiesFormatException;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreProtocolPNames;
import org.xml.sax.SAXException;


public class Ribbentrop extends JFrame {

	private static final long serialVersionUID = -4585938827017554010L;

	private static class CSVFileFilter extends FileFilter {
		@Override
		public boolean accept(File f) {
			String filename = f.getName();
			int extensionIndex = filename.lastIndexOf('.');
			if (extensionIndex == -1) {
				return false;
			}
			String extensionString =
				filename.substring(extensionIndex + 1).toLowerCase();
			return extensionString.equals("csv");
		}
		@Override
		public String getDescription() {
			return "CSV ファイル (*.csv)";
		}
	}

	private static enum DialogResult {
		CONNECT,
		SAVE,
		OK,
		CANCEL,
	}

	private class AuthenticationDialog extends JDialog {
		private static final long serialVersionUID = 1633235700518188308L;
		private DialogResult result = null;
		private final JTextField idField =
			new JTextField(properties.getProperty("clubDAM.id"), 14);
		private final JPasswordField passwordField = new JPasswordField(14);
		public AuthenticationDialog() {
			super(Ribbentrop.this, "clubDAMからデータを読み込む", true);
			Container contentPane = getContentPane();
			GridBagLayout gbl = new GridBagLayout();
			GridBagConstraints gbc = new GridBagConstraints();
			contentPane.setLayout(gbl);
			JLabel idLabel = new JLabel("ID（ハイフン不要）");
			gbl.setConstraints(idLabel, gbc);
			contentPane.add(idLabel);
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			gbc.weightx = 1.0;
			gbl.setConstraints(idField, gbc);
			contentPane.add(idField);
			gbc.gridwidth = GridBagConstraints.RELATIVE;
			gbc.weightx = 0.0;
			JLabel passwordLabel = new JLabel("パスワード");
			gbl.setConstraints(passwordLabel, gbc);
			contentPane.add(passwordLabel);
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			gbc.weightx = 1.0;
			gbl.setConstraints(passwordField, gbc);
			contentPane.add(passwordField);
			gbc.weightx = 0.0;
			JPanel panel = new JPanel();
			JButton connectButton = new JButton("接続");
			connectButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					result = DialogResult.CONNECT;
					setVisible(false);
				}
			});
			panel.add(connectButton);
			JButton cancelButton = new JButton("キャンセル");
			cancelButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					result = DialogResult.CANCEL;
					setVisible(false);
				}
			});
			panel.add(cancelButton);
			gbl.setConstraints(panel, gbc);
			contentPane.add(panel);
			pack();
		}
		public DialogResult getResult() {
			return result;
		}
		public String getID() {
			return idField.getText();
		}
		public char[] getPassword() {
			return passwordField.getPassword();
		}
	}

	private class ImageFileDialog extends JDialog {
		private static final long serialVersionUID = -1145179584295824209L;
		private DialogResult result = null;
		private final JTextField widthField = new JTextField("480", 4);
		private final JTextField heightField = new JTextField("640", 4);
		private final JRadioButton jpegButton = new JRadioButton("JPEG", true);
		private final JRadioButton pngButton = new JRadioButton("PNG");
		private final JCheckBox zipCheckBox =
			new JCheckBox("ZIP ファイルに分割して保存");
		public ImageFileDialog() {
			super(Ribbentrop.this, "画像ファイルに保存", true);
			Container contentPane = getContentPane();
			GridBagLayout gbl = new GridBagLayout();
			GridBagConstraints gbc = new GridBagConstraints();
			contentPane.setLayout(gbl);
			JLabel widthLabel = new JLabel("幅");
			gbl.setConstraints(widthLabel, gbc);
			contentPane.add(widthLabel);
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			gbc.weightx = 1.0;
			gbl.setConstraints(widthField, gbc);
			contentPane.add(widthField);
			gbc.gridwidth = GridBagConstraints.RELATIVE;
			gbc.weightx = 0.0;
			final JLabel heightLabel = new JLabel("高さ");
			heightLabel.setEnabled(false);
			gbl.setConstraints(heightLabel, gbc);
			contentPane.add(heightLabel);
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			gbc.weightx = 1.0;
			heightField.setEnabled(false);
			gbl.setConstraints(heightField, gbc);
			contentPane.add(heightField);
			ButtonGroup buttonGroup = new ButtonGroup();
			buttonGroup.add(jpegButton);
			gbl.setConstraints(jpegButton, gbc);
			contentPane.add(jpegButton);
			buttonGroup.add(pngButton);
			gbl.setConstraints(pngButton, gbc);
			contentPane.add(pngButton);
			zipCheckBox.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					boolean isSelected = zipCheckBox.isSelected();
					heightLabel.setEnabled(isSelected);
					heightField.setEnabled(isSelected);
				}
			});
			gbl.setConstraints(zipCheckBox, gbc);
			contentPane.add(zipCheckBox);
			gbc.weightx = 0.0;
			JPanel panel = new JPanel();
			JButton saveButton = new JButton("保存");
			saveButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					result = DialogResult.SAVE;
					setVisible(false);
				}
			});
			panel.add(saveButton);
			JButton cancelButton = new JButton("キャンセル");
			cancelButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					result = DialogResult.CANCEL;
					setVisible(false);
				}
			});
			panel.add(cancelButton);
			gbl.setConstraints(panel, gbc);
			contentPane.add(panel);
			pack();
		}
		public DialogResult getResult() {
			return result;
		}
		public int getWidthValue() throws NumberFormatException {
			int width = Integer.parseInt(widthField.getText());
			if (width <= 0) {
				throw new NumberFormatException();
			}
			return width;
		}
		public int getHeightValue() throws NumberFormatException {
			int height = Integer.parseInt(heightField.getText());
			if (height <= 0 && height != -1) {
				throw new NumberFormatException();
			}
			return height;
		}
		public String getFormat() {
			if (jpegButton.isSelected()) {
				return "JPEG";
			} else if (pngButton.isSelected()) {
				return "PNG";
			} else {
				return null;
			}
		}
		public boolean isToSaveAsZip() {
			return zipCheckBox.isSelected();
		}
	}

	private static final Pattern STATUS_OK =
		Pattern.compile("<status>OK</status>");
	private static final Pattern CDMCARDNO =
		Pattern.compile("var cdmCardNo = '(\\w*)'");
	private static final Pattern CDMTOKEN =
		Pattern.compile("var cdmToken = '(\\w*)'");

	private List<RibbentropElement> data;
	private final Properties properties;
	private final File propertyFile;
	private final PrinterJob pj;
	private PageFormat pf;
	private final RibbentropCanvas canvas;
	private final JScrollPane scrollPane = new JScrollPane();

	public Ribbentrop(List<RibbentropElement> data,
			final Properties properties,
			File propertyFile,
			PrinterJob pj) {
		super("Ribbentrop");
		this.data = data;
		this.properties = properties;
		this.propertyFile = propertyFile;
		this.pj = pj;
		if (pj == null) {
			this.pf = null;
		} else {
			this.pf = pj.defaultPage();
		}
		addWindowListener(new WindowAdapter(){
			@Override
			public void windowClosing(WindowEvent e) {
				handleExit();
			}
		});
		boolean paintingRank =
			new Boolean(properties.getProperty("view.paintingRank", "true")).
			booleanValue();
		String sortType = properties.getProperty("view.sorttype", "max");
		canvas = new RibbentropCanvas(data,
				pf,
				paintingRank,
				RibbentropMergedElement.SORT_TYPE_MAP.get(sortType));
		scrollPane.setViewportView(canvas);
		getContentPane().add(scrollPane, BorderLayout.CENTER);
		JMenuBar menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu("ファイル");
		JMenuItem clubDAMMenuItem = new JMenuItem("clubDAMからデータを読み込む...");
		clubDAMMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				handleClubDAM(canvas);
			}
		});
		fileMenu.add(clubDAMMenuItem);
		JMenuItem loadCSV2MenuItem = new JMenuItem("CSVファイルを読み込む...");
		loadCSV2MenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				handleLoadCSV2(canvas);
			}
		});
		fileMenu.add(loadCSV2MenuItem);
		JMenuItem loadCSVMenuItem = new JMenuItem("旧形式のCSVファイルを読み込む...");
		loadCSVMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				handleLoadCSV(canvas);
			}
		});
		fileMenu.add(loadCSVMenuItem);
		JMenuItem loadHTMLMenuItem = new JMenuItem("旧形式のHTMLファイルを読み込む...");
		loadHTMLMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				handleLoadHTML(canvas);
			}
		});
		fileMenu.add(loadHTMLMenuItem);
		JMenuItem saveCSV2MenuItem = new JMenuItem("CSVファイルへ保存...");
		saveCSV2MenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				handleSaveCSV2();
			}
		});
		fileMenu.add(saveCSV2MenuItem);
		JMenuItem saveCSVMenuItem = new JMenuItem("旧形式のCSVファイルへ保存...");
		saveCSVMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				handleSaveCSV();
			}
		});
		fileMenu.add(saveCSVMenuItem);
		fileMenu.addSeparator();
		JMenuItem imageFileMenuItem = new JMenuItem("画像ファイルに保存...");
		imageFileMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				handleImageFile(canvas);
			}
		});
		fileMenu.add(imageFileMenuItem);
		fileMenu.addSeparator();
		JMenuItem pageMenuItem = new JMenuItem("ページ設定...");
		pageMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				handlePage(canvas);
			}
		});
		if (pj == null) {
			pageMenuItem.setEnabled(false);
		}
		fileMenu.add(pageMenuItem);
		JMenuItem printerMenuItem = new JMenuItem("プリンタ設定...");
		printerMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				boolean status = Ribbentrop.this.pj.printDialog();
				if (status) {
					handlePage(canvas);
				}
			}
		});
		if (pj == null) {
			printerMenuItem.setEnabled(false);
		}
		fileMenu.add(printerMenuItem);
		JMenuItem printMenuItem = new JMenuItem("印刷");
		printMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				PrinterJob pj = Ribbentrop.this.pj;
				pj.setPrintable(canvas, Ribbentrop.this.pf);
				try {
					pj.print();
				} catch (PrinterException ex) {
					JOptionPane.
					showMessageDialog(Ribbentrop.this,
							ex.getMessage(),
							"エラー",
							JOptionPane.ERROR_MESSAGE);
					ex.printStackTrace();
				}
			}
		});
		if (pj == null) {
			printMenuItem.setEnabled(false);
		}
		fileMenu.add(printMenuItem);
		fileMenu.addSeparator();
		JMenuItem exitMenuItem = new JMenuItem("終了");
		exitMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				handleExit();
			}
		});
		fileMenu.add(exitMenuItem);
		menuBar.add(fileMenu);
		JMenu viewMenu = new JMenu("表示");
		JMenuItem blogMenuItem = new JMenuItem("ブログ用テキストの表示");
		blogMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				handleBlog();
			}
		});
		viewMenu.add(blogMenuItem);
		viewMenu.addSeparator();
		ButtonGroup sortBG = new ButtonGroup();
		JMenuItem sortByMaxMenuItem =
			new JRadioButtonMenuItem("最高点でソート", sortType.equals("max"));
		sortByMaxMenuItem.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					properties.setProperty("view.sorttype", "max");
					canvas.sortTypeChanged(RibbentropMergedElement.
							SortType.MAX);
					scrollPane.setViewportView(canvas);
				}
			}
		});
		sortBG.add(sortByMaxMenuItem);
		viewMenu.add(sortByMaxMenuItem);
		JMenuItem sortByMinMenuItem =
			new JRadioButtonMenuItem("最低点でソート", sortType.equals("min"));
		sortByMinMenuItem.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					properties.setProperty("view.sorttype", "min");
					canvas.sortTypeChanged(RibbentropMergedElement.
							SortType.MIN);
					scrollPane.setViewportView(canvas);
				}
			}
		});
		sortBG.add(sortByMinMenuItem);
		viewMenu.add(sortByMinMenuItem);
		JMenuItem sortByAverageMenuItem =
			new JRadioButtonMenuItem("平均点でソート", sortType.equals("average"));
		sortByAverageMenuItem.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					properties.setProperty("view.sorttype", "average");
					canvas.sortTypeChanged(RibbentropMergedElement.
							SortType.AVERAGE);
					scrollPane.setViewportView(canvas);
				}
			}
		});
		sortBG.add(sortByAverageMenuItem);
		viewMenu.add(sortByAverageMenuItem);
		viewMenu.addSeparator();
		JCheckBoxMenuItem viewPaintingRankMenuItem =
			new JCheckBoxMenuItem("最高順位を表示する", paintingRank);
		viewPaintingRankMenuItem.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				boolean isPaintingRank;
				switch (e.getStateChange()) {
				case ItemEvent.SELECTED:
					isPaintingRank = true;
					break;
				case ItemEvent.DESELECTED:
					isPaintingRank = false;
					break;
				default:
					return;
				}
				properties.setProperty("view.paintingRank",
						Boolean.toString(isPaintingRank));
				canvas.rankChanged(isPaintingRank);
			}
		});
		viewMenu.add(viewPaintingRankMenuItem);
		menuBar.add(viewMenu);
		setJMenuBar(menuBar);
		pack();
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		Dimension screenSize = toolkit.getScreenSize();
		Insets screenInsets =
			toolkit.getScreenInsets(getGraphicsConfiguration());
		int maxheight = screenSize.height - screenInsets.bottom - getX();
		if (maxheight < getHeight()) {
			setSize(getWidth(), maxheight);
		}
	}

	private void handleExit() {
		try {
			FileOutputStream fos = new FileOutputStream(propertyFile);
			try {
				properties.storeToXML(fos, "Properties of Ranking Battle Printer");
			} catch (IOException e) {
				JOptionPane.showMessageDialog(this,
						e.getMessage(),
						"エラー",
						JOptionPane.ERROR_MESSAGE);
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			JOptionPane.showMessageDialog(this,
					e.getMessage(),
					"エラー",
					JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
		System.exit(0);
	}

	private void handleClubDAM(RibbentropCanvas canvas) {
		AuthenticationDialog authDialog = new AuthenticationDialog();
		authDialog.setVisible(true);
		if (authDialog.getResult() != DialogResult.CONNECT) {
			return;
		}
		HttpClient client = new DefaultHttpClient();
		client.getParams().setParameter(CoreProtocolPNames.USER_AGENT,
				"Mozilla/5.0 (Windows; U; Windows NT 5.0; ja; rv:1.9) Gecko/2008052906 Firefox/3.0");
		client.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);
		HttpPost method0 =
			new HttpPost("https://www.clubdam.com/app/damtomo/auth/LoginXML.do");
		method0.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);
		ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		nameValuePairs.add(new BasicNameValuePair("procKbn", "1"));
		String id = authDialog.getID();
		nameValuePairs.add(new BasicNameValuePair("loginId", id));
		char[] password = authDialog.getPassword();
		nameValuePairs.add(new BasicNameValuePair("password", new String(password)));
		Arrays.fill(password, (char) 0);
		nameValuePairs.add(new BasicNameValuePair("enc", "sjis"));
		nameValuePairs.add(new BasicNameValuePair("UTCserial", Long.toString(System.currentTimeMillis())));
		try {
			method0.setEntity(new UrlEncodedFormEntity(nameValuePairs));
		} catch (UnsupportedEncodingException e) {
			JOptionPane.showMessageDialog(this,
					e.getMessage(),
					"エラー",
					JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
			return;
		}
		BufferedReader r0;
		try {
			HttpResponse response = client.execute(method0);
			Reader r = new InputStreamReader(response.getEntity().getContent());
			r0 = new BufferedReader(r);
		} catch (ClientProtocolException e) {
			JOptionPane.showMessageDialog(this,
					e.getMessage(),
					"エラー",
					JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
			return;
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this,
					e.getMessage(),
					"エラー",
					JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
			return;
		}
		boolean statusOK = false;
		try {
			String s0;
			while ((s0 = r0.readLine()) != null) {
				Matcher m = STATUS_OK.matcher(s0);
				if (m.find()) {
					statusOK = true;
					break;
				}
			}
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this,
					e.getMessage(),
					"エラー",
					JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
			return;
		} finally {
			try {
				r0.close();
			} catch (IOException e) {
				JOptionPane.showMessageDialog(this,
						e.getMessage(),
						"エラー",
						JOptionPane.ERROR_MESSAGE);
				e.printStackTrace();
			}
		}
		if (!statusOK) {
			JOptionPane.showMessageDialog(this,
					"接続に失敗しました\nIDまたはパスワードを確認してください",
					"接続失敗",
					JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		properties.setProperty("clubDAM.id", id);
		HttpGet method1 = new HttpGet("https://www.clubdam.com/app/damtomo/MyPage.do");
		BufferedReader r1;
		try {
			HttpResponse response = client.execute(method1);
			Reader r = new InputStreamReader(response.getEntity().getContent());
			r1 = new BufferedReader(r);
		} catch (ClientProtocolException e) {
			JOptionPane.showMessageDialog(this,
					e.getMessage(),
					"エラー",
					JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
			return;
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this,
					e.getMessage(),
					"エラー",
					JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
			return;
		}
		String cdmCardNo = null;
		String cdmToken = null;
		try {
			String s1;
			while ((s1 = r1.readLine()) != null) {
				Matcher m1 = CDMCARDNO.matcher(s1);
				if (m1.find()) {
					cdmCardNo = m1.group(1);
				}
				Matcher m2 = CDMTOKEN.matcher(s1);
				if (m2.find()) {
					cdmToken = m2.group(1);
				}
			}
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this,
					e.getMessage(),
					"エラー",
					JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
			return;
		} finally {
			try {
				r1.close();
			} catch (IOException e) {
				JOptionPane.showMessageDialog(this,
						e.getMessage(),
						"エラー",
						JOptionPane.ERROR_MESSAGE);
				e.printStackTrace();
			}
		}
		for (int i = 10; i >= 1; --i) {
			HttpGet method2 =
				new HttpGet("http://www.clubdam.com/app/xml/membership/damtomo/rankingList.do" +
						"?cdmCardNo=" + cdmCardNo +
						"&cdmToken=" + cdmToken +
						"&enc=sjis&page=" + i +
						"&UTCserial=" + System.currentTimeMillis());
			InputStream i2 = null;
			try {
				HttpResponse response = client.execute(method2);
				i2 = response.getEntity().getContent();
				RibbentropData.parseXML(data, i2);
			} catch (ClientProtocolException e) {
				JOptionPane.showMessageDialog(this,
						e.getMessage(),
						"エラー",
						JOptionPane.ERROR_MESSAGE);
				e.printStackTrace();
			} catch (IOException e) {
				JOptionPane.showMessageDialog(this,
						e.getMessage(),
						"エラー",
						JOptionPane.ERROR_MESSAGE);
				e.printStackTrace();
			} catch (SAXException e) {
				JOptionPane.showMessageDialog(this,
						e.getMessage(),
						"エラー",
						JOptionPane.ERROR_MESSAGE);
				e.printStackTrace();
			} catch (ParserConfigurationException e) {
				JOptionPane.showMessageDialog(this,
						e.getMessage(),
						"エラー",
						JOptionPane.ERROR_MESSAGE);
				e.printStackTrace();
			}
		}
		canvas.dataChanged(data);
		scrollPane.setViewportView(canvas);
	}

	private void handleLoadCSV(RibbentropCanvas canvas) {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileFilter(new CSVFileFilter());
		int isFileSelected = fileChooser.showOpenDialog(this);
		if (isFileSelected != JFileChooser.APPROVE_OPTION) {
			return;
		}
		File file = fileChooser.getSelectedFile();
		List<RibbentropElement> data;
		try {
			Reader r = new FileReader(file);
			data = loadCSV(r);
		} catch (FileNotFoundException e) {
			JOptionPane.showMessageDialog(this,
					e.getMessage(),
					"エラー",
					JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
			return;
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this,
					e.getMessage(),
					"エラー",
					JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
			return;
		} catch (RibbentropElement.IllegalCSVFormatException e) {
			JOptionPane.showMessageDialog(this,
					e.getMessage(),
					"エラー",
					JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
			return;
		}
		this.data = data;
		canvas.dataChanged(data);
		scrollPane.setViewportView(canvas);
	}

	private void handleLoadCSV2(RibbentropCanvas canvas) {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileFilter(new CSVFileFilter());
		int isFileSelected = fileChooser.showOpenDialog(this);
		if (isFileSelected != JFileChooser.APPROVE_OPTION) {
			return;
		}
		File file = fileChooser.getSelectedFile();
		List<RibbentropElement> data;
		try {
			InputStream is = new FileInputStream(file);
			Reader r = new InputStreamReader(is, "UTF-8");
			data = loadCSV2(r);
		} catch (FileNotFoundException e) {
			JOptionPane.showMessageDialog(this,
					e.getMessage(),
					"エラー",
					JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
			return;
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this,
					e.getMessage(),
					"エラー",
					JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
			return;
		} catch (RibbentropElement.IllegalCSVFormatException e) {
			JOptionPane.showMessageDialog(this,
					e.getMessage(),
					"エラー",
					JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
			return;
		}
		this.data = data;
		canvas.dataChanged(data);
		scrollPane.setViewportView(canvas);
	}

	private void handleLoadHTML(RibbentropCanvas canvas) {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setMultiSelectionEnabled(true);
		fileChooser.setFileFilter(new FileFilter() {
			@Override
			public boolean accept(File f) {
				String filename = f.getName();
				int extensionIndex = filename.lastIndexOf('.');
				if (extensionIndex == -1) {
					return false;
				}
				String extensionString =
					filename.substring(extensionIndex + 1).toLowerCase();
				return extensionString.equals("html") ||
				extensionString.equals("htm");
			}
			@Override
			public String getDescription() {
				return "HTML ファイル (*.html;*.htm)";
			}
		});
		int isFileSelected = fileChooser.showOpenDialog(this);
		if (isFileSelected != JFileChooser.APPROVE_OPTION) {
			return;
		}
		File[] files = fileChooser.getSelectedFiles();
		for (File file : files) {
			try {
				FileReader fr = new FileReader(file);
				loadHTML(data, fr);
			} catch (FileNotFoundException e) {
				JOptionPane.showMessageDialog(this,
						e.getMessage(),
						"エラー",
						JOptionPane.ERROR_MESSAGE);
				e.printStackTrace();
				return;
			} catch (IOException e) {
				JOptionPane.showMessageDialog(this,
						e.getMessage(),
						"エラー",
						JOptionPane.ERROR_MESSAGE);
				e.printStackTrace();
				return;
			}
		}
		canvas.dataChanged(data);
		scrollPane.setViewportView(canvas);
	}

	private void handleSaveCSV() {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileFilter(new CSVFileFilter());
		int isFileSelected = fileChooser.showSaveDialog(this);
		if (isFileSelected != JFileChooser.APPROVE_OPTION) {
			return;
		}
		File file = fileChooser.getSelectedFile();
		String path = file.getAbsolutePath();
		int extensionIndex = path.lastIndexOf('.');
		String extension;
		if (extensionIndex == -1) {
			extension = null;
		} else {
			extension = path.substring(extensionIndex + 1).toLowerCase();
		}
		if (!"csv".equals(extension)) {
			file = new File(path + ".csv");
		}
		BufferedWriter bw = null;
		try {
			FileOutputStream fos = new FileOutputStream(file);
			bw = new BufferedWriter(new OutputStreamWriter(fos, "Windows-31J"));
			for (RibbentropElement rbe : data) {
				bw.write(rbe.toCSVLine());
			}
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this,
					e.getMessage(),
					"エラー",
					JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		} finally {
			if (bw != null) {
				try {
					bw.close();
				} catch (IOException e) {
					JOptionPane.showMessageDialog(this,
							e.getMessage(),
							"エラー",
							JOptionPane.ERROR_MESSAGE);
					e.printStackTrace();
				}
			}
		}
	}

	private void handleSaveCSV2() {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileFilter(new CSVFileFilter());
		int isFileSelected = fileChooser.showSaveDialog(this);
		if (isFileSelected != JFileChooser.APPROVE_OPTION) {
			return;
		}
		File file = fileChooser.getSelectedFile();
		String path = file.getAbsolutePath();
		int extensionIndex = path.lastIndexOf('.');
		String extension;
		if (extensionIndex == -1) {
			extension = null;
		} else {
			extension = path.substring(extensionIndex + 1).toLowerCase();
		}
		if (!"csv".equals(extension)) {
			file = new File(path + ".csv");
		}
		BufferedWriter bw = null;
		try {
			FileOutputStream fos = new FileOutputStream(file);
			bw = new BufferedWriter(new OutputStreamWriter(fos, "UTF-8"));
			bw.write(0xfeff);
			for (RibbentropElement rbe : data) {
				bw.write(rbe.toCSVLine2());
			}
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this,
					e.getMessage(),
					"エラー",
					JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		} finally {
			if (bw != null) {
				try {
					bw.close();
				} catch (IOException e) {
					JOptionPane.showMessageDialog(this,
							e.getMessage(),
							"エラー",
							JOptionPane.ERROR_MESSAGE);
					e.printStackTrace();
				}
			}
		}
	}

	private void handleImageFile(RibbentropCanvas canvas) {
		ImageFileDialog imageFileDialog = new ImageFileDialog();
		imageFileDialog.setVisible(true);
		if (imageFileDialog.getResult() != DialogResult.SAVE) {
			return;
		}
		int width;
		try {
			width = imageFileDialog.getWidthValue();
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(this,
					"「幅」は正の数値を入力してください",
					"エラー",
					JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
			return;
		}
		int height;
		try {
			height = imageFileDialog.getHeightValue();
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(this,
					"「高さ」は正の数値 を入力してください",
					"エラー",
					JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
			return;
		}
		boolean isToSaveAsZip = imageFileDialog.isToSaveAsZip();
		int numOfPages;
		if (isToSaveAsZip) {
			numOfPages = canvas.getNumberOfPages(height);
			if (numOfPages == -1) {
				JOptionPane.showMessageDialog(this,
						"出力に必要な高さが足りません",
						"エラー",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
		} else {
			numOfPages = 0;
		}
		String format = imageFileDialog.getFormat();
		JFileChooser fileChooser = new JFileChooser();
		FileFilter fileFilter;
		if (isToSaveAsZip) {
			fileFilter = new FileFilter() {
				@Override
				public boolean accept(File f) {
					String filename = f.getName();
					int extensionIndex = filename.lastIndexOf('.');
					if (extensionIndex == -1) {
						return false;
					}
					String extensionString =
						filename.substring(extensionIndex + 1).toLowerCase();
					return extensionString.equals("zip");
				}
				@Override
				public String getDescription() {
					return "ZIP ファイル (*.zip)";
				}
			};
		} else {
			if (format.equals("JPEG")) {
				fileFilter = new FileFilter() {
					@Override
					public boolean accept(File f) {
						String filename = f.getName();
						int extensionIndex = filename.lastIndexOf('.');
						if (extensionIndex == -1) {
							return false;
						}
						String extensionString =
							filename.substring(extensionIndex + 1).toLowerCase();
						return extensionString.equals("jpg") ||
						extensionString.equals("jpeg");
					}
					@Override
					public String getDescription() {
						return "JPEG ファイル (*.jpg;*.jpeg)";
					}
				};
			} else if (format.equals("PNG")) {
				fileFilter = new FileFilter() {
					@Override
					public boolean accept(File f) {
						String filename = f.getName();
						int extensionIndex = filename.lastIndexOf('.');
						if (extensionIndex == -1) {
							return false;
						}
						String extensionString =
							filename.substring(extensionIndex + 1).toLowerCase();
						return extensionString.equals("png");
					}
					@Override
					public String getDescription() {
						return "PNG ファイル (*.png)";
					}
				};
			} else {
				return;
			}
		}
		fileChooser.setFileFilter(fileFilter);
		int isFileSelected = fileChooser.showSaveDialog(this);
		if (isFileSelected != JFileChooser.APPROVE_OPTION) {
			return;
		}
		File file = fileChooser.getSelectedFile();
		String path = file.getAbsolutePath();
		int extensionIndex = path.lastIndexOf('.');
		String extension;
		if (extensionIndex == -1) {
			extension = null;
		} else {
			extension = path.substring(extensionIndex + 1).toLowerCase();
		}
		if (isToSaveAsZip) {
			if (!"zip".equals(extension)) {
				file = new File(path + ".zip");
			}
		} else {
			if (format.equals("JPEG")) {
				if (!"jpg".equals(extension) && !"jpeg".equals(extension)) {
					file = new File(path + ".jpg");
				}
			} else if (format.equals("PNG")) {
				if (!"png".equals(extension)) {
					file = new File(path + ".png");
				}
			}
		}
		try {
			BufferedOutputStream bos =
				new BufferedOutputStream(new FileOutputStream(file));
			if (isToSaveAsZip) {
				ZipOutputStream zos = new ZipOutputStream(bos);
				try {
					for (int i = 0; i < numOfPages; i++) {
						String filename;
						if (format.equals("JPEG")) {
							filename = (i + 1) + ".jpg";
						} else if (format.equals("PNG")) {
							filename = (i + 1) + ".png";
						} else {
							filename = null;
						}
						ZipEntry ze = new ZipEntry(filename);
						zos.putNextEntry(ze);
						paintOutputStream(zos,
								canvas,
								width,
								height,
								i,
								format);
						zos.closeEntry();
					}
				} catch (IOException e) {
					JOptionPane.showMessageDialog(this,
							e.getMessage(),
							"エラー",
							JOptionPane.ERROR_MESSAGE);
					e.printStackTrace();
					return;
				} finally {
					try {
						zos.close();
					} catch (IOException e) {
						JOptionPane.showMessageDialog(this,
								e.getMessage(),
								"エラー",
								JOptionPane.ERROR_MESSAGE);
						e.printStackTrace();
					}
				}
			} else {
				try {
					paintOutputStream(bos,
							canvas,
							width,
							canvas.getPreferredSize().height,
							0,
							format);
				} catch (IOException e) {
					JOptionPane.showMessageDialog(this,
							e.getMessage(),
							"エラー",
							JOptionPane.ERROR_MESSAGE);
					e.printStackTrace();
					return;
				} finally {
					try {
						bos.close();
					} catch (IOException e) {
						JOptionPane.showMessageDialog(this,
								e.getMessage(),
								"エラー",
								JOptionPane.ERROR_MESSAGE);
						e.printStackTrace();
					}
				}
			}
		} catch (FileNotFoundException e) {
			JOptionPane.showMessageDialog(this,
					e.getMessage(),
					"エラー",
					JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}

	private void handlePage(RibbentropCanvas canvas) {
		PageFormat newPF = pj.pageDialog(this.pf);
		if (newPF != this.pf) {
			this.pf = newPF;
			canvas.pageChanged(newPF);
			scrollPane.setViewportView(canvas);
		}
	}

	private void handleBlog() {
		final JFrame frame = new JFrame("ブログ用テキストの表示");
		Container contentPane = frame.getContentPane();
		StringBuffer sb = new StringBuffer();
		for (RibbentropElement rbe : data) {
			sb.append(rbe.toBlogLine());
		}
		JTextArea textArea = new JTextArea(sb.toString(), 80, 32);
		JScrollPane scrollPane = new JScrollPane(textArea);
		contentPane.add(scrollPane, BorderLayout.CENTER);
		JPanel panel = new JPanel();
		JButton okButton = new JButton("OK");
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				frame.setVisible(false);
			}
		});
		panel.add(okButton);
		contentPane.add(panel, BorderLayout.SOUTH);
		frame.pack();
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		Dimension screenSize = toolkit.getScreenSize();
		Insets screenInsets =
			toolkit.getScreenInsets(getGraphicsConfiguration());
		int maxheight = screenSize.height - screenInsets.bottom - getX();
		if (maxheight < frame.getHeight()) {
			frame.setSize(frame.getWidth(), maxheight);
		}
		frame.setVisible(true);
	}

	private static List<RibbentropElement> loadCSV(Reader r)
	throws IOException, RibbentropElement.IllegalCSVFormatException {
		ArrayList<RibbentropElement> data =
			new ArrayList<RibbentropElement>();
		BufferedReader br = new BufferedReader(r);
		try {
			String line;
			while ((line = br.readLine()) != null) {
				data.add(RibbentropElement.parseCSVLine(line));
			}
		} catch (IOException e) {
			throw e;
		} catch (RibbentropElement.IllegalCSVFormatException e) {
			throw e;
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				throw e;
			}
		}
		return data;
	}

	private static List<RibbentropElement> loadCSV2(Reader r)
	throws IOException, RibbentropElement.IllegalCSVFormatException {
		ArrayList<RibbentropElement> data =
			new ArrayList<RibbentropElement>();
		BufferedReader br = new BufferedReader(r);
		br.mark(1);
		if (br.read() != 0xfeff) {
			br.reset();
		}
		try {
			String line;
			while ((line = br.readLine()) != null) {
				data.add(RibbentropElement.parseCSVLine2(line));
			}
		} catch (IOException e) {
			throw e;
		} catch (RibbentropElement.IllegalCSVFormatException e) {
			throw e;
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				throw e;
			}
		}
		return data;
	}

	private static void loadHTML(List<RibbentropElement> data, Reader r)
	throws IOException {
		List<RibbentropElement> newData;
		try {
			newData = RibbentropData.parseHTML(r);
		} catch (IOException e) {
			throw e;
		} finally {
			try {
				r.close();
			} catch (IOException e) {
				throw e;
			}
		}
		for (int i = newData.size() - 1; i >= 0; --i) {
			RibbentropElement rbe = newData.get(i);
			if (!data.contains(rbe)) {
				data.add(rbe);
			}
		}
	}

	private static void paintOutputStream(OutputStream os,
			RibbentropCanvas canvas,
			int width,
			int height,
			int pageIndex,
			String format)
	throws IOException {
		BufferedImage bi;
		if (format.equals("JPEG")) {
			bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			Graphics2D g2d = bi.createGraphics();
			g2d.setColor(Color.WHITE);
			g2d.fillRect(0, 0, width, height);
		} else if (format.equals("PNG")) {
			bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		} else {
			return;
		}
		Graphics2D g2d = bi.createGraphics();
		canvas.paintForImageFile(g2d, width, height, pageIndex);
		ImageIO.write(bi, format, os);
	}

	public static void main(String[] args) {
		PrinterJob pj = PrinterJob.getPrinterJob();
		if (pj.getPrintService() == null) {
			JOptionPane.showMessageDialog(null,
					"印刷可能なプリンタが存在しません",
					"エラー",
					JOptionPane.ERROR_MESSAGE);
			pj = null;
		}
		File homeFile = new File(System.getProperty("user.home", ""));
		File propertyFile =
			new File(homeFile, ".rankingbattleprinter.properties.xml");
		Properties properties = new Properties();
		try {
			FileInputStream fis = new FileInputStream(propertyFile);
			try {
				properties.loadFromXML(fis);
			} catch (InvalidPropertiesFormatException e) {
				JOptionPane.
				showMessageDialog(null,
						e.getMessage(),
						"エラー",
						JOptionPane.ERROR_MESSAGE);
				e.printStackTrace();
			} catch (IOException e) {
				JOptionPane.
				showMessageDialog(null,
						e.getMessage(),
						"エラー",
						JOptionPane.ERROR_MESSAGE);
				e.printStackTrace();
			} finally {
				try {
					fis.close();
				} catch (IOException e) {
					JOptionPane.
					showMessageDialog(null,
							e.getMessage(),
							"エラー",
							JOptionPane.ERROR_MESSAGE);
					e.printStackTrace();
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		List<RibbentropElement> data = new ArrayList<RibbentropElement>();
		for (String arg : args) {
			int extensionIndex = arg.lastIndexOf('.');
			if (extensionIndex == -1) {
				continue;
			}
			String extension = arg.substring(extensionIndex + 1).toLowerCase();
			if (extension.equals("csv")) {
				try {
					Reader r = new FileReader(arg);
					List<RibbentropElement> newData = loadCSV(r);
					data = newData;
					break;
				} catch (FileNotFoundException e) {
					JOptionPane.
					showMessageDialog(null,
							e.getMessage(),
							"エラー",
							JOptionPane.ERROR_MESSAGE);
					e.printStackTrace();
				} catch (IOException e) {
					JOptionPane.showMessageDialog(null,
							e.getMessage(),
							"エラー",
							JOptionPane.ERROR_MESSAGE);
					e.printStackTrace();
				} catch (RibbentropElement.IllegalCSVFormatException e) {
					JOptionPane.showMessageDialog(null,
							e.getMessage(),
							"エラー",
							JOptionPane.ERROR_MESSAGE);
					e.printStackTrace();
				}
			} else if (extension.equals("html") || extension.equals("htm")) {
				try {
					Reader r = new FileReader(arg);
					loadHTML(data, r);
				} catch (FileNotFoundException e) {
					JOptionPane.
					showMessageDialog(null,
							e.getMessage(),
							"エラー",
							JOptionPane.ERROR_MESSAGE);
					e.printStackTrace();
				} catch (IOException e) {
					JOptionPane.
					showMessageDialog(null,
							e.getMessage(),
							"エラー",
							JOptionPane.ERROR_MESSAGE);
					e.printStackTrace();
				}
			}
		}
		Ribbentrop brp =
			new Ribbentrop(data, properties, propertyFile, pj);
		brp.setVisible(true);
	}

}
