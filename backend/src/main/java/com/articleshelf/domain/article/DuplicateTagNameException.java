package com.articleshelf.domain.article;

public class DuplicateTagNameException extends RuntimeException {
    public enum Reason {
        DUPLICATE_NAME,
        MERGE_TARGET_SAME
    }

    private final Reason reason;

    public DuplicateTagNameException(String name) {
        this(Reason.DUPLICATE_NAME, "tag already exists: " + name);
    }

    private DuplicateTagNameException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public static DuplicateTagNameException mergeTargetSame() {
        return new DuplicateTagNameException(Reason.MERGE_TARGET_SAME, "merge target must be different");
    }

    public Reason getReason() {
        return reason;
    }
}
