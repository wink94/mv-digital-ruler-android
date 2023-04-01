#ifndef HOMOGENEOUS_BG_DETECTOR_H
#define HOMOGENEOUS_BG_DETECTOR_H

#include <opencv2/opencv.hpp>

using namespace std;
using namespace cv;

class HomogeneousBgDetector
{
public:
    HomogeneousBgDetector();
   vector<vector<cv::Point>> detect_objects(cv::Mat frame);

private:
    int min_area_threshold = 5000;
    double approx_poly_dp_ratio = 0.03;
    int adaptive_threshold_block_size = 19;
    int adaptive_threshold_constant = 5;
};

#endif
