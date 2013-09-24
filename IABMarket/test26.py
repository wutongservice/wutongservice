
import jsonschema
import util

ML_JSON_SCHEMA = {
    u'type': u'object',
    u'properties': {
        u'default': {u'type':u'string'},
        u'zh_CN': {u'type':u'string'},
        u'en_US': {u'type':u'string'},
        },
    u'required': [u'default']
}


o = {u'default': u'ff'}

jsonschema.validate(util.kws_to_26(o), util.kws_to_26(ML_JSON_SCHEMA))