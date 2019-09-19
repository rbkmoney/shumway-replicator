package com.rbkmoney.shumway.replicator.domain.verification;

import com.rbkmoney.shumway.replicator.domain.replication.Status;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AccountCheckResult {
    private Status status;
    private Long accountNumber;
    private Long totalAccountAmount;
    private List<Long> invalidAccounts;
}
