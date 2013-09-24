package com.borqs.server.market.controllers.filevalidators;


import com.borqs.server.market.utils.StringUtils2;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;


import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ProductFileValidators  {
    private final Map<AppIdWithCategory, ProductFileValidator> validators = new HashMap<AppIdWithCategory, ProductFileValidator>();

    public ProductFileValidators() {
    }

    public Map<AppIdWithCategory, ProductFileValidator> getValidators() {
        return validators;
    }

    public void setValidators(Map<AppIdWithCategory, ProductFileValidator> validators) {
        this.validators.clear();
        if (validators != null)
            this.validators.putAll(validators);
    }

    private static ProductFileValidator createValidator(String classname) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        return (ProductFileValidator) Class.forName(classname).newInstance();
    }

    public void setValidatorsExpression(String expr) throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        Map<AppIdWithCategory, ProductFileValidator> m = new HashMap<AppIdWithCategory, ProductFileValidator>();
        if (StringUtils.isNotBlank(expr)) {
            String[] entries = StringUtils2.splitArray(expr, ",", true);
            for (String entry : entries) {
                String appIdWithCategoryStr = StringUtils.substringBefore(entry, ":").trim();
                String validatorClassname = StringUtils.substringAfter(entry, ":").trim();
                String appId = StringUtils.substringBefore(appIdWithCategoryStr, "@").trim();
                String categoryId = StringUtils.substringAfter(appIdWithCategoryStr, "@").trim();
                ProductFileValidator validator = createValidator(validatorClassname);
                m.put(new AppIdWithCategory(appId, categoryId), validator);
            }
        }
        setValidators(m);
    }

    public ProductFileValidator.ValidateResult validate(String appId, String categoryId, File f) {
        ProductFileValidator validator = validators.get(new AppIdWithCategory(appId, categoryId));
        if (validator != null) {
            return validator.validate(f);
        } else {
            return ProductFileValidator.ValidateResult.skip();
        }
    }

    private static class AppIdWithCategory {
        public final String appId;
        public final String categoryId;

        private AppIdWithCategory(String appId, String categoryId) {
            this.appId = appId;
            this.categoryId = categoryId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            AppIdWithCategory that = (AppIdWithCategory) o;

            return StringUtils.equals(appId, that.appId)
                    && StringUtils.equals(categoryId, that.categoryId);
        }

        @Override
        public int hashCode() {
            int result = ObjectUtils.hashCode(appId);
            result = 31 * result + ObjectUtils.hashCode(categoryId);
            return result;
        }
    }
}
