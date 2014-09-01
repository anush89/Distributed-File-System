Distributed-File-System
=======================
Let there be seven data servers, S0, S1, . . . S6 and five clients, C0, C1, . . . , C4. There exist communication channels between all servers, and from each client to all the servers. All communication channels are FIFO and reliable when they are operational. Occasionally, a channel may be disrupted in which case no message can be communicated across that channel. There exists a hash function, H, such that for each object, Ok, H(Ok) yields a value in the range 0 − 6.

• When a client, Ci has to insert/update an object, Ok, it performs the write at three servers numbered: H(Ok), H(Ok)+1 modulo 7, and H(Ok)+2 modulo 7.

• When a client, Cj has to read an object, Ok, it can read the value from any one of the three servers: H(Ok), H(Ok)+1 modulo 7, or H(Ok)+2 modulo 7.

Requirements
============
1. A client should be able to randomly choose any of the three replicas of an object when it wishes to read the value of the object.

2. When a client wishes to update/insert an object into the data repository, it should be able to successfully perform the operation on at least two, and if possible all the three servers that are required to store the object.

3. If a client is unable to access at least two out of the three servers that are required to store an object, then the client does not perform updates to any replica of that object.

4. If two clients try to concurrently write to the same object and at least two replicas are available, the two writes must be performed in the same order at all the replicas of the object.

Running instruction
===================
Compile and run ServerNode.java on server nodes s1 through s7 then run ClientNode.java on c1 through c4.
