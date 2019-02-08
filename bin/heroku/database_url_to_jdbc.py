#!/usr/bin/env python

import sys
from urlparse import urlparse

url = urlparse(sys.argv[1])
jdbc_url = "jdbc:postgresql://%s:%d%s?user=%s&password=%s" % (url.hostname, url.port, url.path, url.username, url.password)

print(jdbc_url)
