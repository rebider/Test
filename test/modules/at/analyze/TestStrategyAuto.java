package modules.at.analyze;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import modules.at.TradeUtil;
import modules.at.feed.convert.TickToBarConverter;
import modules.at.feed.history.HistoryLoader;
import modules.at.model.AlgoSetting;
import modules.at.model.AlgoSetting.TradeDirection;
import modules.at.model.Bar;
import modules.at.model.ConfigRangeContainer;
import modules.at.model.Position;
import modules.at.model.Tick;
import modules.at.model.Trade;
import modules.at.model.visual.VChart;
import modules.at.model.visual.VMarker;
import modules.at.model.visual.VPlot;
import modules.at.stg.Strategy;
import modules.at.stg.Strategy.Decision;
import modules.at.stg.StrategyMA;
import modules.at.visual.BarChartUtil;
import modules.at.visual.ChartBase;

import org.jfree.chart.annotations.XYAnnotation;
import org.jfree.chart.renderer.xy.CandlestickRenderer;
import org.jfree.ui.Layer;

import utils.Formatter;

public class TestStrategyAuto {
	
	private double LOCK_PROFIT = Double.NaN;//keeps changing, and LOCK_PROFIT always > CUT_LOSS
	
	//private AlgoSetting as = new AlgoSetting();
	
	public static void main(String[] args) throws Exception {
		TestStrategyAuto tsa = new TestStrategyAuto();
		long b0 = System.currentTimeMillis();
		//loop AlgoSettings
		ConfigRangeContainer<AlgoSetting> crc = new ConfigRangeContainer<AlgoSetting>(AlgoSetting.class);
		List<AlgoSetting> asList = crc.getConfigList();
		for(AlgoSetting as : asList){
			tsa.testByDates(as); //testByDates
		}
		long e0 = System.currentTimeMillis();
		System.out.println("total time used: "+ (e0-b0));
	}

	private void testByDates(AlgoSetting as) throws Exception{
		String stockCode = "qqq";//qqq, tna, tza 
		String[][] dateTimeArr = 
				//initAllDates(stockCode); //all dates under data/naz/tick/output/qqq
		        initListedDate(); //listed dates only
		//get barList
		for(int i=0;i<dateTimeArr.length;i++){
			String tickFileName = dateTimeArr[i][0] + "-" + dateTimeArr[i][1] + ".txt";
			List<Tick> tickList = HistoryLoader.getNazHistTicks(stockCode, tickFileName, dateTimeArr[i][0]);
			List<List<Bar>> barLists = new ArrayList<List<Bar>>();
			List<Bar> barList = TickToBarConverter.convert(tickList, as.getBarTimePeriod());
			List<Bar> barList2 = TickToBarConverter.convert(tickList, 5*as.getBarTimePeriod());
			barLists.add(barList);
			barLists.add(barList2);
			

			//initialize strategy
			Strategy strategy =
				//new StrategyMacd();
				//new StrategyReversal();
				new StrategyMA(as);
			    //new MACrossStrategy();
			
			//get tradeList
			List<Trade> tradeList = auto(dateTimeArr[i][0], barList, strategy, as);
			System.out.print(stockCode + ":" + dateTimeArr[i][0] + "-" + dateTimeArr[i][1]+", ["
					+ Formatter.DECIMAL_FORMAT.format(((modules.at.stg.StrategyMA.IndicatorsMA)strategy.getIndicators()).minSMALow2Diff)+", "
					+ Formatter.DECIMAL_FORMAT.format(((modules.at.stg.StrategyMA.IndicatorsMA)strategy.getIndicators()).maxSMALow2Diff)+"]"
					);
			TradeUtil.printTrades(tradeList, false);
			//System.out.println();
			
			//display chart with trade and mark info
			VChart vchart = createMarkedChart(barLists, tradeList, strategy.getDecisionMarkerList(), strategy);
			vchart.setTitle(tickFileName);
			
			boolean saveToFile = false;//save to file | display
		    ChartBase cb = new ChartBase(vchart, !saveToFile && dateTimeArr.length==1);
		    if(saveToFile){
		    	String fileName = "D:/user/stock/us/screen-snapshot/MAStrategy/tmp/"+as.getId()+"_"+i+"_"+dateTimeArr[i][0]+".png"; 
		    	cb.saveToFile(fileName);
		    	System.out.println(fileName+" is created.");
		    }
		}
	    
	}
	

	
	private List<Trade> auto(String dateStr, List<Bar> barList, Strategy strategy, AlgoSetting as) throws Exception {

		List<Trade> tradeList = new ArrayList<Trade>();
		for (Bar bar : barList) {
			//System.out.println("TestAuto:time="+Formatter.DEFAULT_DATETIME_FORMAT.format(bar.getDate())+", price="+Formatter.DECIMAL_FORMAT.format(bar.getClose()));
			Trade trade = decide(bar, dateStr, strategy, as);
			if(trade != null){
				tradeList.add(trade);
			}
			//System.out.println("-------------------------------------------");
		}
		return tradeList;
	}
	
	
	/**
	 * position.qty==0: long, short, 
	 * position.qty!=0: take profit, cut loss
	 * 
	 * @param rsi
	 * @param price
	 * @param time
	 * @param position
	 * @return
	 * @throws Exception
	 */
	private Trade decide(Bar bar, String dateStr, Strategy strategy, AlgoSetting as) throws Exception{
		double price = bar.getClose();
		long time = bar.getDate().getTime();
		String tmpTimeStr = Formatter.DEFAULT_DATETIME_MM_FORMAT.format(new Date(time));
		if("20110930-13:01".equals(tmpTimeStr)){
			System.out.println();
		}
		
		Position position = Position.getInstance(as);
		int pQty = position.getQty();
		double stopPrice = position.getStopLossPrice();
//		System.out.println(Formatter.DEFAULT_DATETIME_FORMAT.format(new Date(time))+", stopPrice="+Formatter.DECIMAL_FORMAT4.format(stopPrice));

		Trade trade = null;
		TradeTimeLot tradeLot = getTradeLot(time, dateStr);
		switch (tradeLot){
			case PreTrade :
				break;
			case InTrade :
				
				Decision preBarDecision = strategy.getPreBarDecision();//must be called before strategy.update(bar), as this method changes preBarDecision
				Strategy.Decision decision = preBarDecision;//decision basing on preBar
				trade = processDecision(position, decision, bar, as);

				break;
			case WrapUp :
				if(pQty != 0){
					trade = new Trade(price, -1 * pQty, time, Trade.Type.WrapUp);
					if(pQty>0){
						position.updatePosition(-1 * pQty, bar.getLow());
					} else if(pQty<0){
						position.updatePosition(-1 * pQty, bar.getHigh());
					}
				}
				break;
			case AfterTrade:
				break;
			default: 
				break;
		}
		if(trade!=null){
			trade.setPnl(0);//TODO: is this necessary?
		}
		strategy.update(bar);//update strategy
		position.updateStopPrice(price);
		return trade;
	}
	
