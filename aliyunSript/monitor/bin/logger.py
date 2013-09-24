#!/usr/bin/python

import os
import logging

DEF_BASE_PATH = os.path.abspath(os.path.dirname(__file__))
DEF_LOG_FORMAT = '[%(asctime)s] %(levelname)s %(message)s'
DEF_LOG_FILENAME = 'monitor.log'
DEF_LOG_LEVEL = logging.INFO

def init_config(path=DEF_BASE_PATH, format=DEF_LOG_FORMAT, filename=DEF_LOG_FILENAME, level=DEF_LOG_LEVEL):
    logging.basicConfig(level=level,
        format=format,
        filename=os.path.join(path, filename),
        filemode='a')
		