package engine;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Var;
import dataparser.dlParser;
import util.Fact;
import util.Literal;
import util.Rule;

@CheckReturnValue
class AnwserTree{
	  final Fact curAnwser;
	  AnwserTree par=null;
	  final ArrayList<AnwserTree> child;
	  public AnwserTree(Fact curAnwser){
		  this.curAnwser=curAnwser;
		  child= new ArrayList<>();
		 
	  }

	  @Override
	  public String toString(){
		  return curAnwser.toString();
	  }
}

@CheckReturnValue
public class Inference {
       private HashMap<String,Fact> database;
       private final dlParser parser;
       private ArrayList<Rule> rules;
       private boolean hasPro;
       public HashMap<String,ArrayList<Fact>> factMap;
       public boolean useMax=false;
       public boolean useProduction=false;
       public Inference(String textName,boolean hasPro) throws IOException{
    	   parser=new dlParser();
    	   parser.dataReader(textName);
    	   this.hasPro=hasPro;
    	   parser.parseData(this.hasPro);
    	   database=parser.buildMap();
    	   rules=parser.rules; 
    	   
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
       }

       private void getOnePath(@Var AnwserTree a, ArrayList<Fact> fs){
    	      while(!a.curAnwser.predicate.equals("root")){
    	    	  fs.add(a.curAnwser);
    	    	  a=a.par;
    	      }
    	      
       }
       private void dfsTree(int depth, AnwserTree a, ArrayList<Fact> fs, Rule r){
    	      if(depth==r.bodys.length)
    	      {
    	    	  
    	    	  getOnePath(a,fs);
    	      }
    	      else{
    	    	  for(int i=0;i<a.child.size();i++){
    	    		  //System.out.println("cur node is: "+a+" cur depth is"+depth);
    	    		  dfsTree(depth+1,a.child.get(i),fs,r);
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
 	    	  if(useProduction)
 	    		  p=getProduction(fs);
 	    	  f.pro=p*r.pro;
 	      }
 	      //System.out.println("new fact is generate "+f+" from "+fs);
 	      return f;
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
       private ArrayList<ArrayList<Fact>> Tree(Rule r){
    	   String[] s=new String[1];
    	   s[0]="root";
    	   Fact f=new Fact("root",s);
    	   HashMap<String,String> model= new HashMap<>();
    	   AnwserTree a=new AnwserTree(f);
    	   buildTree(0,r,model,a);
    	   //System.out.println(a.child.size());
    	   //printTree(a);
    	   ArrayList<ArrayList<Fact>> collection= new ArrayList<>();
    	   for(int i=0;i<a.child.size();i++){
    		   ArrayList<Fact> temp= new ArrayList<>();
    		   dfsTree(1,a.child.get(i),temp,r);
    		   
    		   collection.add(temp);
    	   }
    	   //System.out.println("facts collection is: "+collection+" for the rule"+r);
    	   return collection;
       }
       private void buildTree(int depth, Rule r, HashMap<String, String> model, AnwserTree parNode){
    	      if(depth<r.bodys.length){
    	    	  Literal curGoal=r.bodys[depth];
        	      //System.out.println(curGoal+"the depth is"+depth);

//        	      for(String str:model.keySet()){
//        	    	  curModel.put(str, model.get(str));
//        	      }   	
//        	      System.out.println("curGoal is "+curGoal);
//        	      System.out.println("curModel is "+model);
//        	      System.out.println("curNode is: "+parNode);
        	      if(factMap.containsKey(curGoal.predicate)){
        	    	  
        	    	  
        	      
        	      for(Fact f:factMap.get(curGoal.predicate)){
					  @Var boolean canMatch=true;
//        	    	  if(f.toString().equals(lastFact))
//        	    		  continue;
        	    	  for(int i=0;i<f.constants.length;i++){
        	    		  //System.out.println(f.constants[i]);
        	    		  if(model.containsKey(curGoal.variables[i].trim())){
        	    			  if(!model.get(curGoal.variables[i].trim()).equals(f.constants[i].trim())){
        	    				  canMatch=false;
        	    				  break;
        	    			  }
        	    		  }
//        	    		  if(curModel.containsKey(curGoal.variables[i].trim())){
//        	    			  curModel.put(curGoal.variables[i].trim(), f.constants[i].trim());
//        	    			  //System.out.println(curModel);
//        	    		  }
//        	    		  else{
//        	    			  if(!curModel.get(curGoal.variables[i].trim()).equals(f.constants[i].trim())){
//        	    				  canMatch=false;
//        	    				  System.out.println("zhe li bu pi pei?"+curGoal.variables[i].trim()+" and "+f.constants[i].trim());
//        	    				  System.out.println("last fact is: "+lastFact);
//        	    				  System.out.println("current fact is: "+f);
//        	    				  break;
//        	    			  }
//        	    		  }
        	    	  }
        	    	  
        	    	  if(canMatch){
//        	    		  System.out.println("is match!");
//        	    		  System.out.println(lastFact+" match with: "+f);
        	    		  HashMap<String,String> curModel= new HashMap<>();
        	    		  for(int i=0;i<f.constants.length;i++){
        	    			  if(!curModel.containsKey(curGoal.variables[i].trim()))
        	    				  curModel.put(curGoal.variables[i].trim(), f.constants[i].trim());
        	    		  }
//        	    		  System.out.println("curModel is: "+curModel);
        	    		  AnwserTree node=new AnwserTree(f);
    	    			  parNode.child.add(node);
    	    			  node.par=parNode;
    	    			  buildTree( depth+1,r,curModel,node);
        	    		  
        	    		  }
     	    	  
        	      }
    	      }
//        	      else{
//        	    	  System.out.println("can not find: "+curGoal.predicate);
//        	    	  System.out.println("because the database is: "+factMap);
//        	      }
    	      }
       }

	   private double getMin(ArrayList<Fact> f){
       	      @Var double min=1;
		   for (Fact fact : f) {
			   if (fact.pro < min)
				   min = fact.pro;
		   }
		      return min;
		      
	   }
	   private double getProduction(ArrayList<Fact> f){
		      @Var double res=1;
		   for (Fact fact : f) {
			   res = res * fact.pro;
		   }
		      return res;
	   }
	   private Fact combineFacts(ArrayList<Fact> fs){
		      if(fs.size()==1||!hasPro)
		    	  return fs.get(0);
		      else{
				  @Var double sum=0;
		    	  for(Fact f:fs){
		    		  sum=calPro(sum,f.pro);
		    	  }
		    	  Fact res= new Fact(fs.get(0).predicate,fs.get(0).constants);
		    	  res.pro=sum;
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
	   private boolean isUpdate(@Var ArrayList<Fact> idb, int count){
		      @Var boolean res=false;
		      idb=dealIDB(idb);
		      
		      for(Fact f:idb){
		    	  if(!database.containsKey(f.eString())){
		    		  database.put(f.eString(), f);
		    		  res=true;
		    	  
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
		    			  if(database.get(f.eString()).pro!=f.pro){
		    				  if(count==1){
		    					  double p=database.get(f.eString()).pro;
		    					  database.get(f.eString()).pro=calPro(p,f.pro);
		    					  res=true;
		    				  }
		    				  else if(database.get(f.eString()).pro<f.pro){
		    				  database.get(f.eString()).pro=f.pro;
		    				  res=true;
		    				  }
		    				  else
		    					  res=false;
		    				  if(res){
		    				  for(Fact fact:factMap.get(f.predicate)){
		    					   if(fact.eString().equals(f.eString())){
		    						   fact.pro=database.get(f.eString()).pro;
		    						   break;
		    					   }
		    				  }
		    				 
		    			  }
		    			  }
		    			 
		    		  }
		    	  }
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

	   public void naive(){

       	      @Var boolean isupdate=true;
		      System.out.println("edb: "+factMap);
	    	  System.out.println("rules is "+rules);
	    	  @Var int count=1;
		      while(isupdate){
		    	  //System.out.println("curEDB is: "+factMap);
		    	  isupdate=false;
				  @Var ArrayList<Fact> idb= new ArrayList<>();
		    	  
		    	  for(Rule r:rules){
					  @Var ArrayList<ArrayList<Fact>> temp= new ArrayList<>();
		    		  //temp=(ArrayList<Fact>) findAllMatch(r,false).clone();
		    		  temp=inferFacts(Tree(r),r);
		    		  //System.out.println("current infer is: "+temp);

					  for (ArrayList<Fact> facts : temp) {
						  idb.addAll(facts);
					  }
		    	  }
//		    	  System.out.println("idb is: "+idb);
		    	  //System.out.println("---------------------------");
		    	  
		    	  isupdate=isUpdate(idb,count);
		    	  count++;
		      }
		      System.out.println("The iteration time is: "+count);
	   }
	}

