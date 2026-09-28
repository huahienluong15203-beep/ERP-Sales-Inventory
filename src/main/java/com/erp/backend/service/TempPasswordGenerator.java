package com.erp.backend.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Sinh mật khẩu tạm 10 ký tự, luôn có cả chữ và số (khớp quy tắc mật khẩu của S1-04).
 * Bỏ các ký tự dễ nhầm: 0/O, 1/l/I.
 */
@Component
public class TempPasswordGenerator {

    private static final String LETTERS = "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String DIGITS = "23456789";
    private static final String ALL = LETTERS + DIGITS;
    private static final int LENGTH = 10;

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        char[] chars = new char[LENGTH];
        chars[0] = LETTERS.charAt(random.nextInt(LETTERS.length()));
        chars[1] = DIGITS.charAt(random.nextInt(DIGITS.length()));
        for (int i = 2; i < LENGTH; i++) {
            chars[i] = ALL.charAt(random.nextInt(ALL.length()));
        }
        // Xáo trộn để chữ và số không nằm cố định ở 2 vị trí đầu
        for (int i = LENGTH - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char tmp = chars[i];
            chars[i] = chars[j];
            chars[j] = tmp;
        }
        return new String(chars);
    }
}
