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

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
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

						bestNames.add(similarIndices.get(i).getName());
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
				new FileReader("movieFile/movies.csv"), ',', '\"', 1);

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
		
		
		System.out.println("Size of the movies csv : "+ posterNames.size());
		System.out.println("Size of the Feature csv : "+ sizeOfCsv());
		
		if( posterNames.size()==sizeOfCsv())
		{
			System.out.println("Number of entries in both file are equal");			
		}
		else {
			System.out.println("Warning there is a change in the entries..... ");
			
			
			for (int i=sizeOfCsv();i<posterUrls.size();i++)
			{
				Mat mat=new Mat();
				mat=Converter.img2Mat(ImageIO.read(new URL(posterUrls.get(i).toString())));
				Imgproc.resize(mat, mat, new Size(450,600));
				posters_TrainDescriptors.add(counter, featuresORB.getORBFeaturesDescriptorMat(mat));
				writeDataToCSV(posters_TrainDescriptors.get(i));
			}
		}
		
		
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
		
		int matcounter=0;
		
		LinkedList<Mat> matList = new LinkedList<Mat>();
		CSVReader csvReader = new CSVReader(new FileReader(
				"movieFile/features.csv"), ',');
		String[] row;
		List<String[]> dataMat = new ArrayList<String[]>();
		dataMat = csvReader.readAll();

		
		int rw = 0;
		int cl = 0;
		int matrow=-1;

		for(int i=0;i<dataMat.size();i++)
		{
			row=dataMat.get(i);
			
			////////Check the details ////
			if(row[0].equals("detail"))
			{	
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
		return matList;
	}

	public int sizeOfCsv() throws IOException
	{
		
		int matcounter=0;
		CSVReader csvReader = new CSVReader(new FileReader("movieFile/features.csv"), ',');
		String[] row;
		List<String[]> dataMat = new ArrayList<String[]>();
		dataMat = csvReader.readAll();

		for(int i=0;i<dataMat.size();i++)
		{
			row=dataMat.get(i);
			////////Check the details ////
			if(row[0].equals("detail"))
			{
				matcounter++;
			}
		}
		
		return matcounter;
	}
	
	
	
	/*
	 * This service provides the list of movies depending upon the Genre type
	 * passed as input parameter.
	 */
	public GenreModel movieListByGenre(String genre) {

		System.out.println("Request Received on path /genredetail");
		System.out.println(genre);

		movieList.clear();
		String genreCode = getGenreCode(genre);

		try {

			InputStream input = new URL(
					"https://api.themoviedb.org/3/discover/movie?with_genres="
							+ genreCode
							+ "&api_key=746bcc0040f68b8af9d569f27443901f")
					.openStream();

			Map<String, Object> response = toMapObject(IOUtils.toString(input,
					"UTF-8"));
			if (response == null) {
				System.out.println("Response is null!!");
			} else {
				List<Map<String, Object>> filmsArray = (ArrayList<Map<String, Object>>) response
						.get("results");

				for (int i = 0; i < 10; i++) {

					final LinkedList<String> movieInfoList = new LinkedList<String>();

					if (filmsArray.get(i).get("poster_path").toString() == null) {
						movieInfoList.add("No Poster Available");
					} else {
						movieInfoList.add("http://image.tmdb.org/t/p/w300"
								+ filmsArray.get(i).get("poster_path")
										.toString());
					}
					if (filmsArray.get(i).get("title").toString() == null) {
						movieInfoList.add("No Title Available");
					} else {
						movieInfoList.add(filmsArray.get(i).get("title")
								.toString());
					}
					if (filmsArray.get(i).get("release_date").toString() == null) {
						movieInfoList.add("No Release Date Available");
					} else {
						movieInfoList.add(filmsArray.get(i).get("release_date")
								.toString());
					}
					if (filmsArray.get(i).get("overview").toString() == null) {
						movieInfoList.add("No overview Available");
					} else {
						movieInfoList.add(filmsArray.get(i).get("overview")
								.toString());
					}
					movieList.add(movieInfoList);
				}
			}

		} catch (Exception e) {

			System.out.println(e.getMessage().toString()
					+ "Error in genre details service");
		}
		return new GenreModel(movieList);
	}

	/*
	 * This service provides the detail of writers,directors and actors when
	 * name is passed as input parameter.
	 */
	@RequestMapping(value = "/persondetail", method = RequestMethod.GET)
	public @ResponseBody PersonDetailModel personDetail(String personName) {
		System.out.println("Request Received on path /persondetail");
		System.out.println(personName);

		final LinkedList<String> actorsInfoList = new LinkedList<String>();
		actorsInfoList.clear();

		List<String> tmdbId = new ArrayList<String>();
		List<String> tmdbDOB = new ArrayList<String>();

		String personCode = personResource(toTrim(personName));
		try {
			InputStream input = new URL("http://imdb.wemakesites.net/api/"
					+ URLEncoder.encode(personCode, "UTF-8")).openStream();

			Map<String, Object> response = toMapObject(IOUtils.toString(input,
					"UTF-8"));
			Map<String, Object> data = (Map<String, Object>) response
					.get("data");
			List<Object> mediaLinks = (ArrayList<Object>) data
					.get("mediaLinks");
			List<Map<String, Object>> filmography = (ArrayList<Map<String, Object>>) data
					.get("filmography");
			if (response == null) {
				System.out.println("The Responce is null !!! ");
			} else {

				if (data.get("id") == null) {
					actorsInfoList.add("no id available");
				} else {
					actorsInfoList.add((String) data.get("id"));
				}
				if (data.get("title") == null) {
					actorsInfoList.add("no title available");
				} else {
					actorsInfoList.add((String) data.get("title"));
				}
				if (data.get("image") == null) {
					actorsInfoList.add("no image available");
				} else {
					actorsInfoList.add((String) data.get("image"));
				}
				if (data.get("description") == null) {
					actorsInfoList.add("no description available");
				} else {
					actorsInfoList.add((String) data.get("description"));
				}
				tmdbId = getTMDBId(actorsInfoList.getFirst().toString());

				for (int i = 0; i < tmdbId.size(); i++) {
					actorsInfoList.add(tmdbId.get(i).toString());
				}

				tmdbDOB = getDOBInfo(tmdbId.get(0));

				for (int i = 0; i < tmdbDOB.size(); i++) {
					actorsInfoList.add(tmdbDOB.get(i).toString());
				}

			}
		} catch (Exception e) {

			System.out.println(e.getMessage().toString() + " error ");
		}

		return new PersonDetailModel(actorsInfoList);
	}

	/*
	 * This function provides the list of movies depending upon the Genre type
	 * passed as input parameter.
	 */
	private String personResource(String name) {
		String actorCode = "";
		try {
			InputStream input = new URL(
					"http://imdb.wemakesites.net/api/search?q="
							+ URLEncoder.encode(name, "UTF-8")).openStream();

			Map<String, Object> response = toMapObject(IOUtils.toString(input,
					"UTF-8"));
			if (response == null) {
				System.out.println("Response is null!!");
			} else {
				Map<String, Object> data = (Map<String, Object>) response
						.get("data");
				Map<String, Object> results = (Map<String, Object>) data
						.get("results");
				List<Map<String, Object>> names = (ArrayList<Map<String, Object>>) results
						.get("names");
				actorCode = names.get(0).get("id").toString();
			}

		} catch (Exception e) {

			System.out.println(e.getMessage().toString() + " error ");
		}

		return actorCode.toString();
	}

	/*
	 * This function provides the list of movies depending upon the Genre type
	 * passed as input parameter.
	 */
	private List<String> getTMDBId(String imdbId) {
		List<String> tmdbData = new ArrayList<String>();
		try {
			URL url = new URL(
					"https://api.themoviedb.org/3/find/"
							+ imdbId
							+ "?external_source=imdb_id&api_key=3eaf57ed7c6daae4f7ef9c460134ac0f");
			if (url == null) {
				System.out.println("url returned null");
				System.out.println("Url is  null!!");
				throw new NullPointerException();
			}
			System.out.println("Url is not null!!");
			InputStream input = url.openStream();

			Map<String, Object> response = toMapObject(IOUtils.toString(input,
					"UTF-8"));

			if (response == null) {
				System.out.println("Response is null!!");
				tmdbData.add("No ID Found");
				tmdbData.add("No Image");
				tmdbData.add("No Image");
				tmdbData.add("No Image");
			} else {

				List<Map<String, Object>> personResult = (ArrayList<Map<String, Object>>) response
						.get("person_results");
				if (personResult.get(0).get("id").toString() == null) {
					tmdbData.add("No ID Found");
				} else {
					tmdbData.add(personResult.get(0).get("id").toString());
				}
				List<Map<String, Object>> moviesResult = (ArrayList<Map<String, Object>>) personResult
						.get(0).get("known_for");

				if (moviesResult == null) {
					tmdbData.add("No Image");
				} else {
					for (int i = 0; i < 3; i++) {
						tmdbData.add("http://image.tmdb.org/t/p/w300"
								+ moviesResult.get(i).get("poster_path")
										.toString());
					}
				}
			}

		} catch (Exception e) {
			tmdbData.add("No ID Found");
			tmdbData.add("No Image");
			tmdbData.add("No Image");
			tmdbData.add("No Image");
			System.out.println(e.getMessage().toString()
					+ "Error in get TMDBID service");
		}
		return tmdbData;
	}

	/*
	 * This function get the DOB and poster of writers, directors and actors
	 * when tmdbId is passed as input parameter.
	 */
	private List<String> getDOBInfo(String tmdbId) {
		List<String> tmdbDOBData = new ArrayList<String>();
		tmdbDOBData.clear();
		try {
			InputStream input = new URL("http://api.themoviedb.org/3/person/"
					+ tmdbId + "?api_key=3eaf57ed7c6daae4f7ef9c460134ac0f")
					.openStream();

			Map<String, Object> response = toMapObject(IOUtils.toString(input,
					"UTF-8"));

			if (response == null) {
				System.out.println("Response is null!!");
				tmdbDOBData.add("NO DOB DATA");
				tmdbDOBData.add("No Popularity Data Available");
				tmdbDOBData.add("No Place of birth mention");
			} else {

				if (response.get("birthday").toString() == null) {
					tmdbDOBData.add("NO DOB DATA");
				} else {
					tmdbDOBData.add(response.get("birthday").toString());
				}
				if (response.get("popularity").toString() == null) {
					tmdbDOBData.add("No Popularity Data Available");
				} else {
					tmdbDOBData.add(response.get("popularity").toString());
				}
				if ((response.get("place_of_birth").toString()) == null) {
					tmdbDOBData.add("No Place of birth mention");
				} else {
					tmdbDOBData.add(response.get("place_of_birth").toString());
				}

			}

		} catch (Exception e) {
			tmdbDOBData.add("NO DOB DATA");
			tmdbDOBData.add("No Popularity Data Available");
			tmdbDOBData.add("No Place of birth mention");
			System.out.println(e.getMessage().toString()
					+ "Error in get TMDBID date of birth service");
		}

		return tmdbDOBData;
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

	/*
	 * This function is used to remove white-spaces and special characters.
	 */
	public static String toTrim(String name) {
		name = name.replace(" ", "");
		System.out.println(name.indexOf("("));
		if (name.contains("(")) {
			name = name.substring(0, name.indexOf("("));
		}
		if (name.contains(")")) {
			name = name.replace(")", "");
		}
		System.out.println(name);
		return name;
	}

	/*
	 * This is small lookup table for IMDB genre types.
	 */
	public enum GenreList {
		Action(28), Adventure(12), Animation(16), Comedy(35), Crime(80), Documentary(
				99), Drama(18), Family(10751), Fantasy(14), Foreign(10769), History(
				36), Horror(27), Music(10402), Mystery(9648), Romance(10749), SciFi(
				878), TVMovie(10770), Thriller(53), War(10752), Western(37);

		private int value;

		private GenreList(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	}

	/*
	 * This function return's the genre code when genre type is passed as input
	 * parameter.
	 */
	public static String getGenreCode(String genreName) {
		int result = 0;
		genreName = genreName.replace(" ", "");
		genreName = genreName.replace("-", "");
		switch (genreName) {
		case "Action":
			result = GenreList.Action.value;
			break;
		case "Adventure":
			result = GenreList.Adventure.value;
			break;
		case "Animation":
			result = GenreList.Animation.value;
			break;
		case "Comedy":
			result = GenreList.Comedy.value;
			break;
		case "Crime":
			result = GenreList.Crime.value;
			break;
		case "Documentary":
			result = GenreList.Documentary.value;
			break;
		case "Drama":
			result = GenreList.Drama.value;
			break;
		case "Family":
			result = GenreList.Family.value;
			break;
		case "Fantasy":
			result = GenreList.Fantasy.value;
			break;
		case "Foreign":
			result = GenreList.Foreign.value;
			break;
		case "History":
			result = GenreList.History.value;
			break;
		case "Horror":
			result = GenreList.Horror.value;
			break;
		case "Music":
			result = GenreList.Music.value;
			break;
		case "Mystery":
			result = GenreList.Mystery.value;
			break;
		case "Romance":
			result = GenreList.Romance.value;
			break;
		case "SciFi":
			result = GenreList.SciFi.value;
			break;
		case "RealityTV":
			result = GenreList.TVMovie.value;
			break;
		case "Thriller":
			result = GenreList.Thriller.value;
			break;
		case "War":
			result = GenreList.War.value;
			break;
		case "Western":
			result = GenreList.Western.value;
			break;
		default:
			result = 0;
			break;
		}
		return String.valueOf(result);
	}
}
