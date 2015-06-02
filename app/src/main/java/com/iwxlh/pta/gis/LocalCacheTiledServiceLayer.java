package com.iwxlh.pta.gis;

import java.io.File;
import java.io.FileOutputStream;

import com.iwxlh.pta.misc.FileHolder;
import com.wxlh.pta.lib.debug.PtaDebug;

public class LocalCacheTiledServiceLayer extends GoogleMapsTiledServiceLayer {
	private static final String TAG = LocalCacheTiledServiceLayer.class.getName();
	private String cachePath;// 本地存储路径
	// getUrl() + "/tile" + "/" + level + "/" + row + "/" + col;
	private String SD_CARD_PATH = "%s/%d/%d/%d/";

	public LocalCacheTiledServiceLayer(String layerurl, String cachepath) {
		super();
		this.cachePath = cachepath;
	}

	@Override
	protected byte[] getTile(int level, int col, int row) throws Exception {

		byte[] bytes = null;
		bytes = getOfflineCacheFile(level, col, row);
		if (bytes == null) {
			bytes = super.getTile(level, col, row);
			addOfflineCacheFile(level, col, row, bytes);
		}
		return bytes;
	}

	private byte[] addOfflineCacheFile(int level, int col, int row, byte[] bytes) {

		File colfile = new File(cachePath + "/" + level + "/" + col);
		if (!colfile.exists()) {
			colfile.mkdirs();
		}
		File rowfile = new File(cachePath + "/" + level + "/" + col + "/" + row);
		if (!rowfile.exists()) {
			FileOutputStream out = null;
			try {
				out = new FileOutputStream(rowfile);
				out.write(bytes);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (null != out) {
					try {
						out.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		return bytes;
	}

	private byte[] getOfflineCacheFile(int level, int col, int row) {
		String url = "";
		try {
			url = String.format(SD_CARD_PATH, cachePath, level, col, row);
		} catch (Exception e) {
			PtaDebug.e(TAG, "getOfflineCacheFile.exception", e);
		}
		return FileHolder.getBytesFromSdCardFile(url);
	}
}
