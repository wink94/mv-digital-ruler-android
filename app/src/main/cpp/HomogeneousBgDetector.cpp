
#include "HomogeneousBgDetector.h"

using namespace std;

HomogeneousBgDetector::HomogeneousBgDetector()
{
}

std::vector<std::vector<cv::Point>> HomogeneousBgDetector::detect_objects(cv::Mat gray)
{
    // Convert Image to grayscale

    // Create a Mask with adaptive threshold
    cv::Mat mask;
    cv::adaptiveThreshold(gray, mask, 255, cv::ADAPTIVE_THRESH_MEAN_C, cv::THRESH_BINARY_INV, adaptive_threshold_block_size, adaptive_threshold_constant);

    // Find contours
    std::vector<std::vector<cv::Point>> contours;
    std::vector<cv::Vec4i> hierarchy;
    cv::findContours(mask, contours, hierarchy, cv::RETR_EXTERNAL, cv::CHAIN_APPROX_SIMPLE);

    std::vector<std::vector<cv::Point>> objects_contours;

    for (auto cnt : contours)
    {
        double area = cv::contourArea(cnt);
        if (area > min_area_threshold)
        {
            cv::approxPolyDP(cnt,cnt, approx_poly_dp_ratio * cv::arcLength(cnt, true), true);
            objects_contours.push_back(cnt);
        }
    }

    return objects_contours;
}



