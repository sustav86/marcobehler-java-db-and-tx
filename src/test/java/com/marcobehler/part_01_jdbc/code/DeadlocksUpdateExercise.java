package com.marcobehler.part_01_jdbc.code;

import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Anton Sustavov
 * @since 2020/03/08
 */
public class DeadlocksUpdateExercise {

    @Before
    public void setUp() {
        try (Connection connection = getConnection()) {
            createTables(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void deadlock_update_exercise() throws SQLException {
        System.out.println("Do we reach the end of the test without a " +
                "deadlock?...");
        try (Connection connectionFromJackBauer = getConnection()) {
            connectionFromJackBauer.setAutoCommit(false);
            connectionFromJackBauer.createStatement().execute("" +
                    "insert into items (name) values ('CTU Field Agent Report')");
            try (Connection connectionFromHabibMarwan = getConnection()) {
                connectionFromHabibMarwan.setAutoCommit(false);
                connectionFromHabibMarwan.createStatement().execute(
                        "update items set name = 'destroyed' where name = 'CTU Field Agent Report'");
//                connectionFromHabibMarwan.createStatement().execute(
//                        "delete from items where name = 'CTU Field Agent Report'");
                // do not forget to do the study drills!
                connectionFromHabibMarwan.commit();
            }
//            try (Connection connection = getConnection()) {
//                assertThat(getItemsCount(connection), equalTo(0));
//                assertThat(getItemName(connection), equalTo("destroyed"));
//            }
            connectionFromJackBauer.commit();
            try (Connection connection = getConnection()) {
                assertThat(getItemsCount(connection), equalTo(1));
                assertThat(getItemName(connection), equalTo("CTU Field Agent Report"));
            }
        }
        System.out.println("Yes!");
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

    private String getItemName(Connection connection) throws SQLException {
        // forget this for now, we simply want to know how many items
        // there are in the items table after rolling back
        ResultSet resultSet = connection.createStatement()
                .executeQuery("select name as name from items");
        resultSet.next();
        String name = resultSet.getString("name");
        System.out.println("Name in the items table: " + name);
        resultSet.close();
        return name;
    }

    private void createTables(Connection conn) {
        try {
            conn.createStatement().execute("create table bids (id " +
                    "identity, user VARCHAR, time TIMESTAMP ," +
                    " amount NUMBER, currency VARCHAR) ");
            conn.createStatement().execute("create table items (id " +
                    "identity, name VARCHAR unique)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    // simply what we did in OpenConnectionExerciseJava6/7.java
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:h2:mem:exercise_db;" +
                "DB_CLOSE_DELAY=-1");
    }
}
