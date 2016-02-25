package org.xululabs.flickwiz.service.coreclassess;

import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgcodecs.Imgcodecs;

public class FeaturesORB {
	
	private FeatureDetector featureDetector;
	private DescriptorExtractor descriptorExtractor;
	private Mat trainDescriptor;
	private MatOfKeyPoint trainKeyPoint;

	public FeaturesORB() {
		trainKeyPoint = new MatOfKeyPoint();		
		featureDetector = FeatureDetector.create(FeatureDetector.ORB);
		descriptorExtractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
	}

	public Mat getORBFeaturesDescriptorMat(Mat trainMat) {

		trainDescriptor = new Mat(trainMat.size(), trainMat.type());
		featureDetector.detect(trainMat, trainKeyPoint);
		descriptorExtractor.compute(trainMat, trainKeyPoint, trainDescriptor);

		return trainDescriptor;
	}

}
