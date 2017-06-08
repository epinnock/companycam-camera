#ifndef CCIP_DOCSCAN_HPP
#define CCIP_DOCSCAN_HPP

class DocScanner
{
    cv::Mat imageResized;
    cv::Mat imageGray;
    cv::Mat imageBlur;
    cv::Mat imageEdges;

public:
    DocScanner();
    void test(int &x);
    cv::Mat scan(const cv::Mat& imageOrig, std::vector<cv::Vec4i>& lines);
};

#endif