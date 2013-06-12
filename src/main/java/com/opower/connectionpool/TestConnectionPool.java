package com.opower.connectionpool;

import java.sql.Connection;
import java.sql.SQLException;

public class TestConnectionPool{  
	 
    public static void main(String[] args) throws SQLException {  

    	DBConnectionPool dbPool = new DBConnectionPool();    	
    	dbPool.setDriver("org.sqlite.JDBC");
    	dbPool.setUrl("jdbc:sqlite:sample.db");  
    	
    	//it should be 3 used connection in pool
	    Connection cnn1 = dbPool.getConnection();  
	    Connection cnn2 = dbPool.getConnection();  
	    Connection cnn3 = dbPool.getConnection();  
	    dbPool.logConnectionPoolSummary();      

    	//it should be 3 cached connection in pool
  	    cnn1.close();
	    cnn2.close(); 
	    cnn3.close();  
	    dbPool.logConnectionPoolSummary(); 
	    
    	//it should be 2 cached connection and 1 used connection in pool
	    cnn2 = dbPool.getConnection();   
	    dbPool.logConnectionPoolSummary(); 	
	    
	    //it should be 2 cached connection in pool    
	    dbPool.releaseConnection(cnn2);
	    dbPool.logConnectionPoolSummary();  
	    
	    // it should be 0 connection in pool now
	    dbPool.release();
	    dbPool.logConnectionPoolSummary();	   
    }
}