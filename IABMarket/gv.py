__author__ = 'rongxin.gao@borqs.com'

import dbwrapper
import flask
import httpclient
import config
import os


# DB
db = dbwrapper.DBWrapper(
    host=config.get(u'db', u'host'),
    port=config.getint(u'db', u'port', 3306),
    user=config.get(u'db', u'user'),
    passwd=config.get(u'db', u'passwd', u''),
    db=config.get(u'db', u'db'))

# Http client
wutong_client = httpclient.HttpClient(config.get(u'wutong', u'server', u'http://apitest.borqs.com'))

# WebApp
app = flask.Flask(u'IABMarket', static_folder=u'market')
upload_folder =  os.path.join(os.path.dirname(__file__), u'upload')
if not os.path.exists(upload_folder):
    os.mkdir(upload_folder)
app.config[u'UPLOAD_FOLDER'] = upload_folder
app.secret_key = 'xHVGDBB1RDDWYlp5zApBcQ==' # do not change
app.jinja_options = {'extensions': ['jinja2.ext.do']}

@app.route(u'/upload/<path:filename>')
def base_static(filename):
    return flask.send_from_directory(upload_folder, filename)











