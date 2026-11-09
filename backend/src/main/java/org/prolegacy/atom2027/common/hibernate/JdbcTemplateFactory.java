package org.prolegacy.atom2027.common.hibernate;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JdbcTemplateFactory implements DisposableBean {
    public record ConnectionParams(
            String url,
            String username,
            String password,
            String driverClassName
    ) {
        String cacheKey() {
            return url + "\u0000" + username + "\u0000" + password + "\u0000" + driverClassName;
        }
    }

    private final Map<String, HikariDataSource> pools = new ConcurrentHashMap<>();

    public JdbcTemplate jdbcTemplate(ConnectionParams params) {
        return new JdbcTemplate(dataSource(params));
    }

    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(ConnectionParams params) {
        return new NamedParameterJdbcTemplate(dataSource(params));
    }

    public HikariDataSource dataSource(ConnectionParams params) {
        return pools.computeIfAbsent(params.cacheKey(), _ -> buildPool(params));
    }

    protected HikariDataSource buildPool(ConnectionParams params) {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(params.url());
        ds.setUsername(params.username());
        ds.setPassword(params.password());
        if (params.driverClassName() != null) {
            ds.setDriverClassName(params.driverClassName());
        }
        ds.setPoolName("dyn-" + Integer.toHexString(params.cacheKey().hashCode()));
        ds.setMaximumPoolSize(5);
        ds.setMinimumIdle(0);
        ds.setConnectionTimeout(10_000);
        ds.setIdleTimeout(60_000);
        return ds;
    }

    public void evict(ConnectionParams params) {
        HikariDataSource ds = pools.remove(params.cacheKey());
        if (ds != null) ds.close();
    }

    @Override
    public void destroy() {
        pools.values().forEach(HikariDataSource::close);
        pools.clear();
    }
}
