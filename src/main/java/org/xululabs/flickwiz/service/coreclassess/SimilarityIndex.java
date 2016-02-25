package org.xululabs.flickwiz.service.coreclassess;

import java.net.URL;

public class SimilarityIndex {
	
	private Double index;
	private URL url;
	private String name;
	
	public SimilarityIndex(){}
	public SimilarityIndex(Double index, URL url, String name){
		this.index = index;
		this.url = url;
		this.name = name;
	}
	public Double getIndex() {
		return index;
	}
	public void setIndex(Double index) {
		this.index = index;
	}
	public URL getUrl() {
		return url;
	}
	public void setUrl(URL url) {
		this.url = url;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public String toString(){
		return "index["+index+"], name["+name+"], url["+url+"]";
	}
}
