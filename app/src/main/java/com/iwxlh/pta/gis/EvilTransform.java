/**
 * 
 */
package com.iwxlh.pta.gis;

import com.esri.core.geometry.Point;

/**
 * 
 * 加偏算法
 * 
 * @author janson
 */
public class EvilTransform implements IGPSTransform {
	final static double a = 6378245.0;
	final static double ee = 0.00669342162296594323;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.iwxlh.navi.location.Transform.IGPSTransform#ToChinaCoordinate(double,
	 * double)
	 */
	@Override
	public Point toChinaCoordinate(double x, double y) {
		double dy = transferLatitude(x - 105.0, y - 35.0);
		double dx = transferLongitude(x - 105.0, y - 35.0);
		double radx = y / 180.0 * Math.PI;
		double magic = Math.sin(radx);
		magic = 1 - ee * magic * magic;
		double sqrtMagic = Math.sqrt(magic);
		dy = (dy * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * Math.PI);
		dx = (dx * 180.0) / (a / sqrtMagic * Math.cos(radx) * Math.PI);

		Point point = new Point();
		point.setX(x + dx);
		point.setY(y + dy);

		return point;
	}

	private double transferLatitude(double x, double y) {
		double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
		ret += (20.0 * Math.sin(6.0 * x * Math.PI) + 20.0 * Math.sin(2.0 * x * Math.PI)) * 2.0 / 3.0;
		ret += (20.0 * Math.sin(y * Math.PI) + 40.0 * Math.sin(y / 3.0 * Math.PI)) * 2.0 / 3.0;
		ret += (160.0 * Math.sin(y / 12.0 * Math.PI) + 320 * Math.sin(y * Math.PI / 30.0)) * 2.0 / 3.0;

		return ret;
	}

	private double transferLongitude(double x, double y) {
		double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
		ret += (20.0 * Math.sin(6.0 * x * Math.PI) + 20.0 * Math.sin(2.0 * x * Math.PI)) * 2.0 / 3.0;
		ret += (20.0 * Math.sin(x * Math.PI) + 40.0 * Math.sin(x / 3.0 * Math.PI)) * 2.0 / 3.0;
		ret += (150.0 * Math.sin(x / 12.0 * Math.PI) + 300.0 * Math.sin(x / 30.0 * Math.PI)) * 2.0 / 3.0;

		return ret;
	}

}
