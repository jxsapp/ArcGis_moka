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

import com.iwxlh.pta.R;
import com.iwxlh.pta.boot.PtaApplication;
import com.iwxlh.pta.db.AddressCacheHolder;
import com.iwxlh.pta.widget.BuAddress;
import com.wxlh.pta.lib.debug.PtaDebug;
import com.wxlh.pta.lib.misc.StringUtils;

public class GetSetBuAddressHolder {

	static final String GOOGLE_ADDRESS_URL = "http://maps.googleapis.com/maps/api/geocode/json?latlng=%.5f,%.5f&sensor=true&language=zh";

	public static String getGoogleMapURL(double x, double y) {
		return String.format(Locale.CHINA, GOOGLE_ADDRESS_URL, y, x);
	}

	private Map<BuAddress, String> buAddresss = Collections.synchronizedMap(new WeakHashMap<BuAddress, String>());

	private ExecutorService executorService;

	private BuAddressMemoryCache addressMemoryCache;

	public GetSetBuAddressHolder(int threadSize, BuAddressMemoryCache addressMemoryCache) {
		executorService = Executors.newFixedThreadPool(threadSize);
		this.addressMemoryCache = addressMemoryCache;
	}

	public void setAddress(String oldAddress, double x, double y, BuAddress buAddress) {
		buAddresss.put(buAddress, oldAddress);
		addQueueGetText(oldAddress, x, y, buAddress);
	}

	private static String getGoogleMapKey(double x, double y) {
		return x + "_-_" + y;
	}

	private void addQueueGetText(String oldAddress, double x, double y, BuAddress buAddress) {
		String key = getGoogleMapKey(x, y);
		buAddress.showLoading();
		String address = addressMemoryCache.get(key);// 先从内存中查询

		if (StringUtils.isEmpety(address)) {// 从数据库缓存中查询
			address = AddressCacheHolder.query(key).getAddress();
		}
		if (StringUtils.isEmpety(address)) {// 网络获取
			executorService.submit(new ImageLoadWorker(new ImageMapInfo(oldAddress, x, y, buAddress)));
		} else {
			addressMemoryCache.put(key, address);
			setToBuAddress(address + " " + oldAddress, buAddress);
		}
	}

	private String getAddress(ImageMapInfo imageMapInfo) {
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
								JSONObject obj = results.getJSONObject(0);
								if (obj.has("formatted_address") && !obj.isNull("formatted_address")) {
									rst = obj.getString("formatted_address");
									JSONArray address_components = obj.getJSONArray("address_components");
									if (null != address_components && address_components.length() > 0) {
										for (int index = 0; index < address_components.length(); index++) {
											JSONObject jsonObject = address_components.getJSONObject(index);
											if (jsonObject.toString().indexOf("neighborhood") > 0) {
												rst = rst.replaceAll(jsonObject.getString("long_name"), "");
												break;
											}
										}
									}
								}
							}
						}
					}
				}
			} catch (Exception ex) {
			}

			if (StringUtils.isEmpety(rst)) {
				rst = GetBaiduAddressHolder.getAddress(imageMapInfo.x, imageMapInfo.y, GetBaiduAddressHolder.Type.formatted_address);
			} else {
				int indexZHONGGUO = rst.indexOf("中国");
				int indexYOUZHENGBIANMA = rst.indexOf(" ");
				if (indexZHONGGUO == -1) {
					indexZHONGGUO = 0;
				}
				if (indexYOUZHENGBIANMA == -1) {
					indexYOUZHENGBIANMA = rst.length();
				}
				try {
					rst = rst.substring(indexZHONGGUO + 2, indexYOUZHENGBIANMA);
				} catch (Exception e) {
				}
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
		BuAddress buAddress;

		public ImageMapInfo(String oldAddress, double x, double y, BuAddress i) {
			this.oldAddress = oldAddress;
			this.x = x;
			this.y = y;
			this.buAddress = i;
		}
	}

	private class ImageLoadWorker implements Runnable {
		ImageMapInfo imageMapInfo;

		ImageLoadWorker(ImageMapInfo imageMapInfo) {
			this.imageMapInfo = imageMapInfo;
		}

		@Override
		public void run() {
			String text = getAddress(imageMapInfo);
			if (buAddressReused(imageMapInfo))
				return;
			StringRunable bd = new StringRunable(text, imageMapInfo);
			Activity a = (Activity) imageMapInfo.buAddress.getContext();
			a.runOnUiThread(bd);
		}
	}

	private boolean buAddressReused(ImageMapInfo imageMapInfo) {
		String tag = buAddresss.get(imageMapInfo.buAddress);
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
			if (buAddressReused(imageMapInfo))
				return;
			setToBuAddress(text, imageMapInfo.buAddress);
		}
	}

	private void setToBuAddress(String text, BuAddress buAddress) {
		if (StringUtils.isEmpety(text)) {
			text = PtaApplication.getApplication().getResources().getString(R.string.address_loading_error);
		}
		buAddress.setText(text);
		buAddress.dissmisLoading();
	}

}
