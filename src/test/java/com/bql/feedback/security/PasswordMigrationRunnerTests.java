package com.bql.feedback.security;

import com.bql.feedback.model.Role;
import com.bql.feedback.model.UserAccount;
import com.bql.feedback.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasswordMigrationRunnerTests {

    @Test
    void migratesOnlyPlainTextPasswordsAndSkipsExistingBcryptAndNull() {
        UserAccountRepository repository = mock(UserAccountRepository.class);
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        PasswordMigrationRunner runner = new PasswordMigrationRunner(repository, encoder);

        String existingHash = encoder.encode("secret");
        UserAccount plainAcc = new UserAccount(
                1L, "an@gmail.com", "123456", Role.CUSTOMER, 10L, null, true, LocalDateTime.now());
        UserAccount hashedAcc = new UserAccount(
                2L, "manager@bql.vn", existingHash, Role.CS_MANAGER, null, 5L, true, LocalDateTime.now());
        UserAccount nullPwdAcc = new UserAccount(
                3L, "broken", null, Role.CUSTOMER, 11L, null, false, LocalDateTime.now());

        when(repository.findAll()).thenReturn(List.of(plainAcc, hashedAcc, nullPwdAcc));

        runner.run();

        assertNotEquals("123456", plainAcc.getPassword());
        assertTrue(plainAcc.getPassword().startsWith("$2a$") || plainAcc.getPassword().startsWith("$2b$"));
        assertTrue(encoder.matches("123456", plainAcc.getPassword()));

        assertEquals(existingHash, hashedAcc.getPassword());
        assertNull(nullPwdAcc.getPassword());
        verify(repository).saveAll(List.of(plainAcc));
    }

    @Test
    void skipsSaveAllWhenAllPasswordsAlreadyHashed() {
        UserAccountRepository repository = mock(UserAccountRepository.class);
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        PasswordMigrationRunner runner = new PasswordMigrationRunner(repository, encoder);

        UserAccount hashedAcc = new UserAccount(
                1L, "manager@bql.vn", encoder.encode("123456"), Role.CS_MANAGER, null, 5L, true, LocalDateTime.now());
        when(repository.findAll()).thenReturn(List.of(hashedAcc));

        runner.run();

        verify(repository, never()).saveAll(anyList());
    }
}
