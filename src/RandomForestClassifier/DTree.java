
package RandomForestClassifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
/**
 *
 * Creates a decision tree based on the specifications of random forest trees
 *
 */
public class DTree {


	/** Instead of checking each index we'll skip every INDEX_SKIP indices unless there's less than MIN_SIZE_TO_CHECK_EACH*/
	private static final int INDEX_SKIP=3;
	/** If there's less than MIN_SIZE_TO_CHECK_EACH points, we'll check each one */
	private static final int MIN_SIZE_TO_CHECK_EACH=10;
	/** If the number of data points is less than MIN_NODE_SIZE, we won't continue splitting, we'll take the majority vote */
	private static final int MIN_NODE_SIZE=5;
	/** the number of data records */
	private int N;
	private int testN;
	/** Of the testN, the number that were correctly identified */
	private int correct;
	private int[] importances;
	/** This is the root of the Decision Tree */
	private TreeNode root;
	/** This is a pointer to the Random Forest this decision tree belongs to */
	private RandomForest forest;
	/** This keeps track of all the predictions done by this tree */
	public ArrayList<Double> predictions;

	/**
	 * This constructs a decision tree from a data matrix.
	 * @param data		The data matrix as a List of int arrays - each array is one record, each index in the array is one attribute, and the last index is the class
	 * 					(ie [ x1, x2, . . ., xM, Y ]).
	 * @param forest	The random forest this decision tree belongs to
	 */
	public DTree(ArrayList<double[]> data,RandomForest forest,int num){
		this.forest=forest;
		N=data.size();
		importances=new int[RandomForest.M];
		predictions = new ArrayList<Double>();
	
		
		ArrayList<double[]> train=new ArrayList<double[]>(N); //data becomes the "bootstrap" - that's all it knows
		ArrayList<double[]> test=new ArrayList<double[]>();
	
                BootStrapSample(data,train,test,num);//populates train and test using data
		testN=test.size();
		correct=0;	
		
		root=CreateTree(train,num);//creating tree using training data
		FlushData(root);
	}
	/**
	 * This method will get the classes and will return the updates
	 * 
	 */
	public ArrayList<Double> CalculateClasses(ArrayList<double[]> test,int nu){
		ArrayList<Double> corest = new ArrayList<Double>();int k=0;int korect = 0;
		for(double[] record : test){
			double kls = Evaluate(record);
			corest.add(kls);
			double k1 = record[record.length-1];
			if (kls==k1)
				korect++;
		}
		predictions= corest;
		return corest;
		
	}
	/**
	 * 
	 * @param record 	the data record to be classified
	 * @return			the class the data record was classified into
	 */
	public int Evaluate(double[] record){//need to write this 
		TreeNode evalNode=root;
		
		while (true){
			if (evalNode.isLeaf)
				return evalNode.Class;
			if (record[evalNode.splitAttributeM] <= evalNode.splitValue)
				evalNode=evalNode.left;
			else
				evalNode=evalNode.right;			
		}
	}
	/**
	 * @param test		The data matrix to be permuted
	 * @param m			The attribute index to be permuted
	 * @return			The data matrix with the mth column randomly permuted
	 */
	/**
	 * This creates the decision tree according to the specifications of random forest trees. 
	 * 
	 * @param train		the training data matrix (a bootstrap sample of the original data)
	 * @return			the TreeNode object that stores information about the parent node of the created tree
	 */
	private TreeNode CreateTree(ArrayList<double[]> train, int ntree){
		TreeNode root=new TreeNode();
		root.data=train;
		//System.out.println("creating ");
		RecursiveSplit(root,ntree);
		return root;
	}
	
	private class TreeNode implements Cloneable{
		public boolean isLeaf;
		public TreeNode left;
		public TreeNode right;
		public int splitAttributeM;
		public Integer Class;
		public List<double[]> data;
		public double splitValue;
		public int generation;
		
