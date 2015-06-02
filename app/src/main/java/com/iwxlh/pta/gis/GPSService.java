package com.iwxlh.pta.gis;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import com.esri.core.geometry.Point;
import com.iwxlh.pta.app.PtaService;
import com.iwxlh.pta.boot.PtaApplication;
import com.iwxlh.pta.traffic.AlphaEstimator;
import com.iwxlh.pta.traffic.TestOffset;
import com.wxlh.pta.lib.debug.PtaDebug;

/**
 * 标准经纬度获取服务
 * 
 * @author JiangXusheng
 * @date 2013-6-27 下午4:53:16
 */
public class GPSService extends PtaService {

	private static final String TAG = GPSService.class.getName();

	private GPSServiceBinder gpsServiceBinder = new GPSServiceBinder();
	private Location location = null;
	private boolean hasNewResult = false;

	/**
	 * 建立绑定
	 * 
	 * @param intent
	 * @return
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return gpsServiceBinder;
	}

	/**
	 * 解除绑定 (non-Javadoc)
	 * 
	 * @see android.app.Service#onUnbind(Intent)
	 */

	private LocationListener gpslocationListener;

	@Override
	public boolean onUnbind(Intent intent) {
		destroyListener();
		return super.onUnbind(intent);
	}

	private void destroyListener() {
		if (null != overtimeTimer) {
			overtimeTimer.cancel();
		}
		if (null != locationManager && null != gpslocationListener) {
			locationManager.removeUpdates(gpslocationListener);
		}
	}

	public class GPSServiceBinder extends Binder implements IBindGPS {

		@Override
		public boolean hasNewResult() {
			return hasNewResult;
		}

		@Override
		public Location getLocation() {
			hasNewResult = false;
			return location;
		}

	}

	private LocationManager locationManager;
	/** 此定时器的作用是，在时间周期到来之后，断开位置监听器，重新连接一次，保证位置监听器一直处于活跃状态 */
	private Timer overtimeTimer = new Timer(true);

	@SuppressLint("HandlerLeak")
	private Handler overtimeHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == 1) {
				overtimeTimer.cancel();
				destroyListener();
				sendLocation(location);
				startGPSLocation();
			}
		}
	};

	public void onCreate() {
		if (locationManager == null)
			locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		startGPSLocation();
		// test();
	}

	void test() {
		new Thread(new Runnable() {

			@Override
			public void run() {

				List<com.esri.core.geometry.Point> points = new TestOffset().getPoints();
				for (int i = 1000; i < points.size(); i++) {
					try {
						Thread.sleep(3000);
					} catch (Exception e) {

					}
					com.esri.core.geometry.Point point = points.get(i);
					Location location = new Location("gps");
					location.setLatitude(point.getY());
					location.setLongitude(point.getX());
					sendLocation(location);
				}

			}
		}).start();
	}

	private void startGPSLocation() {
		PtaDebug.e(TAG, "startGPSLocation......");

		overtimeTimer = new Timer(true);
		gpslocationListener = new TaskLocationListener();
		/**
		 * @provider the tv_skin_name of the provider with which to b_register
		 * @minTime minimum time interval between location updates, in
		 *          milliseconds
		 * @minDistance minimum distance between location updates, in meters
		 * @listener a LocationListener whose LocationListener.onLocationChanged
		 *           method will be called for each location update
		 */
		// 没分钟更新一次位置。
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, PtaApplication.getGpsFetchFrequency(), PtaApplication.getGpsMinDistance(), gpslocationListener);//
		overtimeTimer.schedule(new TimerTask() {

			@Override
			public void run() {
				overtimeHandler.obtainMessage().sendToTarget();
			}
		}, PtaApplication.getGpsTimout());// N分钟超时一次，重新获取一次
	}

	private class TaskLocationListener implements LocationListener {

		@Override
		public void onLocationChanged(Location location) {
			sendLocation(location);
		}

		@Override
		public void onProviderDisabled(String provider) {
		}

		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}
	}

	/**
	 * @Des 发送广播
	 * @param location
	 */

	private IGPSTransform igpsTransform = new EvilTransform();

	private void sendLocation(Location location) {// 更新位置信息
		this.location = location;
		if (null != location) {
			Point gpsPoint = igpsTransform.toChinaCoordinate(location.getLongitude(), location.getLatitude());
			this.location.setLatitude(gpsPoint.getY());
			this.location.setLongitude(gpsPoint.getX());
		}
		hasNewResult = true;
		sendBroadCast(location);
	}

	private void sendBroadCast(Location location) {
		if (null == location) {
			return;
		}
		AlphaEstimator.getInstance().addPoint(new com.iwxlh.pta.Protocol.Navigation.Point(location.getLongitude(), location.getLatitude()));
		IBindGPS.SendLocationBroadCast.send(this, location.getLongitude(), location.getLatitude(), true);
	}

	// @SuppressWarnings("unused")
	// private void logger() {
	// try {
	// JSONStringer stringer = new JSONStringer();
	// stringer.object();
	// stringer.key("t").value(System.currentTimeMillis());
	// stringer.key("x").value(location.getLongitude());
	// stringer.key("y").value(location.getLatitude());
	// stringer.endObject();
	// PtaDebug.d(TAG, stringer.toString());
	// } catch (Exception exception) {
	// PtaDebug.e(TAG, "logger JsonException....", exception);
	// }
	// }

	@Override
	public void onDestroy() {
		destroyListener();
		super.onDestroy();
	}

}
