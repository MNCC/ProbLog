package engine;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Var;
import dataparser.dlParser;
import types.Fact;
import types.Literal;
import types.Rule;

@CheckReturnValue
class RuleTree{
	Fact val=null;
	final ArrayList<RuleTree> child;
	RuleTree par=null;

	public RuleTree(Fact val){
		this.val=val;
		child= new ArrayList<>();
	}

	@Override
	public String toString(){
		return val.toString();
	}
}

@CheckReturnValue
public class AdInference {
	private HashMap<String,Fact> database;
    private final dlParser parser;
    private ArrayList<Rule> rules;
    private boolean hasPro;
    public HashMap<String,ArrayList<Fact>> factMap;
    private ArrayList<RuleTree> trees;
    private ArrayList<Fact> preIDB;
    private ArrayList<Fact> curIDB;
    private HashMap<String,Fact> factCollections;
    public boolean useMax=false;

    public AdInference(String textName,boolean hasPro) throws IOException{
 	   parser=new dlParser();
 	   parser.dataReader(textName);
 	   this.hasPro=hasPro;
 	   parser.parseData(this.hasPro);
 	   database=parser.buildMap();
 	   rules=parser.rules; 
 	   
 	   trees= new ArrayList<>();
 	   preIDB= new ArrayList<>();
 	   
 	   curIDB= new ArrayList<>();
 	   factCollections= new HashMap<>();
 	   init();
    }
    private void init(){
 	   factMap= new HashMap<>();
 	   for(Map.Entry<String,Fact> entry:database.entrySet()){
 		   if(!factMap.containsKey(entry.getValue().predicate)){
 			   ArrayList<Fact> f= new ArrayList<>();
 			   f.add(entry.getValue());
 			   factMap.put(entry.getValue().predicate, f);
 		   }
 		   else
 			   factMap.get(entry.getValue().predicate).add(entry.getValue());
 	   }
 	   for(Fact f:parser.edb){
 		   String index=f.eString()+":";
 		   Fact temp=new Fact(f.predicate,f.constants);
 		   if(hasPro)
 			   temp.probability =f.probability;
 		   if(!factCollections.containsKey(index))
 			   factCollections.put(index, temp);
 	   }
    }

    private void getOnePath(@Var RuleTree a, ArrayList<Fact> fs){
 	      while(!a.val.predicate.contains(":-")){
 	    	  fs.add(a.val);
 	    	  a=a.par;
 	      }
 	      
    }
    private void dfsTree(int depth, RuleTree a, ArrayList<Fact> fs, Rule r){
 	      if(depth==r.bodys.length)
 	      {
 	    	 
 	    	  getOnePath(a,fs);
 	      }
 	      else{
 	    	  
 	    	  for(int i=0;i<a.child.size();i++){
 	    		  dfsTree(depth+1,a.child.get(i),fs,r);
 	    	  }
 	      }
    }
    private void semiDfs(int depth, RuleTree a, ArrayList<Fact> fs, Fact newFact, Rule r){
    	 if(depth<r.bodys.length)
	      {

	    		  if(a.val.eString().equals(newFact.eString())){
	    			  dfsTree(depth+1,a,fs,r);
	    		  }
	    		     
	    		  else{
	    			  for(int i=0;i<a.child.size();i++){
	    			    semiDfs(depth+1,a.child.get(i),fs,newFact,r);
	    			    
	    			}
	    	  }
	      }
	      
    }

