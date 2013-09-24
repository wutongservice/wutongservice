#!/usr/bin/python
from __future__ import unicode_literals

__author__ = 'rongxin.gao@borqs.com'

import requests
import json
from optparse import OptionParser
import sys


def _create_opt_parser():
    parser = OptionParser()
    parser.add_option("-r", "--remote", dest="remote", help="Host with port")
    parser.add_option("-t", "--ticket", dest="ticket", default='', help="Ticket")
    return parser


def _make_url(host, api):
    return host + api

def call_api(url, params):
    r = requests.get(url, params=params)
    print r.text
    print '====================='

def main():
    parser = _create_opt_parser()
    opts, args = parser.parse_args()
    try:
        host, ticket, appdesc = opts.remote, opts.ticket, json.load(open(args[0]))
    except Exception, e:
        print>> sys.stderr, e
        print>> sys.stderr, '============================================'
        parser.print_help(sys.stderr)
        return

    app_id = appdesc['id']

    call_api(_make_url(host, '/api/v2/tools/create_app'), {
        'ticket': ticket,
        'id': app_id,
        'name': json.dumps(appdesc['name'])
    })

    if 'categories' in appdesc:
        for category in appdesc['categories']:
            category_id = category['id']
            call_api(_make_url(host, '/api/v2/tools/add_category'), {
                'ticket': ticket,
                'app_id': app_id,
                'category_id': category_id,
                'name': json.dumps(category['name'])
            })
            if 'pricetags' in category:
                for pricetag in category['pricetags']:
                    pricetag_id = pricetag['id']
                    price = pricetag.get('price', None)
                    if price:
                        ps = {
                            'ticket': ticket,
                            'app_id': app_id,
                            'category_id': category_id,
                            'pricetag_id': pricetag_id,
                            'price': json.dumps(price)
                        }
                        if 'google_iab_sku' in pricetag:
                            ps['google_iab_sku'] = pricetag['google_iab_sku']
                        call_api(_make_url(host, '/api/v2/tools/add_paid_pricetag'), ps)
                    else:
                        call_api(_make_url(host, '/api/v2/tools/add_free_pricetag'), {
                            'ticket': ticket,
                            'app_id': app_id,
                            'category_id': category_id,
                            'pricetag_id': pricetag_id
                        })


if __name__ == '__main__':
    main()



