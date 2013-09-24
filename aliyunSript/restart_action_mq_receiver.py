#!/usr/bin/python

import os

print "Stop action mq receiver ..."
os.system('./stop_action_mq_receiver.py')
print "Start action mq receiver ..."
os.system('./start_action_mq_receiver.py')