    private ArrayList<ArrayList<Fact>> inferFacts(ArrayList<ArrayList<Fact>> collection, Rule r){
 	   ArrayList<ArrayList<Fact>> res= new ArrayList<>();
		for (ArrayList<Fact> facts : collection)
			if (!facts.isEmpty())
				res.add(inferTheFact(r, facts));
 	      return res;
    }
    private Fact infer(Rule r, ArrayList<Fact> fs){
 	   HashMap<String,String> model= new HashMap<>();
	       for(int k=0;k<r.bodys.length;k++){
	    	  Literal l=r.bodys[k];
	    	  
	    	  for(int i=0;i<l.variables.length;i++){
	    		  if(!model.containsKey(l.variables[i]))
	    			  model.put(l.variables[i], fs.get(k).constants[i]);
	    	  }
	    	 
	      }
	      Literal tHead=r.head;
	      String pre=tHead.predicate;
	      String[] cons=new String[tHead.variables.length];
	      for(int i=0;i<tHead.variables.length;i++){
	    	  if(model.containsKey(tHead.variables[i]))
	    	     cons[i]=model.get(tHead.variables[i]);
	    	  else{
	    		  cons[i]=model.get(r.bodys[r.bodys.length-1].variables[i]);
	    	  }
	      }
	      Fact f=new Fact(pre,cons);
	      if(hasPro){
			  @Var double p=getMin(fs);
	    	  if(useMax)
	    		  p=getProduction(fs);
	    	  f.probability =p*r.probability;
	      }
	      updateCollection(f,fs);
	      return f;
    }

