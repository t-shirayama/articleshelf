package com.articleshelf.infrastructure.ogp;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Set;

class OgpRequestGuard {
    private static final Set<String> BLOCKED_HOSTS = Set.of("localhost", "localhost.localdomain");
    private static final Set<String> BLOCKED_METADATA_IPS = Set.of(
            "169.254.169.254",
            "100.100.100.200"
    );

    URI validate(String url) {
        try {
            return validate(URI.create(url));
        } catch (IllegalArgumentException exception) {
            throw new UnsafeOgpUrlException();
        }
    }

    URI validate(URI uri) {
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            throw new UnsafeOgpUrlException();
        }
        if (uri.getUserInfo() != null) {
            throw new UnsafeOgpUrlException();
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new UnsafeOgpUrlException();
        }
        String normalizedHost = stripTrailingDot(host.toLowerCase(Locale.ROOT));
        if (BLOCKED_HOSTS.contains(normalizedHost)) {
            throw new UnsafeOgpUrlException();
        }
        validateResolvedAddresses(normalizedHost);
        return uri;
    }

    private void validateResolvedAddresses(String host) {
        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (isBlocked(address)) {
                    throw new UnsafeOgpUrlException();
                }
            }
        } catch (UnknownHostException exception) {
            throw new UnsafeOgpUrlException();
        }
    }

    private boolean isBlocked(InetAddress address) {
        return address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isSiteLocalAddress()
                || address.isLinkLocalAddress()
                || address.isMulticastAddress()
                || isUniqueLocalIpv6(address)
                || BLOCKED_METADATA_IPS.contains(address.getHostAddress());
    }

    private boolean isUniqueLocalIpv6(InetAddress address) {
        byte[] bytes = address.getAddress();
        return bytes.length == 16 && (bytes[0] & 0xfe) == 0xfc;
    }

    private String stripTrailingDot(String value) {
        return value.endsWith(".") ? value.substring(0, value.length() - 1) : value;
    }

    static class UnsafeOgpUrlException extends RuntimeException {
    }
}
