shardableobjectids
==================

BSON objectid customization for sharding with non-hashed or sorting indexes and TimeUUID helpers.

Following the recommendations in http://www.mongodb.org/display/DOCS/Choosing+a+Shard+Key
I implemented these two id generators that follow the ObjectId model. I hope some
day they can get driver support so that saving them in binary format will be possible.

ShardableObjectId: Creates a nice distribution of keys over all buckets/shards for
 linear write/read scaling.

ShardableObjectIdWithMoPrefix: Creates keys prefixed with yyyymm so eg. 201203 so
 that inserts affect only part of the index.
 
 TimeUUID generates TimeUUIDs and parses them. See also TimeUUIDUtils.


Currently I propose just using them as string generators with the toString or
toStringSortableBase64URLSafe methods. The generated strings are safe for copy'n paste and
work in most frameworks as entity ids that can be passed in the URL.
Furthermore for they retain the sorting properties of the original ids even when
ids are compared as encoded strings.

Needs:
 - BSON mongodb driver
 - apache commons codec

Limitations:
 - design has a log of repeated code everywhere (shardable* and BSON ObjectId very similar).
 - depends on commons codec

Those limitations are fine for me of course.


Changes:
2.0 Incompatible with previous version: ShardableObjectID of previous version needs to be deserialized
      with parseNormalBase64().
    Add TimeUUID 
    Reduce Base64Mod memory-churn.
