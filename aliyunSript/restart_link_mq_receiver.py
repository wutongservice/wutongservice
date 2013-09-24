#!/usr/bin/python

import os

print "Stop link mq receiver ..."
os.system('./stop_link_mq_receiver.py')
print "Start link mq receiver ..."
os.system('./start_link_mq_receiver.py')
