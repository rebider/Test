package others.pic;

import java.util.concurrent.TimeUnit;

import utils.BoundedExecutor;

//http://litejav.com/label/巨乳/page/2/
//http://litejav.com/box/uncensored/page/100/

public class GetPicNTitles {

	private static int censoredTotalPgNo = 11036;
	private static int uncensoredTotalPgNo = 718;

	public static void main(String[] args) throws Exception {
		long b0 = System.currentTimeMillis();
		
		String category = "";//熟女, censored, uncensored <-------------------- change here
		
		//submit tasks for downloading mp3
		BoundedExecutor be = new BoundedExecutor(10);
		
		for(int i=1;i<=172;i++){//1-72 <-------------------- change here
			TPic tpic = new TPic(category, i);
			try {
				be.submit(tpic);
				System.out.println(category+", "+i+", submitted.");
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
		
		be.getExecutorService().shutdown();
		try {
			be.getExecutorService().awaitTermination(1, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		long e0 = System.currentTimeMillis();
		System.out.println("Total time:"+ (e0-b0)/1000+" seconds.");
	}


	


}
