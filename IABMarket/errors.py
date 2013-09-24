__author__ = 'rongxin.gao@borqs.com'

E_OK = 0
E_UNKNOWN = 100
E_ILLEGAL_PARAM = 980
E_MISSING_IDENTITY = 103
E_ACCOUNT_ERROR = 104
E_ILLEGAL_PRODUCT = 107
E_ILLEGAL_PRODUCT_VERSION = 108
E_PERMISSION_DENIED = 200
E_TOO_SMALL_VERSION = 201


class APIError(Exception):
    def __init__(self, code, message):
        super(APIError, self).__init__()
        self.code, self.message = code, message