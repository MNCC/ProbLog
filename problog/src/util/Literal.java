package util;



public class Literal {
 
	   public String predicate;
	   public String[] variables;
	   
	   public Literal(){
		   
	   }
	   
	   public Literal(String predicate, String[] variables){
		   this.predicate=predicate;
		   this.variables=variables;
	   }
	   
	   		@Override
		   public String toString(){
	   			  StringBuilder sb=new StringBuilder();
			      sb.append(predicate);
			      sb.append("(");
			      for(int i=0;i<variables.length-1;i++){
			    	  sb.append(variables[i]);
			    	  sb.append(",");
			      }
			      sb.append(variables[variables.length-1]);
			      sb.append(")");
			      return sb.toString();
		   }
	  
}