	/**
	 * return null means don't do anything
	 * To be strict: buy high, sell low 
	 * 
	 */
	private Trade processDecision(Position position, Strategy.Decision preBarDecision, Bar bar, AlgoSetting as){
		Trade trade = null;
		
		int pQty = position.getQty();
	
		long time = bar.getDate().getTime();
		double high = bar.getHigh();
		double low = bar.getLow();
		
		//execute preBarDecision
		//cut loss
		if(Decision.CutLossForLong.equals(preBarDecision)){
			trade = new Trade(low, -1 * pQty, time, Trade.Type.CutLoss);
			position.updatePosition(-1 * pQty, low);
		} else if(Decision.CutLossForShort.equals(preBarDecision)){
			trade = new Trade(high, -1 * pQty, time, Trade.Type.CutLoss);
			position.updatePosition(-1 * pQty, high);
		} else {
			if(as.getTradeDirection().equals(TradeDirection.LongOnly)){
				if(pQty==0 && Decision.LongEntry.equals(preBarDecision)){//buy high for long
					trade = new Trade(high, as.getTradeUnit(), time, Trade.Type.LongEntry);
					position.updatePosition(as.getTradeUnit(), high);
				} else if(pQty>0 && Decision.LongExit.equals(preBarDecision)){//sell low for long
					trade = new Trade(low, -1*pQty*as.getTradeUnit(), time, Trade.Type.LongExit);
					position.updatePosition(-1*pQty*as.getTradeUnit(), low);
				}
			}else if(as.getTradeDirection().equals(TradeDirection.ShortOnly)){
				if(pQty==0 && Decision.ShortEntry.equals(preBarDecision)){
					trade = new Trade(low, -1*as.getTradeUnit(), time, Trade.Type.ShortEntry);
					position.updatePosition(-1*as.getTradeUnit(), low);
				} else if(pQty<0 && Decision.ShortExit.equals(preBarDecision)){
					trade = new Trade(high, -1*pQty*as.getTradeUnit(), time, Trade.Type.ShortExit);
					position.updatePosition(-1*pQty*as.getTradeUnit(), low);
				}
			}else if(as.getTradeDirection().equals(TradeDirection.Both)){
				
			}
		}
		return trade;
	}
	
	
	// time range of trade
	enum TradeTimeLot {
		PreTrade, InTrade, WrapUp, AfterTrade
	}
	private static TradeTimeLot getTradeLot(long time, String dateStr) throws Exception{

		long inTradeBegin = Formatter.DEFAULT_DATETIME_FORMAT.parse(dateStr+"-09:45:00").getTime();
		long inTradeEnd = Formatter.DEFAULT_DATETIME_FORMAT.parse(dateStr+"-15:45:00").getTime();
		long marketClose = Formatter.DEFAULT_DATETIME_FORMAT.parse(dateStr+"-16:00:00").getTime();
		
		if(time<inTradeBegin){
			return TradeTimeLot.PreTrade;
		}else if(time<inTradeEnd){
			return TradeTimeLot.InTrade;
		}else if(time<marketClose){
			return TradeTimeLot.WrapUp;
		}
		return TradeTimeLot.AfterTrade;
	}
	