    @CanIgnoreReturnValue
	private boolean updateCollection(Fact f, ArrayList<Fact> fs){
    	   @Var boolean isUpdate=false;
    	   StringBuilder sb=new StringBuilder();
    	   Fact temp=new Fact(f.predicate,f.constants);
    	   if(hasPro)
    		   temp.probability =f.probability;
    	   sb.append(f.eString());
    	   sb.append(":");
    	   for(Fact e:fs){
    		   sb.append(e.eString());
    		   
    	   }
    	   String index=sb.toString();
    	   if(!factCollections.containsKey(index)){
    		   factCollections.put(index, temp);
    		   isUpdate=true;
    	   }
    	   else{
    		   if(hasPro){
    			   if(factCollections.get(index).probability <f.probability){
    				   factCollections.get(index).probability =f.probability;
    				   isUpdate=true;
    			   }
    		   }
    	   }
    	   return isUpdate;
    }
    private ArrayList<Fact> inferTheFact(Rule r, ArrayList<Fact> fs){
 	      ArrayList<Fact> res= new ArrayList<>();
		  @Var int count=fs.size()-1;
 	      while(count>=0){
 	    	  ArrayList<Fact> temp= new ArrayList<>();
 	    	  for(int i=0;i<r.bodys.length;i++){
 	    		  temp.add(fs.get(count-i));
 	    	  }
 	    	  res.add(infer(r,temp));
 	    	  count=count-r.bodys.length;
 	      }
 	      return res;
    }
    private ArrayList<ArrayList<Fact>> semiTree(Fact curFact, Rule r){
    	  
    	
    	  
    		   
    		   ArrayList<ArrayList<Fact>> res= new ArrayList<>();
    		   @Var RuleTree root=null;
    			   for(RuleTree rt:trees){
    				   if(rt.val.predicate.equals(r.toString())){
    					   root=rt;
    					   break;
    			   }
    			   }
    			   int max=r.bodys.length-1;
    			   if(curFact.predicate.equals(r.bodys[0].predicate)){
		    		   if(hasPro){
		    			   if(!database.containsKey(curFact.eString()))
		    			   doUpdate(0,curFact,root,r);
		    		   }
    				       
		    		   else
		    			   doUpdate(0,curFact,root,r);
    			   }
    			   for(RuleTree tc:root.child){

    				   ArrayList<Fact> temp= new ArrayList<>();
    				   if(hasPro){
    				      if(!database.containsKey(curFact.eString()))
    				          updateTree(1,curFact,tc,max,r);
    				   }
    				   else{
    					   updateTree(1,curFact,tc,max,r);
    				   }
    				   semiDfs(0,tc,temp,curFact,r);
    				   
    				   res.add(temp);
    			   }
    			   
    			   
    		  
    		   
    		   
    	
    	   return inferFacts(res,r);
    }
    private ArrayList<ArrayList<Fact>> Tree(Rule r){
 	   String[] s=new String[1];
 	   s[0]="root";
 	   Fact f=new Fact(r.toString(),s);
 	   HashMap<String,String> model= new HashMap<>();
 	   RuleTree a=new RuleTree(f);
 	   buildTree(0,r,model,a);
 	   trees.add(a);
 	   ArrayList<ArrayList<Fact>> collection= new ArrayList<>();
 	   for(int i=0;i<a.child.size();i++){
 		   ArrayList<Fact> temp= new ArrayList<>();
 		   dfsTree(1,a.child.get(i),temp,r);
 		   
 		   collection.add(temp);
 	   }
 	   return collection;
    }
    private void doUpdate(int depth, Fact newFact, RuleTree node, Rule r){
    	   
    	   RuleTree rt=new RuleTree(newFact);
    	   
    	   node.child.add(rt);
    	   rt.par=node;
    	   if(depth<r.bodys.length-1){
    		   Literal curGoal=r.bodys[depth];
    		   HashMap<String,String> curModel= new HashMap<>();
    		   for(int i=0;i<newFact.constants.length;i++){
	    			  if(!curModel.containsKey(curGoal.variables[i].trim()))
	    				  curModel.put(curGoal.variables[i].trim(), newFact.constants[i].trim());
	    		  }
    		   buildTree(depth+1,r,curModel,rt);
    		   
    	   }
    }
    private void updateTree(int depth, Fact newFact, RuleTree node, int max, Rule r){
    	   
    	   if(depth<=max){
    	   

    			   Literal curGoal=r.bodys[depth];
    			if(curGoal.predicate.equals(newFact.predicate)){
    			   HashMap<String,String> model= new HashMap<>();
    			   Literal lastMatch=r.bodys[depth-1];
    			   Fact lastFact=node.val;
    			   for(int i=0;i<lastMatch.variables.length;i++){
    				   if(!model.containsKey(lastMatch.variables[i].trim()))
    					   model.put(lastMatch.variables[i].trim(), lastFact.constants[i].trim());
    			   }
    			   @Var boolean isMatch=true;
    			   for(int i=0;i<curGoal.variables.length;i++){
    				   if(model.containsKey(curGoal.variables[i].trim())&&!model.get(curGoal.variables[i].trim()).equals(newFact.constants[i].trim()))
    				   {
    					   isMatch=false;
    					   break;
    				   }
    			   }
    			   if(isMatch){
    			   doUpdate(depth,newFact,node,r);
    			   
    			   }
    			   else{
    				   for(RuleTree t:node.child)
    				   updateTree(depth+1,newFact,t,max,r);
    			   }
    			   }
    		   
    	  
    	   }
    }
    private void buildTree(int depth, Rule r, HashMap<String, String> model, RuleTree parNode){
 	      if(depth<r.bodys.length){
 	    	  Literal curGoal=r.bodys[depth];

     	      if(factMap.containsKey(curGoal.predicate)){
     	    	  
     	    	  
     	      
     	      for(Fact f:factMap.get(curGoal.predicate)){
				  @Var boolean canMatch=true;

     	    	  for(int i=0;i<f.constants.length;i++){
     	    		  
     	    		  if(model.containsKey(curGoal.variables[i].trim())){
     	    			  if(!model.get(curGoal.variables[i].trim()).equals(f.constants[i].trim())){
     	    				  canMatch=false;
     	    				  break;
     	    			  }
     	    		  }
	    		  
     	    	  }
     	    	  
     	    	  if(canMatch){
     	    		  HashMap<String,String> curModel= new HashMap<>();
     	    		  for(int i=0;i<f.constants.length;i++){
     	    			  if(!curModel.containsKey(curGoal.variables[i].trim()))
     	    				  curModel.put(curGoal.variables[i].trim(), f.constants[i].trim());
     	    		  }
     	    		  RuleTree node=new RuleTree(f);
 	    			  parNode.child.add(node);
 	    			  node.par=parNode;
 	    			  buildTree( depth+1,r,curModel,node);
     	    		  }
     	      }
 	      }
 	      }
    }

