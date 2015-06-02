package com.iwxlh.pta.gis;

import java.util.Timer;
import java.util.TimerTask;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.iwxlh.pta.Protocol.Navigation.GPSPoint;
import com.iwxlh.pta.app.PtaActivity;
import com.wxlh.pta.lib.app.UILogic;

public interface GPSServiceMaster {

	class GPSServiceViewHolder {

	}

	class GPSServiceLogic extends UILogic<PtaActivity, GPSServiceViewHolder> implements GpsLocationMaster {

		static final String TAG = GPSServiceLogic.class.getName();

		static final int UPDATE_GPS_LOCTION_WHAT = 0x1bc;

		private GpsLocationSendLogic gpsLocationSendLogic = GpsLocationSendLogic.getInstance();
		private IBindGPS standardStatecraft;

		private Handler handler = null;

		public GPSServiceLogic(PtaActivity ptaActivity) {
			super(ptaActivity, new GPSServiceViewHolder());
			handler = new Handler(mActivity.getMainLooper(), new Handler.Callback() {
				@Override
				public boolean handleMessage(Message msg) {

					if (msg.what == UPDATE_GPS_LOCTION_WHAT) {

					}
					return false;
				}
			});
		}

		public void bindGspService() {
			mActivity.bindService(new Intent(mActivity, GPSService.class), statecraftConnection, Context.BIND_AUTO_CREATE);
		}

		public void unbindGspService() {
			mActivity.unbindService(statecraftConnection);
		}

		IBindGPS getGps() {
			return this.standardStatecraft;
		}

		private GPSPoint location2GpsPoint(Location location) {
			GPSPoint loc = new GPSPoint();
			if (null != location) {
				loc.setT(System.currentTimeMillis());
				loc.setY(location.getLatitude());
				loc.setX(location.getLongitude());
			}
			return loc;
		}

		private ServiceConnection statecraftConnection = new ServiceConnection() {
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				standardStatecraft = (IBindGPS) service;
				Timer timer = new Timer(true);
				timer.schedule(new TimerTask() {
					@Override
					public void run() {
						if (standardStatecraft.hasNewResult()) {// 如果有新的位置
							Message msg = new Message();
							msg.obj = standardStatecraft.getLocation();
							msg.what = UPDATE_GPS_LOCTION_WHAT;
							handler.sendMessage(msg);
							gpsLocationSendLogic.addPoint(location2GpsPoint(standardStatecraft.getLocation()));
						}
					}
				}, 1000, 1000);
				Log.e(TAG, "onServiceConnected");
				new SendLocation().start();
			}

			@Override
			public void onServiceDisconnected(ComponentName arg0) {
				Log.e(TAG, "Service Unbound");
			}
		};

		/**
		 * 没30秒钟去传一次位置到服务器
		 * 
		 * @protocol
		 * @author JiangXusheng
		 * @date 2013-8-21 下午6:32:46
		 */
		class SendLocation {
			void start() {
				new Timer().schedule(new TimerTask() {
					@Override
					public void run() {
						gpsLocationSendLogic.submitGpsLocation(mActivity.cuid);
					}
				}, 1000, 60000);
			}
		}
	}

}
