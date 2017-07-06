# ShowAsyncExecutorProblem
Reproducible test case for a problem I've hit with Slick and Play unit-testing

This repo demonstrates an apparent problem with a specific combination of test functions, which result in some sort of inconsistent state that crashes Slick.

### The Problem, and how to Repro

What we know for sure: if you --

* Have a unit-test suite built on top of `GuiceOneAppPerSuite`
* Inside the test, you run `Server.withRouter()`
* Inside the block for `withRouter()`, you try to do Slick's `db.run()`
* Slick consistently crashes.

You can repro the issue by cloning this repo, compiling and running `test`. There are two tests in `ShowProblemSpec.scala`, with identical (trivial) Slick code -- one without `withRouter()`, which succeeds, and one with it, which crashes.

### Why it matters

I came across this while working on a system that is essentially a proxy server. It receives XML-formatted requests from the outside, translates them into JSON, and sends them off to a third-party external server using the WS library. When it gets a response back, it translates the returned JSON to XML, and returns that to the original caller. It's a fairly ordinary use case.

This had existing unit tests, which were based on `Server.withRouter()` to simulate the external server. These had been working well until I went to add some Slick code in the middle of this process (to do audit logging of the transactions). The Slick code consistently crashes during unit tests. After much experimenting, I found that it only crashed in tests that were using `withRouter()` -- tests that didn't bother with the external server (because they were testing error pathways) worked fine.

### Details

The test here fails with the following stack trace:
```
[info]   java.util.concurrent.RejectedExecutionException: Task slick.basic.BasicBackend$DatabaseDef$$anon$2@7acb4ac3 rejected from slick.util.AsyncExecutor$$anon$2$$anon$1@4c95b745[Terminated, pool size = 0, active threads = 0, queued tasks = 0, completed tasks = 2]
[info]   at java.util.concurrent.ThreadPoolExecutor$AbortPolicy.rejectedExecution(ThreadPoolExecutor.java:2047)
[info]   at java.util.concurrent.ThreadPoolExecutor.reject(ThreadPoolExecutor.java:823)
[info]   at java.util.concurrent.ThreadPoolExecutor.execute(ThreadPoolExecutor.java:1369)
[info]   at slick.util.AsyncExecutor$$anon$2$$anon$3.execute(AsyncExecutor.scala:120)
[info]   at slick.basic.BasicBackend$DatabaseDef.runSynchronousDatabaseAction(BasicBackend.scala:233)
[info]   at slick.basic.BasicBackend$DatabaseDef.runSynchronousDatabaseAction$(BasicBackend.scala:231)
[info]   at slick.jdbc.JdbcBackend$DatabaseDef.runSynchronousDatabaseAction(JdbcBackend.scala:38)
[info]   at slick.basic.BasicBackend$DatabaseDef.runInContext(BasicBackend.scala:210)
[info]   at slick.basic.BasicBackend$DatabaseDef.runInContext$(BasicBackend.scala:142)
[info]   at slick.jdbc.JdbcBackend$DatabaseDef.runInContext(JdbcBackend.scala:38)
```

Curiously, the original failing test, in the real code, also fails in AsyncExecutor, but with a different trace:
```
java.lang.IllegalStateException: Cannot initialize ExecutionContext; AsyncExecutor already shut down
        at slick.util.AsyncExecutor$$anon$2.executionContext$lzycompute(AsyncExecutor.scala:61)
        at slick.util.AsyncExecutor$$anon$2.executionContext(AsyncExecutor.scala:59)
        at slick.util.AsyncExecutor$$anon$2.executionContext(AsyncExecutor.scala:51)
        at slick.jdbc.JdbcBackend$DatabaseDef.synchronousExecutionContext(JdbcBackend.scala:63)
        at slick.basic.BasicBackend$DatabaseDef.runSynchronousDatabaseAction(BasicBackend.scala:233)
        at slick.basic.BasicBackend$DatabaseDef.runSynchronousDatabaseAction$(BasicBackend.scala:231)
        at slick.jdbc.JdbcBackend$DatabaseDef.runSynchronousDatabaseAction(JdbcBackend.scala:38)
        at slick.basic.BasicBackend$DatabaseDef.runInContext(BasicBackend.scala:210)
        at slick.basic.BasicBackend$DatabaseDef.runInContext$(BasicBackend.scala:142)
        at slick.jdbc.JdbcBackend$DatabaseDef.runInContext(JdbcBackend.scala:38)
        at slick.basic.BasicBackend$DatabaseDef.runInternal(BasicBackend.scala:78)
        at slick.basic.BasicBackend$DatabaseDef.runInternal$(BasicBackend.scala:77)
        at slick.jdbc.JdbcBackend$DatabaseDef.runInternal(JdbcBackend.scala:38)
        at slick.basic.BasicBackend$DatabaseDef.run(BasicBackend.scala:75)
        at slick.basic.BasicBackend$DatabaseDef.run$(BasicBackend.scala:75)
        at slick.jdbc.JdbcBackend$DatabaseDef.run(JdbcBackend.scala:38)
        ... user-land code, calling db.run()
```
I have no idea why the trace is different in this reduced reproducer; my best guess is that this is using an H2 in-memory DB (to make it easier to set up and run), whereas the real code is using the PostgresProfile.

### What's Going On?

At this point, I'm out of my depth. It is *not* 100% clear that `GuiceOneAppPerSuite` is actually involved -- that's just the only way I know how to set up a unit test using Play+Slick. It *is* clear that `Server.withRouter()` is involved: the problem only manifests inside of it.

If I had to make a half-assed guess, I would say that something about having an Application (from `Server.withRouter()`) inside of another Application (from `GuiceOneAppPerSuite`) is subtly messing up the environment that `AsyncExecutor` depends on -- probably something to do with the `ExecutionContext`. But that's just a guess.
