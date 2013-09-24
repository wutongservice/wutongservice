package com.borqs.server.market.controllers;

import com.borqs.server.market.Errors;
import com.borqs.server.market.ServiceException;
import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.controllers.filevalidators.ProductFileValidator;
import com.borqs.server.market.controllers.filevalidators.ProductFileValidators;
import com.borqs.server.market.deploy.TemporaryDirectories;
import com.borqs.server.market.models.AvailableLocales;
import com.borqs.server.market.models.StatTypes;
import com.borqs.server.market.models.Tags;
import com.borqs.server.market.resfile.Manifest;
import com.borqs.server.market.resfile.ResourceFile;
import com.borqs.server.market.resfile.ResourceFileUtils;
import com.borqs.server.market.service.*;
import com.borqs.server.market.service.impl.PublishImpl;
import com.borqs.server.market.utils.*;
import com.borqs.server.market.utils.i18n.SpringMessage;
import com.borqs.server.market.utils.mybatis.record.RecordsWithTotal;
import com.borqs.server.market.utils.record.CheckResult;
import com.borqs.server.market.utils.record.Record;
import com.borqs.server.market.utils.record.Records;
import com.borqs.server.market.utils.validation.ParamsSchema;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.codehaus.jackson.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.*;

import static com.borqs.server.market.utils.validation.Predicates.*;

@Controller
@RequestMapping("/")
public class PublishController extends AbstractController {
    private PublishService publishService;
    private static final String uploadTemporaryDir = TemporaryDirectories.getUploadTempDirPath();
    private ProductFileValidators fileValidators;
    protected StatisticsService statisticsService;
    protected OrderService orderService;
    protected CommentService commentService;

    public PublishController() {
    }

    public PublishService getPublishService() {
        return publishService;
    }

    @Autowired
    @Qualifier("service.orderService")
    public void setOrderService(OrderService orderService) {
        this.orderService = orderService;
    }

