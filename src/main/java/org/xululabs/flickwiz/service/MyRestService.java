package org.xululabs.flickwiz.service;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.Size;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.imgproc.Imgproc;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.xululabs.flickwiz.service.coreclassess.Converter;
import org.xululabs.flickwiz.service.coreclassess.FeaturesORB;
import org.xululabs.flickwiz.service.coreclassess.SimilarityIndex;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

public class MyRestService {

	private static final LinkedList<URL> posterUrls = new LinkedList<URL>();
	private static final LinkedList<String> posterNames = new LinkedList<String>();
	private static final LinkedList<Mat> posters_TrainDescriptors = new LinkedList<Mat>();
	private LinkedList<URL> bestURLS = new LinkedList<URL>();
	private LinkedList<String> bestNames = new LinkedList<String>();
	private LinkedList<LinkedList<String>> IMDBDetials = new LinkedList<LinkedList<String>>();
	private final ArrayList<LinkedList<String>> movieList = new ArrayList<LinkedList<String>>();
	private ArrayList<String> tempList = new ArrayList();
	private int count = 0;

	private DescriptorMatcher descriptorMatcher;
	private FeaturesORB featuresORB;
	private Mat queryDescriptor;
	private Mat trainDescriptor;
	private MatOfDMatch matches;
	private static boolean startFirstTime = true;

