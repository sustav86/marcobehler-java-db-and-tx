package com.marcobehler.part_03_spring.code;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static junit.framework.TestCase.fail;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import javax.swing.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;

/**
 * @author Anton Sustavov
 * @since 2020/03/13
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(
        classes = LoggingExercise.MySpringConfig.class)
@SuppressWarnings("Duplicates") // for IntelliJ idea only
public class LoggingExercise {
    @Autowired
    private BankTeller teller;
    @Autowired
    private DataSource ds;
    @Before
    public void setUp() {
        try (Connection connection = ds.getConnection()) {
            createTables(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    @Test
    public void exercise() {
        // the getAccountBalance call happens in a transaction
        Long balance = teller.getAccountBalance("Donald Trump");
        assertThat(balance, equalTo(Long.MAX_VALUE));
    }
    public static class BankTeller {
        @Autowired
        private DataSource ds;
        // here spring opens up a connection + tx
        // Propagation.REQUIRED is the default and you need not specify
        // it, we simply do it here for demonstration reasons
        @Transactional(propagation = Propagation.REQUIRED)
        public Long getAccountBalance(String name) {
            // let's return the balance from a database table
            Long balance = new JdbcTemplate(ds).queryForObject(
                    "select balance from accounts " +
                            "where name = ?", Long.class, name);
            System.out.println("The balance for : " + name + " is: " +
                    balance);
            // but as good banks, we also note down every access to the
            // bank
            // account , so we save the access in an account activity table
            MapSqlParameterSource params =
                    new MapSqlParameterSource("date_occurred", new Date());
            params.addValue("description", "Get Account Balance");
            new SimpleJdbcInsert(ds).withTableName("account_activity")
                    .execute(params);
            return balance;
        }
        // and once we leave that method , spring closes the connection
        // = tx
    }

    private void createTables(Connection conn) {
        try {
            conn.createStatement().execute("create table if not exists " +
                    "accounts "
                    + "(name varchar primary key, balance bigint)");
            conn.createStatement().execute("create table if not exists " +
                    "account_activity "
                    + "(date_occurred date, description VARCHAR," +
                    " name varchar, " +
                    "foreign key (name) references accounts(name))");
            conn.createStatement().execute("insert into accounts values"
                    + "('Donald Trump'," + Long.MAX_VALUE + ")");
        } catch (SQLException e) {
            fail(e.getMessage());
        }
    }

    @Configuration
    @EnableTransactionManagement(proxyTargetClass = true)
    public static class MySpringConfig {
        @Bean
        public BankTeller teller() {
            return new BankTeller();
        }
        @Bean
        public DataSource dataSource() {
            JdbcDataSource ds = new JdbcDataSource();
            ds.setURL("jdbc:h2:mem:exercise_db;DB_CLOSE_DELAY=-1");
            ds.setUser("sa");
            ds.setPassword("sa");
            return ds;
        }
        @Bean
        public PlatformTransactionManager txManager() {
            return new DataSourceTransactionManager(dataSource());
        }
    }

}
