package org.xululabs.flickwiz.service;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(commandNames = "getMatch", commandDescription = "returns matching movies list")
public class CommandGetMatch {
    
    @Parameter(names = "--help", help = true, description = "Display help")
    private boolean help = false;
    
    @Parameter(names = "-i", description = "image path" ,required=true)
    private String imagePath;
    
    @Parameter(names = "-max", description = "set list limt" )
    private int max = 5;
    
    @Parameter(names = "-o", description = "path to output file")
    private String outputFile = "output.txt";
    
    
    public String getImagePath() {
        return imagePath;
    }
    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
    public int getMax() {
        return max;
    }
    public void setMax(int max) {
        this.max = max;
    }
    public String getOutputFile() {
        return outputFile;
    }
    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }
    
    public boolean isHelp() {
        return help;
    }
    public void setHelp(boolean help) {
        this.help = help;
    }
    
    
    @Override
    public String toString() {
        return "CommandGetMatch [imagePath=" + imagePath + ", max=" + max + ", outputFile=" + outputFile + "]";
    }
    
    
}
