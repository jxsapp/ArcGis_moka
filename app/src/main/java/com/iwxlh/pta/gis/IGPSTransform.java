package com.iwxlh.pta.gis;

import com.esri.core.geometry.Point;

public interface IGPSTransform {

	Point toChinaCoordinate(double x, double y);

}
