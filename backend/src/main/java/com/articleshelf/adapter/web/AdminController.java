package com.articleshelf.adapter.web;

import com.articleshelf.application.auth.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AuthService authService;

    public AdminController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/users/{username}/password")
    @ResponseStatus(NO_CONTENT)
    public void resetUserPassword(
            @PathVariable String username,
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        authService.resetPasswordByAdmin(username, request.newPassword());
    }

    public record ResetPasswordRequest(@NotBlank @Size(min = 8, max = 128) String newPassword) {
    }
}
