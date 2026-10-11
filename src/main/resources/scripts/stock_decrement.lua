local stock = tonumber(redis.call('GET', KEYS[1]) or '0')
local quantity = tonumber(ARGV[1])

if quantity <= 0 or stock < quantity then
    return -1
end

return redis.call('DECRBY', KEYS[1], quantity)
