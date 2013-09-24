package com.borqs.server.market.controllers;

import com.borqs.server.market.ServiceException;
import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.deploy.DeploymentModeResolver;
import com.borqs.server.market.deploy.MarketDispatcherServlet;
import com.borqs.server.market.utils.CC;
import com.borqs.server.market.utils.DateTimeUtils;
import com.borqs.server.market.utils.JsonResponse;
import com.borqs.server.market.utils.Params;
import com.borqs.server.market.utils.validation.ParamsSchema;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import static com.borqs.server.market.utils.validation.Predicates.notBlank;

@Controller
@RequestMapping("/")
public class HelperController extends AbstractController {

    @RequestMapping(value = "/test", method = RequestMethod.GET)
    public ModelAndView testServer() {
        String deploymentMode = DeploymentModeResolver.getDeploymentMode();
        return new ModelAndView("test", CC.map(
                "deploymentMode=>", deploymentMode,
                "loadedAt=>", DateTimeUtils.toLongString(MarketDispatcherServlet.loadedAt),
                "now=>", DateTimeUtils.toLongString(DateTimeUtils.nowMillis()),
                "maxMemory=>", Runtime.getRuntime().maxMemory(),
                "freeMemory=>", Runtime.getRuntime().freeMemory(),
                "totalMemory=>", Runtime.getRuntime().totalMemory()
        ));
    }

    @RequestMapping(value = "/test_error_log", method = RequestMethod.GET)
    public void testErrorLog() {
        Integer.parseInt("Error integer");
    }
}
