package com.opower.connectionpool;

import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.SQLException;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.opower.connectionpool.DBConnectionPool;

public class TestDBConnectionPool {

	private static DBConnectionPool dbPool;
	private static String url ="jdbc:sqlite:sample.db";
	private static String jdbcDriver ="org.sqlite.JDBC";
	private static Connection cnn; 
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
    	dbPool = new DBConnectionPool();
    	dbPool.setDriver(jdbcDriver);
    	dbPool.setUrl(url); 	
		cnn = dbPool.getConnection();  
		Connection cnn2 = dbPool.getConnection(); 
		cnn2.close(); 
	}
	
	@Test
	public void testGetCachedConnectionCount() {
		assertEquals(1, dbPool.getCachedConnectionCount());	
	}

	@Test
	public void testGetUsedConnectionCount() {
		assertEquals(1, dbPool.getUsedConnectionCount());
	}

	@Test
	public void testGetConnectionCount() {
		assertEquals(2, dbPool.getConnectionCount());	
	}

	@Test
	public void testGetConnection() throws Exception {
		cnn = dbPool.getConnection();  
		assertEquals(0, dbPool.getCachedConnectionCount());	
		assertEquals(2, dbPool.getUsedConnectionCount());	
		assertEquals(2, dbPool.getConnectionCount());
	}

	@Test
	public void testClose() throws SQLException {
		cnn.close();
		assertEquals(1, dbPool.getCachedConnectionCount());	
		assertEquals(1, dbPool.getUsedConnectionCount());
		assertEquals(2, dbPool.getConnectionCount());
	}
	
	@Test
	public void testGetUpSizeConnectionCount() {
		assertEquals(1, dbPool.getUpSizeConnectionCount());
	}

	@Test
	public void testGetDownSizeConnectionCount() {
		assertEquals(0, dbPool.getDownSizeConnectionCount());
	}
	
	@Test
	public void testReleaseConnection() throws Exception  {
		cnn = dbPool.getConnection();  
		dbPool.releaseConnection(cnn);
		assertEquals(0, dbPool.getCachedConnectionCount());	
		assertEquals(1, dbPool.getUsedConnectionCount());
		assertEquals(1, dbPool.getConnectionCount());		
	}
	
    @Test
    public void testReleaseConnection_null() throws Exception {
    	dbPool.releaseConnection(null);
    }
    
	@Test
	public void testReleaseConnection_Closed() throws Exception  {
		cnn = dbPool.getConnection();  
		cnn.close();	
		dbPool.releaseConnection(cnn);
	}
	
    @Test
    public void testReleaseConnection_ConnectionFromOtherSources() throws Exception {
    	Connection mockConnection =  EasyMock.createMock(Connection.class);  
    	EasyMock.replay(mockConnection);
    	dbPool.releaseConnection(mockConnection);
    	EasyMock.verify(mockConnection);
    }    
	
	@Test
	public void testRelease() throws Exception {
		dbPool.release();  
		assertEquals(0, dbPool.getCachedConnectionCount());	
		assertEquals(0, dbPool.getUsedConnectionCount());
		assertEquals(0, dbPool.getConnectionCount());		
	}	
	
	@Test
	public void testGetUrl() {
		assertEquals(url, dbPool.getUrl());
	}

	@Test
	public void testGetUser() {
		assertEquals("", dbPool.getUser());
	}

	@Test
	public void testGetPassword() {
		assertEquals("", dbPool.getPassword());
	}
}
