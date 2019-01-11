input = '{{input}}'
output = '{{output}}'

import os

files = os.listdir(input)
for f in files:
    content = open(os.path.join(input, f)).read()
    result = open(os.path.join(output, "result.txt"), 'a+')
    result.write(content)
    result.close()