		public TreeNode(){
			splitAttributeM=-99;
			splitValue=-99;
			generation=1;
		}
		public TreeNode clone(){ //"data" element always null in clone
			TreeNode copy=new TreeNode();
			copy.isLeaf=isLeaf;
			if (left != null) //otherwise null
				copy.left=left.clone();
			if (right != null) //otherwise null
				copy.right=right.clone();
			copy.splitAttributeM=splitAttributeM;
			copy.Class=Class;
			copy.splitValue=splitValue;
			return copy;
		}
	}
	private class DoubleWrap{
		public double d;
		public DoubleWrap(double d){
			this.d=d;
		}		
	}
	private void RecursiveSplit(TreeNode parent, int Ntreenum){
		
		if (!parent.isLeaf){
			
			//-------------------------------Step A
			Integer Class=CheckIfLeaf(parent.data);
			if (Class != null){
				parent.isLeaf=true;
				parent.Class=Class;
				return;
			}
			
			//-------------------------------Step B
			int Nsub=parent.data.size();
			
			parent.left=new TreeNode();
			parent.left.generation=parent.generation+1;
			parent.right=new TreeNode();
			parent.right.generation=parent.generation+1;
			//System.out.println("Creating left and right nodes for this tree: "+Ntreenum);
			ArrayList<Integer> vars=GetVarsToInclude();//randomly selects Ms.Nos of attributes from M
			
			DoubleWrap lowestE=new DoubleWrap(Double.MAX_VALUE);

			//-------------------------------Step C
			for (int m:vars){
				
				SortAtAttribute(parent.data,m);//sorts on a particular column in the row
				
				ArrayList<Integer> indicesToCheck=new ArrayList<Integer>();
				for (int n=1;n<Nsub;n++){
					double classA=GetClass(parent.data.get(n-1));
					double classB=GetClass(parent.data.get(n));
					if (classA != classB)
						indicesToCheck.add(n);
				}
				
				if (indicesToCheck.size() == 0){//if all the Y-values are same, then get the class directly
					parent.isLeaf=true;
					parent.Class=GetClass(parent.data.get(0));
					continue;
				}
				if (indicesToCheck.size() > MIN_SIZE_TO_CHECK_EACH){
					for (int i=0;i<indicesToCheck.size();i+=INDEX_SKIP){
						CheckPosition(m,indicesToCheck.get(i),Nsub,lowestE,parent,Ntreenum);
						if (lowestE.d == 0)//lowestE now has the minimum conditional entropy so IG is max there
							break;
					}
				}
				else {
					for (int n:indicesToCheck){
						CheckPosition(m,n,Nsub,lowestE,parent,Ntreenum);
						if (lowestE.d == 0)
							break;
					}
				}
				if (lowestE.d == 0)
					break;
			}
			if (parent.left.data.size() == 1){
				parent.left.isLeaf=true;
				parent.left.Class=GetClass(parent.left.data.get(0));							
			}
			else if (parent.left.data.size() < MIN_NODE_SIZE){
				parent.left.isLeaf=true;
				parent.left.Class=GetMajorityClass(parent.left.data);	
			}
			else {
				Class=CheckIfLeaf(parent.left.data);
				if (Class == null){
					parent.left.isLeaf=false;
					parent.left.Class=null;
//					System.out.println("make branch left: m:"+m);
				}
				else {
					parent.left.isLeaf=true;
					parent.left.Class=Class;
				}
			}
				//------------Right Child
			if (parent.right.data.size() == 1){
				parent.right.isLeaf=true;
				parent.right.Class=GetClass(parent.right.data.get(0));								
			}
			else if (parent.right.data.size() < MIN_NODE_SIZE){
				parent.right.isLeaf=true;
				parent.right.Class=GetMajorityClass(parent.right.data);	
			}
			else {
				Class=CheckIfLeaf(parent.right.data);
				if (Class == null){
//					System.out.println("make branch right: m:"+m);
					parent.right.isLeaf=false;
					parent.right.Class=null;
				}
				else {
					parent.right.isLeaf=true;
					parent.right.Class=Class;
				}
			}
			
			if (!parent.left.isLeaf)
				RecursiveSplit(parent.left,Ntreenum);
			if (!parent.right.isLeaf)
				RecursiveSplit(parent.right,Ntreenum);
		}
	}
	/**
	 * Given a data matrix, return the most popular Y value (the class)
	 * @param data	The data matrix
	 * @return		The most popular class
	 */
	private int GetMajorityClass(List<double[]> data){
		int[] counts=new int[RandomForest.C];
		for (double[] record:data){
			int Class=(int)record[record.length-1];//GetClass(record);
			counts[Class-1]++;
		}
		int index=-99;
		int max=Integer.MIN_VALUE;
		for (int i=0;i<counts.length;i++){
			if (counts[i] > max){
				max=counts[i];
				index=i+1;
			}				
		}
		return index;
	}

