package com.magikMaker.bamboo.model;

import java.io.Serializable;

public class BuzzerEventHashModel implements Serializable {

	private static final long serialVersionUID = 1L;
	
	public Boolean tempValue;
	public Boolean humudityValue;
	public Boolean waterValue;
	public Boolean buzzerValue;
	public Boolean getTempValue() {
		return tempValue;
	}
	public void setTempValue(Boolean tempValue) {
		this.tempValue = tempValue;
	}
	public Boolean getHumudityValue() {
		return humudityValue;
	}
	public void setHumudityValue(Boolean humudityValue) {
		this.humudityValue = humudityValue;
	}
	public Boolean getWaterValue() {
		return waterValue;
	}
	public void setWaterValue(Boolean waterValue) {
		this.waterValue = waterValue;
	}
	public Boolean getBuzzerValue() {
		return buzzerValue;
	}
	public void setBuzzerValue(Boolean buzzerValue) {
		this.buzzerValue = buzzerValue;
	}
	
}
