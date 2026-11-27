package com.bql.feedback.security;

import com.bql.feedback.model.UserAccount;
import com.bql.feedback.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Runner tạm chạy 1 lần lúc khởi động để chuyển mật khẩu plain-text cũ trong user_accounts sang BCrypt.
 * Sau khi chạy thành công và xác nhận đăng nhập OK, hãy xóa class này.
 */
@Component
public class PasswordMigrationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PasswordMigrationRunner.class);

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordMigrationRunner(UserAccountRepository userAccountRepository,
                                   PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        List<UserAccount> toUpdate = new ArrayList<>();
        for (UserAccount account : userAccountRepository.findAll()) {
            String password = account.getPassword();
            if (password != null && !password.isBlank() && !isBcryptHash(password)) {
                account.setPassword(passwordEncoder.encode(password));
                toUpdate.add(account);
            }
        }

        if (!toUpdate.isEmpty()) {
            userAccountRepository.saveAll(toUpdate);
            log.info("PasswordMigrationRunner: đã mã hóa BCrypt cho {} tài khoản cũ.", toUpdate.size());
        } else {
            log.info("PasswordMigrationRunner: không có tài khoản plain-text nào cần mã hóa.");
        }
    }

    private boolean isBcryptHash(String password) {
        return password.startsWith("$2a$")
                || password.startsWith("$2b$")
                || password.startsWith("$2y$");
    }
}
