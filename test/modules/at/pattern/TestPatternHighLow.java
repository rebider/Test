package modules.at.pattern;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import modules.at.feed.convert.TickToBarConverter;
import modules.at.feed.history.HistoryLoader;
import modules.at.model.Bar;
import modules.at.model.Tick;
import modules.at.model.Trade;
import modules.at.pattern.PatternHighLow.HighLowVertex;
import modules.at.stg.Indicator;
import modules.at.stg.Setting;
import utils.Formatter;

public class TestPatternHighLow {

	static double LOCK_PROFIT = Double.NaN;//keeps changing, and LOCK_PROFIT always > CUT_LOSS
	
	private static PatternHighLow patternHighLow;
	private static Setting as = new Setting();
	
	
	public static void main(String[] args) throws Exception {
		patternHighLow = new PatternHighLow(as);
		testOneDay();
	}

	private static void testOneDay() throws Exception{
		String stockCode = "qqq";//qqq, tna, tza 
		String[] dateTimeArr = new String[] {"20111021", "200115"};
		
		List<Trade> tradeList = auto(stockCode, dateTimeArr[0], dateTimeArr[1]);
		System.out.println(stockCode + ":" + dateTimeArr[0] + "-" + dateTimeArr[1]);
		printTrades(tradeList, true);
		
		//new MySampleChartBase(stockCode, dateTimeArr[0], dateTimeArr[1], tradeList);
	}
	
	private static List<Trade> auto(String stockCode, String dateStr, String timeStr) throws Exception {
		String tickFileName = dateStr + "-" + timeStr + ".txt";
		List<Tick> tickList = HistoryLoader.getNazHistTicks(stockCode, tickFileName, dateStr);
		List<Bar> barList = TickToBarConverter.convert(tickList, TickToBarConverter.MINUTE);

		Indicator indicators = new Indicator(as);
		//indicators.addObserver(patternHighLow);
		
		List<Trade> tradeList = new ArrayList<Trade>();
		for (Bar bar : barList) {
			indicators.addBar(bar);//update indicators 
		}
		//generate all high/low trades
		LinkedList<HighLowVertex> highList = patternHighLow.getHighList();
		LinkedList<HighLowVertex> lowList = patternHighLow.getLowList();
		for(HighLowVertex point : highList){
			tradeList.add(new Trade(point.getPrice(), -1, point.getBar().getDate().getTime(), Trade.Type.ShortEntry));
		}
		for(HighLowVertex point : lowList){
			tradeList.add(new Trade(point.getPrice(), 1, point.getBar().getDate().getTime(), Trade.Type.LongEntry));
		}
		
		return tradeList;
	}
	
	


	
	private static double printTrades(List<Trade> tradeList, boolean printDetail){
		double pnL = 0;
		for(Trade trade : tradeList){
		    if(printDetail){
		        System.out.println(trade);
		    }
			pnL = pnL + (trade.getPrice() * trade.getQty()*(-1));
		}
		System.out.println("Total pnL : "+Formatter.DECIMAL_FORMAT.format(pnL));
		return pnL;
	}


}
