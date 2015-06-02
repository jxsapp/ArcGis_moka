package com.iwxlh.pta.gis;

import java.net.HttpURLConnection;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Base64;

public class GetBaiduAddressHolder {
	/**
	 * @from: 来源坐标系 （0表示原始GPS坐标，2表示Google坐标）
	 * @to: 转换后的坐标 (4就是百度自己啦，好像这个必须是4才行）
	 * @x: 精度
	 * @y: 纬度
	 */
	static final String BAIDU_CONVERT_URL = "http://api.map.baidu.com/ag/coord/convert?from=2&to=4&x=%.5f&y=%.5f";
	static final String BAIDU_GEOCODER_URL = "http://api.map.baidu.com/geocoder?location=%s,%s&output=json";

	public enum Type {
		formatted_address, district_street, district_street_street_number;
	}

	public static String getAddress(double googleX, double googleY, final Type type) {
		String rst = "";
		HttpGet httpGet = new HttpGet(String.format(BAIDU_CONVERT_URL, googleX, googleY));
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
					if (null != json && json.has("error") && !json.isNull("error") && 0 == json.getInt("error")) {
						String baiduX = new String(Base64.decode(json.getString("x"), Base64.DEFAULT));
						String baiduY = new String(Base64.decode(json.getString("y"), Base64.DEFAULT));

						httpGet = new HttpGet(String.format(BAIDU_GEOCODER_URL, baiduY, baiduX));
						httpGet.setHeader("Accept", "application/json");
						httpGet.setHeader("Content-Type", "application/json;charset=utf-8");
						httpResponse = httpClient.execute(httpGet);

						status = httpResponse.getStatusLine().getStatusCode();
						if (status == HttpURLConnection.HTTP_OK) {
							responseByte = EntityUtils.toByteArray(httpResponse.getEntity());
							if (responseByte.length != 0) {
								str = new String(responseByte, 0, responseByte.length);
								rst = getAddress(str, type);
							}
						}
					}
				}
			}
		} catch (Exception e) {

		}
		return rst;
	}

	/**
	 * { "status":"OK", "result":{ "location":{ "lng":116.327159,
	 * "lat":39.990912 }, "formatted_address":"北京市海淀区中关村南一街7号平房-4号",
	 * "business":"中关村,北京大学,五道口", "addressComponent":{ "city":"北京市",
	 * "district":"海淀区", "province":"北京市", "street":"中关村南一街",
	 * "street_number":"7号平房-4号" }, "cityCode":131 } }
	 * 
	 * @throws JSONException
	 */

	private static String getAddress(String str, Type type) throws JSONException {
		JSONObject json = new JSONObject(str);
		String rst = "";
		if (null != json && json.has("status") && !json.isNull("status") && "OK".equals(json.getString("status").toUpperCase(Locale.CHINA))) {
			JSONObject result = json.getJSONObject("result");
			if (type.ordinal() == Type.formatted_address.ordinal()) {
				rst = result.getString("formatted_address");
			} else {
				JSONObject addressComponent = result.getJSONObject("addressComponent");
				if (type.ordinal() == Type.district_street.ordinal()) {
					rst = addressComponent.get("district") + addressComponent.getString("street");
				} else if (type.ordinal() == Type.district_street_street_number.ordinal()) {
					rst = addressComponent.get("district") + addressComponent.getString("street") + addressComponent.getString("street_number");
				}
			}
		}
		return rst;
	}

}
