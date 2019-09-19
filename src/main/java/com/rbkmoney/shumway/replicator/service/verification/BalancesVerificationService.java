package com.rbkmoney.shumway.replicator.service.verification;

import com.rbkmoney.damsel.accounter.Account;
import com.rbkmoney.damsel.accounter.AccounterSrv;
import com.rbkmoney.damsel.shumpune.Balance;
import com.rbkmoney.damsel.shumpune.Clock;
import com.rbkmoney.damsel.shumpune.LatestClock;
import com.rbkmoney.shumway.replicator.dao.ShumwayDAO;
import com.rbkmoney.shumway.replicator.domain.replication.Status;
import com.rbkmoney.shumway.replicator.domain.verification.AccountCheckResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.LongStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class BalancesVerificationService implements Runnable {

    private final com.rbkmoney.damsel.shumpune.AccounterSrv.Iface shumpuneClient;
    private final AccounterSrv.Iface shumwayClient;
    private final ShumwayDAO shumwayDAO;

    private List<Long> invalidAccounts = new ArrayList<>();
    private AtomicLong currentAcc = new AtomicLong(0);
    private Status status = Status.NOT_STARTED;
    private Long totalAccountsCount;

    @Override
    public void run() {
        log.info("Verification started");
        status = Status.IN_PROGRESS;
        totalAccountsCount = shumwayDAO.totalAccountsCount();
        LongStream.range(0, totalAccountsCount)
                .parallel()
                .forEach(this::verify);
        if (invalidAccounts.size() > 0) {
            status = Status.ERROR;
            log.error("Invalid accounts found: " + invalidAccounts);
        } else {
            status = Status.FINISHED;
        }
        log.info("Verification finished");
    }

    private void verify(long i) {
        try {
            if (!Thread.currentThread().isInterrupted()) {
                reset();
            }
            currentAcc.incrementAndGet();
            Balance balance = shumpuneClient.getBalanceByID(i, Clock.latest(new LatestClock()));
            Account account = shumwayClient.getAccountByID(i);
            if (!((balance.getOwnAmount() - balance.getMinAvailableAmount()) + (balance.getOwnAmount() - balance.getMaxAvailableAmount())
                    == (account.getOwnAmount() - account.getMinAvailableAmount()) + (account.getOwnAmount() - account.getMaxAvailableAmount()))) {
                log.warn("Invalid account number: {}\n" +
                                "shumpune own -  {} / min - {} / max - {}\n" +
                                "shumway own - {} / min - {} / max - {}.", i, balance.getOwnAmount(),
                        balance.getMinAvailableAmount(),
                        balance.getMaxAvailableAmount(),
                        account.getOwnAmount(),
                        account.getMinAvailableAmount(),
                        account.getMaxAvailableAmount());
                invalidAccounts.add(i);
            } else {
                log.info("Account valid: {}", i);
            }
        } catch (Throwable t) {
            log.error("Verification error, accNum: {}", i, t);
//                            throw new RuntimeException("Verification error", t);
        }
    }

    private void reset() {
        log.info("Verification reset");
        status = Status.NOT_STARTED;
        invalidAccounts.clear();
        currentAcc.set(0);
    }

    public AccountCheckResult getStatus() {
        return AccountCheckResult.builder()
                .status(status)
                .accountNumber(currentAcc.get())
                .totalAccountAmount(totalAccountsCount)
                .invalidAccounts(invalidAccounts)
                .build();
    }
}
