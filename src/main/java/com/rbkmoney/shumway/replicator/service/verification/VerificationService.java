package com.rbkmoney.shumway.replicator.service.verification;

import com.rbkmoney.shumway.replicator.domain.verification.AccountCheckResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationService {

    private final BalancesVerificationService balancesVerificationService;

    private Thread verificationThread;
    public void start() {
        verificationThread = new Thread(balancesVerificationService);
        verificationThread.start();
    }

    public AccountCheckResult status() {
        return balancesVerificationService.getStatus();
    }

    public void reset() {
        verificationThread.interrupt();
    }
}
