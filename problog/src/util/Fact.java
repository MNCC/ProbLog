package util;

import com.google.errorprone.annotations.CheckReturnValue;

@CheckReturnValue
public class Fact {

	   public String predicate;
	   public String[] constants;
	   public double pro=1;

	   public Fact(String predicate, String[] constants){
		   this.predicate=predicate;
		   this.constants=constants;
		   
	   }
	   
	   @Override
	   public String toString(){
		   	  StringBuilder sb=new StringBuilder();
		      sb.append(predicate);
		      sb.append("(");
		      for(int i=0;i<constants.length-1;i++){
		    	  sb.append(constants[i]);
		    	  sb.append(",");
		      }
		      sb.append(constants[constants.length-1]);
		      sb.append(")");
		      if(pro!=2){
		    	  sb.append(" :");
		    	  sb.append(pro);
		      }
		      return sb.toString();
	   }
	  public String eString(){
		  StringBuilder sb=new StringBuilder();
	      sb.append(predicate);
	      sb.append("(");
	      for(int i=0;i<constants.length-1;i++){
	    	  sb.append(constants[i]);
	    	  sb.append(",");
	      }
	      sb.append(constants[constants.length-1]);
	      sb.append(")");
	      return sb.toString();
	  } 
}
