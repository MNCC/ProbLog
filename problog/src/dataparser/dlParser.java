package dataparser;

import java.io.BufferedReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Var;
import util.Fact;
import util.Literal;
import util.Rule;

import static java.nio.charset.StandardCharsets.UTF_8;

@CheckReturnValue
public class dlParser {
    
    private final ArrayList<String> dataSentence= new ArrayList<>();
    public final ArrayList<Fact> edb= new ArrayList<>();
    public final ArrayList<Rule> rules= new ArrayList<>();
	public void dataReader(String textName) throws IOException{
		
		try (BufferedReader br = Files.newBufferedReader(Paths.get(System.getProperty("user.dir")+textName), UTF_8)) {
			@Var String line = "";
			while ((line = br.readLine()) != null) {
				dataSentence.add(line);
			}
		}
	}
	public HashMap<String,Fact> buildMap(){
		
		HashMap<String,Fact> res= new HashMap<>();
		for(Fact f:edb){
			if(!res.containsKey(f.eString()))
				res.put(f.eString(), f);
		}
		return res;
	}

	private Literal generateLiteral(String str){
		 
		  int hl=str.indexOf("("), hr=str.indexOf(")");
		  String pre=str.substring(0, hl);
		  String[] val=str.substring(hl+1,hr).split(",");
		  return new Literal(pre,val);
		  
	}

   public void parseData(boolean hasPro){
	   @Var int line=0;
	   for(@Var String s:dataSentence){
		      if(!s.contains("."))
		    	  continue;
		      if(s.contains(":-")){
		    	  try{
		    		  String[] str=s.split(":-");
		    		  String head=str[0].trim();
		    		  String body=str[1].trim();
					  @Var Literal h=generateLiteral(head); // generate a literal from string
					  @Var int last=body.indexOf(".");
					  @Var double pro=1;
		    		  if(hasPro){
		    			  last=body.indexOf(":");
		    			  pro=Double.parseDouble(body.substring(last+1, body.indexOf(".")+2).trim());
		    		  }
		    		  String[] bodys=body.substring(0, last).split(", "); // split body
		    		  Literal[] ls=new Literal[bodys.length];
		    		  for(int i=0;i<bodys.length;i++){
		    			  Literal l=generateLiteral(bodys[i]);
		    			  ls[i]=l;
		    		  }
		    		  Rule r=new Rule(h,ls,pro);
		    		  line++;
		    		  rules.add(r);
		    	  }catch(Exception e){
		    		  System.out.println("this is not a legal syntax rule");
		    		  e.printStackTrace();
		    	  }
		      }
		      else{
		    	  try{
		    		  s=s.trim();
		    		  int left=s.indexOf("(");
		    		  int right=s.indexOf(")");
		    		  int last=s.indexOf(".");
		    		  String pre=s.substring(0, left);
		    		  String[] constants=s.substring(left+1, right).split(",");

		    		  Fact f=new Fact(pre,constants);
		    		  
		    		  if(hasPro){
		    		  int third=s.indexOf(":");
		    		  f.pro=Double.parseDouble(s.substring(third+1, last+2).trim());
		    		  }
		    		  edb.add(f);
		    	  }catch(Exception e){
		    		  System.out.println("this is not a legal fact");
		    	  }
	          }
	    	  
	      }
   }
}