	/**
	 * @param m				the attribute to split on
	 * @param n				the index to check
	 * @param Nsub			the number of records in the data matrix
	 * @param lowestE		the minimum entropy to date
	 * @param parent		the parent node
	 * @return				the entropy of this split
	 */
	private double CheckPosition(int m,int n,int Nsub,DoubleWrap lowestE,TreeNode parent, int nTre){
		//                       var,	index,	train.size,	lowest number,	for a tree
		if (n < 1) //exit conditions
			return 0;
		if (n > Nsub)
			return 0;
		
		List<double[]> lower=GetLower(parent.data,n);
		List<double[]> upper=GetUpper(parent.data,n);
		if (lower == null)
			System.out.println("lower list null");	
		if (upper == null)
			System.out.println("upper list null");
		double[] pl=GetClassProbs(lower);
		double[] pu=GetClassProbs(upper);
		double eL=CalcEntropy(pl);
		double eU=CalcEntropy(pu);
	
		double e=(eL*lower.size()+eU*upper.size())/((double)Nsub);
		if (e < lowestE.d){			
			lowestE.d=e;
			parent.splitAttributeM=m;
			parent.splitValue=parent.data.get(n)[m];
			parent.left.data=lower;	
			parent.right.data=upper;
		}
		return e;//entropy
	}
	/**
	 * Given a data record, return the Y value - take the last index
	 * 
	 * @param record		the data record
	 * @return				its y value (class)
	 */
	public static int GetClass(double[] record){
		return (int)record[RandomForest.M];
	}
	/**
	 * Given a data matrix, check if all the y values are the same. If so,
	 * return that y value, null if not
	 * 
	 * @param data		the data matrix
	 * @return			the common class (null if not common)
	 */
	private Integer CheckIfLeaf(List<double[]> data){
		boolean isLeaf=true;
		double ClassA=GetClass(data.get(0));
		for (int i=1;i<data.size();i++){			
			double[] recordB=data.get(i);
			if (ClassA != GetClass(recordB)){
				isLeaf=false;
				break;
			}
		}
		if (isLeaf)
			return GetClass(data.get(0));
		else
			return null;
	}
	/**
	 * Split a data matrix and return the upper portion
	 * 
	 * @param data		the data matrix to be split
	 * @param nSplit	return all data records above this index in a sub-data matrix
	 * @return			the upper sub-data matrix
	 */
	private List<double[]> GetUpper(List<double[]> data,int nSplit){
		int N=data.size();
		List<double[]> upper=new ArrayList<double[]>(N-nSplit);
		for (int n=nSplit;n<N;n++)
			upper.add(data.get(n));
		return upper;
	}
	/**
	 * Split a data matrix and return the lower portion
	 * 
	 * @param data		the data matrix to be split
	 * @param nSplit	return all data records below this index in a sub-data matrix
	 * @return			the lower sub-data matrix
	 */
	private List<double[]> GetLower(List<double[]> data,int nSplit){
		List<double[]> lower=new ArrayList<double[]>(nSplit);
		for (int n=0;n<nSplit;n++)
			lower.add(data.get(n));
		return lower;
	}
	/**
	 * This class compares two data records by numerically comparing a specified attribute
	
	 */
	private class AttributeComparator implements Comparator{		
		/** the specified attribute */
		private int m;
		/**
		 * Create a new comparator
		 * @param m			the attribute in which to compare on
		 */
		public AttributeComparator(int m){
			this.m=m;
		}
		/**
		 * Compare the two data records. They must be of type int[].
		 * 
		 * @param o1		data record A
		 * @param o2		data record B
		 * @return			-1 if A[m] < B[m], 1 if A[m] > B[m], 0 if equal
		 */
		public int compare(Object o1, Object o2){
			int a=((int[])o1)[m];
			int b=((int[])o2)[m];
			if (a < b)
				return -1;
			if (a > b)
				return 1;
			else
				return 0;
		}		
	}
	/**
	 * Sorts a data matrix by an attribute from lowest record to highest record
	 * 
	 * @param data			the data matrix to be sorted
	 * @param m				the attribute to sort on
	 */
	@SuppressWarnings("unchecked")
	private void SortAtAttribute(List<double[]> data,int m){
		Collections.sort(data,new AttributeComparator(m));
	}
	/**
	 * Given a data matrix, return a probabilty mass function representing 
	 * the frequencies of a class in the matrix (the y values)
	 * 
	 * @param records		the data matrix to be examined
	 * @return				the probability mass function
	 */
	private double[] GetClassProbs(List<double[]> records){
		
		double N=records.size();
		
		double[] counts=new double[RandomForest.C];
		
		for (double[] record:records)
			counts[GetClass(record)-1]++;

		double[] ps=new double[RandomForest.C];
		for (int c=0;c<RandomForest.C;c++)
			ps[c]=counts[c]/N;
		return ps;
	}
	/** ln(2) */
	private static final double logoftwo=Math.log(2);
	/**
	 * 
	 * @param ps			the probability mass function
	 * @return				the entropy value calculated
	 */
	private double CalcEntropy(double[] ps){
		double e=0;		
		for (double p:ps){
			if (p != 0) //otherwise it will divide by zero - see TSK p159
				e+=p*Math.log(p)/logoftwo;
		}
		return -e; //according to TSK p158
	}
	/**
	 * Of the M attributes, select {@link RandomForest#Ms Ms} at random.
	 * 
	 * @return		The list of the Ms attributes' indices
	 */
	private ArrayList<Integer> GetVarsToInclude() {
		boolean[] whichVarsToInclude=new boolean[RandomForest.M];

		for (int i=0;i<RandomForest.M;i++)
			whichVarsToInclude[i]=false;
		
		while (true){
			int a=(int)Math.floor(Math.random()*RandomForest.M);
			whichVarsToInclude[a]=true;
			int N=0;
			for (int i=0;i<RandomForest.M;i++)
				if (whichVarsToInclude[i])
					N++;
			if (N == RandomForest.Ms)
				break;
		}
		
		ArrayList<Integer> shortRecord=new ArrayList<Integer>(RandomForest.Ms);
		
		for (int i=0;i<RandomForest.M;i++)
			if (whichVarsToInclude[i])
				shortRecord.add(i);
		return shortRecord;
	}

