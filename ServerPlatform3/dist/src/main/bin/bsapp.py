#!/usr/bin/python

import sys
import os
import os.path

# print usage
def print_usage():
    print 'Usage:'
    print '  baspp options|_  program arguments'
    print 'Example 1: start java app'
    print '  bsapp vmopts=default:main_class=your.package.MainClass arg1 arg2'
    print 'Example 2: start spring based app'
    print '  bsapp vmopts=default:main_bean=spring your_spring_beans_config_file_in_etc.mainBeanId arg1 arg2'
    print 'Example 3: start app with default options'
    print '  baspp main_class=your.package.MainClass arg1 arg2'

# parse_options('vmopts=server:main_class=Main') => {'vmopts':'server', 'main_class':'Main'}
def parse_options(s):
    opts = {}
    for item in s.split(':'):
        k, v = item.split('=')
        opts[k] = v
    return opts

# path_in_etc('/base/dir', 'default.vmopts') => '/base/dir/etc/default.vmopts'
def path_in_etc(base, file):
    return os.path.join(base , 'etc/' + file)

# vmopts_in_etc('/base/dir', 'default.vmopts') => '-Xms128m -Xmx512m'
def vmopts_in_etc(base, file):
    try:
        path = path_in_etc(base, file)
        a = []
        f = open(path)
        while 1:
            line = f.readline()
            if not line:
                break
            a.append(line.strip())
        f.close()
        return ' '.join(a)
    except Exception, e:
        return ' '

# get_props({'vmopts':'default', '-Dprop1':'val1', '-Dprop2':'val2'}) => "'-Dprop1=val1' '-Dprop2=val2'"
def get_props(opts):
    a = ["'%s=%s'" % (k, opts[k]) for k in opts.keys() if k.startswith('-D')]
    return ' '.join(a)

# execute
def execute(cmd, cmd_args, print_cmd, daemon):
    line = cmd + ' ' + cmd_args
    if daemon:
        line = 'nohup ' + line + '>/dev/null&'
    if print_cmd:
        print line
    os.system(line)

# main
def main(argv):
    if len(argv) <= 1:
        print_usage()
        return

    if 'win' in sys.platform and sys.platform != 'darwin':
        sep = ';'
    else:
        sep = ':'
    base = os.path.abspath(argv[0] + '/../..')
    cp = sep.join([os.path.join(base, s + '/*') for s in ['lib', 'plugin']])
    opts = parse_options(argv[1])
    args = ' '.join(["'%s'"%s for s in argv[2:]])
    daemon = opts.get('daemon', '0')
    os.putenv('BS_HOME', base)

    vmopts = vmopts_in_etc(base, opts.get('vmopts', 'default') + '.vmopts')
    if 'main_class' in opts:
        cmd_args = "-classpath %(cp)s %(vmopts)s %(props)s %(main_class)s %(args)s" % {'cp':cp, 'vmopts':vmopts, 'props':get_props(opts), 'main_class':opts['main_class'], 'args':args}
        execute('java', cmd_args, opts.get('print', '0') != '0')
    elif 'main_bean' in opts:
        beans_conf_file, main_bean_id = opts['main_bean'].split('#')
        conf_path = path_in_etc(base, beans_conf_file+'.xml')
        cmd_args = "-classpath %(cp)s %(vmopts)s %(props)s com.borqs.server.platform.app.AppBootstrap %(beans_conf)s %(main_bean)s %(args)s" % {'cp':cp, 'vmopts':vmopts, 'props':get_props(opts), 'beans_conf':conf_path, 'main_bean':main_bean_id, 'args':args}
        execute('java', cmd_args, opts.get('print', '0') != '0', daemon)
    else:
        print_usage()



# GO!
if __name__ == '__main__':
    main(sys.argv)


