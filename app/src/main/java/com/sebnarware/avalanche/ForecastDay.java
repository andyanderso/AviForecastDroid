package com.sebnarware.avalanche;

public class ForecastDay {

	private String dateString; 
	private int aviLevel;
	
	public ForecastDay(String dateString, int aviLevel) {
	    this.dateString = dateString;
	    this.aviLevel = aviLevel;
	}

	public String getDateString() {
		return dateString;
	}

	public int getAviLevel() {
		return aviLevel;
	}
	
}
