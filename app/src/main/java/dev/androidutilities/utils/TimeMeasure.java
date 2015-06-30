package dev.androidutilities.utils;

import android.support.v4.util.TimeUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;


public class TimeMeasure {
	private long start;
	private long nano;
	private long msec;
	private int actionRes;
	
	public TimeMeasure(int actionRes) {
		this.start = System.nanoTime();
		this.actionRes = actionRes;
	}
	
	public TimeMeasure(int actionRes, List<TimeMeasure> timeMeasures) {
		this.actionRes = actionRes;
		long value = 0;
		int count = 0;
		for (TimeMeasure timeMeasure : timeMeasures) {
			if (timeMeasure.actionRes == actionRes) { 
				value += timeMeasure.getNanoseconds();
				count++;
			}
		}
		this.nano = value / count;
		this.msec = TimeUnit.MILLISECONDS.convert(nano, TimeUnit.NANOSECONDS);
	}
	
	public TimeMeasure end() {
		long stop = System.nanoTime();
		this.nano = stop - start;
		this.msec = TimeUnit.MILLISECONDS.convert(nano, TimeUnit.NANOSECONDS);
		return this;
	}
	
	public int getActionRes() {
		return actionRes;
	}
	
	public String getResult() {
		StringBuilder stringBuilder = new StringBuilder();
		TimeUtils.formatDuration(msec, stringBuilder);
		return stringBuilder.toString();
	}
	
	public long getNanoseconds() {
		return nano;
	}
	
	public long getMsec() {
		return msec;
	}
	
}