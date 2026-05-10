package com.aditya.EzCloudShare.controller;

import com.aditya.EzCloudShare.dto.PaymentDTO;
import com.aditya.EzCloudShare.dto.PaymentVerificationDTO;
import com.aditya.EzCloudShare.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/payments")
public class PaymentController {
    private  final PaymentService paymentService;


    @PostMapping("/create-order")
    public ResponseEntity<?>createOrder(@RequestBody PaymentDTO paymentDTO){
        // call service method to create the order.
        PaymentDTO  response= paymentService.createOrder(paymentDTO);

        if(response.getSuccess()){
            return ResponseEntity.ok(response);
        }
        else return ResponseEntity.badRequest().body(response);

    }


    @PostMapping("/verify-payment")
    public ResponseEntity<?>verifyPayment(@RequestBody PaymentVerificationDTO request){
        // use service method
        PaymentDTO response=paymentService.verifyPayment(request);

        if(response.getSuccess()){
            return ResponseEntity.ok(response);
        }
        else{
            return ResponseEntity.badRequest().body(response);
        }



    }
}
