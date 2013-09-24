package com.borqs.server.market.controllers.filevalidators;


import java.io.File;

public class AniHomeThemeValidator implements ProductFileValidator {

    public AniHomeThemeValidator() {
    }

    @Override
    public ValidateResult validate(File f) {
        // TODO: ...
        return ValidateResult.ok();
    }
}