	   private double getMin(ArrayList<Fact> f){
    	      @Var double min=1;
		   for (Fact fact : f) {
			   if (fact.probability < min)
				   min = fact.probability;
		   }
		      return min;
		      
	   }
	   private double getProduction(ArrayList<Fact> f){
    		  @Var double res=1;
		   for (Fact fact : f) {
			   res = res * fact.probability;
		   }
		      return res;
	   }
	   private Fact combineFacts(ArrayList<Fact> fs){
		      if(fs.size()==1||!hasPro)
		    	  return fs.get(0);
		      else{
				  @Var double sum=0;
		    	  for(Fact f:fs){
		    		  sum=calPro(sum,f.probability);
		    	  }
		    	  Fact res= new Fact(fs.get(0).predicate,fs.get(0).constants);
		    	  res.probability =sum;
		    	  return res;
		      }
	   }
	   private ArrayList<Fact> dealIDB(ArrayList<Fact> idb){
		      HashMap<String,ArrayList<Fact>> map= new HashMap<>();
		      for(Fact f:idb){
		    	  if(!map.containsKey(f.eString())){
		    		  ArrayList<Fact> fs= new ArrayList<>();
		    		  map.put(f.eString(), fs);
		    	  }
		    	  map.get(f.eString()).add(f);
		      }
		      ArrayList<Fact> res= new ArrayList<>();
		      for(Map.Entry<String,ArrayList<Fact>> entry:map.entrySet()){
		    	  res.add(combineFacts(entry.getValue()));
		      }
		      return res;
		      
		      
	   }
	   private void trim(Fact f){
		   for(Rule r:rules){
		   	  @Var RuleTree root=null;
 	    	  for(RuleTree rt:trees){
 	    		  if(rt.val.predicate.equals(r.toString())){
 	    			  root=rt;
 	    			  break;
 	    		  }
 	    	  }
 	    	  if(r.bodys[0].predicate.equals(f.predicate))
 	    		 doUpdate(0,f,root,r);
 	    	  
 	    	 
 	    	 for(RuleTree child:root.child)
 	    	 updateTree(1,f,child,r.bodys.length-1,r);
 	    	 }
 	     
	   }
	   private ArrayList<Fact> dupFactRemove(@Var ArrayList<Fact> idb){
		   HashMap<String,Fact> fs= new HashMap<>();
		   ArrayList<Fact> res= new ArrayList<>();
		   idb=dealIDB(idb);
		   @Var ArrayList<Fact> pfs= new ArrayList<>();
		   for(Map.Entry<String, Fact> entry:factCollections.entrySet()){
		    	  
		    	  Fact f=new Fact(entry.getValue().predicate,entry.getValue().constants);
		    	  if(hasPro)
		    		  f.probability =entry.getValue().probability;
		    	  pfs.add(f);
		      }
		   pfs=dealIDB(pfs);
		   for(Fact f:pfs){
			   if(!fs.containsKey(f.eString()))
				   fs.put(f.eString(), f);
		   }
		   for(Fact f:idb){
			   if(fs.containsKey(f.eString()))
				   res.add(fs.get(f.eString()));
		   }
		  return res; 
	   }
	   private boolean isUpdate(ArrayList<Fact> idb){
		   		@Var boolean res=false;
		      ArrayList<Fact> newF= new ArrayList<>();
		      for(Fact f:idb){
		    	  if(!database.containsKey(f.eString())){

		    		  database.put(f.eString(), f);

		    		  res=true;
		    		  newF.add(f);
		    		  
		    	      if(!factMap.containsKey(f.predicate)){
		    		      ArrayList<Fact> temp= new ArrayList<>();
		    		      temp.add(f);
		    		      factMap.put(f.predicate, temp);
		    		  
		    	          }
		    	     else{
		    		      factMap.get(f.predicate).add(f);
		    	          }
		      }
		    	  else{
		    		  if(hasPro){
		    			  if(database.get(f.eString()).probability !=f.probability){
		    					  database.get(f.eString()).probability =f.probability;
		    					  res=true;
		    				  if(res){
		    				  for(Fact fact:factMap.get(f.predicate)){
		    					   if(fact.eString().equals(f.eString())){
		    						   fact.probability =database.get(f.eString()).probability;
		    						   break;
		    					   }
		    				  }
		    				 
		    			  }
		    			  }
		    			 
		    		  }
		    	  }
		      }
		      if(hasPro){
    		      for(Fact f:newF)
    			    trim(f);
    		  }
		      return res;
		      
	   }
	   private double calPro(double p1, double p2){
		   BigDecimal x1=BigDecimal.valueOf(p1);
		   BigDecimal x2=BigDecimal.valueOf(p2);
		   if(useMax)
			   return Math.max(x1.doubleValue(), x2.doubleValue());
		   else
		       return x1.add(x2).subtract(x1.multiply(x2)).doubleValue();
	   }