	/*
	//test all days in data/naz/tick/output/qqq
	@Deprecated
	private static void testAllDays() throws Exception{
		String stockCode = "qqq";//qqq, tna, tza 
		List<String[]> dateTimeArrList = TradeUtil.getInputParams(stockCode);
		
		double totalDaysPnL = 0;
		for(String[] dateTimeArr : dateTimeArrList){
			String tickFileName = dateTimeArr[0] + "-" + dateTimeArr[1] + ".txt";
			List<Tick> tickList = HistoryLoader.getNazHistTicks(stockCode, tickFileName, dateTimeArr[0]);
			
			List<Bar> barList = TickToBarConverter.convert(tickList, TickToBarConverter.MINUTE);
			
			List<Trade> tradeList = auto(dateTimeArr[0], barList);
			System.out.print(stockCode + ":" + dateTimeArr[0] + "-" + dateTimeArr[1]+"; ");
			totalDaysPnL += TradeUtil.printTrades(tradeList, false);
			//break;
		}
		System.out.println("totalDaysPnL = "+Formatter.DECIMAL_FORMAT.format(totalDaysPnL));
	}
	*/

	private static VChart createMarkedChart(List<List<Bar>> barLists, List<Trade> tradeList, List<VMarker> decisionMarkerList, Strategy strategy){
	    VChart vchart = BarChartUtil.createBasicChart(strategy, barLists);
        VPlot vplotBar = vchart.getPlotList().get(0);	
        //add trade annotations
	    List<XYAnnotation> tradeAnnoList = BarChartUtil.trade2AnnotationList(tradeList);
	    vplotBar.addAnnotations(tradeAnnoList);
	    CandlestickRenderer barRenderer = (CandlestickRenderer)vplotBar.getVseriesList().get(0).getRenderer();//bar renderer, first plot, first series
		//add pattern markers to plotBar
        for(VMarker m : decisionMarkerList){
        	barRenderer.addAnnotation(m.toAnno(),Layer.BACKGROUND);
        }
        return vchart;
	}
	

	private static String[][] initAllDates(String stockCode) {
		List<String[]> dateTimeArrList = TradeUtil.getInputParams(stockCode);
		return dateTimeArrList.toArray(new String[][]{});
	}
	
	private static String[][] initListedDate(){
		//sanple format: "20110915", "194819"
		return new String[][] {
//				{"20111026", "200246"},
//				{"20111117", "200308"},
//				{"20110929", "202100"},
//				{"20110926", "200301"},
//				{"20111116", "213410"},
//				{"20111109", "200435"},
//				{"20110928", "220751"},
//				{"20111004", "195804"},
//				{"20110922", "200738"},
//				{"20111020", "200135"},
//				{"20111110", "200140"},
//				{"20111213", "200215"},
//				{"20110927", "200425"},
//				{"20110920", "212519"},
//				{"20111208", "200123"},
//				{"20111101", "200252"},
//				{"20111003", "224038"},
//				{"20111216", "200137"},
//				{"20111123", "200120"},
//				{"20111122", "200102"},
//				{"20111027", "200154"},
//				{"20111219", "200101"},
//				{"20111108", "200141"},
//				{"20111021", "200115"},
//				{"20111107", "200117"},
				{"20110930", "205736"},
//				{"20111017", "200114"},
//				{"20111214", "200144"},
//				{"20111205", "200105"},
//				{"20111104", "200117"},
//				{"20110915", "194819"},
//				{"20111031", "200117"},
//				{"20111118", "200223"},
//				{"20111215", "200308"},
//				{"20111012", "200149"},
//				{"20111114", "200132"},
//				{"20111025", "200332"},
//				{"20110923", "223948"},
//				{"20110916", "195420"},
//				{"20111202", "200058"},
//				{"20111102", "200117"},
//				{"20111129", "200135"},
//				{"20111221", "200133"},
//				{"20111014", "200153"},
//				{"20111128", "200121"},
//				{"20111007", "001654"},
//				{"20111201", "200119"},
//				{"20111103", "200218"},
//				{"20111011", "200137"},
//				{"20111121", "200136"},
//				{"20111212", "200105"},
//				{"20111018", "200248"},
//				{"20111115", "200120"},
//				{"20111111", "200109"},
//				{"20111028", "200140"},
//				{"20111207", "200059"},
//				{"20111024", "200203"},
//				{"20111013", "200134"},
//				{"20110919", "205230"},
//				{"20111206", "200048"},
//				{"20111130", "200141"},
//				{"20111223", "200030"},
//				{"20111226", "200034"},
//				{"20111220", "200110"},
//				{"20111209", "200132"},
//				{"20111222", "200040"},



				};

	}
}
