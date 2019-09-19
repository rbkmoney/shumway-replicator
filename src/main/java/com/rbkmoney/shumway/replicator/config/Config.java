package com.rbkmoney.shumway.replicator.config;

import com.rbkmoney.damsel.accounter.AccounterSrv;
import com.rbkmoney.damsel.shumpune.MigrationHelperSrv;
import com.rbkmoney.woody.thrift.impl.http.THSpawnClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by vpankrashkin on 16.05.18.
 */
@Configuration
public class Config {

    @Bean
    public com.rbkmoney.damsel.shumpune.AccounterSrv.Iface shumpuneClient(@Value("${shumpune.target.uri}") String uri) throws URISyntaxException {
        return new THSpawnClientBuilder().withAddress(new URI(uri)).withNetworkTimeout(5000).build(com.rbkmoney.damsel.shumpune.AccounterSrv.Iface.class);
    }

    @Bean
    public MigrationHelperSrv.Iface shumpuneMigrationClient(@Value("${migration.target.uri}") String uri) throws URISyntaxException {
        return new THSpawnClientBuilder().withAddress(new URI(uri)).withNetworkTimeout(5000).build(MigrationHelperSrv.Iface.class);
    }

    @Bean
    public AccounterSrv.Iface shumwayClient(@Value("${shumway.target.uri}") String uri) throws URISyntaxException {
        return new THSpawnClientBuilder().withAddress(new URI(uri)).withNetworkTimeout(5000).build(AccounterSrv.Iface.class);
    }

    @Bean(name = "shumpuneDS")
    @ConfigurationProperties(prefix = "shumpune.datasource")
    public DataSource shumpuneDS() {
        return DataSourceBuilder.create().build();
    }

    @Primary
    @Bean(name = "shumwayDS")
    @ConfigurationProperties(prefix = "shumway.datasource")
    public DataSource shumwayDS() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    public AtomicLong lastReplicatedAccount() {
        return new AtomicLong(0);
    }

    @Bean
    public AtomicLong lastReplicatedPosting() {
        return new AtomicLong(0);
    }

}
