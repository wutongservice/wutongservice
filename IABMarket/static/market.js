BORQS_MARKET_HOST = 'http://apitest.borqs.com:6789';

BorqsMarket = {
    ajaxGet: function (api, params, func) {
        var uri = BORQS_MARKET_HOST + api;
        if (!$.isEmptyObject(params)) {
            var a = [];
            for (var k in params) {
                a.push(k + '=' + encodeURIComponent(params[k]))
            }
            uri += ('?' + a.join('&'))
        }

        $.get(uri,function (data) {
            if (data['code'] == 0) {
                func(data['data']);
            } else {
                alert(data['error_message']);
            }
        }).fail(function () {
                alert('ERROR');
            });
    },

    api_publisher_getAllApps: function (func) {
        this.ajaxGet('/api/v1/publisher/apps/all', {}, func);
    },

    api_publisher_getProducts: function(ticket, app, func) {
        this.ajaxGet('/api/v1/publisher/products/all', {ticket:ticket, app:app}, func);
    },

    api_publisher_getProduct: function(ticket, pid, version, lang, func) {
        this.ajaxGet('/api/v1/publisher/products/get', {ticket:ticket, id:pid, version:version, locale:lang}, func);
    },

    api_publisher_activeProduct: function(ticket, pid, flag, func) {
        this.ajaxGet('/api/v1/publisher/products/active', {ticket:ticket, id:pid, flag:flag}, func);
    },

    api_publisher_activeVersion: function(ticket, pid, version, flag, func) {
        this.ajaxGet('/api/v1/publisher/products/versions/active', {ticket:ticket, id:pid, version:version, flag:flag}, func);
    }
}