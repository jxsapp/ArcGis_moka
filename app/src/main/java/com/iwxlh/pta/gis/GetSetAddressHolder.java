package com.iwxlh.pta.gis;

import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.widget.TextView;

import com.iwxlh.pta.db.AddressCacheHolder;
import com.wxlh.pta.lib.debug.PtaDebug;
import com.wxlh.pta.lib.misc.StringUtils;

public class GetSetAddressHolder {

//	static final String GOOGLE_ADDRESS_URL = "http://maps.googleapis.com/maps/api/geocode/json?latlng=34.77381,113.68997&sensor=false&language=zh";
	static final String GOOGLE_ADDRESS_URL = "http://maps.googleapis.com/maps/api/geocode/json?latlng=%.5f,%.5f&sensor=false&language=zh";

	public static String getGoogleMapURL(double x, double y) {
		return String.format(Locale.CHINA, GOOGLE_ADDRESS_URL, y, x);
	}

	private Map<TextView, String> textViews = Collections.synchronizedMap(new WeakHashMap<TextView, String>());

	private ExecutorService executorService;

	private BuAddressMemoryCache addressMemoryCache;

	public GetSetAddressHolder(int threadSize, BuAddressMemoryCache addressMemoryCache) {
		executorService = Executors.newFixedThreadPool(threadSize);
		this.addressMemoryCache = addressMemoryCache;
	}

	public void getText(String oldAddress, double x, double y, TextView textView) {
		textViews.put(textView, oldAddress);
		addQueueGetText(oldAddress, x, y, textView);
	}

	private static String getGoogleMapKey(double x, double y) {
		return x + "_" + y;
	}

	private void addQueueGetText(String oldAddress, double x, double y, TextView textView) {
		String key = getGoogleMapKey(x, y);
		String address = addressMemoryCache.get(key);// 先从内存中查询

		if (StringUtils.isEmpety(address)) {// 从数据库缓存中查询
			address = AddressCacheHolder.query(key).getAddress();
		}
		if (StringUtils.isEmpety(address)) {// 网络获取
			executorService.submit(new ImageLoadWorker(new ImageMapInfo(oldAddress, x, y, textView)));
		} else {
			addressMemoryCache.put(key, address);
			setToTextView(address + " " + oldAddress, textView);
		}
	}

	private String getString(ImageMapInfo imageMapInfo) {
		try {
			String rst = "";
			HttpGet httpGet = new HttpGet(getGoogleMapURL(imageMapInfo.x, imageMapInfo.y));
			httpGet.setHeader("Accept", "application/json");
			httpGet.setHeader("Content-Type", "application/json;charset=utf-8");

			try {
				DefaultHttpClient httpClient = new DefaultHttpClient();
				HttpResponse httpResponse = httpClient.execute(httpGet);

				int status = httpResponse.getStatusLine().getStatusCode();
				if (status == HttpURLConnection.HTTP_OK) {
					byte[] responseByte = EntityUtils.toByteArray(httpResponse.getEntity());
					if (responseByte.length != 0) {
						String str = new String(responseByte, 0, responseByte.length);
						JSONObject json = new JSONObject(str);
						if (null != json && json.has("results") && !json.isNull("results")) {
							JSONArray results = json.getJSONArray("results");
							if (null != results && results.length() > 0) {

								for (int index = 0; index < results.length(); index++) {
									if (results.opt(index).toString().indexOf("route") == -1) {
										continue;
									} else {
										JSONObject route = results.getJSONObject(index);

										if (null != route && route.has("types") && !route.isNull("types")) {
											JSONArray addressArray = route.getJSONArray("address_components");
											String add = "";
											if (null != addressArray && addressArray.length() > 0) {
												for (int i = 0; i < addressArray.length(); i++) {
													JSONObject address = addressArray.getJSONObject(i);
													if (addressArray.opt(i).toString().indexOf("route") != -1) {
														add = address.getString("long_name") + "";
													}
												}
												rst = add;
											}
										}

									}
								}

							}
						}
					}
				}
			} catch (Exception ex) {
				rst = "";
			}
			if (StringUtils.isEmpety(rst)) {
				rst = GetBaiduAddressHolder.getAddress(imageMapInfo.x, imageMapInfo.y, GetBaiduAddressHolder.Type.district_street);
			}
			if (StringUtils.isEmpety(rst)) {
				String id = getGoogleMapKey(imageMapInfo.x, imageMapInfo.y);
				addressMemoryCache.put(id, rst);
				AddressCacheHolder.saveOrUpdate(id, addressMemoryCache.get(id));
			}
			rst = rst + " " + imageMapInfo.oldAddress;
			return rst;
		} catch (Exception ex) {
			PtaDebug.e("GET_SET_IMAGE", "getString catch Exception...\nmessage = " + ex.getMessage());
			return null;
		}
	}

	// Task for the queue
	private class ImageMapInfo {
		String oldAddress;
		double x;
		double y;
		TextView textView;

		public ImageMapInfo(String oldAddress, double x, double y, TextView i) {
			this.oldAddress = oldAddress;
			this.x = x;
			this.y = y;
			this.textView = i;
		}
	}

	private class ImageLoadWorker implements Runnable {
		ImageMapInfo imageMapInfo;

		ImageLoadWorker(ImageMapInfo imageMapInfo) {
			this.imageMapInfo = imageMapInfo;
		}

		@Override
		public void run() {
			if (textViewReused(imageMapInfo))
				return;
			String text = getString(imageMapInfo);
			if (textViewReused(imageMapInfo))
				return;
			StringRunable bd = new StringRunable(text, imageMapInfo);
			Activity a = (Activity) imageMapInfo.textView.getContext();
			a.runOnUiThread(bd);
		}
	}

	private boolean textViewReused(ImageMapInfo imageMapInfo) {
		String tag = textViews.get(imageMapInfo.textView);
		if (tag == null || !tag.equals(imageMapInfo.oldAddress))
			return true;
		return false;
	}

	private class StringRunable implements Runnable {
		private String text;
		private ImageMapInfo imageMapInfo;

		public StringRunable(String b, ImageMapInfo p) {
			text = b;
			imageMapInfo = p;
		}

		public void run() {
			if (textViewReused(imageMapInfo))
				return;
			if (text != null) {
				setToTextView(text, imageMapInfo.textView);
			}
		}
	}

	private void setToTextView(String text, TextView textView) {
		textView.setText(text);
	}

}
