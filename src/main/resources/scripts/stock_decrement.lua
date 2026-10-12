-- KEYS[1] = stock:{eventId}
-- KEYS[2] = ordered:{eventId}
-- ARGV[1] = quantity, ARGV[2] = userId, ARGV[3] = ttl

local quantity = tonumber(ARGV[1])
local userId = ARGV[2]
local stock = tonumber(redis.call('GET', KEYS[1]))

if not stock then
    return -3
end

if redis.call('SISMEMBER', KEYS[2], userId) == 1 then
    return -2
end

if quantity <= 0 or stock < quantity then
    return -1
end

local remaining = redis.call('DECRBY', KEYS[1], quantity)
redis.call('SADD', KEYS[2], userId)
redis.call('EXPIRE', KEYS[2], ARGV[3], 'NX')

return remaining
