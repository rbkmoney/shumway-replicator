package com.rbkmoney.shumway.replicator.service.replication;

import com.rbkmoney.damsel.shumpune.MigrationHelperSrv;
import com.rbkmoney.damsel.shumpune.MigrationPostingPlan;
import com.rbkmoney.damsel.shumpune.Operation;
import com.rbkmoney.shumway.replicator.dao.ShumwayDAO;
import com.rbkmoney.shumway.replicator.domain.PostingLog;
import com.rbkmoney.shumway.replicator.domain.PostingOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Created by vpankrashkin on 19.06.18.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostingReplicatorService implements Runnable {

    private static final int BATCH_SIZE = 1500;
    private static final int STALING_TIME = 500;

    private final ShumwayDAO dao;
    private final MigrationHelperSrv.Iface shumpuneMigrationClient;
    private final AtomicLong lastReplicatedAccount;
    private final AtomicLong lastReplicatedPosting;

    @Override
    public void run() {
        log.info("Start posting replicator from id: {}", lastReplicatedPosting);
        try {

            while (!Thread.currentThread().isInterrupted()) {
                log.info("Get postings from id: {}", lastReplicatedPosting);
                List<PostingLog> postingLogs = dao.getPostingLogs(lastReplicatedPosting.get(), BATCH_SIZE);
                if (postingLogs.isEmpty()) {
                    log.info("Awaiting new postings on: {}", lastReplicatedPosting);
                    Thread.sleep(STALING_TIME * 20);

                } else {
                    if (!validateAccountCoherence(postingLogs)) {
                        log.warn("Posting replication is moving faster than accounts one, awaiting on: {}", lastReplicatedPosting);
                        Thread.sleep(STALING_TIME);
                        continue;
                    }
                    log.info("Extracted {} new postings [{}, {}]", postingLogs.size(), postingLogs.get(0).getId(), postingLogs.get(postingLogs.size() - 1).getId());
                    List<MigrationPostingPlan> migrationPostingPlans = postingLogs.stream()
                            .map(this::convertToProto)
                            .collect(Collectors.toList());
                    migratePostingPlansWithRetry(migrationPostingPlans);

                    lastReplicatedPosting.set(postingLogs.get(postingLogs.size() - 1).getId());
                }
            }
        } catch (InterruptedException e) {
            log.warn("Posting replicator interrupted");
        } catch (Throwable t) {
            log.error("Posting replicator error", t);
            throw new RuntimeException("Posting replicator error", t);
        } finally {
            log.info("Stop posting replicator on: {}", lastReplicatedPosting);
        }
    }

    boolean validateAccountCoherence(List<PostingLog> postingLogs) {
        for (PostingLog postingLog : postingLogs) {
            long lastAccId = lastReplicatedAccount.get();
            if (postingLog.getFromAccountId() > lastAccId || postingLog.getToAccountId() > lastAccId) {
                log.warn("Posting contains account id more than replicated: {}, {}", lastAccId, postingLog);
                return false;
            }
        }
        return true;
    }

    MigrationPostingPlan convertToProto(PostingLog postingLog) {
        return new MigrationPostingPlan(postingLog.getPlanId(),
                postingLog.getBatchId(),
                postingLog.getFromAccountId(),
                postingLog.getToAccountId(),
                postingLog.getAmount(),
                postingLog.getCurrSymCode(),
                postingLog.getDescription(),
                postingLog.getCreationTime().toString(),
                getOperation(postingLog)
        );
    }

    private Operation getOperation(PostingLog postingLog) {
        PostingOperation operation = postingLog.getOperation();
        switch (operation) {
            case HOLD:
                return Operation.HOLD;
            case COMMIT:
                return Operation.COMMIT;
            case ROLLBACK:
                return Operation.ROLLBACK;
            default:
                throw new RuntimeException();
        }
    }

    @Retryable(maxAttempts = 50)
    public void migratePostingPlansWithRetry(List<MigrationPostingPlan> migrationPostingPlans) throws TException {
        try {
            shumpuneMigrationClient.migratePostingPlans(migrationPostingPlans);
        } catch (Throwable e) {
            log.error("Error in migration accounts", e);
            throw e;
        }
    }
}
