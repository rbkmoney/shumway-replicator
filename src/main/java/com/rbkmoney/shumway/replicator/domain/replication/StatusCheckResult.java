package com.rbkmoney.shumway.replicator.domain.replication;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StatusCheckResult {
    private Status status;
    private Long postingPlanNumber;
    private Long totalPostingPlanAmount;
}
