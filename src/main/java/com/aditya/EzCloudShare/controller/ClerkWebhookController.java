package com.aditya.EzCloudShare.controller;

import com.aditya.EzCloudShare.dto.ProfileDTO;
import com.aditya.EzCloudShare.service.ProfileService;
import com.aditya.EzCloudShare.service.UserCreditsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
@Slf4j
public class ClerkWebhookController {

    @Value("${clerk.webhook.secret}")
    private String webhookSecret;

    private final ProfileService profileService;

    private  final UserCreditsService userCreditsService;

    @PostMapping("/clerk")
    public ResponseEntity<?> handleClerkWebhook(@RequestHeader("svix-id") String svixId,
                                                @RequestHeader("svix-timestamp") String svixTimestamp,
                                                @RequestHeader("svix-signature") String svixSignature,
                                                @RequestBody String payload){
        try{
            //log the webhook request for debugging
            log.info("🚀 Webhook endpoint HIT");
            log.info("Received webhooks from clerk with id:{}",svixId);
            log.info("Webhook payload: {}", payload);
            boolean isValid= verifyWebhookSignature( svixId, svixTimestamp,svixSignature,payload);
            if(!isValid){
                log.error("Invalid webhook signature");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid webhook signature ");

            }
            ObjectMapper mapper =new ObjectMapper();
            JsonNode rootNode=mapper.readTree(payload);
            String eventType=rootNode.path("type" ).asText();
            log.info("📌 Event type: {}", eventType);

            switch (eventType){
                case "user.created":
                    log.info("➡️ user.created event received");
                    handleUserCreated(rootNode.path("data"));
                    break;
                case "user.updated":
                    handleUserUpdated(rootNode.path("data"));
                    break;

                case "user.deleted":
                    handleUserDeleted(rootNode.path("data"));
                    break;
            }
            return ResponseEntity.ok().build();
        }catch ( Exception e){
            log.error("❌ Webhook processing failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Webhook failed: " + e.getMessage());



        }

    }

    private void handleUserDeleted(JsonNode data) {
        String clerkId=data.path("id").asText();

        profileService.deleteProfile(clerkId);
    }

    private void handleUserUpdated(JsonNode data) {
        String clerkId=data.path("id").asText();
        String email="";
        JsonNode emailAddresses=data.path("email_addresses");
        if(emailAddresses.isArray() && emailAddresses.size()>0){
            email=emailAddresses.get(0).path("email_address").asText();
        }
        String firstName=data.path("first_name").asText("");
        String lastName=data.path("last_name").asText("");
        String photoUrl=data.path("image_url").asText("");


        ProfileDTO updatedProfile= ProfileDTO.builder()
                .clerkId(clerkId)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .photoUrl(photoUrl)
                .build();
        updatedProfile=profileService.updateProfile(updatedProfile);

        if (updatedProfile==null){
            handleUserCreated(data);
        }
    }

    private void handleUserCreated(JsonNode data) {
        log.info("✅ handleUserCreated CALLED");
        String clerkId=data.path("id").asText();
        String email="";
        JsonNode emailAddresses=data.path("email_addresses");
        if(emailAddresses.isArray() && emailAddresses.size() >0){
            email=emailAddresses.get(0).path("email_address").asText();
        }
        if (email == null || email.trim().isEmpty()) {
            log.error("Email missing in Clerk payload for user: {}", clerkId);
            log.error("Payload data: {}", data.toPrettyString());

            throw new RuntimeException("Email is required but missing from Clerk webhook");
        }
        String firstName=data.path("first_name").asText("");
        String lastName=data.path("last_name").asText("");
        String photoUrl=data.path("image_url").asText("");

        ProfileDTO newProfile= ProfileDTO.builder()
                .clerkId(clerkId)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .photoUrl(photoUrl)
                .build();
        try {
            profileService.createProfile(newProfile);
            userCreditsService.createInitialCredits(clerkId);

            log.info("✅ User created successfully in DB: {}", clerkId);

        } catch (Exception e) {
            log.error("❌ Error saving user to DB for clerkId: {}", clerkId, e);
            throw e;
        }




    }

    private boolean verifyWebhookSignature(String svixId, String svixTimestamp, String svixSignature, String payload) {
        //validate the signature
        return true;
    }


}
