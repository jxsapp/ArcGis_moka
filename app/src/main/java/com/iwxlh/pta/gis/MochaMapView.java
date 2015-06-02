package com.iwxlh.pta.gis;

import android.content.Context;
import android.util.AttributeSet;

import com.esri.android.map.MapView;
import com.esri.android.map.event.OnMapEventListener;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.portal.BaseMap;
import com.esri.core.portal.WebMap;

public class MochaMapView extends MapView {

	public MochaMapView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

	}

	public MochaMapView(Context context, AttributeSet attrs) {
		super(context, attrs);

	}

	public MochaMapView(Context context, SpatialReference spatialreference, Envelope extent) {
		super(context, spatialreference, extent);

	}

	public MochaMapView(Context context, String url, String user, String passwd, String bingMapsAppId, OnMapEventListener listener) {
		super(context, url, user, passwd, bingMapsAppId, listener);

	}

	public MochaMapView(Context context, String url, String user, String passwd, String bingMapsAppId) {
		super(context, url, user, passwd, bingMapsAppId);

	}

	public MochaMapView(Context context, String url, String user, String passwd) {
		super(context, url, user, passwd);

	}

	public MochaMapView(Context context, WebMap webmap, BaseMap basemap, String bingMapsAppId, OnMapEventListener listener) {
		super(context, webmap, basemap, bingMapsAppId, listener);

	}

	public MochaMapView(Context context, WebMap webmap, String bingMapsAppId, OnMapEventListener listener) {
		super(context, webmap, bingMapsAppId, listener);

	}

	public MochaMapView(Context context) {
		super(context);

	}

	public Point getGpsCenter() {
		Point point = new Point(GisHolder.VALIDATE_X_Y, GisHolder.VALIDATE_X_Y);
		try {
			point = GisHolder.mercator2lonLat(super.getCenter());
		} catch (Exception e) {
			point = new Point(GisHolder.VALIDATE_X_Y, GisHolder.VALIDATE_X_Y);
		}
		return point;
	}

	public Point getMochaCenter() {
		return super.getCenter();
	}

}
