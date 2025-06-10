package com.example.attendancesystem;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordTest {

    @Test
    public void generatePasswordHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "admin123";
        String hash = encoder.encode(password);
        
        System.out.println("Password: " + password);
        System.out.println("Generated Hash: " + hash);
        System.out.println("Verification: " + encoder.matches(password, hash));
        
        // Test with existing hash
        String existingHash = "$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.";
        System.out.println("Existing Hash: " + existingHash);
        System.out.println("Existing Hash Verification: " + encoder.matches(password, existingHash));
    }
}
