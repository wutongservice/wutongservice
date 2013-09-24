package com.borqs.server.market.controllers.filevalidators;


import java.io.File;

public interface ProductFileValidator {
    int OK = 1;
    int ERROR = 2;
    int SKIP = 3;

    ValidateResult validate(File f);

    public static class ValidateResult {
        public final int status;
        public final String message;

        private ValidateResult(int status, String message) {
            this.status = status;
            this.message = message;
        }

        public static ValidateResult ok() {
            return new ValidateResult(OK, null);
        }

        public static ValidateResult skip() {
            return new ValidateResult(SKIP, null);
        }

        public static ValidateResult error(String message) {
            return new ValidateResult(ERROR, message);
        }
    }
}
