package com.rbkmoney.shumway.replicator.controller;

import com.rbkmoney.shumway.replicator.domain.verification.AccountCheckResult;
import com.rbkmoney.shumway.replicator.service.verification.VerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/verify")
@RequiredArgsConstructor
public class VerificationController {

    private final VerificationService verificationService;

    @GetMapping("/start")
    public void start() {
        verificationService.start();
    }

    @GetMapping("/status")
    public AccountCheckResult status() {
        return verificationService.status();
    }

    @GetMapping("/reset")
    public void reset() {
        verificationService.reset();
    }


}
