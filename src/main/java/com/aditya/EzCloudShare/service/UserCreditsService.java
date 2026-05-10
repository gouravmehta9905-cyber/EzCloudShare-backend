package com.aditya.EzCloudShare.service;

import com.aditya.EzCloudShare.document.UserCredits;
import com.aditya.EzCloudShare.repository.UserCreditsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserCreditsService {

    private final UserCreditsRepository userCreditsRepository;
    private final ProfileService profileService;

    public UserCredits createInitialCredits(String clerkId){
        UserCredits userCredits =UserCredits.builder()
                .clerkId(clerkId)
                .credits(50)
                .plan("BASIC")
                .build();
        return userCreditsRepository.save(userCredits);

    }
    public UserCredits getUserCredits(String clerkId){
        return userCreditsRepository.findByClerkId(clerkId)
                .orElseGet(()->createInitialCredits(clerkId));

    }

    public UserCredits getUserCredits(){
        String clerkId =profileService.getCurrentProfile().getClerkId();
        return getUserCredits(clerkId);
    }

    public Boolean hasEnoughCredits(int requiredCredits){
        UserCredits userCredits=getUserCredits();
        return userCredits.getCredits()>=requiredCredits;

    }

    @Transactional
    public UserCredits consumeCredit(int creditsToDeduct){
        String clerkId = profileService.getCurrentProfile().getClerkId();

        UserCredits userCredits = userCreditsRepository.findByClerkId(clerkId)
                .orElseThrow(() -> new RuntimeException("User credits not found"));

        if (userCredits.getCredits() < creditsToDeduct) {
            throw new RuntimeException("Insufficient credits");
        }

        userCredits.setCredits(userCredits.getCredits() - creditsToDeduct);

        return userCreditsRepository.save(userCredits);
    }

    @Transactional
    public UserCredits addCredits(String clerkId,Integer creditsToAdd,String plan){
         UserCredits userCredits=userCreditsRepository.findByClerkId(clerkId)
                .orElseGet(()->createInitialCredits(clerkId));
         userCredits.setCredits(userCredits.getCredits()+creditsToAdd);
         userCredits.setPlan(plan);
         return userCreditsRepository.save(userCredits);

    }
}
