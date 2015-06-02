package com.iwxlh.pta.gis;

import java.util.concurrent.RejectedExecutionException;

import org.apache.http.Header;
import org.apache.http.HttpResponse;

import android.annotation.SuppressLint;
import android.util.Log;

import com.esri.android.map.TiledServiceLayer;
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.internal.io.handler.ResponseListener;
import com.esri.core.internal.io.handler.a;
import com.esri.core.io.UserCredentials;

public class GoogleMapsTiledServiceLayer extends TiledServiceLayer {
	private static final double xmin = -22041257.773878;
	private static final double ymin = -32673939.6727517;
	private static final double xmax = 22041257.773878;
	private static final double ymax = 20851350.0432886;

	private static final double XMin = 110.35992000000005;
	private static final double YMin = 31.382360000000062;
	private static final double XMax = 116.65036000000009;
	private static final double YMax = 36.36647000000005;

	private static final double[] res = { 156543.03392800014, 78271.516963999937, 39135.758482000092, 19567.879240999919, 9783.9396204999593, 4891.9698102499797, 2445.9849051249898,
			1222.9924525624949, 611.49622628138, 305.748113140558, 152.874056570411, 76.4370282850732, 38.2185141425366, 19.1092570712683, 9.55462853563415, 4.7773142679493699, 2.3886571339746849,
			1.1943285668550503 /* , 0.59716428355981721, 0.29858214164761665 */};

	private static final double[] scale = { 591657527.591555, 295828763.79577702, 147914381.89788899, 73957190.948944002, 36978595.474472001, 18489297.737236001, 9244648.8686180003,
			4622324.4343090001, 2311162.217155, 1155581.108577, 577790.554289, 288895.277144, 144447.638572, 72223.819286, 6111.909643, 18055.954822, 9027.9774109999998, 4513.9887049999998
	/* ,2256.994353, 1128.4971760000001 */};

	private static String URL = "http://maps.iwxlh.com";
	private ResponseListener responseListener;
	private static Point origin = new Point(-20037508.342787, 20037508.342787);

	public GoogleMapsTiledServiceLayer() {
		this(true);
	}

	public GoogleMapsTiledServiceLayer(boolean initLayer) {
		super(URL);
		this.responseListener = new ResponseListener() {
			public boolean onResponseInterception(HttpResponse response) {
				Header[] arrayOfHeader = response.getHeaders("X-VE-Tile-Info");
				if ((arrayOfHeader != null) && (arrayOfHeader.length > 0)) {
					return "no-tile".equalsIgnoreCase(arrayOfHeader[0].getValue());
				}
				return false;
			}
		};
		this.isBingMap = true;

		if (!(initLayer))
			return;
		try {
			getServiceExecutor().submit(new Runnable() {
				public void run() {
					GoogleMapsTiledServiceLayer.this.initLayer();
				}
			});
		} catch (RejectedExecutionException localRejectedExecutionException) {
			Log.e("ArcGIS", "initialization of the layer failed.", localRejectedExecutionException);
		}
	}

	protected void initLayer() {
		if (getID() == 0L) {
			this.nativeHandle = create();
		}
		if (getID() == 0L) {
			changeStatus(OnStatusChangedListener.STATUS.fromInt(-1000));
		} else {
			try {
				setDefaultSpatialReference(SpatialReference.create(102100));

				GisHolder.lonLat2Mercator(new Point(XMin, YMin));
				GisHolder.lonLat2Mercator(new Point(XMax, YMax));

				setFullExtent(new Envelope(xmin, ymin, xmax, ymax));
				// setFullExtent(new Envelope(minMercatorPoint.getX(),
				// minMercatorPoint.getY(), maxMercatorPoint.getX(),
				// maxMercatorPoint.getY()));
				setTileInfo(new TiledServiceLayer.TileInfo(origin, scale, res, scale.length, 96, 256, 256));
				super.initLayer();
			} catch (Exception localException) {
				changeStatus(OnStatusChangedListener.STATUS.fromInt(-1005));
				Log.e("ArcGIS", "Bing map url =" + getUrl(), localException);
			}
		}
	}

	static final String BASE_URL = "http://mt%d.google.cn/vt/lyrs=m@161000000&v=w2.114&hl=zh-CN&gl=cn&x=%d&y=%d&z=%d&s=Galil";
	static final String BASE_URL_2 = "http://mt2.google.cn/vt/v=w2.116&hl=zh-CN&gl=cn&x=%d&y=%d&z=%d&s=G";

	String get(int lev, int col, int row) {
		return "http://mt" + (col % 4) + ".google.cn/vt/lyrs=m@161000000&v=w2.114&hl=zh-CN&gl=cn&" + "x=" + col + "&" + "y=" + row + "&" + "z=" + lev + "&s=Galil"; // 加载Google街道图
	}

	@SuppressLint("DefaultLocale")
	protected byte[] getTile(int lev, int col, int row) throws Exception {
		String url = String.format(BASE_URL, col % 4, col, row, lev); // 加载Google街道图
//		String url2 = String.format(BASE_URL_2, col, row, lev); // 加载Google街道图
//		PtaDebug.d("url", url);
//		PtaDebug.e("url2", url2);
		return a.a(url, null, null, this.responseListener);
	}

	public void refresh() {
		try {
			getServiceExecutor().submit(new Runnable() {
				public void run() {
					if (!(GoogleMapsTiledServiceLayer.this.isInitialized()))
						return;
					try {
						GoogleMapsTiledServiceLayer.this.clearTiles();
					} catch (Exception localException) {
						Log.e("ArcGIS", "Re-initialization of the layer failed.", localException);
					}
				}
			});
		} catch (RejectedExecutionException localRejectedExecutionException) {
			return;
		}
	}

	public void reinitializeLayer(UserCredentials usercredentials) {
	}

	public void reinitializeLayer(String appID) {
		super.reinitializeLayer(null);
	}

}