	/**
	 * Create a boostrap sample of a data matrix
	 * 
	 * @param data		the data matrix to be sampled
	 * @param train		the bootstrap sample
	 * @param test		the records that are absent in the bootstrap sample
	 */
	private void BootStrapSample(ArrayList<double[]> data,ArrayList<double[]> train,ArrayList<double[]> test,int numb){
		ArrayList<Integer> indices=new ArrayList<Integer>(N);
		for (int n=0;n<N;n++)
			indices.add((int)Math.floor(Math.random()*N));
		ArrayList<Boolean> in=new ArrayList<Boolean>(N);
		for (int n=0;n<N;n++)
			in.add(false); //have to initialize it first
		for (int num:indices){
			train.add((data.get(num)).clone());
			in.set(num,true);
		}//System.out.println("created training-data for tree : "+numb);
		for (int i=0;i<N;i++)
			if (!in.get(i))
				test.add((data.get(i)).clone());//System.out.println("created testing-data for tree : "+numb);//everywhere its set to false we get those to test data
		
//		System.out.println("bootstrap N:"+N+" size of bootstrap:"+bootstrap.size());
	}
	/**
	 * Recursively deletes all data records from the tree. This is run after the tree
	 * has been computed and can stand alone to classify incoming data.
	 * 
	 * @param node		initially, the root node of the tree
	 */
	private void FlushData(TreeNode node){
		node.data=null;
		if (node.left != null)
			FlushData(node.left);
		if (node.right != null)
			FlushData(node.right);
	}
	
	/**
	 * Get the number of data records in the test data matrix that were classified correctly
	 */
	public int getNumCorrect(){
		return correct;
	}
	/**
	 * Get the number of data records left out of the bootstrap sample
	 */
	public int getTotalNumInTestSet(){
		return testN;
	}
	/**
	 * Get the importance level of attribute m for this tree
	 */
	public int getImportanceLevel(int m){
		return importances[m];
	}
}