package com.iwxlh.pta.gis;

import java.util.Locale;

import android.annotation.SuppressLint;

@SuppressLint("DefaultLocale")
public class StaticMapHolder {

	// static final String
	// GOOGLE_MAP_URL="http://maps.googleapis.com/maps/api/staticmap?center=34.77381,113.68997&zoom=16&size=240x120&language=zh&sensor=true";
	static final String GOOGLE_MAP_URL = "http://maps.googleapis.com/maps/api/staticmap?center=%.5f,%.5f&zoom=16&size=%dx%d&language=zh&sensor=true";

	public static final int DEFAULT_HEIHGT = 120;
	public static final int DEFAULT_WIDHT = 240;

	public static String getGoogleMapURL(double x, double y) {
		return getGoogleMapURL(x, y, DEFAULT_WIDHT, DEFAULT_HEIHGT);
	}

	public static String getGoogleMapURL(double x, double y, int width) {
		return String.format(Locale.CHINA, GOOGLE_MAP_URL, y, x, width, DEFAULT_HEIHGT);
	}

	public static String getGoogleMapURL(double x, double y, int width, int height) {
		return String.format(Locale.CHINA, GOOGLE_MAP_URL, y, x, width, height);
	}

}
