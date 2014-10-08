package generativemodel;

import java.util.ArrayList;
import java.util.List;

public class LogSumExp {
	
	
	public static void main(String [] args){
		
		List <Double> terms = new ArrayList<Double>();
		terms.add(Double.NEGATIVE_INFINITY);
		System.out.println(logSumOfExponentials(terms));
		
		System.out.println((Double.NEGATIVE_INFINITY - -435979.0) + "");
		
		/*
		double [] pTerms = new double[]{0.3, 0.2, 0.7};
		double [] lTerms = new double[pTerms.length];
		double psum = 0.;
		for(int i = 0; i < pTerms.length; i++){
			psum += pTerms[i];
			lTerms[i] = Math.log(pTerms[i]);
		}
		
		System.out.println(Math.log(psum) + " " + logSumOfExponentials(lTerms));
		*/
	}
	
	
	public static double logSumOfExponentials(double [] exponentialTerms){
		
		if(exponentialTerms.length == 0){
			return Double.NEGATIVE_INFINITY;
		}
		
		double maxTerm = Double.NEGATIVE_INFINITY;
		for(double d : exponentialTerms){
			if(d > maxTerm){
				maxTerm = d;
			}
		}
		
		if(maxTerm == Double.NEGATIVE_INFINITY){
			return Double.NEGATIVE_INFINITY;
		}
		
		double sum = 0.;
		for(double d : exponentialTerms){
			sum += Math.exp(d - maxTerm);
		}
		
		return maxTerm + Math.log(sum);
	}
	
	
	
	
	public static double logSumOfExponentials(List<Double> exponentialTerms){
		
		if(exponentialTerms.size() == 0){
			return Double.NEGATIVE_INFINITY;
		}
		
		double maxTerm = Double.NEGATIVE_INFINITY;
		for(double d : exponentialTerms){
			if(d > maxTerm){
				maxTerm = d;
			}
		}
		
		if(maxTerm == Double.NEGATIVE_INFINITY){
			return Double.NEGATIVE_INFINITY;
		}
		
		double sum = 0.;
		for(double d : exponentialTerms){
			sum += Math.exp(d - maxTerm);
		}
		
		return maxTerm + Math.log(sum);
	}
	
	
}
