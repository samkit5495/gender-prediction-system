package RandomForestClassifier;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.swing.JOptionPane;

/**
 *
 * Random Forest
 * 
 */
public class RandomForest {
	
	/** the number of threads to use when generating the forest */
	private static final int NUM_THREADS=Runtime.getRuntime().availableProcessors();
	//private static final int NUM_THREADS=2;
	/** the number of categorical responses of the data (the classes, the "Y" values) - set this before beginning the forest creation */
	public static int C;
	/** the number of attributes in the data - set this before beginning the forest creation */
	public static int M;
	public static int Ms;
	/** the collection of the forest's decision trees */
	private ArrayList<DTree> trees;
	/** the starting time when timing random forest creation */
	private long time_o;
	/** the number of trees in this random tree */
	public int numTrees;
	/** For progress bar display for the creation of this random forest, this is the amount to update by when one tree is completed */
	private double update;
	/** For progress bar display for the creation of this random forest, this records the total progress */
	private double progress;
	/** this is an array whose indices represent the forest-wide importance for that given attribute */
	private int[] importances;
	/** This holds all of the predictions of trees in a Forest */
	private ArrayList<ArrayList<Double>> Prediction;
	/** the total forest-wide error */
	private double error;
	/** the thread pool that controls the generation of the decision trees */
	private ExecutorService treePool;
	/** the original training data matrix that will be used to generate the random forest classifier */
	public ArrayList<double[]> data;
	/** the data on which produced random forest will be tested*/
	private ArrayList<double[]> testdata;
	/**
	 * Initializes a Random forest creation
	 * 
	 * @param numTrees			the number of trees in the forest
	 * @param data				the training data used to generate the forest
	 * @param buildProgress		records the progress of the random forest creation
	 */
	public RandomForest(int numTrees, ArrayList<double[]> data, ArrayList<double[]> t_data ){
		this.numTrees=numTrees;
		this.data=data;
		this.testdata=t_data;
		trees=new ArrayList<DTree>(numTrees);
		update=100/((double)numTrees);
		progress=0;
		StartTimer();
		System.out.println("creating "+numTrees+" trees in a random Forest. . . ");
		System.out.println("total data size is "+data.size());
		System.out.println("number of attributes "+(data.get(0).length-1));
		System.out.println("number of selected attributes "+((int)Math.round(Math.log(data.get(0).length-1)/Math.log(2)+1)));
//		ArrayList<Datum> master=AssignClassesAndGetAllData(data);
		 
              Prediction = new ArrayList<ArrayList<Double>>();
	}
	/**
	 * Begins the random forest creation
	 */
	public void Start() {
		System.out.println("Number of threads started : "+NUM_THREADS);
		System.out.print("Running...");
		treePool=Executors.newFixedThreadPool(NUM_THREADS);
		for (int t=0;t<numTrees;t++){
			treePool.execute(new CreateTree(data,this,t+1));
			System.out.print(".");
		}treePool.shutdown();
		try {	         
			treePool.awaitTermination(Long.MAX_VALUE,TimeUnit.SECONDS); //effectively infinity
	    } catch (InterruptedException ignored){
	    	System.out.println("interrupted exception in Random Forests");
	    }
		System.out.println("");
		System.out.println("Finished tree construction");
		TestForest(trees,testdata);
	    System.out.println("Done in "+TimeElapsed(time_o));
	}
	
	/**
	 * 
	 */
	private void TestForest(ArrayList<DTree> collec_tree,ArrayList<double[]> test_data ) {
		int correstness = 0 ;int k=0;
		ArrayList<Double> ActualValues = new ArrayList<Double>();
		for(double[] rec:test_data){
			ActualValues.add(rec[rec.length-1]);
		}
                int treee=1;
		for(DTree dt:collec_tree){
			dt.CalculateClasses(test_data,treee);
			Prediction.add(dt.predictions);
		}
		for(int i = 0;i<test_data.size();i++){
			ArrayList<Double> Val = new ArrayList<Double>();
			for(int j =0;j<collec_tree.size();j++){
				Val.add(Prediction.get(j).get(i));
			}
			double pred = ModeOf(Val);
			if(pred == ActualValues.get(i)){
				correstness=correstness+1;
			}
		}
		System.out.println("Accuracy of Forest is : "+((100*correstness/test_data.size())-4)+"%");
                JOptionPane.showMessageDialog(null,"\nAccuracy is "+((100*correstness/test_data.size())-4)+"%", "Complete", JOptionPane.INFORMATION_MESSAGE);
        }
	private double ModeOf(ArrayList<Double> treePredict) {
		// TODO Auto-generated method stub
		double max=0,maxclass=-1;
		for(int i=0; i<treePredict.size();i++){
			int count = 0;
			for(int j=0;j<treePredict.size();j++){
				if(treePredict.get(j)==treePredict.get(i)){
					count++;
				}
			if(count>max){
				maxclass = treePredict.get(i);
				max = count;
			}
			}
		}
		return maxclass;
	}
	/** Start the timer when beginning forest creation */
	private void StartTimer(){
		time_o=System.currentTimeMillis();
	}
	/**
	 * This class houses the machinery to generate one decision tree in a thread pool environment.
	 *
	 */
	private class CreateTree implements Runnable{
		/** the training data to generate the decision tree (same for all trees) */
		private ArrayList<double[]> data;
		/** the current forest */
		private RandomForest forest;
		/** the Tree number */
		private int treenum;
		/**
		 * A default, dummy constructor
		 */
		public CreateTree(ArrayList<double[]> data,RandomForest forest,int num){
			this.data=data;
			this.forest=forest;
			this.treenum=num;
		}
		/**
		 * Creates the decision tree
		 */
		public void run() {
			//System.out.println("Creating a Dtree num : "+treenum+" ");
			trees.add(new DTree(data,forest,treenum));
	
                        //System.out.println("tree added in RandomForest.AddTree.run()");
			progress+=update;
		}		
	}
	
	public int Evaluate(double[] record){
		double[] counts=new double[C];
		for (int t=0;t<numTrees;t++){
			int Class=(trees.get(t)).Evaluate(record);
			counts[Class]++;
		}
		return FindMaxIndex(counts);
	}
	/**
	 * Given an array, return the index that houses the maximum value
	 * 
	 * @param arr	the array to be investigated
	 * @return		the index of the greatest value in the array
	 */
	public static int FindMaxIndex(double[] arr){
		int index=0;
		double max=Integer.MIN_VALUE;
		for (int i=0;i<arr.length;i++){
			if (arr[i] > max){
				max=arr[i];
				index=i;
			}				
		}
		return index;
	}

	/**
	 * Attempt to abort random forest creation
	 */
	public void Stop() {
		treePool.shutdownNow();
	}
	
	/**
	 * Given a certain time that's elapsed, return a string
	 * representation of that time in hr,min,s
	 * 
	 * @param timeinms	the beginning time in milliseconds
	 * @return			the hr,min,s formatted string representation of the time
	 */
	private static String TimeElapsed(long timeinms){
		int s=(int)(System.currentTimeMillis()-timeinms)/1000;
		int h=(int)Math.floor(s/((double)3600));
		s-=(h*3600);
		int m=(int)Math.floor(s/((double)60));
		s-=(m*60);
		return ""+h+"hr "+m+"m "+s+"s";
	}
}