package com.rbkmoney.shumway.replicator.dao.mapper;

import com.rbkmoney.shumway.replicator.domain.PostingLog;
import com.rbkmoney.shumway.replicator.domain.PostingOperation;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

public class PostingLogMapper implements RowMapper<PostingLog> {

    @Override
    public PostingLog mapRow(ResultSet rs, int rowNum) throws SQLException {
        long id = rs.getLong(1);
        String planId = rs.getString(2);
        long batchId = rs.getLong(3);
        long fromAccountId = rs.getLong(4);
        long toAccountId = rs.getLong(5);
        Instant creationTime = rs.getTimestamp(6).toInstant();
        long amount = rs.getLong(7);
        String currSymCode = rs.getString(8);
        PostingOperation operation = PostingOperation.getValueByKey(rs.getString(9));
        String description = rs.getString(10);
        return new PostingLog(id, planId, batchId, fromAccountId, toAccountId, amount, creationTime, operation, currSymCode, description);
    }
}