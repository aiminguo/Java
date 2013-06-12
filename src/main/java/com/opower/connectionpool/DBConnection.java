package com.opower.connectionpool;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.sql.Connection;

public class DBConnection implements InvocationHandler {
	public Connection connection = null;  

	private static final String MethodNameClose = "close";  
	private Connection stubConnection = null;  
	private boolean isRelease = false;
	
	@Override
	public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {  
		Object obj = null;  
		if ( !isRelease && m.getName().equals(MethodNameClose) ) {  
			DBConnectionPool.closeConnection(this);  
		}  
		else {  
			obj = m.invoke(stubConnection, args);  
		}  
		isRelease = false;
		return obj;  
	}  
	 
	DBConnection(Connection cnn) {  
		this.connection = (Connection) Proxy.newProxyInstance(  
		cnn.getClass().getClassLoader(),  
		cnn.getClass().getInterfaces(), this);  
		stubConnection = cnn;  
	}  
	
	void release() throws SQLException{  
		isRelease = true;
		stubConnection.close();  
	}  
}
