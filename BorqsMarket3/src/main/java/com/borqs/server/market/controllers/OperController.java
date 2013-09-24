package com.borqs.server.market.controllers;


import com.borqs.server.market.Errors;
import com.borqs.server.market.ServiceException;
import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.service.OperService;
import com.borqs.server.market.service.ServiceConsts;
import com.borqs.server.market.service.impl.PublishImpl;
import com.borqs.server.market.utils.*;
import com.borqs.server.market.utils.mybatis.record.RecordsWithTotal;
import com.borqs.server.market.utils.record.CheckResult;
import com.borqs.server.market.utils.record.Record;
import com.borqs.server.market.utils.record.Records;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/")
public class OperController extends AbstractController {

    public static final int PARTITION_COUNT_PER_PAGE = OperService.DEFAULT_PARTITION_COUNT_PER_PAGE;

    private OperService operService;

    public OperController() {
    }

    public OperService getOperService() {
        return operService;
    }

    @Autowired
    @Qualifier("service.defaultOperService")
    public void setOperService(OperService operService) {
        this.operService = operService;
    }

    private void setupNavigationData(ServiceContext ctx, HttpServletRequest req, HttpServletResponse resp, String currentAppId) {
        saveCurrentModule(req, resp, ServiceConsts.MODULE_OPER);
        Attributes.setCurrentAppId(req, currentAppId);

        Records apps = operService.listApps(ctx);
        localeSelector.selectLocale(apps, ctx);
        Attributes.setAllApps(req, apps);

//        if (StringUtils.isNotEmpty(currentAppId)) {
//            Records products = publishService.listProducts(ctx, currentAppId, null, false, false);
//            localeSelector.selectLocale(products, ctx);
//            Attributes.setAllProducts(req, products);
//        }
    }

    @RequestMapping(value = "/oper", method = RequestMethod.GET)
    public ModelAndView indexPage(ServiceContext ctx, HttpServletRequest req, HttpServletResponse resp) {
        if (!ctx.hasAccountId())
            return redirect("/signin");

        saveCurrentModule(req, resp, ServiceConsts.MODULE_OPER);
        return redirect("/oper/welcome");
    }

    @RequestMapping(value = "/oper/welcome", method = RequestMethod.GET)
    public ModelAndView welcomePage(ServiceContext ctx, HttpServletRequest req, HttpServletResponse resp) {
        setupNavigationData(ctx, req, resp, null);
        return new ModelAndView("oper_welcome");
    }

    @RequestMapping(value = "/oper/apps/{appId:.+}", method = RequestMethod.GET)
    public ModelAndView appPage(ServiceContext ctx, @PathVariable("appId") String appId) {
        return redirect("/oper/apps/" + appId + "/partitions");
    }

    @RequestMapping(value = "/oper/apps/{appId:.+}/partitions", method = {RequestMethod.GET, RequestMethod.POST})
    public ModelAndView listPartitions(ServiceContext ctx, HttpServletRequest req, HttpServletResponse resp,
                                       @PathVariable("appId") String appId,
                                       Params params) {
        if (!ctx.hasAccountId())
            return redirect("/signin");

        final String categoryId = params.param("category").asString();
        Records categories = operService.listCategories(ctx, appId);
        if (categories.isEmpty())
            throw new ServiceException(Errors.E_ILLEGAL_CATEGORY, "Illegal category");

        Record firstCategory = categories.get(0);
        if (StringUtils.isBlank(categoryId))
            return redirect("/oper/apps/" + appId + "/partitions?category=" + firstCategory.asString("category_id"));

        boolean hasCategory = categories.hasWithPredicate(new Records.Predicate() {
            @Override
            public boolean predicate(Record rec) {
                return StringUtils.equals(categoryId, rec.asString("category_id"));
            }
        });
        if (!hasCategory)
            throw new ServiceException(Errors.E_ILLEGAL_CATEGORY, "Illegal category_id or app_id");


        int page = params.param("page").asInt(0);


        RecordsWithTotal partitions = operService.listPartitions(ctx, appId, categoryId, Params.of(
                "page=>", page,
                "count=>", PARTITION_COUNT_PER_PAGE)
        );

        localeSelector.selectLocale(categories, ctx);
        localeSelector.selectLocale(partitions, ctx);

        setupNavigationData(ctx, req, resp, appId);
        return new ModelAndView("oper_partitions", CC.map(
                "partitions=>", partitions.getRecords(),
                "pagination=>", Pagination.create(page, PARTITION_COUNT_PER_PAGE, partitions.getTotal()),
                "categories=>", categories,
                "currentCategory=>", categoryId,
                "currentAppId=>", appId
        ));
    }

