package com.sehatharian;

import com.sehatharian.util.PasswordUtil;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;
import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilTest {
    @Test void generatedHashIsVerifiedWithActualLibrary() {
        String hash = PasswordUtil.hashPassword("Temporary test secret");
        assertTrue(hash.startsWith("$2a$12$"));
        assertTrue(BCrypt.checkpw("Temporary test secret", hash));
        assertTrue(PasswordUtil.checkPassword("Temporary test secret", hash));
        assertFalse(PasswordUtil.checkPassword("different", hash));
    }
    @Test void supportedLegacyRevisionTwoAlsoVerifiesRatherThanCheckingPrefixOnly() {
        String salt = BCrypt.gensalt(4);
        String hash = BCrypt.hashpw(AuthFixtures.SECRET, "$2$" + salt.substring(4));
        assertTrue(PasswordUtil.checkPassword(AuthFixtures.SECRET, hash));
        assertFalse(PasswordUtil.checkPassword("incorrect", hash));
    }
    @Test void nullPasswordAndMalformedHashFailClosed() {
        assertFalse(PasswordUtil.checkPassword(null, AuthFixtures.HASH));
        assertFalse(PasswordUtil.checkPassword(AuthFixtures.SECRET, "$2a$04$"));
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hashPassword(null));
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hashPassword(""));
    }
}
