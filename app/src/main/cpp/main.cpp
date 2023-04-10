//
// Created by windula_k on 4/1/2023.
//

#include "main.h"
#include <opencv2/opencv.hpp>
#include "HomogeneousBgDetector.h"
#include <cmath>
#include <math.h>
#include <iostream>
#include <string>
#include <sstream>

using namespace std;
using namespace cv;

cv::Point2f point1(-1, -1);
cv::Point2f point2(-1, -1);

// Returns the Euclidean distance between two points.
float distance(cv::Point2f pt1, cv::Point2f pt2) {
    float dx = pt1.x - pt2.x;
    float dy = pt1.y - pt2.y;
    return sqrt(dx * dx + dy * dy);
}

// Orders a set of 4 points in a consistent order such that the top-left point is
// the first entry in the list, followed by top-right, bottom-right, and bottom-left.
void   order_points(cv::Point2f points[4], cv::Point2f points_out[4]) {
    cv::Point2f ordered_points[4];

    // Compute the sum and difference of the (x, y) coordinates for each point.
    std::vector<float> sums, diffs;
    for (int i = 0; i < 4; i++) {
        sums.push_back(points[i].x + points[i].y);
        diffs.push_back(points[i].x - points[i].y);
    }

    // The top-left point will have the smallest sum, whereas the bottom-right point
    // will have the largest sum.
    int tl_idx = 0, br_idx = 0;
    for (int i = 0; i < 4; i++) {
        if (sums[i] < sums[tl_idx]) {
            tl_idx = i;
        }
        if (sums[i] > sums[br_idx]) {
            br_idx = i;
        }
    }

    // The top-right point will have the smallest difference, whereas the bottom-left
    // point will have the largest difference.
    int tr_idx = 0, bl_idx = 0;
    for (int i = 0; i < 4; i++) {
        if (diffs[i] < diffs[tr_idx]) {
            tr_idx = i;
        }
        if (diffs[i] > diffs[bl_idx]) {
            bl_idx = i;
        }
    }

    // Reconstruct the ordered points list.
    points_out[0] = points[tl_idx];
    points_out[1] = points[tr_idx];
    points_out[2] = points[br_idx];
    points_out[3] = points[bl_idx];

}

double calculateDistance(int x1, int y1, int x2, int y2) {
    double dist = sqrt(pow(x2 - x1, 2) + pow(y2 - y1, 2));
    return dist;
}

Point2d midpoint(Point2d ptA, Point2d ptB) {
    return {(ptA.x + ptB.x) * 0.5, (ptA.y + ptB.y) * 0.5};
}

double calculateDistanceFromPoint2d(cv::Point2d ptA, cv::Point2d ptB) {
    double dist = sqrt(pow(ptA.x - ptB.x, 2) + pow(ptA.y - ptB.y, 2));
    return dist;
}

double calibrationObject(Mat frame,double width = 6,double height = 3) {

    double ref_object_px_per_cm = 0;
    HomogeneousBgDetector detector;

    std::vector<std::vector<cv::Point>> contours = detector.detect_objects(frame);


    for (int i = 0; i < contours.size(); i++) {
        if (ref_object_px_per_cm != 0)
            continue;

        cv::RotatedRect rect = cv::minAreaRect(contours[i]);
        cv::Point2f box[4];
        rect.points(box);

        cv::Point2f box_ordered[4];
        order_points(box, box_ordered);

        for (int j = 0; j < 4; j++) {
            cv::circle(frame, box_ordered[j], 5, cv::Scalar(0, 0, 255), -1);
            cv::line(frame, box_ordered[j], box_ordered[(j + 1) % 4], cv::Scalar(255, 0, 0), 2);
        }

        double w = rect.size.width;
        double h = rect.size.height;

        cv::putText(frame, "Width " + std::to_string(round(w)), cv::Point(rect.center.x + 15, rect.center.y + 50), cv::FONT_HERSHEY_PLAIN, 2, cv::Scalar(100, 200, 0), 2);
        cv::putText(frame, "Height " + std::to_string(round(h)), cv::Point(rect.center.x - 15, rect.center.y - 50), cv::FONT_HERSHEY_PLAIN, 2, cv::Scalar(100, 200, 0), 2);

        cv::Point2f tl = box_ordered[0];
        cv::Point2f bl = box_ordered[3];
        cv::Point2f tr = box_ordered[1];
        cv::Point2f br = box_ordered[2];

        cv::Point2f tlbl = midpoint(tl, bl);
        cv::Point2f trbr = midpoint(tr, br);

        std::vector<cv::Point2f> src_points = {tl, tr, br, bl};
        std::vector<cv::Point2f> dst_points;

        float widthTD = cv::norm(tr - tl);
        float heightTD = cv::norm(br - tr);

// Create the destination rectangle (desired top view)
        dst_points.push_back(cv::Point2f(0, 0));
        dst_points.push_back(cv::Point2f(width, 0));
        dst_points.push_back(cv::Point2f(width, height));
        dst_points.push_back(cv::Point2f(0, height));

// Calculate the perspective transformation matrix
        cv::Mat perspective_transform = cv::getPerspectiveTransform(src_points, dst_points);

        std::vector<cv::Point2f> top_down_points;
        cv::perspectiveTransform(src_points, top_down_points, perspective_transform);
// Apply the perspective transformation
        cv::Mat top_down_view;
        cv::warpPerspective(frame, frame, perspective_transform, cv::Size(widthTD, heightTD));

        double ref_object_px = cv::norm(tlbl - trbr);

        if (ref_object_px != 0 && width != 0)
            ref_object_px_per_cm = ref_object_px / width;

        cv::putText(frame, "Reference Object pixels/cm length " + std::to_string(ref_object_px_per_cm), cv::Point(10, 30), cv::FONT_HERSHEY_PLAIN, 1, cv::Scalar(255, 0, 0), 2);
    }

    return ref_object_px_per_cm;
}

void measureDistance(Mat frame,double refPxPerCm) {
    cv::circle(frame, point1, 3, cv::Scalar(0, 255, 0), -1);
    cv::putText(frame, "Point 1", point1, cv::FONT_HERSHEY_SIMPLEX, 0.5, cv::Scalar(0, 255, 0), 2);

    cv::line(frame, point1, point2, cv::Scalar(255, 0, 0), 2);
    cv::putText(frame, "Point 2", point2, cv::FONT_HERSHEY_SIMPLEX, 0.5, cv::Scalar(255, 0, 0), 2);

    if (refPxPerCm != 0) {
        float dist = calculateDistanceFromPoint2d(point1, point2) / refPxPerCm;
        std::ostringstream dist_str;
        dist_str << "Distance (cm): " << dist;
        cv::putText(frame, dist_str.str(), cv::Point(10, 30), cv::FONT_HERSHEY_SIMPLEX, 0.8, cv::Scalar(255, 255, 255), 2);
    }
}
