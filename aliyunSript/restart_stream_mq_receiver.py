#!/usr/bin/python

import os

print "Stop stream mq receiver ..."
os.system('./stop_stream_mq_receiver.py')
print "Start stream mq receiver ..."
os.system('./start_stream_mq_receiver.py')
