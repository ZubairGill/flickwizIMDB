package org.xululabs.flickwiz.service;

import java.util.ArrayList;
import java.util.LinkedList;

public class GenreModel {

	private ArrayList<LinkedList<String>> moviesInfoByGenre;

	public GenreModel() {

	}

	public GenreModel(ArrayList<LinkedList<String>> details) {
		super();
		this.moviesInfoByGenre = details;

	}

	public void setNames(ArrayList<LinkedList<String>> details) {
		this.moviesInfoByGenre = details;
	}

	public ArrayList<LinkedList<String>> getNames() {
		return moviesInfoByGenre;
	}
}
