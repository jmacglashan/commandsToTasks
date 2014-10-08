package commands.model3.mt;

import java.util.List;

public class TokenedString {

	public static final String NULLTOKEN = "######";
	
	private String [] tokens;
	private int hashCode;
	
	
	public TokenedString(List <String> tokens){
		String [] aTokens = new String [tokens.size()];
		for(int i = 0; i < tokens.size(); i++){
			aTokens[i] = tokens.get(i);
		}
		this.init(aTokens);
	}
	
	public TokenedString(String [] tokens){
		this.init(tokens);
	}
	
	protected void init(String [] tokens){
		this.tokens = tokens;
		StringBuffer buf = new StringBuffer();
		for(String t : tokens){
			buf.append(t);
		}
		this.hashCode = buf.toString().hashCode();
	}
	
	
	public int size(){
		return this.tokens.length;
	}
	
	
	/**
	 * Returns the base-1 index token. If index 0 is queried, it returns the null token string.
	 * @param i which token to return
	 * @return the token at the given base-1 index
	 */
	public String t(int i){
		if(i == 0){
			return NULLTOKEN;
		}
		return this.tokens[i-1];
		
	}
	
	
	@Override
	public String toString(){
		StringBuffer buf = new StringBuffer();
		for(int i = 0; i < this.tokens.length; i++){
			if(i > 0){
				buf.append(" ");
			}
			buf.append(this.tokens[i]);
		}
		
		return buf.toString();
	}
	
	
	@Override
	public boolean equals(Object o){
		if(!(o instanceof TokenedString)){
			return false;
		}
		
		TokenedString to = (TokenedString)o;
		
		if(this.tokens.length != to.tokens.length){
			return false;
		}
		
		for(int i = 0; i < this.tokens.length; i++){
			if(!this.tokens[i].equals(to.tokens[i])){
				return false;
			}
		}
		
		return true;
	}
	
	@Override
	public int hashCode(){
		return this.hashCode;
	}

}
