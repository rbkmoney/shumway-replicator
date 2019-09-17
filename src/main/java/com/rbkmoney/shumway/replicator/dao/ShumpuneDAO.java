package com.rbkmoney.shumway.replicator.dao;

import com.rbkmoney.shumway.replicator.dao.mapper.AccountMapper;
import com.rbkmoney.shumway.replicator.dao.mapper.PostingLogMapper;
import com.rbkmoney.shumway.replicator.domain.Account;
import com.rbkmoney.shumway.replicator.domain.PostingLog;
import com.rbkmoney.shumway.replicator.exception.DAOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.NestedRuntimeException;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;

/**
 * Created by vpankrashkin on 11.05.18.
 */
@Component
public class ShumpuneDAO extends JdbcDaoSupport  {
    private static final String sqlAcc = "SELECT id, curr_sym_code, creation_time, description FROM shm.account WHERE id > ? order by id limit ?";
    private static final String sqlPst = "select id, plan_id, batch_id, from_account_id, to_account_id, creation_time, amount, curr_sym_code, operation, description from shm.posting_log where id > ? order by id limit ?";

    private final AccountMapper accountMapper = new AccountMapper();
    private final PostingLogMapper postingLogMapper = new PostingLogMapper();

    @Autowired
    public ShumpuneDAO(DataSource shumpuneDS) {
        setDataSource(shumpuneDS);
    }

    public Long totalAccountsCount() throws DAOException {
        try {
            return getJdbcTemplate().queryForObject("SELECT COUNT(*) FROM shm.account", Long.class);
        } catch (NestedRuntimeException e) {
            throw new DAOException(e);
        }
    }

    public Long totalPostingsCount() throws DAOException {
        try {
            return getJdbcTemplate().queryForObject("SELECT COUNT(*) FROM shm.posting_log", Long.class);
        } catch (NestedRuntimeException e) {
            throw new DAOException(e);
        }
    }

    public List<Account> getAccounts(long fromId, int limit) throws DAOException {
        try {
            return getJdbcTemplate().query(sqlAcc, new Object[]{fromId, limit}, accountMapper);
        } catch (NestedRuntimeException e) {
            throw new DAOException(e);
        }
    }

    public List<PostingLog> getPostingLogs(long fromId, int limit) throws DAOException {
        try {
            return getJdbcTemplate().query(sqlPst, new Object[]{fromId, limit}, postingLogMapper);
        } catch (NestedRuntimeException e) {
            throw new DAOException(e);
        }
    }
}
