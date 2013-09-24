package com.borqs.server.market.controllers;


import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.service.OrderService;
import com.borqs.server.market.utils.CC;
import com.borqs.server.market.utils.Params;
import com.borqs.server.market.utils.i18n.SpringMessage;
import com.borqs.server.market.utils.mybatis.record.RecordsWithTotal;
import com.borqs.server.market.utils.record.Record;
import com.borqs.server.market.utils.record.Records;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/")
public class OrderController extends AbstractController {

    protected LocaleResolver localeResolver;
    protected OrderService orderService;

    public OrderController() {
    }

    public LocaleResolver getLocaleResolver() {
        return localeResolver;
    }

    @Autowired
    @Qualifier("localeResolver")
    public void setLocaleResolver(LocaleResolver localeResolver) {
        this.localeResolver = localeResolver;
    }


    @Autowired
    @Qualifier("service.orderService")
    public void setOrderService(OrderService orderService) {
        this.orderService = orderService;
    }


    @RequestMapping(value = "/getOrder", method = {RequestMethod.GET, RequestMethod.POST})
    public Object getOrder(ServiceContext ctx, HttpServletRequest req, HttpServletResponse resp, Params params) throws ParseException, IOException {

        int pages = (Integer) params.getInt("pages", 1);
        int count = (Integer) params.getInt("count", 10);
        String product_name = params.getString("product_name", null);
        String product_version = params.getString("product_version", null);
        String orderStartDate = params.getString("orderStartDate", null);
        String orderEndDate = params.getString("orderEndDate", null);
        String orderMonth = params.getString("orderMonth", null);


        if (params.getString("export", "").length() > 0) {
            RecordsWithTotal records = orderService.getOrder(ctx, params, 0, 50000);
            for (Record r : records.getRecords()) {
                String country = (String) r.get("purchaser_locale");
                if (StringUtils.isNotBlank(country)) {
                    country = StringUtils.substringAfter(country, "_");
                    r.set("purchaser_locale", country);
                }
            }
            return getCsv(ctx, resp, records.getRecords());
        }
        RecordsWithTotal records = orderService.getOrder(ctx, params, count * (pages - 1), count);
        for (Record r : records.getRecords()) {
            String country = (String) r.get("purchaser_locale");
            if (StringUtils.isNotBlank(country)) {
                country = StringUtils.substringAfter(country, "_");
                r.set("purchaser_locale", country);
            }
        }


        return new ModelAndView("order", CC.map(
                "pages=>", pages,
                "product_name=>", product_name,
                "product_version=>", product_version,
                "orderStartDate=>", orderStartDate,
                "orderEndDate=>", orderEndDate,
                "orderMonth=>", orderMonth,
                "count=>", count,
                "records=>", records.getRecords(),
                "total=>", records.getTotal()));
    }


    /**
     * export csv
     *
     * @param ctx
     * @param response
     * @param records
     * @return
     * @throws IOException
     */
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
        response.setContentType("application/x-download;charset=utf-8");
        response.setCharacterEncoding("UTF-8");
        String filenamedisplay = URLEncoder.encode("OrderReport.csv", "UTF-8");
        response.addHeader("Content-Disposition", "attachment;filename="
                + filenamedisplay);
        PrintWriter pw = new PrintWriter(response.getOutputStream());

        pw.write(bf.toString());
        pw.flush();
        pw.close();
        return null;
    }
}
