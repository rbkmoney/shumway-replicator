package com.rbkmoney.shumway.replicator.service.replication;


import com.rbkmoney.damsel.shumpune.MigrationHelperSrv;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.shumway.replicator.dao.ShumwayDAO;
import com.rbkmoney.shumway.replicator.domain.Account;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.rbkmoney.shumway.replicator.service.replication.ReplicatorService.SEQ_CHECK_STALING;
import static com.rbkmoney.shumway.replicator.service.replication.ReplicatorService.executeCommand;

/**
 * Created by vpankrashkin on 19.06.18.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class AccountReplicatorService implements Runnable {
    private static final int BATCH_SIZE = 1000;
    private static final int STALING_TIME = 500;

    private final ShumwayDAO dao;
    private final MigrationHelperSrv.Iface client;
    private final AtomicLong lastReplicatedAccount;

    @Override
    public void run() {
        log.info("Start account replicator from id: {}", lastReplicatedAccount.get());
        try {
            while (!Thread.currentThread().isInterrupted()) {
                List<Account> accounts = executeCommand(() -> dao.getAccounts(lastReplicatedAccount.get(), BATCH_SIZE), lastReplicatedAccount, STALING_TIME);
                if (accounts.isEmpty()) {
                    log.info("Awaiting new accounts on: {}", lastReplicatedAccount.get());
                    Thread.sleep(STALING_TIME * 20);
                } else {
                    if (!validateAccountSequence(accounts)) {
                        log.warn("Sequence not validated, awaiting for continuous range on: {}", lastReplicatedAccount.get());
                        Thread.sleep(STALING_TIME);
                        continue;
                    }
                    log.info("Extracted {} new accounts [{}, {}]", accounts.size(), accounts.get(0).getId(), accounts.get(accounts.size() - 1).getId());
                    migrateAccountsWithRetry(accounts);
                    lastReplicatedAccount.set(accounts.get(accounts.size() - 1).getId());
                }
            }
        } catch (InterruptedException e) {
            log.warn("Account replicator interrupted");
        } catch (Throwable t) {
            log.error("Account replicator error", t);
            throw new RuntimeException("Account replicator error", t);
        } finally {
            log.info("Stop account replicator on: {}", lastReplicatedAccount.get());
        }
    }

    boolean validateAccountSequence(List<Account> accounts) {
        long border = accounts.get(accounts.size() - 1).getId();
        long distance = border - lastReplicatedAccount.get();

        if (distance != accounts.size()) {
            log.warn("Gaps in account sequence range: [{}, {}], distance: {}", border, lastReplicatedAccount.get(), distance);
            Instant lastCreationTime = accounts.get(accounts.size() - 1).getCreationTime();
            if (lastCreationTime.plusMillis(SEQ_CHECK_STALING).isBefore(Instant.now())) {
                log.warn("Last time in account pack:{} is old enough, seq check staled [continue]", lastCreationTime);
                return true;
            } else {
                log.warn("Last time in account pack: {} isn't old enough, seq check failed [await]", lastCreationTime);
                return false;
            }
        }
        return true;
    }

    @Retryable(maxAttempts = 50)
    public void migrateAccountsWithRetry(List<Account> accounts) throws TException {
        try {
            client.migrateAccounts(accounts.stream().map(this::convertToThrift).collect(Collectors.toList()));
        } catch (Throwable e) {
            log.error("Error in migration accounts", e);
            throw e;
        }
    }

    com.rbkmoney.damsel.shumpune.Account convertToThrift(Account acc) {
        return new com.rbkmoney.damsel.shumpune.Account()
                .setId(acc.getId())
                .setCreationTime(TypeUtil.temporalToString(acc.getCreationTime()))
                .setDescription(acc.getDescription())
                .setCurrencySymCode(acc.getCurrSymCode());
    }
}
