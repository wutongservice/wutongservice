package com.borqs.server.market.controllers;


import com.borqs.server.market.Errors;
import com.borqs.server.market.ServiceException;
import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.service.AccountService;
import com.borqs.server.market.utils.CC;
import com.borqs.server.market.utils.Params;
import com.borqs.server.market.utils.WebUtils2;
import com.borqs.server.market.utils.i18n.SpringMessage;
import com.borqs.server.market.utils.record.Record;
import com.borqs.server.market.utils.validation.ParamsSchema;
import org.apache.commons.lang.LocaleUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.util.Locale;
import java.util.Map;

import static com.borqs.server.market.utils.validation.Predicates.expression;
import static com.borqs.server.market.utils.validation.Predicates.notBlank;

@Controller
@RequestMapping("/")
public class AccountController extends AbstractController {

    protected AccountService accountService;
    protected LocaleResolver localeResolver;

    public AccountController() {
    }

    public AccountService getAccountService() {
        return accountService;
    }

    @Autowired
    @Qualifier("service.account")
    public void setAccountService(AccountService accountService) {
        this.accountService = accountService;
    }

    @Autowired
    @Qualifier("localeResolver")
    public void setLocaleResolver(LocaleResolver localeResolver) {
        this.localeResolver = localeResolver;
    }


    @RequestMapping(value = "/api/v2/account/active", method = {RequestMethod.GET, RequestMethod.POST})
    public APIResponse activeAccount(ServiceContext ctx, Params params) {
        final String[] allThirdPartyIdCols = {"google_id", "phone"};
        if (!params.hasAnyParams(allThirdPartyIdCols))
            throw new ServiceException(Errors.E_ILLEGAL_PARAM, "Missing third-party id");

        Record r = accountService.activeAccount(ctx, params);
        return APIResponse.of(r);
    }

    private static final ParamsSchema signoutSchema = new ParamsSchema()
            .required("ticket", "string", notBlank());

    @RequestMapping(value = "/api/v2/account/signout", method = {RequestMethod.GET, RequestMethod.POST})
    public APIResponse signout(ServiceContext ctx, Params params) {
        params = signoutSchema.validate(params);
        Record r = accountService.signout(ctx, params.param("ticket").asString());
        return APIResponse.of(r);
    }

    @RequestMapping(value = "/api/v2/account/self", method = {RequestMethod.GET, RequestMethod.POST})
    public APIResponse getSelf(ServiceContext ctx) {
        if (!ctx.hasAccountId())
            throw new ServiceException(Errors.E_ACCOUNT, "Missing account");

        Record r = accountService.getAccount(ctx, ctx.getAccountId(), false);
        if (r == null)
            throw new ServiceException(Errors.E_ACCOUNT, "Account is not exists");

        return APIResponse.of(r);
    }

    private static final ParamsSchema setPasswordSchema = new ParamsSchema()
            .required("password", "string", notBlank());
    @RequestMapping(value = "/api/v2/account/set_password", method = {RequestMethod.GET, RequestMethod.POST})
    public APIResponse setPassword(ServiceContext ctx, Params params) {
        params = setPasswordSchema.validate(params);
        if (!ctx.hasAccountId())
            throw new ServiceException(Errors.E_PERMISSION, "Must login");

        String password = params.param("password").asString();
        Record r = accountService.setPassword(ctx, ctx.getAccountId(), password, false);
        return APIResponse.of(r);
    }

    // pages
    private static final ParamsSchema signinPageSchema = new ParamsSchema()
            .required("username", "Missing", notBlank())
            .required("password", "Missing", notBlank());

    @RequestMapping(value = "/signin", method = {RequestMethod.GET, RequestMethod.POST})
    public ModelAndView signinPage(HttpServletRequest req, HttpServletResponse resp, ServiceContext ctx, Params params) {
        if (WebUtils2.isPostBack(req)) {
            ParamsSchema.ValidateResult vr = signinPageSchema.validateQuietly(params);
            if (vr.error()) {
                return new ModelAndView("signin", vr.getErrorModelForForm(params, "username", "password"));
            } else {
                String username = params.param("username").asString();
                String password = params.param("password").asString();
                try {
                    Record r = accountService.signin(ctx, username, password);
                    WebUtils2.addCookie(resp, CookieNames.TICKET, r.asString("ticket"));
                    String localeStr = params.param("locale").asString("");
                    if (!StringUtils.isEmpty(localeStr)) {
                        localeResolver.setLocale(req, resp, LocaleUtils.toLocale(localeStr));
                    }
                    return redirect("/publish");
                } catch (ServiceException e){
                    return new ModelAndView("signin", CC.map(
                            "username=>", params.param("username").asString(""),
                            "passwordErrorClass=>", "error"
                    ));
                }
            }
        } else {
            if (ctx.hasAccountId()) {
                return redirect("/");
            } else {
                return new ModelAndView("signin");
            }
        }
    }


    @RequestMapping(value = "/signout", method = {RequestMethod.GET, RequestMethod.POST})
    public ModelAndView signoutPage(HttpServletResponse resp, ServiceContext ctx) {
        if (ctx.hasAccountId()) {
            accountService.signout(ctx, ctx.getTicket());
        }
        WebUtils2.deleteCookie(resp, CookieNames.TICKET);
        return redirect("/signin");
    }


    private static final ParamsSchema signupPageSchema = new ParamsSchema()
            .required("email", "Missing", notBlank())
            .required("password", "Missing", notBlank())
            .required("repassword", "Missing", notBlank());
    @RequestMapping(value = "/signup", method = {RequestMethod.GET, RequestMethod.POST})
    public ModelAndView signupPage(HttpServletRequest req, ServiceContext ctx, Params params) {
        if (WebUtils2.isPostBack(req)) {
            ParamsSchema.ValidateResult vr = signupPageSchema.validateQuietly(params);
            if (!vr.ok()) {
                Map<String, Object> r = vr.getErrorModelForForm(params, "email", "password", "repassword");
                return new ModelAndView("signup", r);
            } else {
                String email = params.param("email").asString();
                String password = params.param("password").asString();
                String repwd = params.param("repassword").asString();
                if (!password.equals(repwd)) {
                    return new ModelAndView("signup", CC.map(
                            "password_errorClass=>", "error",
                            "repassword_errorClass=>", "error",
                            "repassword_errorMessage=>", SpringMessage.get("signup.text.repasswordErrorMessage", ctx),
                            "email=>", email
                    ));
                }

                String name = StringUtils.substringBefore(email, "@");
                Record account = new Record()
                        .set("name", name)
                        .set("password", password)
                        .set("email", email);

                try {
                    accountService.signup(ctx, account);
                    return redirect("/signin");
                } catch (Exception e) {
                    return new ModelAndView("signup", CC.map(
                            "email_errorClass=>", "error",
                            "email_errorMessage=>", SpringMessage.get("signup.text.emailErrorMessage", ctx)
                    ));
                }
            }
        }
        return new ModelAndView("signup");
    }
}
