#!/usr/bin/python

import os
import os.path
import sys
import glob

def replace_text_in_file(f, old, new):
    s = open(f).read()
    s = s.replace(old, new)
    ff = open(f, 'w')
    ff.write(s)
    ff.close()

def read_lines(path, strip=True):
    f = None
    try:
        a = []
        f = open(path, 'r')
        while 1:
            line = f.readline()
            if not line:
                break
            a.append(line.strip() if strip else line)
        return a
    finally:
        if f:
            f.close()

class Host(object):
    def __init__(self, name='', host='', user='', pwd='', dir=''):
        super(Host, self).__init__()
        self.name, self.host, self.user, self.pwd, self.dir = name, host, user, pwd, dir

    @staticmethod
    def parse(s):
        name, host, user, pwd, dir = s.split(':')
        return Host(name, host, user, pwd, dir)

    @staticmethod
    def load_hosts(path):
        a = []
        for line in read_lines(path):
            a.append(Host.parse(line))
        return a

    @staticmethod
    def load_host(path, name):
        hosts = Host.load_hosts(path)
        for host in hosts:
            if host.name == name:
                return host
        return None

def load_host(host_name):
    return Host.load_host(os.path.abspath(sys.path[0] + "/../hosts/hosts"), host_name)

def remote_do(host, cmd):
    remote_do_script = os.path.join(sys.path[0], 'remote_do')
    os.system("expect %s '%s' '%s' '%s' '%s'" % (remote_do_script, host.host, host.user, host.pwd, cmd))

def upload_dir(host, local_dir, remote_dir):
    upload_dir_script = os.path.join(sys.path[0], 'upload_dir')
    os.system("expect %s '%s' '%s' '%s' '%s' '%s'" % (upload_dir_script, host.host, host.user, host.pwd, local_dir, remote_dir))

def force_upload_dir(host, local_dir, remote_dir):
    remote_do(host, "rm -rf %s" % remote_dir)
    upload_dir(host, local_dir, remote_dir)

def copy_dir(src, dst):
    print "cp %s/* %s" % (src, dst)
    os.system("cp %s/* %s" % (src, dst))

def dist_bin(host):
    local = os.path.abspath(sys.path[0] + "/../bin")
    remote = os.path.join(host.dir, 'bin')
    force_upload_dir(host, local, remote)

def dist_lib(host):
    local = os.path.abspath(sys.path[0] + "/../lib")
    remote = os.path.join(host.dir, 'lib')
    force_upload_dir(host, local, remote)

def dist_mod(host):
    local = os.path.abspath(sys.path[0] + "/../mod")
    remote = os.path.join(host.dir, 'mod')
    force_upload_dir(host, local, remote)


def dist_etc(host):
    TMP_DIR = '/tmp/borqs_server_dist_tmp'

    etc_dir = os.path.abspath(sys.path[0] + "/../etc")
    os.system("rm -rf '%s'" % TMP_DIR)
    os.mkdir(TMP_DIR)
    copy_dir(os.path.join(etc_dir, 'share'), TMP_DIR)
    copy_dir(os.path.join(etc_dir, host.name), TMP_DIR)
    remote = os.path.join(host.dir, 'etc')
    force_upload_dir(host, TMP_DIR, remote)

def dist_log(host):
    remote = os.path.join(host.dir, 'log')
    remote_do(host, 'mkdir %s' % remote)

def dist_plugin(host):
    remote = os.path.join(host.dir, 'plugin')
    remote_do(host, 'mkdir %s' % remote)

def dist_pid(host):
    remote = os.path.join(host.dir, 'pid')
    remote_do(host, 'mkdir %s' % remote)

def dist_doc(host):
    local = os.path.abspath(sys.path[0] + "/../doc")
    remote = os.path.join(host.dir, 'doc')
    force_upload_dir(host, local, remote)

def dist_tool(host):
    remote = os.path.join(host.dir, 'tool')
    remote_do(host, 'mkdir %s' % remote)


def stop(host):
    pass

def start(host):
    pass

def dist(host):
    stop(host)
    dist_bin(host)
    dist_etc(host)
    dist_mod(host)
    dist_lib(host)
    dist_plugin(host)
    dist_log(host)
    dist_pid(host)
    dist_doc(host)
    start(host)


def print_usage():
    print 'Usage:'
    print '  dist host1 host2 ..'

def main():
    if len(sys.argv) <= 1:
        print_usage()
        return

    hosts = []
    for host_name in sys.argv[1:]:
        hosts.append(load_host(host_name))

    for host in hosts:
        dist(host)
        print '==============================================='
        print 'dist %s' % host.name

if __name__ == '__main__':
    main()