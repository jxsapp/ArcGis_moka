package com.iwxlh.pta.gis;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;

public interface IBindGPS {

	public static final String FETCH_GPS_LOACTION_SEND_BRAODCAST_ACTION = "com.iwxlh.pta.gis.gps.OriginalPoint.location_ACTION";

	public boolean hasNewResult();

	public Location getLocation();

	class SendLocationBroadCast {
		/**
		 * 广播出匹配完的位置和真实的采集到的位置，真实的GPS的位置是需要加偏之后才能显示的
		 * 
		 * @param context
		 * @param x
		 * @param y
		 * @param isNeedOffset
		 */
		static public void send(Context context, double x, double y, boolean isNeedOffset) {
			Intent intent = new Intent(IBindGPS.FETCH_GPS_LOACTION_SEND_BRAODCAST_ACTION);
			Bundle bundle = new Bundle();
			bundle.putDouble("x", x);
			bundle.putDouble("y", y);
			bundle.putBoolean("offset", isNeedOffset);
			intent.putExtras(bundle);
			context.sendStickyBroadcast(intent);
		}
	}
}
