package com.iwxlh.pta.gis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.graphics.Color;
import android.graphics.drawable.Drawable;

import com.esri.android.map.GraphicsLayer;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polyline;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.PictureMarkerSymbol;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.symbol.TextSymbol;
import com.iwxlh.pta.gis.GisHolder.MarkerSymbolType;

public interface GraphicsAddMaster {

	public abstract class GraphicsAddListener {

		public abstract GraphicsLayer getGraphicsLayer();

		public Map<String, Object> getDatas() {
			return new HashMap<String, Object>();
		}

		public List<Point> dataPoints() {
			return new ArrayList<Point>();
		}

		public abstract MarkerSymbolType getMarkerSymbolType();

		public String getText() {
			return "";
		}

		public int getColor() {
			return Color.RED;
		}

		public int getSize() {
			return 5;
		}

		public String getStyle() {
			return "";
		}

		public Drawable getDrawable() {
			return null;
		}

		private float angle = 0f;

		public void setAngle(float angle) {
			this.angle = angle;
		}

		public float getAngle() {
			return angle;
		}

		public boolean toMocha() {
			return true;
		}
	}

	public class PtaGraphicsHelper {
		private GraphicsAddListener graphicsListener;

		public PtaGraphicsHelper(GraphicsAddListener graphicsListener) {
			super();
			this.graphicsListener = graphicsListener;
		}

		public Graphic createSimpleGraphic(Point geometry, GraphicsAddListener ptaGraphicsListener) {
			geometry = GisHolder.checkGeometryPoint(geometry);
			if (ptaGraphicsListener.toMocha()) {
				geometry = GisHolder.lonLat2Mercator(geometry);
			}
			SimpleMarkerSymbol.STYLE style = SimpleMarkerSymbol.STYLE.valueOf(ptaGraphicsListener.getStyle());
			SimpleMarkerSymbol symbol = new SimpleMarkerSymbol(ptaGraphicsListener.getColor(), ptaGraphicsListener.getSize(), style);
			Graphic g = new Graphic(geometry, symbol, graphicsListener.getDatas(), null);
			return g;
		}

		public Graphic createTextGraphic(Point geometry, GraphicsAddListener ptaGraphicsListener) {
			geometry = GisHolder.checkGeometryPoint(geometry);
			if (ptaGraphicsListener.toMocha()) {
				geometry = GisHolder.lonLat2Mercator(geometry);
			}
			TextSymbol symbol = new TextSymbol(ptaGraphicsListener.getSize(), ptaGraphicsListener.getText(), ptaGraphicsListener.getColor());
			Graphic g = new Graphic(geometry, symbol, graphicsListener.getDatas(), null);
			return g;
		}

		public Graphic createPicGraphic(Point geometry, GraphicsAddListener ptaGraphicsListener) {
			geometry = GisHolder.checkGeometryPoint(geometry);
			if (ptaGraphicsListener.toMocha()) {
				geometry = GisHolder.lonLat2Mercator(geometry);
			}
			PictureMarkerSymbol symbol = new PictureMarkerSymbol(ptaGraphicsListener.getDrawable());
			symbol.setAngle(ptaGraphicsListener.getAngle());
			symbol.setOffsetX(0);
			symbol.setOffsetY(5);
			Graphic g = new Graphic(geometry, symbol, graphicsListener.getDatas(), null);
			return g;
		}

		/**
		 * @STYLE_DASH 虚线
		 * @STYLE_DASHDOT 单点虚线
		 * @STYLE_DASHDOTDOT 双点虚线
		 * @STYLE_DOT 点画线
		 * @STYLE_NULL 空线
		 * @STYLE_SOLID 实线
		 * 
		 * @param polyline
		 * @return
		 */
		public Graphic createLineGraphic(Polyline polyline, GraphicsAddListener ptaGraphicsListener, SimpleLineSymbol.STYLE style) {
			SimpleLineSymbol lineSymbol = new SimpleLineSymbol(ptaGraphicsListener.getColor(), ptaGraphicsListener.getSize(), style);
			Graphic g = new Graphic(polyline, lineSymbol);
			return g;
		}

	}

}