	   @CanIgnoreReturnValue
	   private boolean semi_update(){
    	      @Var boolean isChange=false;
		      ArrayList<Fact> newIDB= new ArrayList<>();
		      HashMap<String,Fact> map= new HashMap<>();
		   	  ArrayList<Fact> temp=(ArrayList<Fact>) dealIDB(curIDB).clone();
		      
		    	  
		    	  
		    	  for(Fact f:preIDB){
		    		  if(!map.containsKey(f.eString()))
		    			  map.put(f.eString(), f);
		    	  }
		    	  for(Fact f:temp){
		    		  if(!map.containsKey(f.eString())){
		    			  newIDB.add(f);
		    			  isChange=true;
		    		  }
		    		  else{
		    			  if(hasPro){
		    				  if(map.get(f.eString()).probability !=f.probability){
		    					  newIDB.add(f);
		    					  isChange=true;
		    				  }
		    					  
		    			  }
		    		  }
		    	  }
		    	  

		      preIDB=(ArrayList<Fact>) curIDB.clone();
		      curIDB=(ArrayList<Fact>) dupFactRemove(newIDB).clone();
		     
		      return isChange;
              
	   }
	   public void semi_naive(){

    		  @Var boolean isupdate=true;
		      System.out.println("edb: "+factMap);
	    	  System.out.println("rules is "+rules);
	    	  @Var int count=1;
		      while(isupdate){
		    	  isupdate=false;
				  @Var ArrayList<Fact> idb= new ArrayList<>();

		    	  if(count==1){
		    	  for(Rule r:rules){
					  ArrayList<ArrayList<Fact>> temp=inferFacts(Tree(r),r);
					  for (ArrayList<Fact> facts : temp) {
						  idb.addAll(facts);
					  }
		    	  }
		    	  }
		    	  else{
		    		  ArrayList<ArrayList<ArrayList<Fact>>> fs= new ArrayList<>();
		    		  for(Fact f:curIDB){
		    			  for(Rule r:rules){
							  @Var ArrayList<ArrayList<Fact>> temp= new ArrayList<>();
		    				  temp=semiTree(f,r);
		    				  fs.add(temp);
		    			  }
		    		  }
					  for (ArrayList<ArrayList<Fact>> temp1 : fs) {
						  for (ArrayList<Fact> facts : temp1) {
							  idb.addAll(facts);
						  }
					  }
		    	  }
		    	  idb=dupFactRemove(idb);
		    	  curIDB=(ArrayList<Fact>) idb.clone();
		    	  semi_update();

		    	  isupdate=isUpdate(idb);

		    	  count++;
		      }
		      System.out.println("The iteration time is: "+count);
	   }
	
}
