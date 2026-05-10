package com.aditya.EzCloudShare.service;


import com.aditya.EzCloudShare.document.ProfileDocument;
import com.aditya.EzCloudShare.dto.ProfileDTO;
import com.aditya.EzCloudShare.repository.ProfileRepository;
import com.mongodb.DuplicateKeyException;
import com.mongodb.MongoWriteException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ProfileService {
    private final ProfileRepository profileRepository;

    public ProfileDTO createProfile(ProfileDTO profileDTO){

        if(profileRepository.existsByClerkId(profileDTO.getClerkId())){
            return updateProfile(profileDTO);
        }

          ProfileDocument profile= ProfileDocument.builder()
                .clerkId(profileDTO.getClerkId())
                .email(profileDTO.getEmail())
                .firstName(profileDTO.getFirstName())
                .lastName(profileDTO.getLastName())
                .photoUrl(profileDTO.getPhotoUrl())
                .credits(50)
                .createdAt(Instant.now())
                .build();

        try {
            profile = profileRepository.save(profile);
        } catch (Exception e) {

            // 🔴 handle duplicate key (most important)
            if (e.getMessage() != null && e.getMessage().contains("duplicate key")) {
                System.out.println("Duplicate user detected, updating instead: " + profileDTO.getClerkId());
                return updateProfile(profileDTO);
            }

            // 🔴 rethrow other errors
            throw e;
        }

          return ProfileDTO.builder()
                  .id(profile.getId())
                  .clerkId(profile.getClerkId())
                  .email(profile.getEmail())
                  .firstName(profile.getFirstName())
                  .lastName(profile.getLastName())
                  .photoUrl(profile.getPhotoUrl())
                  .credits(profile.getCredits())
                  .createdAt(profile.getCreatedAt())
                  .build();


    }


    public ProfileDTO updateProfile(ProfileDTO profileDTO){
        ProfileDocument existingProfile= profileRepository.findByClerkId(profileDTO.getClerkId());

        if(existingProfile!=null){
            //Update fields if provided
            if(profileDTO.getEmail() !=null && !profileDTO.getEmail().isEmpty()){
                existingProfile.setEmail(profileDTO.getEmail());
            }

            if(profileDTO.getFirstName() !=null && !profileDTO.getFirstName().isEmpty()){
                existingProfile.setFirstName(profileDTO.getFirstName());

            }

            if(profileDTO.getLastName() !=null && !profileDTO.getLastName().isEmpty()){
                existingProfile.setLastName(profileDTO.getLastName());

            }

            if(profileDTO.getPhotoUrl() !=null && !profileDTO.getPhotoUrl().isEmpty()){
                existingProfile.setPhotoUrl(profileDTO.getPhotoUrl());

            }

            profileRepository.save(existingProfile);

            return ProfileDTO.builder()
                    .id(existingProfile.getId())
                    .email(existingProfile.getEmail())
                    .clerkId(existingProfile.getClerkId())
                    .firstName(existingProfile.getFirstName())
                    .lastName(existingProfile.getLastName())
                    .credits(existingProfile.getCredits())
                    .createdAt(existingProfile.getCreatedAt())
                    .photoUrl(existingProfile.getPhotoUrl())
                    .build();

        }
        return null;


    }

    public boolean existsByClerkId(String clerkId) {
        return profileRepository.existsByClerkId(clerkId);
    }

    public void deleteProfile(String clerkId){
        ProfileDocument existingProfile=profileRepository.findByClerkId(clerkId);
        if(existingProfile !=null){
            profileRepository.delete(existingProfile);
        }


    }
    public  ProfileDocument getCurrentProfile(){
        if(SecurityContextHolder.getContext().getAuthentication()==null){
            throw new UsernameNotFoundException("User not Authenticated");
        }
        String clerkId=SecurityContextHolder.getContext().getAuthentication().getName();
        return profileRepository.findByClerkId(clerkId);

    }
}
