package others;

public class TestString {

    /**
     * @param args
     */
    public static void main(String[] args) {
        //testSplitLine();
        //testSplit();
        //testRemovePackageStr();
    	testFormat();

    }
    
    private static void testFormat(){
    	int batchSize = 24;
    	int startOffset = 720;
    	int jsonIdx = 30;
    	
    	for(int i=0;i<10;i++){
    		jsonIdx = i;
    		startOffset = jsonIdx * batchSize; 
    		System.out.println(
    			String.format("http://corpus.vocabulary.com/examples.json?query=%s&maxResults=%d&startOffset=%d&filter=0&jsonp=json%d", 
    					"elegy", batchSize, startOffset, jsonIdx));
    	
    	}
    }
    
    private static void testSplitLine(){
        String s = "WASHINGTON (CNN) -- A state law mandating \"humane treatment\" of downed livestock headed for the slaughterhouse was unanimously overturned Monday by the Supreme Court.";
        String[] strArr = s.split("\\.|\\s");
        System.out.println(strArr.length);
        
    }

    private static void testSplit(){
        String s = ",abc, def , ehi j ,,";
        String[] split = s.split(",");
        for(int i=0;i<split.length;i++){
            System.out.println("["+split[i]+"]");
        }
    }
    
    private static void testRemovePackageStr(){
        String fullClassName = "net.wgen.threetwelve.outcomes.ui.web.service.internal.RPTServiceController2Test";
        System.out.println(fullClassName.replaceAll(".*?\\.", ""));
    }
    
    private static void test1(){
        //String featureSettingStr = "setLearningMapEnabled";
        String testStr = "setEnabledabcEnabled";
        String resultStr = testStr.replaceAll("^set|Enabled$", "");
        System.out.println(resultStr);
    }
}
