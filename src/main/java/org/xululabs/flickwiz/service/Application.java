package org.xululabs.flickwiz.service;

import java.io.File;
import java.net.URL;
import java.sql.Time;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import javax.print.attribute.standard.DateTimeAtCompleted;
import javax.xml.crypto.Data;

import org.springframework.format.datetime.joda.DateTimeParser;

import com.beust.jcommander.JCommander;

public class Application {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		try{
			
		while(true)
		{
			System.out.print("flickwiz >");
				String cmdInput = scanner.nextLine();	
				if(cmdInput==null || cmdInput.length()< 12)
				{
					continue;
				}
			System.out.println("given input: " + cmdInput);
			executeCommand(cmdInput.split(" "));	
		}
		}catch(Exception e)
		{
			e.printStackTrace();
		}

	}
	
	public static void executeCommand(String[] args)
	{
		long start=System.currentTimeMillis();
		
		JCommander jcommander = new JCommander();
		jcommander.addCommand(new CommandGetMatch());

		jcommander.parse(args);
		String parsedCommand = jcommander.getParsedCommand();

		CommandGetMatch commandGetMatch = (CommandGetMatch) jcommander.getCommands()
				.get(parsedCommand).getObjects().get(0);

		if (commandGetMatch.isHelp()) {
			jcommander.usage(parsedCommand);
			return;
		}

		// /////////////////// Query Images ////////////////////

		File queryImage = new File(commandGetMatch.getImagePath());
		System.out.println("Query Images Name : " + queryImage);

		if (queryImage.exists()) {

			MyRestService mService = new MyRestService();
			ResponseModel mResult = new ResponseModel();
			mResult = mService.getFeatureResult(queryImage);

			LinkedList<String> movieNames = new LinkedList<String>();
			movieNames = mResult.getNames();

			LinkedList<URL> movieUrl = new LinkedList<URL>();
			movieUrl = mResult.getUrls();

			System.out.println("---------- Movie Names Details-------------");
			for (int i = 0; i < movieNames.size(); i++) {

				System.out.println(i + "--" + " Movie Name : "
						+ movieNames.get(i).toString());
			}

			System.out.println("-----------Movie Url Details---------------");
			for (int i = 0; i < movieUrl.size(); i++) {

				System.out.println(i + "--" + " Movie URL : "
						+ movieUrl.get(i).toString());
			}

		} else {
			System.out.println("File Path is incorrect");
		}
		
		System.out.println("----------DONE-------------");
		long end=System.currentTimeMillis();
		long timeinSec=TimeUnit.MILLISECONDS.toSeconds(end-start);
		long timeinMil=end-start;
		System.out.println("Time taken by system is :"+ timeinSec+ "  sec  " +(timeinMil%1000)+ " milisecond.");
		
	}
}