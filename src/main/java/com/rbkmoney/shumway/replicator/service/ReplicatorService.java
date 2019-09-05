package com.rbkmoney.shumway.replicator.service;


import com.rbkmoney.damsel.shumpune.AccounterSrv;
import com.rbkmoney.shumway.replicator.dao.ShumwayDAO;
import com.rbkmoney.shumway.replicator.domain.replication.Status;
import com.rbkmoney.shumway.replicator.domain.replication.StatusCheckResult;
import com.rbkmoney.woody.api.flow.error.WUnavailableResultException;
import com.rbkmoney.woody.api.flow.error.WUndefinedResultException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PreDestroy;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by vpankrashkin on 11.05.18.
 */
@Slf4j
public class ReplicatorService {
    static final long SEQ_CHECK_STALING = 1000 * 60 * 5;

    private ShumwayDAO shumwayDao;

    private final AtomicLong lastReplicatedAccount = new AtomicLong(0);
    private final AtomicLong lastReplicatedPosting = new AtomicLong(0);
    private final Thread accountReplicator;
    private final Thread postingReplicator;

    private Status status = Status.NOT_STARTED;

    public ReplicatorService(ShumwayDAO dao, AccounterSrv.Iface shumpuneClient) {
        this.shumwayDao = dao;
        this.accountReplicator = new Thread(new AccountReplicatorService(dao, shumpuneClient, lastReplicatedAccount), "AccountReplicatorService");
        this.postingReplicator = new Thread(new PostingReplicatorService(dao, shumpuneClient, lastReplicatedAccount, lastReplicatedPosting), "PostingReplicatorService");
    }

    public void fire() {
        new Thread(() -> {
            log.info("Start replicator");
            try {
                status = Status.IN_PROGRESS;
                accountReplicator.start();
                postingReplicator.start();
            } catch (Throwable t) {
                status = Status.ERROR;
                log.error("ReplicatorService error", t);
                throw new RuntimeException("ReplicatorService error", t);
            } finally {
                log.info("Destroy replicator");
            }
        }).start();
    }

    @PreDestroy
    public void destroy() {
        postingReplicator.interrupt();
        accountReplicator.interrupt();
    }

    protected static  <T> T executeCommand(Callable<T> command, Object data, long stalingTime) throws Exception {
        while (true) {
            try {
                return command.call();
            } catch (WUndefinedResultException | WUnavailableResultException e) {
                log.warn("Temporary command error, retry", e);
                Thread.sleep(stalingTime);
            } catch (Exception e) {
                log.error("Failed to execute command with data: {}, retry", data);
                Thread.sleep(stalingTime);
            }
        }
    }

    public StatusCheckResult status() {
        return StatusCheckResult.builder()
                .status(status)
                .accountNumber(lastReplicatedAccount.get())
                .postingPlanNumber(lastReplicatedPosting.get())
                .totalAccountAmount(shumwayDao.totalAccountsCount())
                .totalPostingPlanAmount(shumwayDao.totalPostingsCount())
                .build();
    }
}