    @RequestMapping(value = "/oper/apps/{appId:.+}/promotions", method = {RequestMethod.GET, RequestMethod.POST})
    public ModelAndView listPromotions(ServiceContext ctx, HttpServletRequest req, HttpServletResponse resp,
                                       @PathVariable("appId") String appId,
                                       Params params) {
        if (!ctx.hasAccountId())
            return redirect("/signin");


        final String categoryId = params.param("category").asString();
        Records categories = operService.listCategories(ctx, appId);
        if (categories.isEmpty())
            throw new ServiceException(Errors.E_ILLEGAL_CATEGORY, "Illegal category");

        if (WebUtils2.isPostBack(req)) {
            JsonNode promotionsNode = params.param("promotions").asJson();
            promotionsNode = JsonUtils.asArrayNode(promotionsNode);
            Records promotions = new Records();
            for (int i = 0; i < promotionsNode.size(); i++) {
                JsonNode promotionNode = promotionsNode.get(i);
                Record pr = new Record();
                pr.set("type", promotionNode.path("type").asInt());
                pr.set("target", promotionNode.path("target").asText());
                pr.set("name", promotionNode.get("name"));
                pr.set("description", promotionNode.get("description"));
                pr.set("logo_image", promotionNode.get("logo_image"));
                promotions.add(pr);
            }
            operService.setPromotions(ctx, appId, categoryId, promotions);
            return redirect("/oper/apps/" + appId + "/promotions?category=" + categoryId);
        }

        Record firstCategory = categories.get(0);
        if (StringUtils.isBlank(categoryId))
            return redirect("/oper/apps/" + appId + "/promotions?category=" + firstCategory.asString("category_id"));

        boolean hasCategory = categories.hasWithPredicate(new Records.Predicate() {
            @Override
            public boolean predicate(Record rec) {
                return StringUtils.equals(categoryId, rec.asString("category_id"));
            }
        });
        if (!hasCategory)
            throw new ServiceException(Errors.E_ILLEGAL_CATEGORY, "Illegal category_id or app_id");


        Records promotions = operService.listPromotions(ctx, appId, categoryId);
        Records products = operService.listAllProducts(ctx, appId, categoryId);
        Records partitions = operService.listPartitions(ctx, appId, categoryId, Params.of("page=>", 0, "count=>", 100000)).getRecords();

        products.retainsFields("id", "name");
        partitions.retainsFields("id", "name");

        localeSelector.selectLocale(categories, ctx);
        localeSelector.selectLocale(products, ctx);
        localeSelector.selectLocale(partitions, ctx);

        setupNavigationData(ctx, req, resp, appId);
        return new ModelAndView("oper_promotions", CC.map(
                "categories=>", categories,
                "currentCategory=>", categoryId,
                "currentAppId=>", appId,
                "promotions=>", promotions,
                "availablePartitions=>", partitions,
                "availableProducts=>", products
        ));
    }

