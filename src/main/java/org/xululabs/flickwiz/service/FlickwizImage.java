package org.xululabs.flickwiz.service;
import java.io.Serializable;


public class FlickwizImage implements Serializable{
    
    private static final long serialVersionUID = 4317057191246587355L;
    
    private String name;
    private String ext;
    private String size;
    private String base64Code;
    
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getExt() {
        return ext;
    }
    public void setExt(String ext) {
        this.ext = ext;
    }
    public String getSize() {
        return size;
    }
    public void setSize(String size) {
        this.size = size;
    }
    public String getBase64Code() {
        return base64Code;
    }
    public void setBase64Code(String base64Code) {
        this.base64Code = base64Code;
    }
    
    @Override
    public String toString(){
        return "name["+name+"], ext["+ext+"], size["+size+"]";
    }
}
