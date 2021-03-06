package com.marcobehler.part_01_jdbc.code;

import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Anton Sustavov
 * @since 2020/03/08
 */
public class SavepointExercise {

    @Before
    public void setUp() {
        try (Connection connection = getConnection()) {
            createTables(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void savepoint_exercise() throws SQLException {
        try (Connection connectionFromJackBauer = getConnection()) {
            connectionFromJackBauer.setAutoCommit(false);
            // we start our transaction and immediately create a savepoint
            // you can create the savepoint any time you want and as many
            // as you want
//            Savepoint savepoint = connectionFromJackBauer.setSavepoint("mySavePoint");
            Savepoint savepoint = connectionFromJackBauer.setSavepoint();
            System.out.println("We just created a savepoint!");
            connectionFromJackBauer.createStatement().execute(
                    "insert into items " +
                            "(name) values ('CTU Field Agent Report')");
//            Savepoint savepoint2 = connectionFromJackBauer.setSavepoint("mySavePoint2");
            Savepoint savepoint2 = connectionFromJackBauer.setSavepoint();
            connectionFromJackBauer.createStatement().execute(
                    "insert into items " +
                            "(name) values ('Chloeys Items')");
            connectionFromJackBauer.rollback(savepoint2);
            connectionFromJackBauer.createStatement().execute(
                    "insert into items " +
                            "(name) values ('Nuclear Bomb')");
            connectionFromJackBauer.commit();
        }
        try (Connection connectionFromHabib = getConnection()) {
            int items = getItemsCount(connectionFromHabib);
            assertThat(items, equalTo(1)); // the nuclear bomb
        }
    }

    private int getItemsCount(Connection connection) throws SQLException {
        // forget this for now, we simply want to know how many items
        // there are in the items table after rolling back
        ResultSet resultSet = connection.createStatement()
                .executeQuery("select count(*) as count from items");
        resultSet.next();
        int count = resultSet.getInt("count");
        System.out.println("Items in the items table: " + count);
        resultSet.close();
        return count;
    }

    private void createTables(Connection conn) {
        try {
            conn.createStatement().execute("create table bids (id " +
                            "identity, user VARCHAR, time TIMESTAMP ," +
                    " amount NUMBER, currency VARCHAR) ");
            conn.createStatement().execute("create table items (id " +
                    "identity, name VARCHAR)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // simply what we did in OpenConnectionExerciseJava6/7.java
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:h2:mem:exercise_db;" +
                "DB_CLOSE_DELAY=-1;");
    }
}
