package com.iwxlh.pta.gis;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;

import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.MapView;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Line;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleLineSymbol;
import com.iwxlh.pta.gis.GraphicsAddMaster.GraphicsAddListener;
import com.iwxlh.pta.gis.GraphicsAddMaster.PtaGraphicsHelper;
import com.iwxlh.pta.gis.GraphicsSelectMaster.GraphicsSelectHolder;
import com.iwxlh.pta.gis.GraphicsSelectMaster.GraphicsSelectListener;
import com.iwxlh.pta.misc.FileHolder;
import com.iwxlh.pta.misc.PropertiesHolder;
import com.wxlh.pta.lib.debug.PtaDebug;
import com.wxlh.pta.lib.misc.StringUtils;

/**
 * GIS相关持有工具类
 * 
 * @author JiangXusheng
 * @date 2013-7-6 下午1:16:07
 */
public class GisHolder {
	public static final int VALIDATE_X_Y = 999;

	public static final double AX = 0.83;
	public static final double BX = 1 - AX;

	static final String TAG = GisHolder.class.getName();

	public enum MarkerSymbolType {
		TEXT, IMAGE, SIMPLE, IMAGE_TEXT, SIMPLE_TEXT, SIMPLE_LINE;
	}

	private static final double EARTH_RADIUS = 6378137.0;

