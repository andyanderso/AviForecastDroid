package com.sebnarware.avalanche;

public interface DataListener {
	void regionAdded(RegionData regionData);
	void forecastUpdated(RegionData regionData);
	void dataFetchDone(Throwable error);
}