	public ResponseModel getFeatureResult(File uploadedImage) {
		System.out.println("Request received at : " +"[ "+ Calendar.getInstance().getTime()+" ]" );
		Loader.init();
		
		

		if (startFirstTime) {
			try {
				allFeaturesExtraction();
				startFirstTime = false;
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			System.out.print("Features dataset already loaded :: ");
		}

		FeaturesORB orb = new FeaturesORB();
		queryDescriptor = new Mat();
		matches = new MatOfDMatch();
		List<SimilarityIndex> similarIndices = new ArrayList<SimilarityIndex>();
		try {
			BufferedImage img = ImageIO.read(uploadedImage);
			System.out.println("Query image dimensions : "+img.getWidth() + " * " + img.getHeight());

			descriptorMatcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
			
		
			
			queryDescriptor = orb.getORBFeaturesDescriptorMat(Converter.img2Mat(img));
		
			
			for (int i = 0; i < posters_TrainDescriptors.size(); i++) {
				
				
				
				descriptorMatcher.clear();
				trainDescriptor = posters_TrainDescriptors.get(i);
				descriptorMatcher.match(queryDescriptor, trainDescriptor,
						matches);
				List<DMatch> matchesList = matches.toList();

				Double max_dist = 0.0;
				Double min_dist = 100.0;

				for (int j = 0; j < queryDescriptor.rows(); j++) {
					Double dist = (double) matchesList.get(j).distance;
					if (dist < min_dist)
						min_dist = dist;
					if (dist > max_dist)
						max_dist = dist;
				}

				LinkedList<DMatch> good_matches = new LinkedList<>();
				double goodMatchesSum = 0.0;

				// good match = distance > 2*min_distance ==> put them in a list
				for (int k = 0; k < queryDescriptor.rows(); k++) {
					if (matchesList.get(k).distance < Math.max(2 * min_dist,
							0.02)) {
						good_matches.addLast(matchesList.get(k));
						goodMatchesSum += matchesList.get(k).distance;
					}
				}

				double simIndex = (double) goodMatchesSum
						/ (double) good_matches.size();
				similarIndices.add(new SimilarityIndex(simIndex, posterUrls
						.get(i), posterNames.get(i)));

				// System.out.println("Similarity with image "
				// +i+"==>"+simIndex);

			}

			Comparator<SimilarityIndex> indexComparator = new Comparator<SimilarityIndex>() {
				public int compare(SimilarityIndex index1,
						SimilarityIndex index2) {
					return index1.getIndex().compareTo(index2.getIndex());
				}
			};

			Collections.sort(similarIndices, indexComparator);
			bestURLS.clear();
			bestNames.clear();
			IMDBDetials.clear();
			tempList.clear();

			try {
				count = 0;
				for (int i = 0; i < similarIndices.size(); i++) {

					if (!tempList.contains(similarIndices.get(i).getName()
							.toString())) {

						bestNames.add(similarIndices.get(i).getName()+"    Similarity ratio for that image : " +similarIndices.get(i).getIndex());
						bestURLS.add(similarIndices.get(i).getUrl());
						IMDBDetials.add(getImdbData(similarIndices.get(i)
								.getName()));
						tempList.add(similarIndices.get(i).getName());
						++count;

						
					}
					if (count == 5) {
						System.out.println("Number of movies as result : " + count);
						count = 0;
						break;
					}
				}

			} catch (Exception e) {
				System.out.println(e.getMessage() + " Passing data to List");
			}

		} catch (Exception ex) {
			System.out
					.println("Base64Code to bufferedImage conversion exception");
			System.out.println(ex.getMessage());
		}

		// System.out.println(posters_TrainDescriptors.get(0).dump());
		// System.out.println(posters_TrainDescriptors.get(0).toString());

		return new ResponseModel(bestNames, bestURLS, IMDBDetials);
	}

	/*
	 * This function is used to get IMDB movie details.
	 */
	private LinkedList<String> getImdbData(String movie) {
		final LinkedList<String> dataIMDB = new LinkedList<>();
		dataIMDB.clear();
		try {
			InputStream input = new URL("http://www.omdbapi.com/?t="
					+ URLEncoder.encode(movie, "UTF-8")).openStream();
			Map<String, String> map = new Gson().fromJson(
					new InputStreamReader(input, "UTF-8"),
					new TypeToken<Map<String, String>>() {
					}.getType());

			dataIMDB.add(map.get("Title"));
			dataIMDB.add(map.get("Year"));
			dataIMDB.add(map.get("Released"));
			dataIMDB.add(map.get("Runtime"));
			dataIMDB.add(map.get("Genre"));
			dataIMDB.add(map.get("Director"));
			dataIMDB.add(map.get("Writer"));
			dataIMDB.add(map.get("Actors"));
			dataIMDB.add(map.get("Plot"));
			dataIMDB.add(map.get("imdbRating"));
			dataIMDB.add(map.get("imdbID"));

			dataIMDB.addFirst("http://www.imdb.com/title/"
					+ map.get("imdbID").toString() + "/");

		} catch (Exception e) {
			System.out.println(e.getMessage().toString());
		}
		return dataIMDB;
	}

	/*
	 * This function read the movies poster from the CSV file and extract
	 * features and store them in the LinkedList.
	 */
	private void allFeaturesExtraction() throws IOException {
		int counter = 0;
		// Loader.init();
		featuresORB = new FeaturesORB();
		String[] nextLine;
		// String checkString = new String();

		CSVReader reader = new CSVReader(
				new FileReader("movieFile/movies.csv"), ',','\"',1);

		while ((nextLine = reader.readNext()) != null) {
			// nextLine[] is an array of values from the line

			String imageUrl = (String.valueOf(nextLine[1].charAt(0)).equals(
					"\"") ? nextLine[1].substring(1, nextLine[1].length() - 1)
					: nextLine[1]);

			String imageName = (String.valueOf(nextLine[0].charAt(0)).equals(
					"\"") ? nextLine[0].substring(1, nextLine[0].length() - 1)
					: nextLine[0]);

			System.out.println(counter);
			System.out.println("Name ==> " + imageName);
			System.out.println("Url ==> " + imageUrl);
			
			/*
			String destinationFile="data/"+imageName+counter+".jpg";
			destinationFile=destinationFile.trim();
			destinationFile=destinationFile.replace(" ","");
			destinationFile=destinationFile.replace(":","");
			System.out.println(destinationFile);
			saveImage(imageUrl, destinationFile);
			*/
			
			System.out.println();
			posterNames.add(counter, imageName);
			posterUrls.add(counter, new URL(imageUrl));
			++counter;

		}
		reader.close();
		
		
		System.out.println("Size of the movies csv : "+ posterUrls.size());
		System.out.println("Size of the Feature csv : "+ sizeOfCsv());
		
	//	if( posterNames.size()==sizeOfCsv()+1)
	//	{
		//	System.out.println("Number of entries in both file are equal");			
	//	}
	//	else {
		/*
			System.out.println("Warning there is a change in the entries..... ");
			
			
			for (int i=0;i<posterUrls.size();i++)
			{
				Mat mat=new Mat();
				
				try{
					
				//	try{
						System.out.println("Getting input stream....");
						BufferedImage	img=ImageIO.read(new URL(posterUrls.get(i).toString()));
					
				//	}catch(IIOException e)
				//	{
					//	Thread.sleep(1000);
					//	img=ImageIO.read(new URL(posterUrls.get(i).toString()));
				//	}
					
					mat=Converter.img2Mat(img);
					Imgproc.resize(mat, mat, new Size(450,600));
					
					posters_TrainDescriptors.add(featuresORB.getORBFeaturesDescriptorMat(mat));
					writeDataToCSV(posters_TrainDescriptors.getLast());				
					System.err.println(" Written Successfully "+"[ "+i+" ]");
				}catch(Exception e)
				{
					System.err.println("[ "+i+" ]");
					System.err.println(posterUrls.get(i));
					BufferedWriter bw=new BufferedWriter(new FileWriter(new File("missing.txt"),true));
					bw.write("[ "+i+" ] "+posterUrls.get(i));
					bw.newLine();
					e.printStackTrace();
					
				}
			}
		
			//}*/
		
		
	if(new File("movieFile/features.csv").exists())
		{
			System.out.println("Feature file is read");
		posters_TrainDescriptors.addAll(readDataFromCSV());
		}
		else{
			System.out.println("File is not present");
			 
			
			for(int i=0;i<posters_TrainDescriptors.size();i++)
			{
			writeDataToCSV(posters_TrainDescriptors.get(i));
			}
		}
		
		/*
		for(int i=0;i<posters_TrainDescriptors.size();i++)
		{
		
		
		BufferedWriter bw=new BufferedWriter(new FileWriter("AllMatrixInfo.txt",true));
		bw.write(posterNames.get(i));
		bw.write( "  Feature matrix # "+ i+ "--->");
		bw.write(" "+posters_TrainDescriptors.get(0).rows()+" * "+posters_TrainDescriptors.get(0).cols());
		bw.newLine();
		bw.close();
		}
		*/
		/*
		for(int i=0;i<posters_TrainDescriptors.size();i++)
		{
		BufferedWriter bw=new BufferedWriter(new FileWriter("AllMatrixData.txt",true));
		bw.write("Feature matrix # "+ i);
		bw.write(posters_TrainDescriptors.get(0).rows()+" * "+posters_TrainDescriptors.get(0).cols());
		bw.write(posters_TrainDescriptors.get(0).dump());
		bw.close();
		}*/
		
		
	}

	public static void saveImage(String imageUrl, String destinationFile) throws IOException {
	

		URL url = new URL(imageUrl);
		InputStream is = url.openStream();
		OutputStream os = new FileOutputStream(destinationFile);

		byte[] b = new byte[2048];
		int length;

		while ((length = is.read(b)) != -1) {
			os.write(b, 0, length);
		}

		is.close();
		os.close();
	}
	
	
	private void writeDataToCSV(Mat mat) throws IOException {

		CSVWriter csvWriter = new CSVWriter(new FileWriter(
				"movieFile/features.csv", true));
		List<String[]> dataMat = new ArrayList<String[]>();
	dataMat.add(new String[] {"detail",mat.rows()+"",mat.cols()+"" });
	//	dataMat.add(new String[] {"detail",10+"",10+"" });

		double[] data;
		for (int row = 0; row <mat.rows(); row++) {
			String[] temp = new String[mat.cols()];
			for (int col = 0; col <mat.cols(); col++) {
				data = mat.get(row, col);
				//System.out.println(data[0]);
				String d = Double.toString(data[0]);
				temp[col] = d;
			}
			dataMat.add(temp);

		}
		
		csvWriter.writeAll(dataMat);
		csvWriter.close();
	}


	
	public LinkedList<Mat> readDataFromCSV() throws IOException {
		
		System.out.println("In the reading block");
		int matcounter=0;
		
		LinkedList<Mat> matList = new LinkedList<Mat>();
		CSVReader csvReader = new CSVReader(new FileReader(
				"movieFile/features.csv"), ',');
		String[] row;
		
		
		int rw = 0;
		int cl = 0;
		int matrow=-1;

		while((row=csvReader.readNext())!=null)
		{
			
			
			////////Check the details ////
			if(row[0].equals("detail"))
			{	
				//System.out.println(" Start of the new Matrix: "+ matrow);
				//System.out.print(++matcounter);	
				rw=Integer.parseInt(row[1]);
				cl=Integer.parseInt(row[2]);
				//System.out.println(" dimension : [ "+rw +" * "+ cl+" ]");
				Mat m=new Mat(rw, cl, CvType.CV_8UC1);
				matList.add(m);
				matrow=-1;
				continue;
			}
			else{
				matrow++;
				for (int j = 0; j <cl; j++) 
				{
					 
					  double d=Double.parseDouble(row[j]);
					//  System.out.print("\t"+row[j]);
					  matList.get(matList.size()-1).put(matrow, j,d); 
					  double[] dd=matList.get(matList.size()-1).get(matrow, j); 
				}							
				
				if(rw==matrow+1)
				{
					
					//System.out.println("Matrix is fully read..");
					
					
				}
			}
		//	System.out.println();	
			
		}
		//System.out.println(matList.get(0).dump());
		/*
		for(int i=0;i<matList.size();i++){
			System.out.println();
			System.out.println(matList.get(i).dump());
				}
		*/
		System.out.println(matList.getLast().dump());
		return matList;
	}

	public int sizeOfCsv() throws IOException
	{
		

		int matcounter=0;
		CSVReader csvReader = new CSVReader(new FileReader("movieFile/features.csv"), ',');
		String[] row;
		
		while((row=csvReader.readNext())!=null)
		{
			
			if(row[0].equals("detail"))
			{
				matcounter++;
				//System.err.println("[ "+matcounter+" ]" +" [ "+row[1] +" * "+row[2] +" ]");
			}
		}
		System.out.println(" Control is in size of csv block : "+ matcounter);
		return matcounter;
	}
	
	
	
		/*
	 * This function is used to read the JSON response.
	 */
	public static Map<String, Object> toMapObject(String data) {
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> map = null;
		try {
			map = mapper.readValue(data,
					new TypeReference<Map<String, Object>>() {
					});
		} catch (Exception ex) {
			System.err
					.println("cannot convet to map<String, Object> : " + data);
			System.err.println(ex.getMessage());
		}

		return map;
	}

}