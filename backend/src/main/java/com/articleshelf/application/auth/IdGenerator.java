package com.articleshelf.application.auth;

import java.util.UUID;

public interface IdGenerator {
    UUID nextUuid();
}
