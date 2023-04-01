//
// Created by windula_k on 4/1/2023.
//

#ifndef MV_CPP_ANDROID_DIGITAL_RULER_MAIN_H
#define MV_CPP_ANDROID_DIGITAL_RULER_MAIN_H

#include <opencv2/opencv.hpp>
#include "HomogeneousBgDetector.h"
#include <cmath>
#include <math.h>
#include <iostream>
#include <string>
#include <sstream>


float distance(cv::Point2f pt1, cv::Point2f pt2);
void   order_points(cv::Point2f points[4], cv::Point2f points_out[4]);
double calculateDistance(int x1, int y1, int x2, int y2);
Point2d midpoint(Point2d ptA, Point2d ptB);
double calculateDistanceFromPoint2d(cv::Point2d ptA, cv::Point2d ptB);
int calibrationObject(Mat frame,double width ,double height );


#endif //MV_CPP_ANDROID_DIGITAL_RULER_MAIN_H
