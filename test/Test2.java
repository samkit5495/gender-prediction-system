/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.BufferedReader;
import java.io.FileReader;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author samkit5495
 */
public class Test2 {
    
    public Test2() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    // @Test
    // public void hello() {}@
    
    @Test
    public void test_data_column()
    {
         try{
            BufferedReader bf = new BufferedReader(new FileReader("testing_voice_data.csv"));
            assertEquals(21, bf.readLine().split(",").length);
        }
        catch(Exception e){}
    }
}
