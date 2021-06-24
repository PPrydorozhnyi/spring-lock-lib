# Spring lock lib for distributed locks in Redis

Simple locking solution with Spring and Redis

## Maven dependency

```
<dependency>
  <groupId>com.prydorozhyi</groupId>
  <artifactId>lock-lib</artifactId>
  <version>1.0.0</version>
</dependency>
```
        
## Important notes
### Timout problem
Suppose the first client requests to get a lock, but the server response is longer than the lease time; as a result, the client uses the expired key, and at the same time, another client could get the same key, now both of them have the same key simultaneously! It violet the mutual exclusion.

To solve this problem, we can set a timeout for Redis clients, and it should be less than the lease time.

### Single Instance of Redis and Node Outage

Redis persist in-memory data on disk in two ways:

 - Redis Database (RDB): performs point-in-time snapshots of your dataset at specified intervals and store on the disk. 

 - Append-only File (AOF): logs every write operation received by the server, that will be played again at server startup, reconstructing the original dataset.

By default, only RDB is enabled with the following configuration (for more information please check https://download.redis.io/redis-stable/redis.conf).
If Redis restarted (crashed, powered down, I mean without a graceful shutdown) at this duration, we lose data in memory so other clients can get the same lock.

To solve this issue, we must enable AOF with the fsync=always option before setting the key in Redis. Note that enabling this option has some performance impact on Redis, but we need this option for strong consistency.

### Master-Replica - Redisson partly solves this
By default, replication in Redis works asynchronously; this means the master does not wait for the commands to be processed by replicas and replies to the client before. The problem is before the replication occurs, the master may be failed, and failover happens; after that, if another client requests to get the lock, it will succeed! Or suppose there is a temporary network problem, so one of the replicas does not receive the command, the network becomes stable, and failover happens shortly; the node that didn't receive the command becomes the master. Eventually, the key will be removed from all instances!

So far, so good, but there is another problem; replicas may lose writing (because of a faulty environment). For example, a replica failed before the save operation was completed, and at the same time master failed, and the failover operation chose the restarted replica as the new master. After synching with the new master, all replicas and the new master do not have the key that was in the old master!

To make all slaves and the master fully consistent, we should enable AOF with fsync=always for all Redis instances before getting the lock.

## Useful links

- [Distributed Lock Implementation With Redis](https://dzone.com/articles/distributed-lock-implementation-with-redis)
- [Distributed locks with Redis](https://redis.io/topics/distlock)
- [Distributed locks and synchronizers](https://github.com/redisson/redisson/wiki/8.-Distributed-locks-and-synchronizers)