    @RequestMapping(value = "/oper/apps/{appId:.+}/new_partition", method = {RequestMethod.GET, RequestMethod.POST})
    public ModelAndView newPartitionPage(ServiceContext ctx, HttpServletRequest req, HttpServletResponse resp,
                                         @PathVariable("appId") String appId,
                                         Params params) {
        if (!ctx.hasAccountId())
            return redirect("/signin");

        String categoryId = params.param("category").asString("");

        setupNavigationData(ctx, req, resp, appId);
        if (WebUtils2.isPostBack(req)) {
            params.aggregateMultipleLocale("name", "description");
            CheckResult cr = operService.checkPartition(ctx, appId, categoryId, params);
            if (cr.ok()) {
                Record newPartition = operService.addPartition(ctx, appId, categoryId, params.put("list", ""));
                return redirect("/oper/partitions/" + newPartition.asString("id"));
            } else {
                return new ModelAndView("oper_partition", CC.map(
                        "mode=>", "create",
                        "partition=>", cr,
                        "currentAppId=>", appId
                ));
            }
        } else {
            return new ModelAndView("oper_partition", CC.map(
                    "mode=>", "create",
                    "partition=>", new Record(),
                    "currentAppId=>", appId
            ));
        }
    }

    @RequestMapping(value = "/oper/partitions/{partitionId:.+}", method = {RequestMethod.GET, RequestMethod.POST})
    public ModelAndView showPartitionPage(ServiceContext ctx, HttpServletRequest req, HttpServletResponse resp,
                                         @PathVariable("partitionId") String partitionId,
                                         Params params) {
        final String DISPLAY_INFO_COOKIE = "oper.partition.displayInfo";

        if (!ctx.hasAccountId())
            return redirect("/signin");

        Record partition = operService.getPartition(ctx, partitionId);
        if (partition == null)
            throw new ServiceException(Errors.E_ILLEGAL_PARTITION, "Illegal partition");

        String appId = partition.asString("app_id");
        String categoryId = partition.asString("category_id");

        setupNavigationData(ctx, req, resp, appId);
        if (WebUtils2.isPostBack(req)) {
            params.aggregateMultipleLocale("name", "description");
            CheckResult cr = operService.checkPartition(ctx, appId, categoryId, params);
            if (cr.ok()) {
                operService.updatePartition(ctx, partitionId, params);
                if (params.hasParam("products")) {
                    String[] productIds = StringUtils2.splitArray(params.param("products").asString(""), '\n', true);
                    operService.setPartitionList(ctx, partitionId, Arrays.asList(productIds));
                }
                return redirect("/oper/apps/" + appId + "/partitions");
            } else {
                WebUtils2.setCookie(resp, DISPLAY_INFO_COOKIE, "1");
                return new ModelAndView("oper_partition", CC.map(
                        "mode=>", "update",
                        "partition=>", cr,
                        "currentAppId=>", appId,
                        "partitionDisplayName=>", "",
                        "displayInfo=>", true
                ));
            }
        } else {
            String displayInfo = WebUtils2.getCookieWithDelete(req, resp, DISPLAY_INFO_COOKIE, "0");
            partition.disperseMultipleLocale("name", "description");
            String partitionDisplayName = partition.asString("name_" + ctx.getClientLocale("en_US"));
            return new ModelAndView("oper_partition", CC.map(
                    "mode=>", "update",
                    "partition=>", partition,
                    "currentAppId=>", appId,
                    "partitionDisplayName=>", partitionDisplayName,
                    "displayInfo=>", PrimitiveTypeConverter.toBoolean(displayInfo, false)
            ));
        }
    }

    @RequestMapping(value = "/oper/delete_partition", method = {RequestMethod.GET, RequestMethod.POST})
    public ModelAndView deletePartition(ServiceContext ctx, @RequestParam("id") String id) {
        if (!ctx.hasAccountId())
            return redirect("/signin");

        Record partition = operService.getPartition(ctx, id);
        operService.deletePartition(ctx, id);
        if (partition == null) {
            return redirect("/oper/");
        } else {
            return redirect("/oper/apps/" + partition.asString("app_id") + "/partitions");
        }
    }
}
