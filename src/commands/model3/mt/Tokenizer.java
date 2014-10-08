package commands.model3.mt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class Tokenizer {

	List <String> delimiters;
	Set <String> filter;
	boolean isBlacklistFilter = true;
	
	
	public Tokenizer(boolean addSpace){
		this.delimiters = new ArrayList<String>();
		if(addSpace){
			this.delimiters.add(" ");
		}
		this.filter = new HashSet<String>();
	}
	
	public Tokenizer(boolean addSpace, boolean addCommonFilters){
		this.delimiters = new ArrayList<String>();
		if(addSpace){
			this.delimiters.add(" ");
		}
		this.filter = new HashSet<String>();
		if(addCommonFilters){
			this.filter.add(" ");
			this.filter.add(".");
			this.filter.add(",");
			this.filter.add("(");
			this.filter.add(")");
			this.filter.add("/");
			this.filter.add("-");
			this.filter.add(";");
		}
	}
	
	public void setToBlackListFilter(){
		this.isBlacklistFilter = true;
	}
	
	public void setToWhiteListFilter(){
		this.isBlacklistFilter = false;
	}
	
	public void addDelimiter(String d){
		this.delimiters.add(d);
	}
	
	public void addTokenFilter(String token){
		this.filter.add(token);
	}
	
	public TokenedString tokenize(String input){
		
		return new TokenedString(tokenizeHelper(input, 0));
		
	}
	
	protected List <String> tokenizeHelper(String s, int dIndex){
		
		List <String> res = new ArrayList<String>();
		
		String delim = this.delimiters.get(dIndex);
		
		String [] tokens = s.split(delim);
		for(int i = 0; i < tokens.length; i++){
			if(dIndex < this.delimiters.size()-1){
				res.addAll(this.tokenizeHelper(tokens[i], dIndex+1));
			}
			else{
				if((this.isBlacklistFilter && !this.filter.contains(tokens[i])) || (!this.isBlacklistFilter && this.filter.contains(tokens[i]))){
					res.add(tokens[i]);
				}
			}
		}
		
		return res;
		
	}

}
