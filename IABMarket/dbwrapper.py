__author__ = 'rongxin.gao@borqs.com'

import MySQLdb
import DBUtils.PooledDB
import config

SQL_ESCAPES = {
    '\\':'\\\\',
    '\0':'\\0',
    "'":"\\'",
    '"':'\\"',
    '\b':'\\b',
    '\n':'\\n',
    '\r':'\\r',
    '\t':'\\t',
}
def sqlval(v):
    import StringIO
    if isinstance(v, basestring):
        buff = StringIO.StringIO()
        buff.write("'")
        for c in v:
            if c in SQL_ESCAPES:
                buff.write(SQL_ESCAPES[c])
            else:
                buff.write(c)
        buff.write("'")
        return unicode(buff.getvalue())
    elif isinstance(v, list):
        return u','.join([sqlval(x) for x in v])
    else:
        return str(v)

def _cursor_execute(cursor, sql, params=None):
    cursor.execute(sql, params)
    if config.getboolean(u'misc', u'debug', False):
        print cursor._last_executed

class Fetch(object):
    def __init__(self, sql, cursor_handler, sql_params=None):
        self.sql = sql
        self.cursor_handler = cursor_handler
        self.sql_params = sql_params

    def __call__(self, conn):
        cursor, result = None, None
        try:
            cursor = conn.cursor(cursorclass=MySQLdb.cursors.DictCursor)
            _cursor_execute(cursor, self.sql, self.sql_params)
            result = self.cursor_handler(cursor)
        finally:
            if cursor:
                cursor.close()
        return result

def FetchFirst(sql, params=None):
    return Fetch(sql, lambda cursor: cursor.fetchone(), sql_params=params)

def FetchAll(sql, params=None):
    return Fetch(sql, lambda cursor: cursor.fetchall(), sql_params=params)

def FetchColumn(col, sql, params=None, defval=None):
    def handle_cursor(cursor):
        a = []
        for row in cursor:
            a.append(row.get(col, defval))
        return a
    return Fetch(sql, handle_cursor, sql_params=params)

def FetchValue(sql, params=None, defval=None):
    def handle_cursor(cursor):
        row = cursor.fetchone()
        return row.values()[0] if row else defval
    return Fetch(sql, handle_cursor, sql_params=params)

class Update(object):
    def __init__(self, sql, params=None):
        self.sql = sql
        self.params = params

    def __call__(self, conn):
        cursor = None
        try:
            cursor = conn.cursor(cursorclass=MySQLdb.cursors.DictCursor)
            n = 0
            if isinstance(self.sql, basestring):
                if isinstance(self.params, list):
                    for params in self.params:
                        _cursor_execute(cursor, self.sql, params)
                        n += cursor.rowcount
                else:
                    _cursor_execute(cursor, self.sql, self.params)
                    n += cursor.rowcount
            elif isinstance(self.sql, list):
                for sql in self.sql:
                    if isinstance(sql, basestring):
                        _cursor_execute(cursor, sql)
                        n += cursor.rowcount
                    elif isinstance(sql, tuple):
                        if len(sql) == 1:
                            _cursor_execute(cursor, sql[0])
                            n += cursor.rowcount
                        elif len(sql) == 2:
                            _cursor_execute(cursor, sql[0], sql[1])
                            n += cursor.rowcount
                        else:
                            raise Exception(u'Illegal sql item')
            else:
                raise Exception(u"Illegal sql")
        finally:
            if cursor:
                cursor.close()
        return n




class DBWrapper(object):
    def __init__(self, **kvs):
        self.db = DBUtils.PooledDB.PooledDB(MySQLdb, mincached=0, maxcached=10, maxshared=10, maxusage=10000,
            setsession=[u'SET AUTOCOMMIT = 0'], host=kvs[u'host'], port=kvs.get(u'port', 3306), user=kvs[u'user'], passwd=kvs[u'passwd'], db=kvs[u'db'], charset=u'utf8')

    def open_conn(self, conn_handler):
        conn, result = None, None
        try:
            conn = self.db.connection()
            result = conn_handler(conn)
            conn.commit()
        except BaseException, e:
            conn.rollback()
            raise e
        finally:
            if conn:
                conn.close()
        return result

    def fetch(self, sql, cursor_handler, sql_params=None):
        return self.open_conn(Fetch(sql, cursor_handler, sql_params=sql_params))

    def fetch_first(self, sql, sql_params=None):
        return self.open_conn(FetchFirst(sql, params=sql_params))

    def fetch_all(self, sql, sql_params=None):
        return self.open_conn(FetchAll(sql, params=sql_params))

    def fetch_column(self, col, sql, sql_params=None, defval=None):
        return self.open_conn(FetchColumn(col, sql, params=sql_params, defval=defval))

    def fetch_value(self, sql, sql_params=None, defval=None):
        return self.open_conn(FetchValue(sql, params=sql_params, defval=defval))

    def update(self, sql, sql_params=None):
        return self.open_conn(Update(sql, params=sql_params))