	/** 计算两点间的距离 */
	public static double gps2m(double lat_a, double lng_a, double lat_b, double lng_b) {
		double radLat1 = (lat_a * Math.PI / 180.0);
		double radLat2 = (lat_b * Math.PI / 180.0);
		double a = radLat1 - radLat2;
		double b = (lng_a - lng_b) * Math.PI / 180.0;
		double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2) + Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(b / 2), 2)));
		s = s * EARTH_RADIUS;
		s = Math.round(s * 10000) / 10000;
		return s;
	}

	// 计算方位角pab。
	/** 计算方位角 */
	public static double gps2d(double lat_a, double lng_a, double lat_b, double lng_b) {
		double d = 0;
		lat_a = lat_a * Math.PI / 180;
		lng_a = lng_a * Math.PI / 180;
		lat_b = lat_b * Math.PI / 180;
		lng_b = lng_b * Math.PI / 180;

		d = Math.sin(lat_a) * Math.sin(lat_b) + Math.cos(lat_a) * Math.cos(lat_b) * Math.cos(lng_b - lng_a);
		d = Math.sqrt(1 - d * d);
		d = Math.cos(lat_b) * Math.sin(lng_b - lng_a) / d;
		d = Math.asin(d) * 180 / Math.PI;

		// d = Math.round(d*10000);
		return d;
	}

	public static double calulateXYAnagle(double startx, double starty, double endx, double endy) {
		double tan = Math.atan(Math.abs((endy - starty) / (endx - startx))) * 180 / Math.PI;
		if (endx > startx && endy > starty)// 第一象限
		{
			return -tan;
		} else if (endx > startx && endy < starty)// 第二象限
		{
			return tan;
		} else if (endx < startx && endy > starty)// 第三象限
		{
			return tan - 180;
		} else {
			return 180 - tan;
		}

	}

	public static class AngleHolder {

		static final double AX = 0.65;
		static final double BX = 1 - AX;

		public com.iwxlh.pta.Protocol.Navigation.Point lastPoint = new com.iwxlh.pta.Protocol.Navigation.Point();

		public float getAngle(float from, com.iwxlh.pta.Protocol.Navigation.Point point) {

			// x0' = x0,
			// y1' = y1;

			// x1' = k * x0' + (1- k) * x1,
			// y1' = k * x1' + (1 - k) * x1;
			double xPieN_1 = lastPoint.getX();
			double yPieN_1 = lastPoint.getY();

			double xPieN = getPieValue(point.x, xPieN_1);
			double yPieN = getPieValue(point.y, yPieN_1);

			lastPoint.setX(xPieN);
			lastPoint.setY(yPieN);

			double angle = 0;
			double threshold = 0.000001;
			double deltaX = xPieN - xPieN_1;
			double deltaY = yPieN - yPieN_1;

			if (deltaX > threshold) {
				double tan = 1.2062 * (deltaY) / (deltaX);
				angle = Math.atan(tan) / Math.PI * 180;
			} else if (deltaX < -threshold) {
				double tan = 1.2062 * (deltaY) / (deltaX);
				angle = 180 + (Math.atan(tan) / Math.PI * 180);
			} else {
				if (deltaY > threshold) {
					angle = 90;
				} else if (deltaY < -threshold) {
					angle = -90;
				} else {
					return from;
				}
			}

			angle = 90 - angle;

			return (float) angle;
		}

		private double getPieValue(double dN, double dPieN_1) {
			return AX * dN + BX * dPieN_1;
		}

	}

	public static float ANC_NO = 20000;

	public static float getAngle(com.iwxlh.pta.Protocol.Navigation.Point point, com.iwxlh.pta.Protocol.Navigation.Point lastPoint) {

		// x0' = x0,
		// y1' = y1;

		// x1' = k * x0' + (1- k) * x1,
		// y1' = k * x1' + (1 - k) * x1;
		double xPieN_1 = lastPoint.getX();
		double yPieN_1 = lastPoint.getY();

		double xPieN = getPieValue(point.x, xPieN_1);
		double yPieN = getPieValue(point.y, yPieN_1);

		double angle = 0;
		double threshold = 0.000001;
		double deltaX = xPieN - xPieN_1;
		double deltaY = yPieN - yPieN_1;

		if (deltaX > threshold) {
			double tan = 1.2062 * (deltaY) / (deltaX);
			angle = Math.atan(tan) / Math.PI * 180;
		} else if (deltaX < -threshold) {
			double tan = 1.2062 * (deltaY) / (deltaX);
			angle = 180 + (Math.atan(tan) / Math.PI * 180);
		} else {
			if (deltaY > threshold) {
				angle = 90;
			} else if (deltaY < -threshold) {
				angle = -90;
			} else {
				return ANC_NO;
			}
		}

		angle = 90 - angle;

		return (float) angle;
	}

	private static double getPieValue(double dN, double dPieN_1) {
		return AX * dN + BX * dPieN_1;
	}

	public static double getScreenM(MapView mapView, Point point1, Point point2, boolean toMocha) {

		point1 = GisHolder.checkGeometryPoint(point1);
		point2 = GisHolder.checkGeometryPoint(point2);

		if (toMocha) {
			point1 = GisHolder.lonLat2Mercator(point1);
			point2 = GisHolder.lonLat2Mercator(point2);
		}

		Point screenPoint1 = mapView.toScreenPoint(point1);
		Point screenPoint2 = mapView.toScreenPoint(point2);
		return Math.sqrt((screenPoint1.getX() - screenPoint2.getX()) * (screenPoint1.getX() - screenPoint2.getX()) + (screenPoint1.getY() - screenPoint2.getY())
				* (screenPoint1.getY() - screenPoint2.getY()));
	}

	public static boolean isValidateData(Point point) {
		boolean rst = false;
		if (null != point && !point.isEmpty() && point.getX() != VALIDATE_X_Y && point.getY() != VALIDATE_X_Y && point.getX() != 0.0 && point.getY() != 0.0) {
			rst = true;
		}
		return rst;
	}

	public static boolean isValidateData4Navi(com.iwxlh.pta.Protocol.Navigation.Point point) {
		boolean rst = false;
		if (null != point && point.getX() != VALIDATE_X_Y && point.getY() != VALIDATE_X_Y && point.getX() != 0.0 && point.getY() != 0.0) {
			rst = true;
		}
		return rst;
	}

	public static void zoomIn(MapView mapView) {
		mapView.zoomin();
	}

	public static void zoomOut(MapView mapView) {
		mapView.zoomout();
	}

	public static long double2Long(double x) {
		return (long) (x * 1000000 + 0.5);
	}

	public static double long2Double(long x) {
		return (x / 1000000.0);
	}

	public static Point checkGeometryPoint(Point point) {
		try {
			if (null == point) {
				point = new Point();
			}
			if (point.isEmpty()) {
				point.setX(VALIDATE_X_Y);
				point.setY(VALIDATE_X_Y);
			}
		} catch (Exception e) {
			point.setX(VALIDATE_X_Y);
			point.setY(VALIDATE_X_Y);
		}
		return point;
	}

	/**
	 * 在地图上绘制一个点
	 * 
	 * @param point
	 */
	public static List<Integer> addGraphic(Point pt, GraphicsAddListener ptaGraphicsListener) {

		pt = GisHolder.checkGeometryPoint(pt);

		List<Integer> graphicIds = new ArrayList<Integer>();
		GraphicsLayer layer = ptaGraphicsListener.getGraphicsLayer();
		if (layer != null && layer.isInitialized() && layer.isVisible()) {

			int type = ptaGraphicsListener.getMarkerSymbolType().ordinal();
			PtaGraphicsHelper graphicsHelper = new PtaGraphicsHelper(ptaGraphicsListener);
			if (type == MarkerSymbolType.TEXT.ordinal()) {
				graphicIds.add(layer.addGraphic(graphicsHelper.createTextGraphic(pt, ptaGraphicsListener)));
			} else if (type == MarkerSymbolType.IMAGE.ordinal()) {
				graphicIds.add(layer.addGraphic(graphicsHelper.createPicGraphic(pt, ptaGraphicsListener)));
			} else if (type == MarkerSymbolType.SIMPLE.ordinal()) {
				graphicIds.add(layer.addGraphic(graphicsHelper.createSimpleGraphic(pt, ptaGraphicsListener)));
			} else if (type == MarkerSymbolType.IMAGE_TEXT.ordinal()) {
				graphicIds.add(layer.addGraphic(graphicsHelper.createPicGraphic(pt, ptaGraphicsListener)));
				graphicIds.add(layer.addGraphic(graphicsHelper.createTextGraphic(pt, ptaGraphicsListener)));
			} else if (type == MarkerSymbolType.SIMPLE_TEXT.ordinal()) {
				graphicIds.add(layer.addGraphic(graphicsHelper.createSimpleGraphic(pt, ptaGraphicsListener)));
				graphicIds.add(layer.addGraphic(graphicsHelper.createTextGraphic(pt, ptaGraphicsListener)));
			}
		}
		return graphicIds;
	}

	public static int graphicSimpleGraphic(Point pt, GraphicsAddListener ptaGraphicsListener) {

		pt = GisHolder.checkGeometryPoint(pt);
		int graphicIds = -1;
		GraphicsLayer layer = ptaGraphicsListener.getGraphicsLayer();
		if (layer != null && layer.isInitialized() && layer.isVisible()) {
			PtaGraphicsHelper graphicsHelper = new PtaGraphicsHelper(ptaGraphicsListener);
			graphicIds = layer.addGraphic(graphicsHelper.createSimpleGraphic(pt, ptaGraphicsListener));
		}
		return graphicIds;
	}

	public static int graphicImageGraphic(Point pt, GraphicsAddListener ptaGraphicsListener) {

		pt = GisHolder.checkGeometryPoint(pt);
		int graphicIds = -1;
		GraphicsLayer layer = ptaGraphicsListener.getGraphicsLayer();
		if (layer != null && layer.isInitialized() && layer.isVisible()) {
			PtaGraphicsHelper graphicsHelper = new PtaGraphicsHelper(ptaGraphicsListener);
			graphicIds = layer.addGraphic(graphicsHelper.createPicGraphic(pt, ptaGraphicsListener));
		}
		return graphicIds;
	}

	public static int drawLine(GraphicsAddListener ptaGraphicsListener, SimpleLineSymbol.STYLE style, boolean toMocha) {
		return drawLine(ptaGraphicsListener, style, false, toMocha).drawLineId;
	}

	public static class LineInfo {
		public int drawLineId;
		public List<Point> points;
	}

	public static LineInfo drawLine(GraphicsAddListener ptaGraphicsListener, SimpleLineSymbol.STYLE style, boolean is, boolean toMocha) {

		PtaGraphicsHelper graphicsHelper = new PtaGraphicsHelper(ptaGraphicsListener);
		LineInfo info = new LineInfo();
		int id = -999999;
		List<Point> rstPoints = null;
		if (is) {
			rstPoints = new ArrayList<Point>();
		}
		GraphicsLayer drawLayer = ptaGraphicsListener.getGraphicsLayer();
		if (drawLayer != null && drawLayer.isInitialized() && drawLayer.isVisible()) {
			Polyline polyline = new Polyline();

			Point startPoint = null;
			Point endPoint = null;
			// 绘制完整的线段
			List<Point> points = ptaGraphicsListener.dataPoints();
			if (points.size() >= 2) {// 有两个点才能画出线段来
				for (int i = 1; i < points.size(); i++) {
					startPoint = points.get(i - 1);
					endPoint = points.get(i);

					startPoint = GisHolder.checkGeometryPoint(startPoint);
					endPoint = GisHolder.checkGeometryPoint(endPoint);
					if (is) {
						rstPoints.addAll(getSubPoints(startPoint, endPoint, !toMocha));
					}

					if (toMocha) {
						startPoint = GisHolder.lonLat2Mercator(startPoint);
						endPoint = GisHolder.lonLat2Mercator(endPoint);
					}

					Line line = new Line();
					line.setStart(startPoint);
					line.setEnd(endPoint);
					polyline.addSegment(line, false);
				}
			}
			id = drawLayer.addGraphic(graphicsHelper.createLineGraphic(polyline, ptaGraphicsListener, style));
		}
		info.drawLineId = id;
		info.points = rstPoints;
		return info;
	}

	private static final double DETA = 0.00005;

	public static List<Point> getSubPoints(Point start, Point end, boolean toMocha) {
		List<Point> points = new ArrayList<Point>();

		double top = end.getY() - start.getY();
		double bottom = end.getX() - start.getX();

		double k = 0;
		if (toMocha) {
			points.add(GisHolder.lonLat2Mercator(start));
		} else {
			points.add(start);
		}
		if (bottom == 0) {
			double x = start.getX();
			double y = start.getY();
			while (y <= end.getY() - DETA) {
				y += DETA;
				if (toMocha) {
					points.add(GisHolder.lonLat2Mercator(new Point(x, y)));
				} else {
					points.add(new Point(x, y));
				}
			}
		} else {
			k = top / bottom;
			double x = start.getX();
			double y = start.getY();
			points.add(start);
			while (x <= end.getX() - DETA) {
				y = k * ((x + DETA) - x) + y;
				x += DETA;
				if (toMocha) {
					points.add(GisHolder.lonLat2Mercator(new Point(x, y)));
				} else {
					points.add(new Point(x, y));
				}
			}
		}
		if (toMocha) {
			points.add(GisHolder.lonLat2Mercator(end));
		} else {
			points.add(end);
		}
		return points;
	}

	public static void selectGraphic(float x, float y, GraphicsSelectListener graphicsListener) {
		// 获得图层
		GraphicsLayer layer = graphicsListener.getGraphicsLayer();
		if (layer != null && layer.isInitialized() && layer.isVisible()) {
			Graphic result = null;
			// 检索当前 光标点（手指按压位置）的附近的 graphic对象
			result = new GraphicsSelectHolder(x, y, graphicsListener).getGraphicsFromLayer();
			if (result != null) {
				graphicsListener.callBack(x, y, result);
			} else {
				graphicsListener.error(x, y);
			}
		}
	}

	/**
	 * 把地图转换成一个图片
	 * 
	 * @param mapview
	 * @return
	 */
	public static Bitmap getViewBitmap(MapView mapview, int x, int y) {
		mapview.clearFocus();
		mapview.setPressed(false);
		// 能画缓存就返回false
		boolean willNotCache = mapview.willNotCacheDrawing();
		mapview.setWillNotCacheDrawing(false);
		int color = mapview.getDrawingCacheBackgroundColor();
		mapview.setDrawingCacheBackgroundColor(0);
		if (color != 0) {
			mapview.destroyDrawingCache();
		}
		mapview.buildDrawingCache();
		Bitmap cacheBitmap = null;
		while (cacheBitmap == null) {
			cacheBitmap = mapview.getDrawingMapCache(x, y, mapview.getWidth(), mapview.getHeight());
		}
		Bitmap bitmap = Bitmap.createBitmap(cacheBitmap);
		// Restore the list_item
		mapview.destroyDrawingCache();
		mapview.setWillNotCacheDrawing(willNotCache);
		mapview.setDrawingCacheBackgroundColor(color);
		return bitmap;
	}

	public static Bitmap getViewBitmap(MapView mapview) {
		return getViewBitmap(mapview, 0, 0);
	}

	public static Bitmap getViewBitmap(MapView mapView, boolean crop) {
		return getViewBitmap(mapView, crop, 0);
	}

	public static Bitmap getViewBitmap(MapView mapView, boolean crop, int roundPx) {

		Bitmap bitmap = getViewBitmap(mapView);
		if (crop) {
			bitmap = FileHolder.corp(bitmap, bitmap.getWidth(), bitmap.getHeight() / 3, roundPx);
		}
		return bitmap;
	}

	private static DecimalFormat format = new DecimalFormat("0.0");

	public static String getRouteDes(double time, double km) {
		StringBuilder rst = new StringBuilder("");
		int mode = (int) (time * 60);

		if (mode / 60 > 0) {
			rst.append(mode / 60);
			rst.append("小时");
		}
		if (mode % 60 != 0) {
			rst.append(mode % 60);
			rst.append("分钟");
		}
		if (!StringUtils.isEmpety(rst.toString())) {
			rst.append("/");
		}
		rst.append(format.format(km));
		rst.append("公里");
		return rst.toString();
	}

	public static boolean isCurrrentInHeNanProvince(double x, double y) {
		boolean is = false;
		boolean isX = x > PropertiesHolder.getDoubleValue("he_nan_XMin") && x < PropertiesHolder.getDoubleValue("he_nan_XMax");
		boolean isY = y > PropertiesHolder.getDoubleValue("he_nan_YMin") && y < PropertiesHolder.getDoubleValue("he_nan_YMax");
		if (isX && isY) {
			is = true;
		}
		return is;
	}

	// 经纬度转墨卡托
	/**
	 * SpatialReference.WKID_WGS84(4326) to
	 * SpatialReference.WKID_WGS84_WEB_MERCATOR_AUXILIARY_SPHERE(102100)
	 * 
	 * @param lonLat
	 * @return
	 */
	public static Point lonLat2Mercator(Point lonLat) {

		// double x = lonLat.getX() * 20037508.34 / 180;
		// double y = Math.log(Math.tan((90 + lonLat.getY()) * Math.PI / 360)) /
		// (Math.PI / 180);
		// y = y * 20037508.34 / 180;

		Point point = (Point) GeometryEngine.project(lonLat, SpatialReference.create(SpatialReference.WKID_WGS84), SpatialReference.create(SpatialReference.WKID_WGS84_WEB_MERCATOR_AUXILIARY_SPHERE));

		// PtaDebug.e(TAG, "x:"+x+".."+point.getX()+",y"+y+".."+point.getY());

		return point;
	}

	// 墨卡托转经纬度
	/**
	 * SpatialReference.WKID_WGS84_WEB_MERCATOR_AUXILIARY_SPHERE(102100) to
	 * SpatialReference.WKID_WGS84(4326)
	 * 
	 * @param mercator
	 * @return
	 */
	public static Point mercator2lonLat(Point mercator) {
		// double x = mercator.getX() / 20037508.34 * 180;
		// double y = mercator.getY() / 20037508.34 * 180;
		// y = 180 / Math.PI * (2 * Math.atan(Math.exp(y * Math.PI / 180)) -
		// Math.PI / 2);

		Point point = (Point) GeometryEngine
				.project(mercator, SpatialReference.create(SpatialReference.WKID_WGS84_WEB_MERCATOR_AUXILIARY_SPHERE), SpatialReference.create(SpatialReference.WKID_WGS84));
		return point;
	}

	public static Point[] getPoints(Point center, double radius) {
		Point[] points = new Point[50];
		double sin;
		double cos;
		double x;
		double y;
		for (double i = 0; i < 50; i++) {
			sin = Math.sin(Math.PI * 2 * i / 50);
			cos = Math.cos(Math.PI * 2 * i / 50);
			x = center.getX() + radius * 1.2 * sin;
			y = center.getY() + radius * cos;
			points[(int) i] = new Point(x, y);
		}
		return points;
	}

	public static void getCircle(Point center, double radius, Polygon circle) {
		circle.setEmpty();
		try {
			Point[] points = getPoints(center, radius);
			circle.startPath(GisHolder.lonLat2Mercator(points[0]));
			for (int i = 1; i < points.length; i++) {
				points[i] = GisHolder.checkGeometryPoint(points[i]);
				circle.lineTo(GisHolder.lonLat2Mercator(points[i]));
			}
		} catch (Exception e) {
			PtaDebug.e("", "", e);
		}
	}
}
