package com.aditya.EzCloudShare.service;

import com.aditya.EzCloudShare.document.PaymentTransaction;
import com.aditya.EzCloudShare.document.ProfileDocument;
import com.aditya.EzCloudShare.dto.PaymentDTO;
import com.aditya.EzCloudShare.dto.PaymentVerificationDTO;
import com.aditya.EzCloudShare.repository.PaymentTransactionRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.Formatter;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final ProfileService profileService;
    private final UserCreditsService userCreditsService;
    private final PaymentTransactionRepository paymentTransactionRepository;



    @Value("${razorpay.key.id}")
    private String razorpayKeyId;
    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;


    public PaymentDTO createOrder(PaymentDTO paymentDTO){
        try {
            ProfileDocument currentProfile=profileService.getCurrentProfile();
            String clerkId=currentProfile.getClerkId();

            RazorpayClient razorpayClient=new RazorpayClient(razorpayKeyId,razorpayKeySecret);
            JSONObject orderRequest=new JSONObject();
            orderRequest.put("amount",paymentDTO.getAmount());
            orderRequest.put("currency",paymentDTO.getCurrency());
            orderRequest.put("receipt","order_"+System.currentTimeMillis());

            Order order =razorpayClient.orders.create(orderRequest);
            String orderId=order.get("id");


            // Create pending transaction record
            PaymentTransaction transaction=PaymentTransaction.builder()
                    .clerkId(clerkId)
                    .orderId(orderId)
                    .planId(paymentDTO.getPlanId())
                    .amount(paymentDTO.getAmount())
                    .currency(paymentDTO.getCurrency())
                    .status("PENDING")
                    .transactionDate(LocalDateTime.now())
                    .userEmail(currentProfile.getEmail())
                    .userName(currentProfile.getFirstName()+" "+currentProfile.getLastName())
                    .build();

            paymentTransactionRepository.save(transaction);

            return PaymentDTO.builder()
                    .orderId(orderId)
                    .success(true)
                    .message("Order created successfully")
                    .build();



        } catch (Exception e) {
            return PaymentDTO.builder()
                    .success(false)
                    .message("Error creating order:"+e.getMessage())
                    .build();
        }

    }

    public PaymentDTO verifyPayment(PaymentVerificationDTO request){
        try {
            ProfileDocument currentProfile=profileService.getCurrentProfile();
            String clerkId=currentProfile.getClerkId();

            String data=request.getRazorpay_order_id()+"|" +request.getRazorpay_payment_id();
            String generatedSignature=generateHmacSha256Signature(data, razorpayKeySecret);

            if(!generatedSignature.equals(request.getRazorpay_signature())){
                updateTransactionStatus(request.getRazorpay_order_id(),"FAILED",request.getRazorpay_payment_id(),null);
                return PaymentDTO.builder()
                        .success(false)
                        .message("payment signature verification failed")
                        .build();
            }

            //Add credits based on plan.
            int creditsToAdd=0;
            String plan="BASIC";
            switch (request.getPlanId()){
                case "premium":
                    creditsToAdd=500;
                    plan="PREMIUM";
                    break;
                case "ultimate":
                    creditsToAdd=5000;
                    plan="ULTIMATE";
                    break;
            }
            if(creditsToAdd>0){
                userCreditsService.addCredits(clerkId,creditsToAdd,plan);
                updateTransactionStatus(request.getRazorpay_order_id(),"SUCCESS",request.getRazorpay_payment_id(),creditsToAdd);
                return PaymentDTO.builder()
                        .success(true)
                        .message("Payment verified and credits added successfully")
                        .credits(userCreditsService.getUserCredits(clerkId).getCredits())
                        .build();

            }else{
                updateTransactionStatus(request.getRazorpay_order_id(),"FAILED", request.getRazorpay_payment_id(), null);
                return PaymentDTO.builder()
                        .success(false)
                        .message("Invalid plan selected")
                        .build();
            }

        } catch (Exception e) {
            try {
                updateTransactionStatus(request.getRazorpay_order_id(),"ERROR", request.getRazorpay_payment_id(), null);

            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            return PaymentDTO.builder()
                    .success(false)
                    .message("Error verifying payment "+e.getMessage())
                    .build();
        }
    }

    private void updateTransactionStatus(String razorpayOrderId, String status, String razorpayPaymentId, Integer creditsToAdd) {
        paymentTransactionRepository.findAll().stream()
                .filter(t->t.getOrderId() !=null && t.getOrderId().equals(razorpayOrderId))
                .findFirst()
                .map(transaction->{
                    transaction.setStatus(status);
                    transaction.setPaymentId(razorpayPaymentId);
                    if(creditsToAdd!=null) {
                        transaction.setCreditsAdded(creditsToAdd);
                    }
                    return paymentTransactionRepository.save(transaction);

                })
                .orElse(null);


    }

    //Generate HMAC SHA256 signature for payment verification

    private String generateHmacSha256Signature(String data, String secret) {
        try {
            if (data == null || secret == null) {
                throw new IllegalArgumentException("Data and secret must not be null");
            }

            final String HMAC_SHA256 = "HmacSHA256";

            SecretKeySpec secretKey = new SecretKeySpec(
                    secret.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    HMAC_SHA256
            );

            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(secretKey);

            byte[] rawHmac = mac.doFinal(
                    data.getBytes(java.nio.charset.StandardCharsets.UTF_8)
            );

            return toHexString(rawHmac);

        } catch (Exception e) {
            throw new RuntimeException("Error generating HMAC SHA256 signature", e);
        }
    }

    private String toHexString(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);

        for (byte b : bytes) {
            String s = Integer.toHexString(0xff & b);
            if (s.length() == 1) {
                hex.append('0');
            }
            hex.append(s);
        }

        return hex.toString();
    }


}
