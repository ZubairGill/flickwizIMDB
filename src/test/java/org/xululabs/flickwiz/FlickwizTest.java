package org.xululabs.flickwiz;

import java.util.Map;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.xululabs.flickwiz.service.MyRestService;
/**
 * Unit test for simple App.
 */
public class FlickwizTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public FlickwizTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( FlickwizTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {
    	assert(true);
    }
       
    public void testToTrimSucess()
    {
    	String testValue="Stan Lee (Marvel comic book)";
    	String expectedValue="StanLee";
    	String resultValue=MyRestService.toTrim(testValue);   	
    	assertEquals(expectedValue,resultValue);
    }
    
    
    public void testGetGenreCodeSucess()
    {
    	String testValue="Action";
    	String expectedValue="28";
    	String resultValue=MyRestService.getGenreCode(testValue);   	
    	assertEquals(expectedValue,resultValue);
    }
    
    
    public void testToMapObject()
    {
    	String testValue="{\"name\":\"Mahesh\", \"age\":21}";
    	Map<String,Object> result=MyRestService.toMapObject(testValue);
    	  System.out.println(result.toString()); 	
    	String expectedValue="{name=Mahesh, age=21}";
    	  assertEquals(expectedValue,result.toString());
    }
    
    public void testPersonDetailService()
    {
    	String testValue="Brad pit";
    	
    }
    
    
}
