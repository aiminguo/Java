package com.opower.connectionpool;

import java.io.File;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedList;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;


public class DBConnectionPool implements ConnectionPool {
	private static LinkedList<DBConnection> listCachedConnection = new LinkedList<DBConnection> ();  
	private static HashSet<DBConnection>  setUsedConnection = new HashSet<DBConnection> ();  
	private String url = "";  
	private String user = "";  
	private String password = "";  
	private static final Logger logger = LogManager.getLogger(DBConnectionPool.class.getName());
	private static final String ErrorStackPrefix = "!!!";
	
	private static long lastCheckedTime = System.currentTimeMillis(); 
	private static final long MaxConnection = 20;
	private static final long MaxCachedHour = 5; //5 hours or whatever
	private static final long MaxCachedTime = 60 * 60 * 1000 * MaxCachedHour;  
	
	public DBConnectionPool() {
		File log4jfile = new File("log4j.properties");
		PropertyConfigurator.configure(log4jfile.getAbsolutePath());	
	}

	/**  
	 * Set the JDBC driver   
	 */		
	public void setDriver(String jdbcDriver) {  
		try {  
			Driver driver = (Driver) Class.forName(jdbcDriver).newInstance(); 
			DriverManager.registerDriver(driver);  
		} catch (Exception e) {  
			logger.error(ErrorStackPrefix, e);
		}  
	}  

	/**  
	 * Request a connection from pool, and add it to used pool   
	 */	
	@Override
	public synchronized Connection getConnection() throws SQLException { 
		//Check if there are closed connections or too many connections 
		killConnection();  
		
		//try to get from cached pool first
		while (listCachedConnection.size() > 0) {  
			try {  
				DBConnection cnn = (DBConnection) listCachedConnection.removeFirst();  
				//double check to make sure connection is not closed
				if (cnn.connection.isClosed()) {  
					continue;  
				}  
				setUsedConnection.add(cnn);  
				return cnn.connection;  
			} catch (Exception e) {  
				logger.error(ErrorStackPrefix, e);
			}  
		}  
		
		//no cached connection in pool, request a new batch of connections to scale up
		int upSizeCount = getUpSizeConnectionCount();  
		LinkedList<DBConnection> list = new LinkedList<DBConnection>();  
		DBConnection dbCnn = null;  
		for (int i = 0; i < upSizeCount; i++) {  
			dbCnn = getNewConnection();  
			if (null != dbCnn) {  
				list.add(dbCnn);  
			}  
		} 
		
		//for whatever reason(memory/resource of web or db server) ,no more connections can be requested
		if (0 == list.size()) {  
			return null;  
		}  
		
		dbCnn = (DBConnection) list.removeFirst();  
		setUsedConnection.add(dbCnn);  
		
		//Add the rest to the cached pool
		listCachedConnection.addAll(list);  
		list.clear();  
		 
		return dbCnn.connection;  
	} 
	
	/**  
	 * Release the connection from pool, and removed it from used pool   
	 */	  
	@Override
	public synchronized void releaseConnection(Connection connection) throws SQLException {
		if (null == connection) {
			return;
		}
		//find the DBConnection in used pool
		DBConnection toBeRemovedDBConnection = null;
		for (DBConnection dbCnn : setUsedConnection) {
			if (connection == dbCnn.connection) {
				toBeRemovedDBConnection = dbCnn;
				break;
			}
		}
		
		//find the DBConnection in cached pool
		if (null == toBeRemovedDBConnection) {
			for (DBConnection dbCnn : listCachedConnection) {
				if (connection == dbCnn.connection) {
					toBeRemovedDBConnection = dbCnn;
					break;
				}
			}
		}
		
		if (null != toBeRemovedDBConnection) {
			setUsedConnection.remove(toBeRemovedDBConnection);
			toBeRemovedDBConnection.release();	
		} else {
			logger.warn("Connection is not found in pool, please investigate!");
		}
	}

	/**  
	 * remove the connection from used pool and add it to cached pool   
	 */	
	public static synchronized void closeConnection(DBConnection conn) { 
		boolean exist = setUsedConnection.remove(conn);  
		if (exist) {  
			listCachedConnection.addLast(conn);  
		}  
	}
	
	/**  
	 * release all the connections in pool   
	 */ 
	public int release() {  
		int count = 0;  
		//clean up Cached Connection  
		for (DBConnection dbCnn : listCachedConnection) { 
			try {  
				dbCnn.release();  
				count++;  
			} catch (Exception e) {  
				logger.error(ErrorStackPrefix, e);
			}  
		}  
		listCachedConnection.clear();  
		
		//clean up Used Connection
		for (DBConnection dbCnn : setUsedConnection) {
			try {  
				dbCnn.release();  
				count++;  
			} catch (Exception e) {  
				logger.error(ErrorStackPrefix, e);
			}  
		}
		setUsedConnection.clear();  		 
		return count;  
	}  
	 
