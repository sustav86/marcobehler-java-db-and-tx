package com.marcobehler.part_03_spring.code;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static junit.framework.TestCase.fail;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(
        classes = ProgrammaticSpringExercise.MySpringConfig.class)
@SuppressWarnings("Duplicates") // for IntelliJ idea only
public class ProgrammaticSpringExercise {
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
    public void exercise() throws SQLException {
        Long balance = teller.getAccountBalance("Donald Trump");
        assertThat(balance, equalTo(Long.MAX_VALUE));
    }

    public static class BankTeller {
        @Autowired
        private PlatformTransactionManager txManager;
        @Autowired
        private DataSource ds;
        public Long getAccountBalance(final String name) throws SQLException {
            Long balance = new TransactionTemplate(txManager).execute(
                    new TransactionCallback<Long>() {
                        @Override
                        public Long doInTransaction(TransactionStatus status) {
                            System.out.println("This time we open up a " +
                                    "transaction with a TransactionTemplate");
                            // let's return the balance from a database table
                            Long balance = new JdbcTemplate(ds).queryForObject(
                                    "select balance from accounts " +
                                            "where name = ?", Long.class, name);
                            System.out.println("The balance for : " + name +
                                    " is:" + balance);
                            return balance;
                        }
                    });
            // or instead of using the transaction template, you could
            // use this to open and close tx
            //
            TransactionStatus tx = txManager.getTransaction(null);
            new JdbcTemplate(ds).update("insert into accounts values" + "('Donald Trump j'," + Long.MAX_VALUE + ")");
            txManager.commit(tx);
            return balance;
        }
    }

    private void createTables(Connection conn) {
        try {
            conn.createStatement().execute("create table if not exists " +
                    "account_activity "
                    + "(date_occurred date, account_holder VARCHAR," +
                    " description VARCHAR)");
            conn.createStatement().execute("create table if not exists " +
                    "accounts "
                    + "(name varchar primary key, balance bigint)");
            conn.createStatement().execute("insert into accounts values"
                    + "('Donald Trump'," + Long.MAX_VALUE + ")");
        } catch (SQLException e) {
            fail(e.getMessage());
        }


    }

    @Configuration
    @EnableTransactionManagement(proxyTargetClass = true)
    public static class MySpringConfig {
        @Bean(name = "bankTeller")
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