    @Autowired
    @Qualifier("service.statisticsService")
    public void setStatisticsService(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @Autowired
    @Qualifier("service.commentService")
    public void setCommentService(CommentService CommentService) {
        this.commentService = CommentService;
    }


    @Autowired
    @Qualifier("service.defaultPublishService")
    public void setPublishService(PublishService publishService) {
        this.publishService = publishService;
    }

    public ProductFileValidators getFileValidators() {
        return fileValidators;
    }

    @Autowired
    @Qualifier("productFileValidators")
    public void setFileValidators(ProductFileValidators fileValidators) {
        this.fileValidators = fileValidators;
    }

    @RequestMapping(value = "/api/v2/publish/apps/list", method = RequestMethod.GET)
    public APIResponse listApps(ServiceContext ctx) {
        Records apps = publishService.listApps(ctx);
        return APIResponse.of(apps);
    }

    private static final ParamsSchema getAppSchema = new ParamsSchema()
            .required("id", "id: string not blank", notBlank());

    @RequestMapping(value = "/api/v2/publish/apps/get", method = RequestMethod.GET)
    public APIResponse getApp(ServiceContext ctx, Params params) {
        params = getAppSchema.validate(params);
        Record app = publishService.getApp(ctx, params.param("id").asString());
        return APIResponse.of(app);
    }


    private static final ParamsSchema listCategoriesSchema = new ParamsSchema()
            .required("app_id", "not blank string", notBlank());

    @RequestMapping(value = "/api/v2/publish/categories/list", method = RequestMethod.GET)
    public APIResponse listCategories(ServiceContext ctx, Params params) {
        params = listCategoriesSchema.validate(params);
        Records categories = publishService.listCategories(ctx, params.param("app_id").asString(), true);
        return APIResponse.of(categories);
    }


    private static final ParamsSchema uploadSchema = new ParamsSchema()
            .required("file", "Product file", typeIs(FileItem.class))
            .optional("create_product", false, "boolean", asBoolean());

    @RequestMapping(value = "/api/v2/publish/upload", method = RequestMethod.POST)
    public APIResponse uploadProduct(ServiceContext ctx, Params params) throws Exception {
        params = uploadSchema.validate(params);
        FileItem fi = (FileItem) params.param("file").value;

        File tmpResFile = new File(FilenameUtils.concat(uploadTemporaryDir, FilenameUtils2.changeFilenameWithoutExt(fi.getName(), "upload-" + RandomUtils2.randomLong())));
        try {
            fi.write(tmpResFile);
            ResourceFile resFile = new ResourceFile(tmpResFile.getAbsolutePath());
            Record r = publishService.uploadProduct(ctx, resFile,
                    params.param("create_product").asBoolean(false),
                    new Params(params).removeParams("file", "create_product"));
            return APIResponse.of(r);
        } finally {
            FileUtils.deleteQuietly(tmpResFile);
        }
    }

    //    @RequestMapping(value = "/api/v2/publish/delete_version", method = {RequestMethod.GET, RequestMethod.POST})
//    public APIResponse deleteVersion(ServiceContext ctx, Params params) throws Exception {
//        params = uploadSchema.validate(params);
//        String id = params.param("product_id").toString();
//        Integer version = params.param("version").asInt();
//
//        publishService.deleteVersion(ctx, id, version);
//        Record r = Record.of("product_id", id, "version", version);
//
//        return APIResponse.of(r);
//    }



    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // pages

    private void setupNavigationData(ServiceContext ctx, HttpServletRequest req, HttpServletResponse resp, String currentAppId) {
        saveCurrentModule(req, resp, ServiceConsts.MODULE_PUBLISH);
        Attributes.setCurrentAppId(req, currentAppId);


        Records apps = publishService.listApps(ctx);
        localeSelector.selectLocale(apps, ctx);
        Attributes.setAllApps(req, apps);

        if (StringUtils.isNotEmpty(currentAppId)) {
            Records products = publishService.listProducts(ctx, currentAppId, null, false, false);
            localeSelector.selectLocale(products, ctx);
            Attributes.setAllProducts(req, products);
        }
    }

    @RequestMapping(value = "/publish", method = RequestMethod.GET)
    public ModelAndView indexPage(ServiceContext ctx, HttpServletRequest req, HttpServletResponse resp) {
        if (!ctx.hasAccountId())
            return redirect("/signin");

        saveCurrentModule(req, resp, ServiceConsts.MODULE_PUBLISH);
        return redirect("/publish/welcome");
    }

    @RequestMapping(value = "/publish/welcome", method = RequestMethod.GET)
    public ModelAndView welcomePage(ServiceContext ctx, HttpServletRequest req, HttpServletResponse resp) {
        setupNavigationData(ctx, req, resp, null);
        return new ModelAndView("publish_welcome");
    }

    @RequestMapping(value = "/publish/apps/{appId:.+}", method = RequestMethod.GET)
    public ModelAndView showAppPage(ServiceContext ctx, HttpServletRequest req, HttpServletResponse resp, @PathVariable("appId") String appId) {
        if (!ctx.hasAccountId())
            return redirect("/signin");

        Records products = publishService.listProducts(ctx, appId, null, true, false);
        List<String> productIds = products.asStringList("id");

        // today/total download_count
        products
                .setField("today_download", 0).setField("all_download", 0)
                .setField("today_purchase", 0).setField("all_purchase", 0);
        if (!productIds.isEmpty()) {
            Records todayDownloadCounts = statisticsService.sumCounts(ctx,
                    new String[]{StatisticsService.DOWNLOAD_COUNT, StatisticsService.PURCHASE_COUNT},
                    DateRange.today(),
                    StatisticsService.Dimension.of("product_id").in(productIds),
                    null);

            Records allDownloadCounts = statisticsService.sumCounts(ctx,
                    new String[]{StatisticsService.DOWNLOAD_COUNT, StatisticsService.PURCHASE_COUNT},
                    DateRange.allDates(),
                    StatisticsService.Dimension.of("product_id").in(productIds),
                    null);

            for (Record product : products) {
                for (Record today : todayDownloadCounts) {
                    if (StringUtils.equals(product.asString("id"), today.asString("product_id"))) {
                        product.set("today_download", today.get(StatisticsService.DOWNLOAD_COUNT));
                        product.set("today_purchase", today.get(StatisticsService.PURCHASE_COUNT));
                    }
                }
                for (Record all : allDownloadCounts) {
                    if (StringUtils.equals(product.asString("id"), all.asString("product_id"))) {
                        product.set("all_download", all.get(StatisticsService.DOWNLOAD_COUNT));
                        product.set("all_purchase", all.get(StatisticsService.PURCHASE_COUNT));
                    }
                }
            }
        }

        localeSelector.selectLocale(products, ctx);

        // display price
        for (Record product : products) {
            JsonNode priceNode = product.getJsonNode("price", null);
            String price;
            if (priceNode != null) {
                price = priceNode.path("display").asText();
            } else {
                price = "0";
            }
            product.set("price", price);
        }

        setupNavigationData(ctx, req, resp, appId);
        LinkedHashMap<String, Record> groupedProducts = new LinkedHashMap<String, Record>();

        // group
        Records categories = publishService.listCategories(ctx, appId, false);
        localeSelector.selectLocale(categories, ctx);
        for (Record category : categories) {
            String categoryId = category.asString("category_id");
            String categoryName = category.asString("name");
            Records categoryProducts = new Records();
            for (Record r : products) {
                if (StringUtils.equals(r.asString("category_id"), categoryId)) {
                    categoryProducts.add(r);
                }

                Records rs = (Records) r.get("versions");
                localeSelector.selectLocale(rs, ctx);
                if (rs != null && !rs.isEmpty()) {
                    r.put("last_version", rs.get(0));
                }
            }
            groupedProducts.put(categoryId, Record.of("category_name=>", categoryName, "products=>", categoryProducts));
        }

        // sort by name
        for (Record r : groupedProducts.values()) {
            Records categoryProducts = (Records) r.get("products");
            if (categoryProducts != null)
                categoryProducts.sortAsLong("created_at", false);
        }
        return new ModelAndView("publish_app", CC.map(
                "products=>", groupedProducts,
                "appId=>", appId));
    }


    @RequestMapping(value = "/publish/products/{productId:.+}/{version:\\d+}/active", method = {RequestMethod.GET, RequestMethod.POST})
    public ModelAndView activeVersion(ServiceContext ctx, Params params,
                                      @PathVariable("productId") String productId,
                                      @PathVariable("version") int version) throws Exception {
        if (!ctx.hasAccountId())
            return redirect("/signin");

        if (!params.hasParam("active"))
            throw new ServiceException(Errors.E_ILLEGAL_PARAM, "Missing param 'active'");

        boolean active = params.param("active").asBoolean(true);
        publishService.activeVersion(ctx, productId, version, active);

        return redirect("/publish/products/" + productId);
    }


    @RequestMapping(value = "/publish/products/{productId:.+}/{version:\\d+}/release", method = {RequestMethod.GET, RequestMethod.POST})
    public ModelAndView releaseVersion(ServiceContext ctx, Params params,
                                      @PathVariable("productId") String productId,
                                      @PathVariable("version") int version) throws Exception {
        if (!ctx.hasAccountId())
            return redirect("/signin");

        publishService.releaseVersion(ctx, productId, version);

        return redirect("/publish/products/" + productId);
    }

    @RequestMapping(value = "/publish/products/{productId:.+}/info", method = {RequestMethod.GET, RequestMethod.POST})
    public ModelAndView showProductInfoPage(ServiceContext ctx, Params params, @PathVariable("productId") String productId) {
        return showProductPageWithoutVersion(ctx, params, productId);
    }

    @RequestMapping(value = "/publish/products/{productId:.+}", method = {RequestMethod.GET, RequestMethod.POST})
    public ModelAndView showProductPageWithoutVersion(ServiceContext ctx, Params params, @PathVariable("productId") String productId) {
        if (!ctx.hasAccountId())
            return redirect("/signin");

        String firstPostfix = params.param("first").asBoolean(false) ? "?first=1" : "";
        Record product = publishService.getProduct(ctx, productId, true, false);
        if (product == null)
            throw new ServiceException(Errors.E_ILLEGAL_PRODUCT, "Illegal product " + productId);

        Records versions = (Records) product.get("versions");
        if (versions.isEmpty()) {
            return redirect("/publish/products/" + productId + "/upload");
        } else {
            Record lastVersionRec = versions.get(0);
            int version = lastVersionRec.asInt("version");
            return redirect("/publish/products/" + productId + "/" + version + firstPostfix);
        }
    }


    private static Record createProductForUpdate(String id, int version, Params params) {
        return Record.of(
                "id=>", id,
                "version=>", version,
                "name=>", params.param("name").asString(null),
                "description=>", params.param("description").asString(null),
                "tags=>", params.param("tags").asString(null),
                "author_name=>", params.param("author_name").asString(null),
                "author_email=>", params.param("author_email").asString(null),
                "author_phone=>", params.param("author_phone").asString(null),
                "author_website=>", params.param("author_website").asString(null),
                "logo_image=>", PublishImpl.getFileParam(params, "logo_image"),
                "cover_image=>", PublishImpl.getFileParam(params, "cover_image"),
                "promotion_image=>", PublishImpl.getFileParam(params, "promotion_image"),
                "screenshot1_image=>", PublishImpl.getFileParam(params, "screenshot1_image"),
                "screenshot2_image=>", PublishImpl.getFileParam(params, "screenshot2_image"),
                "screenshot3_image=>", PublishImpl.getFileParam(params, "screenshot3_image"),
                "screenshot4_image=>", PublishImpl.getFileParam(params, "screenshot4_image"),
                "screenshot5_image=>", PublishImpl.getFileParam(params, "screenshot5_image")
        );
    }

    private static Record createVersionForUpdate(String id, int version, Params params) {
        return Record.of(
                "id=>", id,
                "version=>", version,
                "version_name=>", params.param("version_name").asString(),
                "recent_change=>", params.param("recent_change").asString()
        );
    }


    private void getCountStatModelsHelper(Map<String, Object> models, ServiceContext ctx, DateRange range, String productId, String countType) {
        String[] conds = new String[]{"product_id='" + productId + "'"};

        Records totalPerDayLine = statisticsService.dateBasedLineChart(ctx, countType, range,
                StatisticsService.Dimension.ofDummy("Downloads"), conds, StatisticsService.OPT_FORMAT_COUNTRY_NAME);

        Records totalPerDayByCountryLine = statisticsService.dateBasedLineChart(ctx, countType, range,
                StatisticsService.Dimension.of("country", 5), conds, StatisticsService.OPT_FORMAT_COUNTRY_NAME);

        Records totalPerDayByCountryDonut = statisticsService.donutChart(ctx, countType, range,
                StatisticsService.Dimension.of("country", 5), conds, StatisticsService.OPT_FORMAT_COUNTRY_NAME);

        Records totalPerDayByVersionLine = statisticsService.dateBasedLineChart(ctx, countType, range,
                StatisticsService.Dimension.of("version"), conds, StatisticsService.OPT_FORMAT_COUNTRY_NAME);

        Records totalPerDayByVersionDonut = statisticsService.donutChart(ctx, countType, range,
                StatisticsService.Dimension.of("version"), conds, StatisticsService.OPT_FORMAT_COUNTRY_NAME);

        models.put("totalPerDayLine", totalPerDayLine);
        models.put("totalPerDayByCountryLine", totalPerDayByCountryLine);
        models.put("totalPerDayByCountryDonut", totalPerDayByCountryDonut);
        models.put("totalPerDayByVersionLine", totalPerDayByVersionLine);
        models.put("totalPerDayByVersionDonut", totalPerDayByVersionDonut);
    }

    private void getPurchaseCountStatModels(Map<String, Object> models, ServiceContext ctx, DateRange range, String productId) {
        getCountStatModelsHelper(models, ctx, range, productId, StatisticsService.PURCHASE_COUNT);
    }


    private void getDownloadCountStatModels(Map<String, Object> models, ServiceContext ctx, DateRange range, String productId) {
        getCountStatModelsHelper(models, ctx, range, productId, StatisticsService.DOWNLOAD_COUNT);
    }


    @RequestMapping(value = "/publish/products/{productId:.+}/stat", method = RequestMethod.GET)
    public ModelAndView showProductStatPage(ServiceContext ctx, HttpServletRequest req, HttpServletResponse resp,
                                            @PathVariable("productId") String productId, Params params) {
        if (!ctx.hasAccountId())
            return redirect("/signin");

        Record product = publishService.getProduct(ctx, productId, true, false);
        if (product == null)
            throw new ServiceException(Errors.E_ILLEGAL_PRODUCT, "Illegal product " + productId);

        String appId = product.asString("app_id");
        setupNavigationData(ctx, req, resp, appId);

        Records allStatTypes = StatTypes.allStatTypes(ctx);
        int months = getStatMonthsWithParams(params, "months", 1);
        String statType = params.param("stat_type").asString(StatTypes.STAT_PURCHASE_COUNT);

        DateRange range = DateRange.monthsAgo(months);
        Map<String, Object> models = CC.map(
                "product=>", product,
                "current_stat_type=>", statType,
                "current_months=>", months <= 0 ? "all" : Integer.toString(months),
                "available_stat_types=>", allStatTypes
        );

        if (StatTypes.STAT_PURCHASE_COUNT.equalsIgnoreCase(statType)) {
            getPurchaseCountStatModels(models, ctx, range, productId);
        } else if (StatTypes.STAT_DOWNLOAD_COUNT.equalsIgnoreCase(statType)) {
            getDownloadCountStatModels(models, ctx, range, productId);
        } else {
            throw new ServiceException(Errors.E_ILLEGAL_PARAM, "Illegal stat_type");
        }

        return new ModelAndView("publish_productStat", models);
    }

    @RequestMapping(value = "/publish/products/{productId:.+}/{version:\\d+}", method = {RequestMethod.GET, RequestMethod.POST})
    public ModelAndView showProductPageWithVersion(ServiceContext ctx, HttpServletRequest req, HttpServletResponse resp,
                                                   Params params,
                                                   @PathVariable("productId") String productId,
                                                   @PathVariable("version") int version) {
        if (!ctx.hasAccountId())
            return redirect("/signin");

        boolean first = params.param("first").asBoolean(false);

        Record product = publishService.getProduct(ctx, productId, true, false);
        Record versionRec = publishService.getVersion(ctx, productId, version);

        if (product == null)
            throw new ServiceException(Errors.E_ILLEGAL_PRODUCT, "Illegal product " + productId);

        String appId = product.asString("app_id");

        if (versionRec == null)
            throw new ServiceException(Errors.E_ILLEGAL_VERSION, "Illegal version " + version);

        setupNavigationData(ctx, req, resp, appId);
        if (WebUtils2.isPostBack(req)) {
            params.aggregateMultipleLocale("name", "description", "version_name", "recent_change");
            CheckResult pcr = publishService.checkProductForUpdate(ctx, productId, params, product);
            CheckResult vcr = publishService.checkVersionForUpdate(ctx, productId, version, params, versionRec);
            if (!pcr.ok() || !vcr.ok()) {
                pcr.disperseMultipleLocale("name", "description");
                vcr.disperseMultipleLocale("version_name", "recent_change");
                return new ModelAndView("publish_product", CC.map("product=>", pcr, "version=>", vcr));
            }
            publishService.updateProduct(ctx, createProductForUpdate(productId, version, params));
            publishService.updateVersion(ctx, createVersionForUpdate(productId, version, params));

            return redirect("/publish/products/" + productId + "/" + version + (first ? "?first=1" : ""));
        } else {
            localeSelector.selectLocale((Records) product.get("versions"), ctx);
            product.disperseMultipleLocale("name", "description");
            versionRec.disperseMultipleLocale("version_name", "recent_change");
            versionRec.put("url_filename", FilenameUtils.getName(versionRec.asString("url")));
            return new ModelAndView("publish_product", CC.map(
                    "product=>", product,
                    "version=>", versionRec,
                    "showActiveVersionPrompt=>", first
            ));
        }
    }


    private ProductFileValidator.ValidateResult validateFile(String appId, String categoryId, File f) {
        if (fileValidators != null) {
            return fileValidators.validate(appId, categoryId, f);
        } else {
            return ProductFileValidator.ValidateResult.skip();
        }
    }

    private Records getCategories(ServiceContext ctx, String appId) {
        Records categories = publishService.listCategories(ctx, appId, false);
        localeSelector.selectLocale(categories, ctx);
        return categories;
    }

    private Record getLastVersion(ServiceContext ctx, String productId) {
        Record product = publishService.getProduct(ctx, productId, true, false);
        if (product == null)
            return new Record();

        Records versions = (Records) product.get("versions");
        if (versions.isEmpty())
            return new Record();

        Record lastVersion = versions.get(0);
        return lastVersion
                .set("available_tags", product.asString("available_tags"))
                .set("available_mods", product.asString("available_mods"));
    }

    private static Record findCategory(Records categories, String categoryId) {
        Record categoryRec = null;
        if (StringUtils.isNotEmpty(categoryId)) {
            for (Record cr0 : categories) {
                if (StringUtils.equals(cr0.asString("category_id"), categoryId)) {
                    categoryRec = cr0;
                    break;
                }
            }
        }
        return categoryRec;
    }

    private ModelAndView uploadProductHelper(ServiceContext ctx, Params params, HttpServletRequest req, HttpServletResponse resp, String appId, String categoryId, String productId) throws Exception {
        boolean forCreate = productId == null;
        if (WebUtils2.isPostBack(req)) {
            FileItem fi = (FileItem) params.param("file").value;
            if (fi == null)
                throw new ServiceException(Errors.E_ILLEGAL_PARAM, "Missing file");

            File tmp = new File(FilenameUtils.concat(uploadTemporaryDir, FilenameUtils2.changeFilenameWithoutExt(fi.getName(), "upload-" + RandomUtils2.randomLong())));
            boolean deleteTmp = true;
            try {
                fi.write(tmp);
                if (ResourceFileUtils.isResourceFile(tmp)) {
                    ResourceFile resFile = new ResourceFile(tmp);
                    Params ps0 = new Params(params).retainsParams("qualified_app_id", "qualified_product_id", "qualified_category_id", "as_beta");
                    CheckResult cr = publishService.checkProductForUpload(ctx, resFile, forCreate, ps0);
                    cr.disperseMultipleLocale("name", "description", "recent_change", "version_name");
                    if (cr.ok()) {
                        Record product = publishService.uploadProduct(ctx, resFile, forCreate, ps0);
                        if (forCreate) {
                            return redirect("/publish/products/" + product.asString("id") + "/price?first=1");
                        } else {
                            return redirect("/publish/products/" + product.asString("id") + "?first=1");
                        }
                    } else {
                        return new ModelAndView("publish_uploadError", CC.map("result=>", cr));
                    }
                } else {
                    deleteTmp = false;

                    String fileStatus = new TemporaryFile(FilenameUtils.getName(tmp.getAbsolutePath()), fi.getName(), DateTimeUtils.nowMillis()).encode();
                    int asBeta = params.param("as_beta").asInt(0) != 0 ? 1 : 0;
                    setupNavigationData(ctx, req, resp, appId);
                    if (forCreate) {
                        Records categories = getCategories(ctx, appId);
                        Record categoryRec = findCategory(categories, categoryId);
                        Record result = new Record()
                                .set("app_id", appId)
                                .set("version", "1")
                                .set("version_name", AvailableLocales.multipleLocalesAsJsonNode("1.0"))
                                .set("id", appId + ".auto_" + RandomUtils2.randomString(96))
                                .set("category_id", ObjectUtils.toString(categoryId))
                                .set("available_tags", categoryRec != null ? categoryRec.asString("available_tags", "") : "")
                                .set("available_mods", categoryRec != null ? categoryRec.asString("available_mods", "") : "");
                        result.disperseMultipleLocale("name", "description", "version_name", "recent_change");
                        return new ModelAndView("publish_inputManifestForProduct", CC.map(
                                "result=>", result,
                                "categories=>", categories,
                                "fileStatus=>", fileStatus,
                                "asBeta=>", asBeta
                        ));
                    } else {
                        Record lastVersionRec = getLastVersion(ctx, productId);
                        int lastVersion = lastVersionRec.asInt("version", 1);
                        Record result = new Record()
                                .set("app_id", appId)
                                .set("id", productId)
                                .set("last_version", lastVersion)
                                .set("version", lastVersion + 1)
                                .set("min_app_version", lastVersionRec.asInt("min_app_version", 0))
                                .set("max_app_version", lastVersionRec.asInt("max_app_version", Integer.MAX_VALUE))
                                .set("supported_mod", lastVersionRec.get("supported_mod"))
                                .set("last_version_name", lastVersionRec.getJsonNode("version_name"))
                                .set("available_mods", lastVersionRec.asString("available_mods"));
                        result.disperseMultipleLocale("last_version_name");
                        return new ModelAndView("publish_inputManifestForVersion", CC.map(
                                "result=>", result,
                                "fileStatus=>", fileStatus,
                                "asBeta=>", asBeta
                        ));
                    }
                }
            } finally {
                if (deleteTmp) {
                    FileUtils.deleteQuietly(tmp);
                }
            }
        } else {
            return new ModelAndView("publish_upload", CC.map(
                    "qualifiedAppId=>", appId,
                    "qualifiedProductId=>", forCreate ? "" : productId,
                    "mode=>", forCreate ? "uploadProduct" : "uploadVersion"));
        }
    }

    private void getManifestAndImagesByParamsForProduct(String appId, Params params, Manifest manifest, Record images) {
        manifest.setId(params.param("id").asString());
        manifest.setAppId(appId);
        manifest.setCategory(params.param("category_id").asString());
        manifest.setDefaultLanguage("en_US");
        manifest.setName(params.param("name").asJson());
        manifest.setDescription(params.param("description").asJson());
        manifest.setAuthorName(params.param("author_name").asString());
        manifest.setAuthorEmail(params.param("author_email").asString());
        manifest.setAuthorWebsite(params.param("author_website").asString());
        manifest.setAuthorPhone(params.param("author_phone").asString());
        manifest.setTags(Tags.trimTags(params.param("tags").asString()));
        for (String imageName : new String[]{
                "logo_image",
                "cover_image",
                "screenshot1_image",
                "screenshot2_image",
                "screenshot3_image",
                "screenshot4_image",
                "screenshot5_image",}) {
            images.put(imageName, params.param(imageName).asFileItem());
        }

        setVersionInfoByParams(params, manifest);
    }

    private void setVersionInfoByParams(Params params, Manifest manifest) {
        manifest.setVersion(params.param("version").asInt());
        manifest.setMinAppVersion(params.param("min_app_version").asInt(0));
        manifest.setMaxAppVersion(params.param("max_app_version").asInt(Integer.MAX_VALUE));
        manifest.setSupportedMod(StringUtils2.splitArray(params.param("supported_mod").asString(), ",", true));
        manifest.setVersionName(params.param("version_name").asJson());
        manifest.setRecentChange(params.param("recent_change").asJson());
    }

    private void getManifestAndImagesByParamsForVersion(Record product, Params params, Manifest manifest, Record images) {
        manifest.setId(product.asString("id"));
        manifest.setAppId(product.asString("app_id"));
        manifest.setCategory(product.asString("category_id"));
        manifest.setDefaultLanguage("en_US");
        manifest.setName(product.getJsonNode("name"));
        manifest.setDescription(product.getJsonNode("description"));
        manifest.setAuthorName(product.asString("author_name"));
        manifest.setAuthorEmail(product.asString("author_email"));
        manifest.setAuthorWebsite(product.asString("author_website"));
        manifest.setAuthorPhone(product.asString("author_phone"));
        for (String imageName : new String[]{
                "logo_image",
                "cover_image",
                "screenshot1_image",
                "screenshot2_image",
                "screenshot3_image",
                "screenshot4_image",
                "screenshot5_image",}) {
            images.put(imageName, product.asString(imageName));
        }

        setVersionInfoByParams(params, manifest);
    }

    private File createOrgFile(TemporaryFile tf) {
        return new File(FilenameUtils.concat(uploadTemporaryDir, tf.filename));
    }

    private File createTmpFileByOrg(File tmpOrg, TemporaryFile tf) {
        File tmp;
        if (ResourceFileUtils.isResourceFile(tmpOrg)) {
            tmp = new File(FilenameUtils.concat(uploadTemporaryDir, FilenameUtils2.changeFilenameWithoutExt(tf.filename, "pack-" + RandomUtils2.randomLong())));
        } else {
            tmp = new File(FilenameUtils.concat(uploadTemporaryDir, "pack-" + RandomUtils2.randomLong() + ".zip"));
        }
        return tmp;
    }

    @RequestMapping(value = "/publish/apps/{appId:.+}/upload/fill_manifest", method = RequestMethod.POST)
    public ModelAndView fillManifestForProductPage(ServiceContext ctx,
                                                   Params params,
                                                   HttpServletRequest req,
                                                   HttpServletResponse resp,
                                                   @PathVariable("appId") String appId) throws Exception {
        if (!ctx.hasAccountId())
            return redirect("/signin");

        params.aggregateMultipleLocale("name", "description", "version_name", "recent_change");
        setupNavigationData(ctx, req, resp, appId);
        Manifest manifest = new Manifest();
        Record images = new Record();
        getManifestAndImagesByParamsForProduct(appId, params, manifest, images);
        TemporaryFile tf = TemporaryFile.decode(params.param("fileStatus").asString());
        File tmpOrg = createOrgFile(tf);
        File tmp = createTmpFileByOrg(tmpOrg, tf);

        boolean deleteTmpOrg = false;
        try {
            ResourceFileUtils.packResource(tmp, tmpOrg, tf.orgFilename, manifest, images);
            ResourceFile resFile = new ResourceFile(tmp);
            Params ps0 = new Params();
            ps0.put("as_beta", params.param("as_beta").asInt(0));
            CheckResult cr = publishService.checkProductForUpload(ctx, resFile, true, ps0);
            if (cr.ok()) {
                deleteTmpOrg = true;
                Record product = publishService.uploadProduct(ctx, resFile, true, ps0);
                return redirect("/publish/products/" + product.asString("id") + "/price?first=1");
            } else {
                String categoryId = params.param("category").asString("");
                Records categories = getCategories(ctx, appId);
                Record categoryRec = findCategory(categories, categoryId);
                cr.set("category_id", categoryId);
                cr.set("available_tags", categoryRec != null ? categoryRec.asString("available_tags", "") : "");
                cr.set("available_mods", categoryRec != null ? categoryRec.asString("available_mods", "") : "");
                cr.disperseMultipleLocale("name", "description", "version_name", "recent_change");
                return new ModelAndView("publish_inputManifestForProduct", CC.map(
                        "result=>", cr,
                        "categories=>", categories,
                        "fileStatus=>", tf.encode()
                ));
            }
        } finally {
            FileUtils.deleteQuietly(tmp);
            if (deleteTmpOrg)
                FileUtils.deleteQuietly(tmpOrg);
        }
    }

    @RequestMapping(value = "/publish/products/{productId:.+}/upload/fill_manifest", method = RequestMethod.POST)
    public ModelAndView fillManifestForVersionPage(ServiceContext ctx,
                                                   Params params,
                                                   HttpServletRequest req,
                                                   HttpServletResponse resp,
                                                   @PathVariable("productId") String productId) throws Exception {
        if (!ctx.hasAccountId())
            return redirect("/signin");

        params.aggregateMultipleLocale("name", "description", "version_name", "recent_change");
        Record product = publishService.getProduct(ctx, productId, false, false);
        if (product == null)
            throw new ServiceException(Errors.E_ILLEGAL_PRODUCT, "Illegal product");

        String appId = product.asString("app_id");
        setupNavigationData(ctx, req, resp, appId);

        Manifest manifest = new Manifest();
        Record images = new Record();
        getManifestAndImagesByParamsForVersion(product, params, manifest, images);
        TemporaryFile tf = TemporaryFile.decode(params.param("fileStatus").asString());
        File tmpOrg = createOrgFile(tf);
        File tmp = createTmpFileByOrg(tmpOrg, tf);

        boolean deleteTmpOrg = false;
        try {
            ResourceFileUtils.packResource(tmp, tmpOrg, tf.orgFilename, manifest, images);
            ResourceFile resFile = new ResourceFile(tmp);
            Params ps0 = Params.of(
                    "qualified_app_id=>", product.asString("app_id"),
                    "qualified_product_id=>", product.asString("id"),
                    "qualified_category_id=>", product.asString("category_id"),
                    "as_beta=>", params.param("as_beta").asInt(0)
            );
            CheckResult cr = publishService.checkProductForUpload(ctx, resFile, false, ps0);
            if (cr.ok()) {
                deleteTmpOrg = true;
                Record product1 = publishService.uploadProduct(ctx, resFile, false, ps0);
                return redirect("/publish/products/" + product1.asString("id") + "?first=1");
            } else {
                Record lastVersionRec = getLastVersion(ctx, productId);
                cr.set("last_version_name", lastVersionRec.getJsonNode("version_name"));
                cr.set("available_mods", lastVersionRec.asString("available_mods"));
                cr.disperseMultipleLocale("name", "description", "version_name", "recent_change");
                return new ModelAndView("publish_inputManifestForVersion", CC.map(
                        "result=>", cr,
                        "fileStatus=>", tf.encode()
                ));
            }
        } finally {
            FileUtils.deleteQuietly(tmp);
            if (deleteTmpOrg)
                FileUtils.deleteQuietly(tmpOrg);
        }
    }

    @RequestMapping(value = "/publish/apps/{appId:.+}/upload", method = {RequestMethod.GET, RequestMethod.POST})
    public ModelAndView uploadProductPage(ServiceContext ctx,
                                          HttpServletRequest req, HttpServletResponse resp,
                                          Params params, @PathVariable("appId") String appId) throws Exception {
        if (!ctx.hasAccountId())
            return redirect("/signin");

        setupNavigationData(ctx, req, resp, appId);
        return uploadProductHelper(ctx, params, req, resp, appId, params.param("category").asString(null), null);
    }

    @RequestMapping(value = "/publish/products/{productId:.+}/upload", method = {RequestMethod.GET, RequestMethod.POST})
    public ModelAndView uploadVersionPage(ServiceContext ctx,
                                          HttpServletRequest req, HttpServletResponse resp,
                                          Params params,
                                          @PathVariable("productId") String productId) throws Exception {
        if (!ctx.hasAccountId())
            return redirect("/signin");


        Record product = publishService.getProduct(ctx, productId, false, false);
        if (product == null)
            throw new ServiceException(Errors.E_ILLEGAL_PRODUCT, "Illegal product");

        String appId = product.asString("app_id");
        setupNavigationData(ctx, req, resp, appId);
        return uploadProductHelper(ctx, params, req, resp, appId, product.asString("category_id"), productId);
    }

    private static String formatPriceRow(JsonNode node) {
        return String.format("<li>%s&nbsp;&nbsp;%s</li>", node.path("cs").asText(), node.path("amount").asText());
    }

    private static String formatPaidAndPrice(ServiceContext ctx,int paid, JsonNode price) {
        StringBuilder buff = new StringBuilder();
        if (paid == ServiceConsts.PT_PRICE_FREE) {
            buff.append("0");
        } else if (paid == ServiceConsts.PT_PRICE_PAID) {
            if (price != null && price.isObject()) {
                //buff.append(price.path("display").asText());
                StringBuilder rowsBuff = new StringBuilder();
                if (price.has("cs")) {
                    rowsBuff.append(formatPriceRow(price));
                } else {
                    Iterator<Map.Entry<String, JsonNode>> iter = price.getFields();
                    while (iter.hasNext()) {
                        JsonNode priceNode = iter.next().getValue();
                        if (priceNode.has("cs"))
                            rowsBuff.append(formatPriceRow(priceNode));
                    }
                }
                buff.append(String.format("<ul style=\"list-style:none;margin:0;\">%s</ul>", rowsBuff));
            }
        }
        return buff.toString();
    }

    private static void fillPricetagDisplayInfo(ServiceContext ctx, Record pricetag) {
        String googleInfo = formatPaidAndPrice(ctx, pricetag.asInt("paid", 0), pricetag.getJsonNode("price"));
        String cmccInfo = formatPaidAndPrice(ctx, pricetag.asInt("cmcc_mm_paid", 0), pricetag.getJsonNode("cmcc_mm_price"));
        pricetag.set("google_price_display", googleInfo);
        pricetag.set("cmcc_mm_price_display", cmccInfo);
    }

    @RequestMapping(value = "/publish/products/{productId:.+}/price", method = {RequestMethod.GET, RequestMethod.POST})
    public ModelAndView setPrice(ServiceContext ctx, Params params, HttpServletRequest req, HttpServletResponse resp, @PathVariable("productId") String productId) {
        if (!ctx.hasAccountId())
            return redirect("/signin");

        boolean first = params.param("first").asBoolean(false);
        String firstPostfix = first ? "?first=1" : "";
        Record product = publishService.getProduct(ctx, productId, false, true);
        if (product == null)
            throw new ServiceException(Errors.E_ILLEGAL_PRODUCT, "Illegal product");

        if (WebUtils2.isPostBack(req)) {
            String pricetagId = params.param("pricetag_id").asString();
            if (pricetagId == null)
                throw new ServiceException(Errors.E_ILLEGAL_PARAM, "Missing pricetag_id");

            Record newProduct = new Record().set("id", productId).set("pricetag_id", pricetagId);
            publishService.updateProduct(ctx, newProduct);
            return redirect("/publish/products/" + productId + firstPostfix);
        } else {
            setupNavigationData(ctx, req, resp, product.asString("app_id"));
            Records pricetags = (Records) product.get("pricetags");
            //localeSelector.selectLocale(pricetags, ctx);
            for (Record pricetag : pricetags) {
                fillPricetagDisplayInfo(ctx, pricetag);
            }
            return new ModelAndView("publish_selectPrice", CC.map(
                    "product=>", product,
                    "pricetags=>", pricetags,
                    "current_pricetag_id=>", product.asString("pricetag_id"),
                    "first=>", first,
                    "version=>", params.param("version").asString("")
            ));
        }
    }

    @RequestMapping(value = "/publish/products/{productId:.+}/publish_channel", method = {RequestMethod.GET, RequestMethod.POST})
    public ModelAndView setPublishChannel(ServiceContext ctx, Params params, HttpServletRequest req, HttpServletResponse resp, @PathVariable("productId") String productId) {
        if (!ctx.hasAccountId())
            return redirect("/signin");

        Record product = publishService.getProduct(ctx, productId, false, true);
        if (product == null)
            throw new ServiceException(Errors.E_ILLEGAL_PRODUCT, "Illegal product");

        if (WebUtils2.isPostBack(req)) {
            String publishChannels = params.param("publish_channels").asString(null);
            if (publishChannels == null)
                throw new ServiceException(Errors.E_ILLEGAL_PARAM, "Illegal param publish_channels");
            publishChannels = Tags.trimTags(publishChannels);
            publishService.updateProduct(ctx, new Record().set("id", productId).set("publish_channels", publishChannels));
            return redirect("/publish/products/" + productId);
        } else {
            setupNavigationData(ctx, req, resp, product.asString("app_id"));
            return new ModelAndView("publish_setPublishChannel", CC.map(
                    "product=>", product,
                    "version=>", params.param("version").asString("")
            ));
        }
    }


    @RequestMapping(value = "/publish/products/{productId:.+}/orders", method = {RequestMethod.GET, RequestMethod.POST})
    public Object getOrder(ServiceContext ctx, HttpServletRequest req, HttpServletResponse resp,
                           @PathVariable("productId") String productId, Params params) throws ParseException, IOException {
        if (!ctx.hasAccountId())
            return redirect("/signin");

        Record product = publishService.getProduct(ctx, productId, false, false);
        if (product == null)
            throw new ServiceException(Errors.E_ILLEGAL_PRODUCT, "Illegal product");

        String appId = product.asString("app_id");
        setupNavigationData(ctx, req, resp, appId);

        Paging paging = params.getPaging(10);
        String productName = params.param("product_name").asString();
        String version = params.param("product_version").asString();
        String orderStartDate = params.param("orderStartDate").asString();
        String orderEndDate = params.param("orderEndDate").asString();
        String orderMonth = params.param("orderMonth").asString();

        params.put("product_id", productId);

        if (!params.param("export").asString("").isEmpty()) {
            params.removeParams("orderMonth");
            RecordsWithTotal records = orderService.getOrder(ctx, params, new Paging(0, 50000));
            for (Record r : records.getRecords()) {
                String country = (String) r.get("purchaser_locale");
                if (StringUtils.isNotBlank(country)) {
                    country = StringUtils.substringAfter(country, "_");
                    r.set("purchaser_locale", country);
                }
            }
            return getCsv(ctx, resp, records.getRecords());
        }
        RecordsWithTotal records = orderService.getOrder(ctx, params, paging);
        for (Record r : records.getRecords()) {
            String country = (String) r.get("purchaser_locale");
            if (StringUtils.isNotBlank(country)) {
                country = StringUtils.substringAfter(country, "_");
                r.set("purchaser_locale", country);
            }
        }


        return new ModelAndView("publish_productOrders", CC.map(
                "pages=>", paging.getPage(),
                "product_name=>", productName,
                "product_version=>", version,
                "orderStartDate=>", orderStartDate,
                "orderEndDate=>", orderEndDate,
                "orderMonth=>", orderMonth,
                "count=>", paging.getCount(),
                "product=>", product,
                "records=>", records.getRecords(),
                "total=>", records.getTotal()));


    }


    @RequestMapping(value = "/publish/products/{productId:.+}/comments", method = {RequestMethod.GET, RequestMethod.POST})
    public Object getComments(ServiceContext ctx, HttpServletRequest req, HttpServletResponse resp,
                              @PathVariable("productId") String productId, Params params) throws ParseException, IOException {
        if (!ctx.hasAccountId())
            return redirect("/signin");

        Record product = publishService.getProduct(ctx, productId, false, false);
        if (product == null)
            throw new ServiceException(Errors.E_ILLEGAL_PRODUCT, "Illegal product");

        String appId = product.asString("app_id");
        setupNavigationData(ctx, req, resp, appId);

        Paging paging = params.getPaging(10);
        String productName = params.param("product_name").asString();
        Integer version = params.param("product_version").asIntObject();
        String orderStartDate = params.param("orderStartDate").asString();
        String orderEndDate = params.param("orderEndDate").asString();
        String orderMonth = params.param("orderMonth").asString();

        params.put("product_id", productId);

        RecordsWithTotal records = commentService.listComments(ctx, productId, version, paging);

        return new ModelAndView("publish_productComments", CC.map(
                "pages=>", paging.getPage(),
                "product_name=>", productName,
                "product_version=>", version,
                "orderStartDate=>", orderStartDate,
                "orderEndDate=>", orderEndDate,
                "orderMonth=>", orderMonth,
                "count=>", paging.getCount(),
                "product=>", product,
                "records=>", records.getRecords(),
                "total=>", records.getTotal()));


    }

    private Object getCsv(ServiceContext ctx, HttpServletResponse response, Records records) throws IOException {
        //1， 查找对象列表
        List callLogList = new ArrayList();

        //组织字符串,注意添加换行符
        StringBuilder bf = new StringBuilder();

        bf.append(SpringMessage.get("order.text.productName", ctx)).append(",")
                .append(SpringMessage.get("order.text.productVersion", ctx))
                .append(",").append(SpringMessage.get("order.text.productCategory", ctx))
                .append(",").append(SpringMessage.get("order.text.orderDate", ctx))
                .append(",").append(SpringMessage.get("order.text.purchaser", ctx))
                .append(",").append(SpringMessage.get("order.text.purchaserCountry", ctx))
                .append(",").append(SpringMessage.get("order.text.orderId", ctx)).append("\n");

        for (Record r : records) {
            bf.append(r.get("name")).append(",").
                    append(r.get("product_version")).append(",").
                    append(r.get("product_category_id")).append(",").
                    append(r.get("created_at")).append(",").
                    append(r.get("purchaser_id")).append(",").
                    append(r.get("purchaser_locale")).append(",").
                    append(r.get("id")).append("\n");
        }
        //输出文件
        response.setContentType("application/csv;charset=utf-8");
        response.setCharacterEncoding("UTF-8");
        String filenamedisplay = URLEncoder.encode("OrderReport.csv", "UTF-8");
        response.addHeader("Content-Disposition", "attachment;filename="
                + filenamedisplay);
        OutputStream osw = new BufferedOutputStream(response.getOutputStream());
        OutputStreamWriter os = new OutputStreamWriter(osw, "UTF-8");

        //add utf-8 encoding to file
        osw.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
        os.write(bf.toString());
        os.flush();
        os.close();
        osw.close();
        /*PrintWriter pw = new PrintWriter(response.getOutputStream());

        pw.write(bf.toString());
        pw.flush();
        pw.close();*/
        return null;
    }

    private static class TemporaryFile {
        static final String KEY = "rC7yB18h0JjE";

        final String filename;
        final String orgFilename;
        final long createdAt;

        private TemporaryFile(String filename, String orgFilename, long createdAt) {
            this.filename = filename;
            this.orgFilename = orgFilename;
            this.createdAt = createdAt;
        }

        public String encode() {
            String json = JsonUtils.toJson(CC.map(
                    "file=>", filename,
                    "orgFile=>", orgFilename,
                    "createdAt=>", createdAt
            ), false);
            return EncryptUtils.desEncryptBase64(json, KEY);
        }

        public static TemporaryFile decode(String s) {
            try {
                String json = EncryptUtils.desDecryptBase64(s, KEY);
                JsonNode jn = JsonUtils.parseJson(json);
                return new TemporaryFile(jn.path("file").asText(), jn.path("orgFile").asText(), jn.path("createdAt").asLong());
            } catch (IOException e) {
                throw new ServiceException(Errors.E_ILLEGAL_PARAM, "State error");
            }
        }
    }
}
