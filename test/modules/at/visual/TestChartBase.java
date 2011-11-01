package modules.at.visual;

import java.util.ArrayList;
import java.util.List;

import modules.at.feed.convert.TickToBarConverter;
import modules.at.feed.history.HistoryLoader;
import modules.at.model.Bar;
import modules.at.model.Tick;
import modules.at.model.visual.VChart;
import modules.at.model.visual.VPlot;
import modules.at.model.visual.VSeries;
import modules.at.model.visual.VXY;

public class TestChartBase {

	private static String stockCode = "qqq";
	private static String dateStr = "20111014";
	private static String timeStr = "200153";
	/**
	 * @param args
	 */

	public static void main(String args[]) {
	    VChart vchart = new VChart("TestChartBase.java");
	    
	    List<Bar> barList = getBarList();
	    
	    /**
	     * bar plot0
	     */
	    VPlot vplotBar = new VPlot(4);
	    vplotBar.addSeries(new VSeries("Bar", null, barList, java.awt.Color.red));
	    vplotBar.addAnnotation(BarChartUtil.getLine(1318599059100L, 57.9, 1318599598000L, 58.1));
	    vchart.addPlot(vplotBar);	    
	    
	    /**
	     * indicators plot1
	     */
	    //MA plot
	    VPlot vplotIndicator = new VPlot(1);
	    vplotIndicator.addSeries(new VSeries("MAFast",BarChartUtil.getVXYList(BarChartUtil.SeriesType.MAFast, barList), null, java.awt.Color.red));
	    vplotIndicator.addSeries(new VSeries("MASlow", BarChartUtil.getVXYList(BarChartUtil.SeriesType.MASlow, barList), null, java.awt.Color.blue));
	    vchart.addPlot(vplotIndicator);

	    //RSI plot
	    VPlot vplotRsi = new VPlot(1);
	    vplotRsi.addSeries(new VSeries("Rsi", BarChartUtil.getVXYList(BarChartUtil.SeriesType.Rsi, barList), null, java.awt.Color.red));
	    vchart.addPlot(vplotRsi);
	    
	    
	    new ChartBase(vchart);
	}


	// get bar list
	private static List<Bar> getBarList() {
		String tickFileName = dateStr + "-" + timeStr+".txt";
		List<Bar> barList = new ArrayList<Bar>();
		try {
			// change begin -> for new date
			String nazTickOutputDateStr = dateStr;// change for new date
			List<Tick> tickList = HistoryLoader.getNazHistTicks(stockCode, tickFileName, nazTickOutputDateStr);
			// change end -> for new date
			barList = TickToBarConverter.convert(tickList, TickToBarConverter.MINUTE);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return barList;
	}

}