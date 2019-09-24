import os

input = os.getcwd()

#output created by service
output = os.path.join(os.getcwd(), 'output')

input_files = [f for f in os.listdir(input) if os.path.isfile(f)]

for f in input_files:
    content = open(os.path.join(input, f)).read()
    result = open(os.path.join(output, "result.txt"), 'a+')
    result.write(content)
    result.close()
