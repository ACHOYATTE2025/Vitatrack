package com.portfolio.VistaTrack.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class MdpConfig {

     /**
     * BCrypt password encoder used to hash passwords before storing them in Firestore.
     * BCrypt automatically handles salting — never store plain-text passwords.
     *
     * @return BCryptPasswordEncoder with default strength (10 rounds)
     */

    @Bean
    public PasswordEncoder passwordEncoder(){
    return  new BCryptPasswordEncoder();

}

}
