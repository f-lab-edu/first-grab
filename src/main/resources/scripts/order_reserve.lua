-- KEYS[1] = active:{eventId} ZSet userId -> 입장 만료 ms
-- KEYS[2] = stock:{eventId}  String
-- KEYS[3] = bought:{eventId} Hash userId -> 누적 예약 수량
-- KEYS[4] = reserved:{eventId} Hash orderId -> "userId:quantity:admitUntil"
-- ARGV[1] = userId, ARGV[2] = quantity, ARGV[3] = perUserLimit
-- ARGV[4] = now(ms), ARGV[5] = expireAt(epoch sec), ARGV[6] = orderId

local admitUntil = redis.call('ZSCORE', KEYS[1], ARGV[1])
if not admitUntil or tonumber(admitUntil) < tonumber(ARGV[4]) then
    return -4
end

local stock = tonumber(redis.call('GET', KEYS[2]))
if not stock then
    return -3
end

local quantity = tonumber(ARGV[2])
local bought = tonumber(redis.call('HGET', KEYS[3], ARGV[1]) or '0')
if bought + quantity > tonumber(ARGV[3]) then
    return -2
end

if stock < quantity then
    return -1
end

if redis.call('HEXISTS', KEYS[4], ARGV[6]) == 1 then
    return -5
end

redis.call('ZREM', KEYS[1], ARGV[1])
redis.call('HINCRBY', KEYS[3], ARGV[1], quantity)
redis.call('EXPIREAT', KEYS[3], ARGV[5], 'NX')
redis.call('HSET', KEYS[4], ARGV[6], ARGV[1] .. ':' .. ARGV[2] .. ':' .. admitUntil)
redis.call('EXPIREAT', KEYS[4], ARGV[5], 'NX')
return redis.call('DECRBY', KEYS[2], quantity)
