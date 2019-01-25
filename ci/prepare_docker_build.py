import os
import shutil

path = 'target/scala-2.12/'

files = list((file for file in os.listdir(path)
         if os.path.isfile(os.path.join(path, file))))
print(files)
assert len(files) == 1
assert files[0].endswith('.jar')
jar = os.path.join(path, files[0])
shutil.copy(jar, 'ci/procaas.jar')
