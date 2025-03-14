package it.tabaqui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

public class Main {
	
	// Static fields
	
	public static final Pattern PROPERTY_PATTERN = Pattern.compile("\\s*([a-zA-Z0-9._-]+)\\s*=\\s*(.*)\\s*");
	
	// Static methods
	
	public static void main(String[] args) throws Exception {
		// Look & feel
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
		}
		// Lettura font di sistema
		Font font;
		try (InputStream is = Main.class.getClassLoader().getResourceAsStream("font.ttf")) {
			font = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(16.0f);
		}
		// Lettura properties
		Properties properties = new Properties();
		try (InputStream is = Main.class.getClassLoader().getResourceAsStream("configuration.properties")) {
			properties.load(is);
		}
		// Sovrascrittura properties lette con quelle fornite come argomenti (in formato compatibile chiave=valore)
		Arrays.stream(args)
				.map(PROPERTY_PATTERN::matcher)
				.filter(Matcher::matches)
				.forEach((v) -> properties.setProperty(v.group(1), v.group(2)));
		// Lettura/validazione parametri
		String host = properties.getProperty("tabaqui.host", "");
		if (host.isBlank()) {
			throw new IllegalArgumentException("tabaqui.host: valore non valido");
		}
		int port;
		try {
			port = Integer.parseInt(properties.getProperty("tabaqui.port"));
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("tabaqui.port: valore non valido");
		}
		long refreshRate;
		try {
			refreshRate = Long.parseLong(properties.getProperty("tabaqui.refresh-rate"));
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("tabaqui.refresh-rate: valore non valido");
		}
		if (refreshRate <= 0) {
			throw new IllegalArgumentException("tabaqui.refresh-rate: valore non valido");
		}
		long timeWindow;
		try {
			timeWindow = Long.parseLong(properties.getProperty("tabaqui.time-window"));
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("tabaqui.time-window: valore non valido");
		}
		if (timeWindow <= 0) {
			throw new IllegalArgumentException("tabaqui.time-window: valore non valido");
		}
		long alertValue;
		try {
			alertValue = Long.parseLong(properties.getProperty("tabaqui.alert-value"));
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("tabaqui.alert-value: valore non valido");
		}
		if (alertValue <= 0) {
			throw new IllegalArgumentException("tabaqui.alert-value: valore non valido");
		}
		// Connessione a JVM remota ed esecuzione applicazione
		try (JMXConnector connector = JMXConnectorFactory
				.connect(new JMXServiceURL(String.format("service:jmx:rmi:///jndi/rmi://%s:%d/jmxrmi", host, port)))) {
			// Costruzione ObjectName per MBean da monitorare e recupero connessione
			ObjectName name = new ObjectName("java.lang:type=Memory");
			MBeanServerConnection connection = connector.getMBeanServerConnection();
			// Serie di dati
			TimeSeries series = new TimeSeries("<default>");
			series.setMaximumItemAge(timeWindow);
			TimeSeriesCollection collection = new TimeSeriesCollection(series);
			// Grafico
			JFreeChart chart = ChartFactory.createTimeSeriesChart(null, null, null, collection, false, false, false);
			chart.setBackgroundPaint(null);
			// Renderer grafico
			XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
			renderer.setSeriesPaint(0, Color.BLUE);
			renderer.setSeriesStroke(0, new BasicStroke(2.0f));
			renderer.setSeriesShapesVisible(0, false);
			// Area grafico
			XYPlot xyPlot = chart.getXYPlot();
			xyPlot.setRenderer(renderer);
			xyPlot.setDomainGridlinesVisible(false);
			xyPlot.setRangeGridlinesVisible(false);
			// Asse X
			ValueAxis xAxis = xyPlot.getDomainAxis();
			xAxis.setTickLabelsVisible(false);
			xAxis.setTickMarksVisible(false);
			xAxis.setFixedAutoRange(timeWindow);
			// Asse Y
			NumberAxis yAxis = NumberAxis.class.cast(xyPlot.getRangeAxis());
			yAxis.setTickLabelFont(font);
			yAxis.setNumberFormatOverride(new DecimalFormat("#,##0 MB"));
			yAxis.setAutoRange(true);
			yAxis.setAutoRangeIncludesZero(true);
			yAxis.setAutoRangeMinimumSize(10.0);
			ChartPanel chartPane = new ChartPanel(chart);
			chartPane.setPreferredSize(new Dimension(320, 240));
			// Etichetta di alert
			JLabel alertLabel = new JLabel("", JLabel.CENTER);
			alertLabel.setFont(font);
			alertLabel.setForeground(Color.RED);
			// Etichetta valore corrente
			JLabel valueLabel = new JLabel("0.00 MB", JLabel.RIGHT);
			valueLabel.setFont(font);
			// Pannello etichette
			JPanel labelPane = new JPanel();
			labelPane.setLayout(new GridLayout(0, 1, 0, 4));
			labelPane.add(alertLabel);
			labelPane.add(valueLabel);
			// Pannello principale
			JPanel contentPane = new JPanel();
			contentPane.setLayout(new BorderLayout(4, 4));
			contentPane.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
			contentPane.add(chartPane, BorderLayout.CENTER);
			contentPane.add(labelPane, BorderLayout.PAGE_END);
			// Creazione JFrame
			JFrame frame = new JFrame("MemoryMonitor");
			frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			frame.setResizable(false);
			frame.setContentPane(contentPane);
			frame.pack();
			frame.setLocationRelativeTo(null);
			SwingUtilities.invokeAndWait(() -> frame.setVisible(true));
			// Ciclo di lettura/refresh
			while (frame.isVisible()) {
				// Leggo il dato dall'MBean Memory
				CompositeData data = CompositeData.class.cast(connection.getAttribute(name, "HeapMemoryUsage"));
				long used = Long.class.cast(data.get("used"));
				// Se il dato supera il valore di allerta aggiorno l'etichetta di avviso
				if (used > alertValue) {
					alertLabel.setText("Valore di allerta superato!");
				} else {
					alertLabel.setText("");
				}
				// Aggiorno l'etichetta del valore corrente
				valueLabel.setText(String.format("%.02f MB", used / (1024.0 * 1024.0)));
				// Inserisco il nuovo dato (convertito in MB) nel grafico
				series.add(new Millisecond(), used / (1024.0 * 1024.0));
				// Attendo il prossimo ciclo di lettura/refresh
				Thread.sleep(Duration.ofMillis(refreshRate));
			}
		}
	}
}
