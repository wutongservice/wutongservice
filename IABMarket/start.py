#!/usr/bin/python

__author__ = 'rongxin.gao@borqs.com'

import config
import gv

__import__('purchaser_controllers')
__import__('account_controllers')
__import__('publisher_controllers')

wsgi_server = config.get(u'web', u'wsgi_server', u'')
if wsgi_server.lower() == u'tornado':
    print u'Tornado start'
    from tornado.wsgi import WSGIContainer
    from tornado.httpserver import HTTPServer
    from tornado.ioloop import IOLoop
    server = HTTPServer(WSGIContainer(gv.app))
    server.listen(config.getint(u'web', u'port'), address=config.get(u'web', u'host', u''))
    IOLoop.instance().start()
else:
    print u'Flask start'
    gv.app.run(host=config.get(u'web', u'host', u'0.0.0.0'),
            port=config.getint(u'web', u'port', 6789),
            debug=config.getboolean(u'misc', u'debug', False))





