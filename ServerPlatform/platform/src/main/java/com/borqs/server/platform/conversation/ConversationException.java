package com.borqs.server.platform.conversation;


import com.borqs.server.platform.ErrorCode;
import com.borqs.server.platform.PlatformException;

public class ConversationException extends PlatformException {
    public ConversationException() {
        super(ErrorCode.CONVERSATION_ERROR);
    }

    public ConversationException(String format, Object... args) {
        super(ErrorCode.CONVERSATION_ERROR, format, args);
    }

    public ConversationException(Throwable cause, String format, Object... args) {
        super(ErrorCode.CONVERSATION_ERROR, cause, format, args);
    }

    public ConversationException(Throwable cause) {
        super(ErrorCode.CONVERSATION_ERROR, cause);
    }
}
