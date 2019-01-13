# convert jpg files to png with imagemagic docker image

import os
import subprocess

# input = '{{input}}'
# output = '{{output}}'

input = '/home/dru/git/pyaas/src/test/resources/input'
output = '/home/dru/git/pyaas/src/test/resources/output'

for f in os.listdir(input):
    if not f.endswith(".png"):
        continue
    #mount input and output folder
    cmd = 'sudo docker run --rm -v {}:/images -v {}:/out v4tech/imagemagick convert /images/{} /out/{}'.format(input,
                                                                                                                  output,
                                                                                                                  f,
                                                                                                                  f.replace(
                                                                                                                      ".png",
                                                                                                                      ".jpg"))

    subprocess.call(cmd, shell=True)
