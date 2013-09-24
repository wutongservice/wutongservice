#!/usr/bin/python

import os

if os.path.isfile('/home/wutong/.bpid/main.pid') == False:
    os.system('nohup ./run_test &')
else:
    print "The process is running, please restart the program."
