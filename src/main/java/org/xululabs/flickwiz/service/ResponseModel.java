package org.xululabs.flickwiz.service;

import java.net.URL;
import java.util.LinkedList;

public class ResponseModel {

	
	private LinkedList<String> names;
	private LinkedList<URL> urls;
	private LinkedList<LinkedList<String>> IMDBDetials;
	
	public ResponseModel()
	{
		
	}
	
	public ResponseModel(LinkedList<String> names, LinkedList<URL> urls, LinkedList<LinkedList<String>> iMDBDetials) {
		super();
		this.names = names;
		this.urls = urls;
		IMDBDetials = iMDBDetials;
	}


	public LinkedList<LinkedList<String>> getIMDBDetials() {
		return IMDBDetials;
	}


	public void setIMDBDetials(LinkedList<LinkedList<String>> iMDBDetials) {
		IMDBDetials = iMDBDetials;
	}


	public LinkedList<String> getNames() {
		return names;
	}

	public void setNames(LinkedList<String> names) {
		this.names = names;
	}

	public LinkedList<URL> getUrls() {
		return urls;
	}

	public void setUrls(LinkedList<URL> urls) {
		this.urls = urls;
	}


	

}
