package com.akshara.mart.config;

import com.akshara.mart.model.User;
import com.akshara.mart.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AuthService {
    @Autowired UserRepo users;
    private final Map<String, Long> tokens = new ConcurrentHashMap<>();

    public static String hash(String pw) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(pw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public Map<String, Object> session(User u) {
        String t = UUID.randomUUID().toString();
        tokens.put(t, u.id);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("token", t);
        m.put("user", u);
        return m;
    }

    public User require(String token, String... roles) {
        if (token == null || !tokens.containsKey(token))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please login first");
        User u = users.findById(tokens.get(token))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please login again"));
        if (roles.length > 0 && !Arrays.asList(roles).contains(u.role))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to do this");
        return u;
    }
}
