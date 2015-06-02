package com.iwxlh.pta.gis;

import com.esri.android.map.GraphicsLayer;
import com.esri.core.geometry.Point;
import com.esri.core.map.Graphic;
import com.wxlh.pta.lib.debug.PtaDebug;

/**
 * 选择一个已经标注好的点，选择是的是前面描绘的点
 * 
 * @author JiangXusheng
 * @date 2013-7-23 上午10:34:44
 */
public interface GraphicsSelectMaster {

	public abstract class GraphicsSelectListener {
		public abstract GraphicsLayer getGraphicsLayer();

		public abstract void callBack(final float x, final float y, Graphic graphic);

		public abstract MochaMapView getMochaMapView();

		public void error(float x, float y) {

		}

	}

	public class GraphicsSelectHolder {

		private String TAG = GraphicsSelectHolder.class.getName();

		private double xScreen, yScreen;

		public GraphicsSelectHolder(double xScreen, double yScreen, GraphicsSelectListener selectGraphicsListener) {
			super();
			this.xScreen = xScreen;
			this.yScreen = yScreen;
			this.selectGraphicsListener = selectGraphicsListener;
		}

		private GraphicsSelectListener selectGraphicsListener;

		public Graphic getGraphicsFromLayer() {
			Graphic result = null;
			try {
				int[] graphiciIDs = selectGraphicsListener.getGraphicsLayer().getGraphicIDs();
				if (null == graphiciIDs) {
					return null;
				}
				double xScreen = this.xScreen;
				double yScreen = this.yScreen;
				for (int i = 0; i < graphiciIDs.length; i++) {
					Graphic graphicVar = selectGraphicsListener.getGraphicsLayer().getGraphic(graphiciIDs[i]);
					if (graphicVar != null) {
						Point pointVar = (Point) graphicVar.getGeometry();
						pointVar = GisHolder.checkGeometryPoint(pointVar);
						pointVar = selectGraphicsListener.getMochaMapView().toScreenPoint(pointVar);
						if (null != pointVar) {
							double pointX = pointVar.getX();
							double pointY = pointVar.getY();
							if (Math.sqrt((xScreen - pointX) * (xScreen - pointX) + (yScreen - pointY) * (yScreen - pointY)) < 50) {
								result = graphicVar;
								break;
							}
						}
					}
				}
			} catch (Exception e) {
				PtaDebug.e(TAG, "Exception", e);
				return null;
			}
			return result;
		}
	}

}
