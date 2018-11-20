# Influencer Stats

This application gathers and aggregates stats for your influencer social media campaigns.

## Setup
### YouTube mock server
You need YouTube mock server to be able to test performance without going over YouTube API limits. To build the image, run `docker build -t miciek/influencer-stats-youtube youtube`. To run it, execute `docker run -d --rm -p 8081:80 miciek/influencer-stats-youtube`.

### Running the application
After executing `sbt run`, you need to configure the first `collection`:

```
curl -XPUT -H "Content-Type: application/json" localhost:8080/collections/99757a95-f758-499f-a170-bea93b2d8bcf -d '{ "videos": [ "-4lB5EKS5Uk", "-jlLkTtgWUk", "1FEFpk-uIYo" ] }'
```

Then, you will be able to fetch the stats for videos in this `collection`:

```
curl localhost:8080/collections/99757a95-f758-499f-a170-bea93b2d8bcf/stats
```

## Performance tests
To run performance tests, you will need [wrk](https://github.com/wg/wrk). To analyse what's going on inside the application, please install [async-profiler](https://github.com/jvm-profiling-tools/async-profiler).

Before starting, let's first establish the performance of our YouTube mock server:

```
> wrk -t1 -c1 -d30s --latency http://localhost:8081/youtube/v3/videos
Running 30s test @ http://localhost:8081/youtube/v3/videos
  1 threads and 1 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency   591.88us  689.01us  29.57ms   95.23%
    Req/Sec     1.87k   295.13     2.32k    79.33%
  Latency Distribution
     50%  471.00us
     75%  548.00us
     90%  756.00us
     99%    2.53ms
  55956 requests in 30.00s, 41.51MB read
Requests/sec:   1865.16
Transfer/sec:      1.38MB
```

Additionally, let's see what is the performance of collections with no videos (no additional requests to YouTube server are made):

```
wrk -t1 -c1 -d30s --latency http://localhost:8080/collections/39757a95-e758-499f-a170-bea93b2d8bca/stats
Running 30s test @ http://localhost:8080/collections/39757a95-e758-499f-a170-bea93b2d8bca/stats
  1 threads and 1 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency   682.73us    3.35ms  47.60ms   97.16%
    Req/Sec     5.83k   776.99     6.53k    83.67%
  Latency Distribution
     50%  155.00us
     75%  164.00us
     90%  196.00us
     99%   21.81ms
  174201 requests in 30.01s, 29.41MB read
Requests/sec:   5804.37
Transfer/sec:      0.98MB
```

Remember that each test should be run several times to warm up JVM.

### Flamegraph generation
To compare different versions, we will use [flamegraphs](http://www.brendangregg.com/flamegraphs.html). The command below generates flamegraph for the load-tested application (should be started after around 15s of `wrk`):

```
jps # to get the <PID> of the application
cd async-profiler
./profiler.sh -d 30 -f /tmp/flamegraph.svg <PID>
```

Generated flamegraphs are stored in [flamegraphs](./flamegraphs) directory.

### Version 1 (log-all/list-state/akka-http)
```
> wrk -t1 -c16 -d30s --timeout 10s --latency http://localhost:8080/collections/99757a95-f758-499f-a170-bea93b2d8bcf/stats
Running 30s test @ http://localhost:8080/collections/99757a95-f758-499f-a170-bea93b2d8bcf/stats
  1 threads and 16 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     2.35s   214.19ms   2.83s    60.82%
    Req/Sec    22.90     22.91   117.00     83.10%
  Latency Distribution
     50%    2.29s
     75%    2.55s
     90%    2.64s
     99%    2.82s
  194 requests in 30.07s, 36.00KB read
Requests/sec:      6.45
Transfer/sec:      1.20KB
```

### Version 2 (akka-http/Hammock + IO)
```
> wrk -t1 -c1 -d60s --latency http://localhost:8080/collections/99757a95-f758-499f-a170-bea93b2d8bcf/stats
Running 1m test @ http://localhost:8080/collections/99757a95-f758-499f-a170-bea93b2d8bcf/stats
  1 threads and 1 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     1.66ms  595.48us  30.56ms   95.45%
    Req/Sec   610.99     42.68   696.00     77.67%
  Latency Distribution
     50%    1.58ms
     75%    1.66ms
     90%    1.82ms
     99%    2.83ms
  36501 requests in 1.00m, 6.61MB read
Requests/sec:    608.10
Transfer/sec:    112.83KB
```

### Version 3 (http4s/Hammock + IO)
```
> wrk -t1 -c1 -d60s --latency http://localhost:8080/collections/99757a95-f758-499f-a170-bea93b2d8bcf/stats
Running 1m test @ http://localhost:8080/collections/99757a95-f758-499f-a170-bea93b2d8bcf/stats
  1 threads and 1 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     1.64ms  372.82us  16.55ms   93.89%
    Req/Sec   614.36     36.72   666.00     90.00%
  Latency Distribution
     50%    1.56ms
     75%    1.64ms
     90%    1.80ms
     99%    2.83ms
  36693 requests in 1.00m, 5.74MB read
Requests/sec:    611.32
Transfer/sec:     97.91KB
```

### Version 4 (http4s + IO)
```
> wrk -t1 -c1 -d60s --latency http://localhost:8080/collections/99757a95-f758-499f-a170-bea93b2d8bcf/stats
Running 1m test @ http://localhost:8080/collections/99757a95-f758-499f-a170-bea93b2d8bcf/stats
  1 threads and 1 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     2.54ms    1.82ms  26.75ms   85.22%
    Req/Sec   386.28    291.05     1.05k    77.48%
  Latency Distribution
     50%    1.12ms
     75%    4.18ms
     90%    4.50ms
     99%    5.83ms
  11638 requests in 1.00m, 1.51MB read
  Non-2xx or 3xx responses: 6289
Requests/sec:    193.81
Transfer/sec:     25.82KB
```

### Version 5 (http4s + IO - fix: just one client)
```
> wrk -t1 -c1 -d60s --latency http://localhost:8080/collections/99757a95-f758-499f-a170-bea93b2d8bcf/stats
Running 1m test @ http://localhost:8080/collections/99757a95-f758-499f-a170-bea93b2d8bcf/stats
  1 threads and 1 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     1.69ms  485.81us  19.99ms   94.23%
    Req/Sec   599.17     47.29   700.00     83.67%
  Latency Distribution
     50%    1.59ms
     75%    1.70ms
     90%    1.90ms
     99%    3.00ms
  35789 requests in 1.00m, 5.60MB read
Requests/sec:    596.22
Transfer/sec:     95.49KB
```