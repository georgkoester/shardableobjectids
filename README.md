shardableobjectids
==================

BSON objectid customization for sharding

Following the recommendations in http://www.mongodb.org/display/DOCS/Choosing+a+Shard+Key
I implemented these two id generators that follow the ObjectId model. I hope some
day they can get driver support so that saving them in binary format will be possible.

ShardableObjectId: Creates a nice distribution of keys over all buckets/shards for
 linear write/read scaling.

ShardableObjectIdWithMoPrefix: Creates keys prefixed with yyyymm so eg. 201203 so
 that inserts affect only part of the index.


Currently I propose just using them as string generators with the toString or
toStringBase64URLSafe methods. The generated strings are safe for copy'n paste and
work in most frameworks as entity ids that can be passed in the URL.

Needs:
 - BSON mongodb driver
 - apache commons codec

Limitations:
 - design has a log of repeated code everywhere (shardable* and BSON ObjectId very similar).
 - string generation not optimized. Too lazy to change apache commons codec now.
 - depends on commons codec

Those limitations are fine for me of course.
