package com.borqs.server.market.models;


import com.borqs.server.market.Errors;
import com.borqs.server.market.ServiceException;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;

import java.util.regex.Pattern;

public class ValidateUtils {

    private static final Pattern PACKAGE_PATTERN = Pattern.compile("(\\w|_)+(\\.(\\w|_)+)*");

    private static boolean validatePackageName(String s) {
        return s != null && PACKAGE_PATTERN.matcher(s).matches();
    }

    public static boolean validateAppId(String val) {
        return validatePackageName(val);
    }

    public static String checkAppId(String val, String errorMessage) {
        if (!validateAppId(val))
            throw new ServiceException(Errors.E_ILLEGAL_PARAM, errorMessage);
        return val;
    }

    public static boolean validateCategoryId(String val) {
        return validatePackageName(val);
    }

    public static String checkCategoryId(String val, String errorMessage) {
        if (!validateCategoryId(val))
            throw new ServiceException(Errors.E_ILLEGAL_PARAM, errorMessage);
        return val;
    }

    public static boolean validatePricetagId(String val) {
        return validatePackageName(val);
    }

    public static String checkPricetagId(String val, String errorMessage) {
        if (!validatePricetagId(val))
            throw new ServiceException(Errors.E_ILLEGAL_PARAM, errorMessage);
        return val;
    }


    public static boolean validateProductId(String val) {
        return validatePackageName(val) && !validateFileIdForUserShare(val);
    }

    public static String checkProductId(String val, String errorMessage) {
        if (!validateProductId(val))
            throw new ServiceException(Errors.E_ILLEGAL_PARAM, errorMessage);
        return val;
    }

    public static boolean validateFileIdForUserShare(String fileId) {
        return ProductIds.productIdIsUserShared(fileId);
    }

    public static String checkFileIdForUserShare(String fileId, String errorMessage) {
        if (!validateFileIdForUserShare(fileId))
            throw new ServiceException(Errors.E_ILLEGAL_PARAM, errorMessage);
        return fileId;
    }

    public static boolean validateProductVersion(String val) {
        try {
            int n = Integer.parseInt(val);
            return n > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static String checkProductVersion(String val, String errorMessage) {
        if (!validateProductVersion(val))
            throw new ServiceException(Errors.E_ILLEGAL_PARAM, errorMessage);
        return val;
    }

    public static boolean validateMultipleLocaleText(String json) {
        // TODO: ..
        return true;
    }

    public static boolean validateMultipleLocaleText(JsonNode jn) {
        // TODO: ...
        return true;
    }

    public static boolean validateMultipleLocalePrice(String json) {
        // TODO: ..
        return true;
    }

    public static boolean validateMultipleLocalePrice(JsonNode jn) {
        // TODO: ...
        return true;
    }

    public static boolean validateLocale(String val) {
        // TODO: ...
        return true;
    }

    public static String checkLocale(String val, String errorMessage) {
        if (!validateLocale(val))
            throw new ServiceException(Errors.E_ILLEGAL_PARAM, errorMessage);

        return val;
    }

}
