package org.xululabs.flickwiz.service.coreclassess;

import java.awt.image.BufferedImage;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class Converter {

	public static BufferedImage mat2Img(Mat in) {
		BufferedImage out;
		byte[] data = new byte[in.cols() * in.rows() * (int) in.elemSize()];
		int type;
		in.get(0, 0, data);
		if (in.channels() == 1)
			type = BufferedImage.TYPE_BYTE_GRAY;
		else
			type = BufferedImage.TYPE_3BYTE_BGR;
		out = new BufferedImage(in.cols(), in.rows(), type);
		out.getRaster().setDataElements(0, 0, in.cols(), in.rows(), data);
		return out;
	}

	public static Mat img2Mat(BufferedImage in) {
		Mat out;
		byte[] data;
		int r, g, b;

		out = new Mat(in.getHeight(), in.getWidth(), CvType.CV_8UC3);
		data = new byte[in.getWidth() * in.getHeight() * (int) out.elemSize()];
		int[] dataBuff = in.getRGB(0, 0, in.getWidth(), in.getHeight(), null,
				0, in.getWidth());
		for (int i = 0; i < dataBuff.length; i++) {
			data[i * 3] = (byte) ((dataBuff[i] >> 16) & 0xFF);
			data[i * 3 + 1] = (byte) ((dataBuff[i] >> 8) & 0xFF);
			data[i * 3 + 2] = (byte) ((dataBuff[i] >> 0) & 0xFF);
		}
		out.put(0, 0, data);
		return out;
	}

}
