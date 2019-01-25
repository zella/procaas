import os

input = os.getcwd()

output = os.path.join(os.getcwd(), 'output')
os.mkdir(output)

input_files = [f for f in os.listdir(input) if os.path.isfile(f)]

for f in input_files:
    content = open(os.path.join(input, f)).read() + '_v2'
    result = open(os.path.join(output, f), 'a+')
    result.write(content)
    result.close()
