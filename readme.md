# jBgJob

jBgJob (Java Background Job) lets you schedule Java jobs to be ran in the background.  They can run in any combination of other threads in the same JVM, other JVMs, or multiple other JVMs, even on different machines.

Sample Usage
```java
// somewhere in your application, you probably only need one instance
public static final Scheduler bgScheduler = new ThreadScheduler();
// or
public static final Scheduler bgScheduler = new RedisScheduler();

// then, to schedule a background job anywhere in your code
if(bgScheduler.schedule(PrintingJob.class, new PrintDTO())
    System.out.println("job successfully scheduled!");
else
    System.out.println("error scheduling job, handle...");
// for definitions of referenced PrintingJob and PrintDTO, look in the included test classes
```

Basically, you implement the method BackgroundJob.process(T dto), then use an implementation of Scheduler to schedule any number of DTOs to be processed by that BackgroundJob in a particular queue.  Wherever those jobs end up getting executed at, an instance of that BackgroundJob gets created and potentially cached to process as many of those DTOs as is needed.  Each instance of BackgroundJob is guaranteed to only run in a single thread so it doesn't need to worry about concurrency.  ScheduledItemExecutor, used by all currently implemented processors, currently creates instances of particular BackgroundJob's when needed up to the number of threads running in the thread pool and caches them when not being used, using ObjectPool.

There are currently two implementations of Scheduler:
* ThreadScheduler - This starts a Thread pool in the current JVM and schedules DTOs to be processed in it.  This is surely useful for testing when no redis instance is available, but may be useful for small amounts of asynchronous jobs you may want to schedule as well.
* RedisScheduler - This serializes DTOs to a [redis][1] list acting as a FIFO queue, which can then be read and processed by an instance of RedisThread running anywhere with access to the same redis server.

There are currently three implementations of a Redis Processor:
* RedisThread: Simply reads from the specified queue with BRPOP and processes the job, no errors are recorded and there is no visibility into which jobs are currently being processed.
* RedisProcessingQueueThread: Extends RedisThread, but reads from the queue with BRPOPLPUSH, pushing into a 'processing' queue and then removes the job from the processing queue when finished, either after success or an error.
* RedisErrorQueueThread: Extends RedisProcessingQueueThread, but if the job ends in an error (throws Throwable), a serialized ScheduledItemError is placed into the 'error' queue with the currentTimeMillis the exception occurred, the full stack trace, and the job that caused it.  This can then be examined programmatically or manually later with the possibility of fixing the issue and re-running the job.

Useful system properties (set with -DpropertyName=propertyValue)
------------
* scheduler.executor.numThreads (default '5'): Number of threads in each ScheduledItemExecutor ThreadPool
* scheduler.default.queue (default 'default'): Queue to place job in when no queue is specified
* redis.host (default 'localhost'): Redis host to connect to
* redis.debug (default 'false'): Print debug statements about interaction with redis, useful for development
* redis.queuePrefix (default java.net.InetAddress.getLocalHost().getHostName()): Prefix to place in front of queue name, so one redis instance can be used by multiple machines if desired
* redis.timeout (default '5'): Used in RedisThread, timeout for blocking reads waiting for items to be put in the queue.  After each timeout it checks the key queuePrefix + 'shutdown' for value 'shutdown' to see if it should shutdown after processing what it has already read.
* redis.maxTimeoutsBeforeClose (default '0' (never close)): If greater than 0, will shutdown after this many read timeouts, probably only useful for automated testing, or making it shutdown after processing the entire queue.
* redis.processingQueueSuffix (default 'processing'): Only used in instances of RedisProcessingQueueThread, suffix to add onto queue name for name of processing queue
* redis.errorQueueSuffix (default 'error'): Only used in instances of RedisErrorQueueThread, suffix to add onto queue name for name of error queue

As an illustration, for host 'foo', using all the defaults above the default redis queue would be 'foo-default', the processing queue would be 'foo-default-processing' and the error queue would be 'foo-default-error'.

Licensing
------------
This project is licensed under the [GNU/LGPLv2.1][2], which allows use in Open Source or Proprietary programs.  If you need to modify this code though, you should contribute back to it.

Contributing
------------

1. Fork it. (Alternatively, if you **really** can't use github/git, email me a patch.)
2. Create a branch (`git checkout -b my_jBgJob`)
3. Commit your changes (`git commit -am "Implemented method X"`)
4. Push to the branch (`git push origin my_jBgJob`)
5. Open a [Pull Request][3]
6. Enjoy a refreshing beverage and wait

[1]:   http://redis.io/
[2]:   https://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
[3]: https://github.com/moparisthebest/jBgJob/pulls
