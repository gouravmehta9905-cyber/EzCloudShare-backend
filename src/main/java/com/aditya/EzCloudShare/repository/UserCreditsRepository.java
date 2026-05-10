package com.aditya.EzCloudShare.repository;

import com.aditya.EzCloudShare.document.UserCredits;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserCreditsRepository extends MongoRepository<UserCredits,String> {

    Optional<UserCredits> findByClerkId(String clerkId);
}
