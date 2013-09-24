#!/usr/bin/python

import os

print "Stop main process ..."
os.system('./stop_main.py')
print "Start main process ..."
os.system('./start_main.py')
