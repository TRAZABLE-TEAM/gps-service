package mx.trazable.gps.core.exception;


import lombok.experimental.StandardException;

import java.util.List;

@StandardException
public class PermissionException extends RuntimeException {
    private Integer userId;
    private Integer orgId;
    private List<Integer> actions;

    public PermissionException(Integer userId, Integer sedeId, Integer orgId, List<Integer> actions) {
        super(String.format("Permission Denied {userId: %s, sedeId: %s, orgId: %s, actions: %s, message: null}", userId, sedeId, orgId, actions));
        this.userId = userId;
        this.orgId = orgId;
        this.actions = actions;
    }

    public PermissionException(String message, Integer userId,Integer sedeId, Integer orgId, List<Integer> actions) {
        super(String.format("Permission Denied {userId: %s, sedeId: %s, orgId: %s, actions: %s, message: %s}", userId,sedeId, orgId, actions, message));
        this.userId = userId;
        this.orgId = orgId;
        this.actions = actions;
    }

    public PermissionException() {
        this((String)null, (Throwable)null);
    }

    public PermissionException(final String message) {
        this(message, (Throwable)null);
    }

    public PermissionException(final Throwable cause) {
        this(cause != null ? cause.getMessage() : null, cause);
    }

    public PermissionException(final String message, final Throwable cause) {
        super(message);
        if (cause != null) {
            super.initCause(cause);
        }

    }
}
