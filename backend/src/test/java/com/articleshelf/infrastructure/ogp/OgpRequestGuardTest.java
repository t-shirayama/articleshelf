package com.articleshelf.infrastructure.ogp;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OgpRequestGuardTest {
    private final OgpRequestGuard guard = new OgpRequestGuard();

    @Test
    void acceptsPublicHttpAndHttpsUrls() {
        assertThatCode(() -> guard.validate("http://93.184.216.34/article")).doesNotThrowAnyException();
        assertThatCode(() -> guard.validate("https://93.184.216.34/article")).doesNotThrowAnyException();
    }

    @Test
    void rejectsUnsupportedSchemesAndUserInfo() {
        assertThatThrownBy(() -> guard.validate("file:///etc/passwd"))
                .isInstanceOf(OgpRequestGuard.UnsafeOgpUrlException.class);
        assertThatThrownBy(() -> guard.validate("http://user:pass@93.184.216.34/"))
                .isInstanceOf(OgpRequestGuard.UnsafeOgpUrlException.class);
    }

    @Test
    void rejectsLocalhostAndLoopbackAddresses() {
        assertThatThrownBy(() -> guard.validate("http://localhost/article"))
                .isInstanceOf(OgpRequestGuard.UnsafeOgpUrlException.class);
        assertThatThrownBy(() -> guard.validate("http://127.0.0.1/article"))
                .isInstanceOf(OgpRequestGuard.UnsafeOgpUrlException.class);
        assertThatThrownBy(() -> guard.validate("http://[::1]/article"))
                .isInstanceOf(OgpRequestGuard.UnsafeOgpUrlException.class);
    }

    @Test
    void rejectsPrivateLinkLocalMulticastAndMetadataAddresses() {
        assertThatThrownBy(() -> guard.validate("http://10.0.0.1/article"))
                .isInstanceOf(OgpRequestGuard.UnsafeOgpUrlException.class);
        assertThatThrownBy(() -> guard.validate("http://172.16.0.1/article"))
                .isInstanceOf(OgpRequestGuard.UnsafeOgpUrlException.class);
        assertThatThrownBy(() -> guard.validate("http://192.168.0.1/article"))
                .isInstanceOf(OgpRequestGuard.UnsafeOgpUrlException.class);
        assertThatThrownBy(() -> guard.validate("http://169.254.169.254/latest/meta-data"))
                .isInstanceOf(OgpRequestGuard.UnsafeOgpUrlException.class);
        assertThatThrownBy(() -> guard.validate("http://224.0.0.1/article"))
                .isInstanceOf(OgpRequestGuard.UnsafeOgpUrlException.class);
        assertThatThrownBy(() -> guard.validate("http://[fc00::1]/article"))
                .isInstanceOf(OgpRequestGuard.UnsafeOgpUrlException.class);
    }
}
