package com.iwxlh.pta.gis;

import java.util.ArrayList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import com.iwxlh.pta.Protocol.Navigation.GPSPoint;
import com.iwxlh.pta.Protocol.Navigation.GPSPointHandler;
import com.iwxlh.pta.Protocol.Navigation.IGPSPointView;
import com.iwxlh.pta.app.PtaActivity;
import com.iwxlh.pta.boot.PtaApplication;
import com.wxlh.pta.lib.app.UILogic;
import com.wxlh.pta.lib.debug.PtaDebug;

public interface GpsLocationMaster {

	public interface GpsLocationBroadcastListener {
		public void updateCurrentLocation(double x, double y, double alpha);
	}

	class GpsLocationBroadcastReceiver extends BroadcastReceiver {

		private GpsLocationBroadcastListener gpsLocationBroadcastListener;

		public GpsLocationBroadcastReceiver(GpsLocationBroadcastListener gpsLocationBroadcastListener) {
			super();
			this.gpsLocationBroadcastListener = gpsLocationBroadcastListener;
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			if (null != intent && null != intent.getExtras()) {
				Bundle bundle = intent.getExtras();
				double x = bundle.getDouble("x", 0);
				double y = bundle.getDouble("y", 0);
				this.gpsLocationBroadcastListener.updateCurrentLocation(x, y, 0);
			}
		}

	}

	// -----------------------------------------------------------------------
	class GpsLocationBroadcastViewHolder {

	}

	class GpsLocationBroadCastLogic extends UILogic<PtaActivity, GpsLocationBroadcastViewHolder> {

		private GpsLocationBroadcastReceiver broadcastReceiver;

		public GpsLocationBroadCastLogic(PtaActivity t, GpsLocationBroadcastListener gpsLocationBroadcastListener) {
			super(t, new GpsLocationBroadcastViewHolder());
			broadcastReceiver = new GpsLocationBroadcastReceiver(gpsLocationBroadcastListener);
		}

		public void register() {
			IntentFilter filter = new IntentFilter();
			filter.addAction(IBindGPS.FETCH_GPS_LOACTION_SEND_BRAODCAST_ACTION);// 原始的POINT
			mActivity.registerReceiver(broadcastReceiver, filter);
		}

		public void unregister() {
			mActivity.unregisterReceiver(broadcastReceiver);
		}

	}

	/*------------------------------------------------------------------------------------------------------------------------------
	 * 
	 * 
	 * 
	 *-----------------------------------------------------------------------------------------------------------------------------*/
	/**
	 * 定期的获取位置并上报
	 * 
	 * @protocol GPSPointHandler
	 * @author JiangXusheng
	 * @date 2013-8-7 上午9:50:16
	 */
	class GpsLocationSendLogic {
		final String TAG = GpsLocationSendLogic.class.getName();
		private List<GPSPoint> points = new ArrayList<GPSPoint>();

		private static GpsLocationSendLogic logic = null;

		private GpsLocationSendLogic() {
			super();
		}

		public static GpsLocationSendLogic getInstance() {
			if (null == logic) {
				logic = new GpsLocationSendLogic();
			}
			return logic;
		}

		public void addPoint(GPSPoint gpsPoint) {
			points.add(gpsPoint);
		}

		public void submitGpsLocation(String uid) {
			if (points.size() > 0) {
				List<GPSPoint> sendPoints = new ArrayList<GPSPoint>(points);
				points.clear();
				new GPSPointHandler(new IGPSPointView() {

					@Override
					public void submitGPSPointsSuccess(GPSPoint gpsPoint) {
						/* 如果地图加偏之后 */
						sendBroadCast(gpsPoint);
					}

					@Override
					public void submitGPSPointsFailed(int errorcode) {
						PtaDebug.e(TAG, "提交失败:" + errorcode);
					}
				}).submitGPSLocation(uid, sendPoints);
			}
		}

		private void sendBroadCast(GPSPoint gpsPoint) {
			PtaDebug.d(TAG, "位置匹配完成：" + gpsPoint);
			IBindGPS.SendLocationBroadCast.send(PtaApplication.getApplication(), gpsPoint.getX(), gpsPoint.getY(), false);
		}
	}

}