	/**  
	 * get upsize connection count, not always add 1 at a time   
	 */ 
	public int getUpSizeConnectionCount() {  
		int count = 1;  
		int current = getConnectionCount();  
		count = current / 5;  
		if (count < 1) {  
			count = 1;  
		}  
		return count;  
	}  
	 
	/**  
	 * get downsize connection count, not always remove 1 at a time  
	 */ 
	public int getDownSizeConnectionCount() {  
		int current = getConnectionCount();  
		if (current < MaxConnection) {  
			return 0;  
		}  
		return 5;  
	}  
	 
	public synchronized void logConnectionPoolSummary() {  
		String summary = getConnectionPoolSummary();
		logger.info(summary);
		//System.out.println(summary);  
	}  
	
	/**  
	 * get the number of cached connection in pool   
	 */ 	 
	public synchronized int getCachedConnectionCount() {  
		return listCachedConnection.size();  
	}  
	 
	/**  
	 * get the number of used connection in pool   
	 */ 	 
	public synchronized int getUsedConnectionCount() {  
		return setUsedConnection.size();  
	}  
	 
	/**  
	 * get the total number of connection in pool   
	 */ 	
	public synchronized int getConnectionCount() {  
		return listCachedConnection.size() + setUsedConnection.size();  
	}  
	 
	public String getUrl() {  
		return url;  
	}  
	 
	public void setUrl(String url) {  
		if (null == url) {  
			return;  
		}  
		this.url = url.trim();  
	}  
	 
	public String getUser() {  
		return user;  
	}  
	 
	public void setUser(String user) {  
		if (null == user) {  
			return;  
		}  
		this.user = user.trim();  
	}  
	 
	public String getPassword() {  
		return password;  
	}  
	 
	public void setPassword(String password) {  
		if (null == password) {  
			return;  
		}  
		this.password = password.trim();  
	}
	
	private void killConnection() {  
		long time = System.currentTimeMillis();  
		 
		if (time < lastCheckedTime) {  
			time = lastCheckedTime;  
			return;  
		}  
		//no need to check very often  
		if (time - lastCheckedTime < MaxCachedTime) {  
			return;  
		}  
		lastCheckedTime = time;  
		 
		//begin check for the cached connection in pool
		for (DBConnection cnn : listCachedConnection) {    
			try {  
				if (cnn.connection.isClosed()) {  
					logger.warn("Closed connection in cached pool, please investigate!");
					listCachedConnection.remove(cnn);  
				}  
			} catch (Exception e) {  
				logger.error(ErrorStackPrefix, e);
				listCachedConnection.remove(cnn);  
			}  
		}  
	 
		//downsize the connection pool only required  
		int downSizeCount = getDownSizeConnectionCount();
		if (listCachedConnection.size() < downSizeCount) {  
			return;  
		}  else {
			logger.info("Release connection from cached pool.");
		}
		 
		for (int i = 0; i < downSizeCount; i++) {  
			DBConnection dbCnn = (DBConnection) listCachedConnection.removeFirst();  
			try {  
				dbCnn.release();  
			} catch (Exception e) {  
				logger.error(ErrorStackPrefix, e);
			}  
		}  
	}  	
	
	private DBConnection getNewConnection() {  
		try {  
			Connection cnn = null;
			if (null == user) { 
				cnn = DriverManager.getConnection(url); 
			} else {
				cnn = DriverManager.getConnection(url, user, password); 
			}
			logger.info("Request new DBConnection and add it to pool.");	
			DBConnection dbcnn = new DBConnection(cnn);  
			return dbcnn;  
			
		} catch (Exception e) {  
			logger.error(ErrorStackPrefix, e);
		}  
		return null;  
	} 	
	private synchronized String getConnectionPoolSummary() {  

		StringBuffer msg = new StringBuffer();  
		msg.append("Connetion Pool Summary:");  
		msg.append("\r\n\t");  
		msg.append("total count=>" + getConnectionCount());  
		msg.append("\t");  
		msg.append("Cached count=>" + getCachedConnectionCount());  
		msg.append("\t");  
		msg.append("used count=>" + getUsedConnectionCount());   
		msg.append("\r\n");  		
		return new String(msg);    
	}	
}  
 