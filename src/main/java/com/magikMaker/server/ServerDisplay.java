/*
 * ServerDisplay.java
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.magikMaker.server;

import java.util.Calendar;

/**
 *
 * @author Administrator
 */
public class ServerDisplay implements com.magikMaker.server.ServerDisplayListenner {

	private int min;

	private int count;

	/** Creates a new instance of ServerDisplay */
	public ServerDisplay() {
	}

	@Override
	public void setDisplay(String _displayText) {

		Calendar cDate = Calendar.getInstance();

		int cMin = cDate.get(Calendar.MINUTE);

		if ( !(this.min == cMin) ) {

			this.min = cMin;

			count = 0;

		} 

		// System.out.println((getDateTimeString() + ":> " + _displayText));

	}

	public String getDateTimeString() {

		StringBuffer sb = new StringBuffer();

		Calendar dateTime = Calendar.getInstance();

		int i = dateTime.get(Calendar.DATE);

		if ( i < 10 ) sb.append('0');

		sb.append(i);

		sb.append('/');

		i = dateTime.get(Calendar.MONTH);

		i++;

		if ( i < 10 ) sb.append('0');

		sb.append(i);

		sb.append('/');

		i = dateTime.get(Calendar.YEAR);

		sb.append(i);

		sb.append(' ');

		i = dateTime.get(Calendar.HOUR_OF_DAY);

		if ( i < 10 ) sb.append('0');

		sb.append(i);

		sb.append(':');

		i = dateTime.get(Calendar.MINUTE);

		if ( i < 10 ) sb.append('0');

		sb.append(i);

		sb.append(':');

		i = dateTime.get(Calendar.SECOND);

		if ( i < 10 ) sb.append('0');

		sb.append(i);

		sb.append(':');

		i = dateTime.get(Calendar.MILLISECOND);

		if ( i < 100 ) sb.append('0');
		if ( i < 10 ) sb.append('0');

		sb.append(i);

		sb.append(", ");

		sb.append(count++);

		sb.append(", ");

		return sb.toString();

	}

}
