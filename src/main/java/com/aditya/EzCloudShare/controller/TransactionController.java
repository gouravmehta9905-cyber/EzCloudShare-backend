package com.aditya.EzCloudShare.controller;

import com.aditya.EzCloudShare.document.PaymentTransaction;
import com.aditya.EzCloudShare.document.ProfileDocument;
import com.aditya.EzCloudShare.repository.PaymentTransactionRepository;
import com.aditya.EzCloudShare.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final ProfileService profileService;

    @GetMapping
    public ResponseEntity<?> getUserTransactions(){
        ProfileDocument currentProfile=profileService.getCurrentProfile();
        String clerkId=currentProfile.getClerkId();

        List<PaymentTransaction> transactions=paymentTransactionRepository.findByClerkIdAndStatusOrderByTransactionDateDesc(clerkId,"SUCCESS");
        return ResponseEntity.ok(transactions);
    }
}
