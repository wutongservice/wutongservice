#!/usr/bin/python

import csv
import sys
import telnetlib
import thread
import time

try:
    from termcolor import colored
    SUPPORT_COLOR = True
except:
    global colored
    SUPPORT_COLOR = False

COL_NAME              = 'n'
COL_THREAD            = 'T'
COL_LEVEL             = 'l'
COL_MESSAGE           = 'm'
COL_TIMESTAMP         = 't'
COL_ERROR             = 'E'

COL_REMOTE            = 'r'
COL_ACCESS            = 'a'
COL_VIEWER            = 'v'
COL_APP               = 'A'
COL_USER_AGENT        = 'u'
COL_INTERNAL          = 'i'
COL_PRIVACY_ENABLED   = 'p'

COLUMNS = 'tlnTravAuipmE'

IDX_NAME = COLUMNS.index(COL_NAME)
IDX_THREAD = COLUMNS.index(COL_THREAD)
IDX_LEVEL = COLUMNS.index(COL_LEVEL)
IDX_MESSAGE = COLUMNS.index(COL_MESSAGE)
IDX_TIMESTAMP = COLUMNS.index(COL_TIMESTAMP)
IDX_ERROR = COLUMNS.index(COL_ERROR)
IDX_REMOTE = COLUMNS.index(COL_REMOTE)
IDX_ACCESS = COLUMNS.index(COL_ACCESS)
IDX_VIEWER = COLUMNS.index(COL_VIEWER)
IDX_APP = COLUMNS.index(COL_APP)
IDX_USER_AGENT = COLUMNS.index(COL_USER_AGENT)
IDX_INTERNAL = COLUMNS.index(COL_INTERNAL)
IDX_PRIVACY_ENABLED = COLUMNS.index(COL_PRIVACY_ENABLED)

COLOR_MAP = {
    'TRACE':'blue',
    'DEBUG':'green',
    'OPER':'cyan',
    'INFO':'white',
    'WARN':'magenta',
    'ERROR':'red',
}


def make_row_filter(argv):
    filters = []
    for arg in argv[1:]:
        if arg.startswith('-f'):
            filters.append('(' + arg[2:] + ')')
    
    if filters: 
        return eval('lambda n, T, l, m, t, E, r, a, v, A, u, i, p: ' +' and '.join(filters))
    else:
        return lambda n, T, l, m, t, E, r, a, v, A, u, i, p: True
 
def write_row(row, cols, color):
    if SUPPORT_COLOR and color:
        level = row[IDX_LEVEL]
        clr = COLOR_MAP[level]
        if cols:
            for c in cols:
                idx = COLUMNS.index(c)
                if c >= 0:
                    print colored(row[idx] + ',', clr),
            print 
        else:
            print colored(','.join(row) + ',', clr)

    else:
        if cols:
            for c in cols:
                idx = COLUMNS.index(c)
                if c >= 0:
                    print row[idx] + ',',
            print 
        else:
            print ','.join(row) + ',', 'green'
    

def get_output_cols(argv):
    cols = ''
    for arg in argv[1:]:
        if arg.startswith('-c'):
            cols += arg[2:]
    return cols
    
def get_output_color(argv):
    for arg in argv[1:]:
        if arg == '-C':
            return True
    return False

def get_host(argv):
    for arg in argv[1:]:
        if arg.startswith('-H'):
            return arg[2:]
    return None
    
def get_port(argv):
    for arg in argv[1:]:
        if arg.startswith('-p'):
            return int(arg[2:])
    return 11300
    
def get_init_cmds(argv):
    cmds = []
    for arg in argv[1:]:
        if arg.startswith('-v'):
            cmds.append('view ' + arg[2:])
        if (arg.startswith('-r')):
            cmds.append('remote ' + arg[2:])
    cmds.append('on')
    return cmds

def is_help(argv):
    for arg in argv[1:]:
        if arg == '-h':
            return True
    return False

        
def get_line_iterator(tn):
    class TelnetLineIterator:
        def __init__(self, tn):
            self.tn = tn
        def __iter__(self):
            return self
        def next(self):
            try:
                while True:
                    s = tn.read_until('\n').strip('\r\n')
                    if s:
                        return s
            except EOFError:
                raise StopIteration()
            
    return TelnetLineIterator(tn)
    
def print_usage():
    print 'usage:'
    print '  python logview.py -cCOLS -fCONDS -Hhost -pport -vuserIds -rremotes -C'
    print '     -cCOLS   - log columns e.g. -c"tlm"'
    print '                COLUMNS:'
    print '                   n - log name'
    print '                   T - thread name'
    print '                   l - log level (DEBUG|OPER|INFO|WARN|ERROR)'
    print '                   m - log message'
    print '                   t - log timestamp'
    print '                   r - remote address'
    print '                   a - access id'
    print '                   v - viewer user id'
    print '                   A - APP id'
    print '                   u - User-Agent'
    print '                   i - internal call (0|1)'
    print '                   p - privacy enabled (0|1)'
    print '     -fCONDS  - filter condition e.g. -f\'l=="DEBUG"\''
    print '     -Hhost   - telnet server host'
    print '     -pport   - telnet server port (default 11300)'
    print '     -vusers  - Only output log for specified users when telnet remote e.g. -v10012,10013'
    print '     -rremotes - Only output log for specified ip when telnent remote e.g. -r192.168.5.189'
    print '     -C       - Output colorful log if you install python module "termcolor"'
            

   
def main(argv):
    if is_help(argv):
        print_usage()
        return

    filter = make_row_filter(argv)
    cols = get_output_cols(argv)
    color = get_output_color(argv)
    host = get_host(argv)
    
    if host:
        tn = telnetlib.Telnet(host, get_port(argv))
        input = get_line_iterator(tn)
    else:
        input = sys.stdin
        
    reader = csv.reader(input, delimiter=',', quotechar='"')
          
    def print_rows():
        for row in reader:
            if filter(row[IDX_NAME], 
                  row[IDX_THREAD], 
                  row[IDX_LEVEL], 
                  row[IDX_MESSAGE], 
                  row[IDX_TIMESTAMP], 
                  row[IDX_ERROR], 
                  row[IDX_REMOTE],
                  row[IDX_ACCESS], 
                  row[IDX_VIEWER], 
                  row[IDX_APP], 
                  row[IDX_USER_AGENT],
                  row[IDX_INTERNAL],
                  row[IDX_PRIVACY_ENABLED]):
                write_row(row, cols, color)
        
    if host:
        t = thread.start_new_thread(print_rows, ())
        for cmd in get_init_cmds(argv):
            tn.write(cmd + '\n')
            
        while True:
            cmd = raw_input().strip()
            tn.write(cmd + '\n')
            if cmd == 'exit' or cmd == 'quit':                
                break
            
        print 'Bye bye'
        time.sleep(3)
    else:
        print_rows()
        
    
if __name__ == '__main__':
    main(sys.argv)
