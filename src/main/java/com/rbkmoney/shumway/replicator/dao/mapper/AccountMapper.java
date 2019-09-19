package com.rbkmoney.shumway.replicator.dao.mapper;

import com.rbkmoney.shumway.replicator.domain.Account;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class AccountMapper implements RowMapper<Account> {

    @Override
    public Account mapRow(ResultSet rs, int i) throws SQLException {
        long id = rs.getLong(1);
        String currSymCode = rs.getString(2);
        Instant creationTime = rs.getObject(3, LocalDateTime.class).toInstant(ZoneOffset.UTC);
        String description = rs.getString(4);
        return new Account(id, creationTime, currSymCode, description);
    }
}
