package org.xululabs.flickwiz.service;


import java.util.LinkedList;

public class PersonDetailModel {

	private LinkedList<String> personInfo;

	public PersonDetailModel() {

	}

	public PersonDetailModel(LinkedList<String> details) {
		super();
		this.personInfo = details;

	}

	public void setNames(LinkedList<String> details) {
		this.personInfo = details;
	}

	public LinkedList<String> getNames() {
		return personInfo;
	}
}
