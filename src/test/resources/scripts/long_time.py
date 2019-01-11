output = '/home/dru/git/pyass/py/out'

import os

from time import sleep
sleep(1) #1sec

result = open(os.path.join(output, 'someOutFile'), 'a+')
result.write('ok')
result.close()