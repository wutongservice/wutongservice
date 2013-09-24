#!/usr/bin/python

from command import *
import os

if os.path.isfile('/home/wutong/.bpid/main.pid'):
    execCmd('kill -9 `cat /home/wutong/.bpid/main.pid`')
    os.remove('/home/wutong/.bpid/main.pid')
