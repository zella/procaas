input = '/home/dru/git/pyass/py/in'
output = '/home/dru/git/pyass/py/out'

import os

files = os.listdir(input)
for f in files:
    content = open(os.path.join(input, f)).read()
    result = open(os.path.join(output, f), 'a+')
    result.write(content)
    result.close()
