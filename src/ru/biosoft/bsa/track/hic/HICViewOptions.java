package ru.biosoft.bsa.track.hic;

import java.util.Set;

import ru.biosoft.bsa.view.SiteViewOptions;

public class HICViewOptions extends SiteViewOptions {
	int imageHeight = 300;//in pixels

	public int getImageHeight() {
		return imageHeight;
	}

	public void setImageHeight(int imageHeight) {
		this.imageHeight = imageHeight;
	}

	String zoom;
	String[] allZooms;

	public String getZoom() {
		return zoom;
	}

	public void setZoom(String zoom) {
		this.zoom = zoom;
	}
	
	public String[] getAllZooms()
	{
		return allZooms;
	}
	
	
	String normalization;
	Set<String> allNormalizations;

	public String getNormalization() {
		return normalization;
	}
	public void setNormalization(String normalization) {
		this.normalization = normalization;
	}
	
	public Set<String> getAllNormalizations()
	{
		return allNormalizations;
	}
	
	
	double maxValue = 30;

	public double getMaxValue() {
		return maxValue;
	}

	public void setMaxValue(double maxValue) {
		this.maxValue = maxValue;
	}
	

	
}
