# convert jpg files to png with imagemagic docker image

import os
import subprocess

input = os.getcwd()

output = os.path.join(os.getcwd(), 'output')
os.mkdir(output)

for f in os.listdir(input):
    if not f.endswith(".png"):
        continue
    #mount input and output folder
    cmd = 'docker run --rm -v {}:/images -v {}:/out v4tech/imagemagick convert /images/{} /out/{}'.format(input,
                                                                                                                  output,
                                                                                                                  f,
                                                                                                                  f.replace(
                                                                                                                      ".png",
                                                                                                                      ".jpg"))

    subprocess.call(cmd, shell=True)
