#!/usr/bin/python

from command import *
import os

if os.path.isfile('/home/wutong/.bpid/notif_mq_receiver.pid'):
    execCmd('kill -9 `cat /home/wutong/.bpid/notif_mq_receiver.pid`')
    os.remove('/home/wutong/.bpid/notif_mq_receiver.pid')
