# procaas
[![CircleCI](https://circleci.com/gh/zella/procaas/tree/master.svg?style=svg)](https://circleci.com/gh/zella/procaas/tree/master)

Process as service

This project(http server) allows to run process(something like bash over http) by http requests.

### Feautures 

**Excuting processes**

Examples:
    
`["ls" "-lah"]`
`["python", "-c", "yourscript", "arg1"]`
`["yourBinary"]`
    

**Read stdout, also in chunked mode**

for example if you have process like:

    for (i = 1 to 100)
	     doLongWork(i)
	     stdout(i+' percent')
	     
You can read response without buffering in chunked mode.

**Sending files and folders(in zip mode) along with process**

Files will be placed in unique working directory and cleaned after execution.

**Read result files**

You can grab files in `{workdir}/output` directory(also in zip mode). Output directory configurable - relative to process workdir

**Set environment variables to process**

**Specify computation strategy**

**io** - backed by cached unbouneded thread pool, so all submitted processes will be executed in parallel.

**cpu** - backed by thread pool with size of available processors, usefull for heavy cpu processes.

### Use cases

Remote control applications

Backend for "function as service". (You can run processes like "docker run --rm" or "python -c")

Remote worker

### Api

Here single entry point

**`POST /process`**

It accepts `multipart/form-data` with single json parameter `data` and files.

**Parameters of `data`:**

**`cmd`** - string array, required. 

  Example: `["ls","-lah"]`
  
  **`zipInputMode`** - boolean, required, default = false.
  
  If mode enabled, you should pass single zip file. It will be unpacked to workdir of process. It uses, if you want preserve directory structure.
If disabled(by default), all files will be stored in workdir. Workdir will be cleaned after execution(sucessfull or not)

  Example: `true`
  
**`stdin`** - string, optional.

Process stdin, example: python in interactive mode.

Example: `some stdin here`

**`envs`** - JsonObject, optional.

Environment variables for process

Example: `{"MY_NAME":"1991","MY_YEAR":"DRU"}`

**`outPutMode`** - string, optional, default = stdout

How pass result of execution:

`stdout` - simple grab stdout  
`chunkedStdout`- grab and emits stdout by line, chunked transfer encoding  
`zip`- pack all files(and dirs) inside `{workdir}/output/` folder. There no way to transfer multiple files unpacked(but here no compression)  
`file` - return single file. Expect process produce singe file, throw error otherwise.

Example: `zip`

**`outputDir`** - string, optional, default = output

Name of folder where files will be grabbed

Example: `someFolderName`

**`timeoutMillis`** - long, optional, default - see configuration section

Process timeoutin in millis

Example: `60000`

**`computation`** - set computaion strategy, default - io

`io`- backed by cached unbouneded thread pool, so all submitted processes will be executed in parallel.

`cpu`- backed by thread pool with size of available processors, usefull for heavy cpu processes.

Example: `cpu`

So minimal `data` will be:

	{
	   "cmd" : ["echo", "Hello world"]
	}
	
#### About errors:
In case of wrong input parameters or wrong process exit code, 400 will be thrown with stack trace (and 32 stderr lines in case of process failure)

### Configuration

Server configured with env vars(defaults on right side):

`HTTP_PORT: 8666`
`WORK_DIR: /tmp/procaas_workdir` - process-specific dirs created here
`DEFAULT_OUTPUT_DIR_NAME: output` - see above
`PROCESS_TIMEOUT: 5m` - default process timeout, hocon syntax

### Installation

1) Run in docker

Here basic image available on docker hub: `zella/procaas`:

	FROM openjdk:11

	ENV HTTP_PORT=8666
	COPY ci/procaas.jar /app/
	EXPOSE ${HTTP_PORT}
	HEALTHCHECK CMD curl --fail -s http://localhost:${HTTP_PORT}/about || exit 1
	ENTRYPOINT java -jar /app/procaas.jar
	
You can extend it and install what you need, or just use your image - install jdk11 and copy single jar (available in releases TODO).

2) Run standalone executable jar. Required java11 installed.

java -jar procaas-xxx.jar

### How to build

Install **scala sbt**
$cd procaas
$sbt assembly
Find procaas-xxx.jar in target/scala-2.12/
