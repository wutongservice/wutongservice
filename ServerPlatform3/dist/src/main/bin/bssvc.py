#!/usr/bin/python

import os
import os.path
import sys
import time

import bsapp

# (svc_name, pid_file_name, config_file_name, logback_config_file_name)
SERVICES = [
    ('web', 'web.pid', 'conf.web', 'log'),
    ('recv', 'recv.pid', 'conf.recv', 'log'),
    ('sched', 'sched.pid', 'conf.sched', 'log'),
]

def print_usage():
    print 'Usage:'
    print '  bssvc start|stop|restart %s' % '|'.join([svc[0]  for svc in SERVICES])

def get_service(svc_name):
    for svc in SERVICES:
        if svc[0] == svc_name:
            return svc
    return None

def has_svc(svc_name):
    for svc in SERVICES:
        if svc[0] == svc_name:
            return True
    return False

def get_pid_path(base, svc):
    if svc:
        return os.path.join(base, 'pid/'+ svc[1])
    else:
        return None

def get_log_config_path(base, svc):
    if svc:
        return os.path.join(base, 'etc/%s.xml' % svc[3])
    else:
        return None


def do(base, action, svc_name):
    if action == 'start':
        svc = get_service(svc_name)
        arg1 = 'main_bean=%s#main:daemon=1:-Dpid.path=%s:-Dlogback.configurationFile=%s:-DBS_HOME=%s' % (svc[2], get_pid_path(base, svc), get_log_config_path(base, svc), base)
        bsapp.main([os.path.join(base, 'bin/bsapp'), arg1])
    elif action == 'stop':
        svc = get_service(svc_name)
        os.system('kill -15 `cat %s`' % get_pid_path(base, svc))
    elif action == 'restart':
        do(base, 'stop', svc_name)
        for i in xrange(3):
            time.sleep(1)
            print 'wait %s seconds...' % i
        do(base, 'start', svc_name)

def main(argv):
    if len(argv) != 3:
        print_usage()
        return

    base = os.path.abspath(sys.path[0] + '/..')
    action, svc_name = argv[1], argv[2]

    if action not in ['start', 'stop', 'restart']:
        print_usage()
        return

    if not has_svc(svc_name):
        print_usage()
        return

    do(base, action, svc_name)


if __name__ == '__main__':
    main(sys.argv)