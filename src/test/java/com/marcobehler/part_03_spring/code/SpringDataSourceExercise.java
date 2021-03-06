package com.marcobehler.part_03_spring.code;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Anton Sustavov
 * @since 2020/03/08
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringDataSourceExercise.MySpringConfig.class)
@SuppressWarnings("Duplicates") // for IntelliJ idea only
public class SpringDataSourceExercise {
    private static final TransactionDefinition TX_DEFAULTS = null;

    @Autowired // feel free to use javax.inject.@Inject here instead
    private DataSource ds;
    @Autowired
    private PlatformTransactionManager txManager;

    @Test
    public void exercise() {
        assertNotNull(ds);
        // and can get connections
        try (Connection connection = ds.getConnection()) {
            System.out.println("Yay, we have an open connection");
            assertTrue(connection.isValid(1000));
        } catch (Exception e) {
            fail(e.getMessage());
        }
        // and our tx manager who
        assertNotNull(txManager);

        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        // explicitly setting the transaction name is something that can only be done programmatically
        def.setName("SomeTxName");
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

        // spring's "low-level" way of programmatically opening up tx's...
//        TransactionStatus transaction =
//                txManager.getTransaction(TX_DEFAULTS);
        TransactionStatus transaction =
                txManager.getTransaction(def);
        System.out.println("And now an open transaction!");
        // ...and closing
        txManager.commit(transaction);
    }

    // our spring java-config
    @Configuration
    public static class MySpringConfig {
        @Bean
        public DataSource dataSource() {
            JdbcDataSource ds = new JdbcDataSource();
            ds.setURL("jdbc:h2:mem:exercise_db;DB_CLOSE_DELAY=-1");
            ds.setUser("sa");
            ds.setPassword("sa");
            return ds;
        }
        // there is different transaction managers, for different
        // scenarios (i.e. for hibernate etc.) But we are using a plain
        // datasource here, hence we need only a plain
        // DataSourceTransactionManager
        @Bean
        public PlatformTransactionManager txManager() {
            return new DataSourceTransactionManager(dataSource());
        }
    }
}
