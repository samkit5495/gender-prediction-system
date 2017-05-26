/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author aish
 */
public class Test5 {
    
    public Test5() {
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
    // public void hello() {}
    @Test
    public void check_after_delete()
    {
    try{
              
                Configuration con=HBaseConfiguration.create();
                HBaseAdmin admin=new HBaseAdmin(con); 
                operations o=new operations();
                String table_name="test";
                o.create_table(table_name);
                o.delete_table(table_name);
                assertEquals(false,admin.isTableAvailable(table_name));
                            
          }
          catch(Exception e)
          {
          
          }}
}
