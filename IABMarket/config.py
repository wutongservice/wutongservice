__author__ = 'rongxin.gao@borqs.com'

import ConfigParser
import os.path

def _read_config():
    cfg = ConfigParser.ConfigParser()
    cfg.read(os.path.join(os.path.dirname(__file__), u'config.ini'))
    return cfg

# Config
_config = _read_config()


def get(section, option, defval=None):
    return _config.get(section, option) if _config.has_option(section, option) else defval

def getint(section, option, defval=None):
    return _config.getint(section, option) if _config.has_option(section, option) else defval

def getboolean(section, option, defval=None):
    return _config.getboolean(section, option) if _config.has_option(section, option) else defval