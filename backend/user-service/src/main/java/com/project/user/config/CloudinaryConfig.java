package com.project.user.config;
import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import java.util.Map;
@Configuration
public class CloudinaryConfig {
    @Bean Cloudinary cloudinary(@Value("${cloudinary.cloud_name}") String cloud,
            @Value("${cloudinary.api_key}") String key, @Value("${cloudinary.api_secret}") String secret) {
        return new Cloudinary(Map.of("cloud_name", cloud, "api_key", key, "api_secret", secret));
    }
}
