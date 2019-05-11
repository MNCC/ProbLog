package util;

import com.google.errorprone.annotations.CheckReturnValue;

@CheckReturnValue
public class Rule {

	   public Literal head;
	   public Literal[] bodys;
	   public double pro;

	   public Rule(Literal head,Literal[] bodys, double pro ){
		   this.head=head;
		   this.bodys=bodys;
		   this.pro=pro;
	   }

	   @Override
	   public String toString(){
	   		  StringBuilder sb=new StringBuilder();
		      sb.append(head.toString());
		      sb.append(" :-");
		      for(int i=0;i<bodys.length-1;i++){
		    	  sb.append(bodys[i].toString());
		    	  sb.append(" ,");
		      }
		      sb.append(bodys[bodys.length-1]);
		      if(pro!=1){
		    	  sb.append(" :");
		    	  sb.append(pro);
		      }
		      return sb.toString();
	   }
}
