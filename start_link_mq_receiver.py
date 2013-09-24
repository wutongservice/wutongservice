#!/usr/bin/python

import os

if os.path.isfile('/home/wutong/.bpid/link_mq_receiver.pid') == False:
    os.system('nohup ./run_memcached_server &')
else:
    print "The process is running, please restart the program."
