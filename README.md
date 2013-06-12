# Connection Pool Homework

This is the java codes for the connection pool homework assignment with design, coding, and unit/functional testing.

## Instructions

Please clone the repository and deliver your solution via an archive format of your choice, including all project files, within 1 calendar week.

Write a connection pool class that implements this interface (it is also located in `src/main/java/com/opower/connectionpool/ConnectionPool.java`):

    public interface ConnectionPool {
        java.sql.Connection getConnection() throws java.sql.SQLException;
        void releaseConnection(java.sql.Connection con) throws java.sql.SQLException;
    }

While we know there are many production-ready implementations of connection pools, this assignment allows for a variety of solutions to a real-world problem. 

## Using maven

It contains JUnit, EasyMock and Log4J 

    mvn compile      # compiles your code in src/main/java
    mvn test-compile # compile test code in src/test/java
    mvn test         # run tests in src/test/java for files named Test*.java


[maven]:http://maven.apache.org/

