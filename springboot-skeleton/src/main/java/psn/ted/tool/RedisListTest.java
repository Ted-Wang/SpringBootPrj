package psn.ted.tool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

public class RedisListTest {
	public static void main(String[] args) {
		long begin = System.currentTimeMillis();
//		List<String> list = new ArrayList<>(9999);
//		for (int i = 1; i <= 9999; i++) {
//			list.add("S"+i);
//		}
		
		List<Integer> list = new ArrayList<>(9999);
		for (int i = 1; i <= 9999; i++) {
			list.add(i);
		}
		
//		List<Integer> numbers = Stream.iterate(1, n -> n + 1)
//                .limit(9999)
//                .collect(Collectors.toList());
//		Collections.shuffle(numbers);
		
		Collections.shuffle(list);

		System.out.println(list);
		System.out.println(System.currentTimeMillis() - begin);

		String testList = "list";
		String lockKey = "key";
		String lockVal = Thread.currentThread().toString();
		System.out.println(lockVal);

		RedisTemplate<String, String> redisTemplate = getRedisTemplate();
		RedisTemplate<String, Integer> redisTemplate2 = getRedisTemplate();
		if (!redisTemplate.hasKey(testList)) {
			ValueOperations<String, String> valOps = redisTemplate.opsForValue();
			valOps.setIfAbsent(lockKey, lockVal);
//			System.out.println(valOps.get(lockKey).equals(lockVal));
			if (valOps.get(lockKey).equals(lockVal)) {
				if (!redisTemplate.hasKey(testList)) {
//					ListOperations<String, String> listOps = redisTemplate.opsForList();
					ListOperations<String, Integer> listOps = redisTemplate2.opsForList();
					listOps.rightPushAll(testList, list);
//					listOps.rightPush(testList, ""+1);
//					listOps.rightPush(testList, ""+2);
					redisTemplate.expire(testList, 86400, TimeUnit.SECONDS);
				}
			}
		}
	}

	private static <K, V> RedisTemplate<K, V> getRedisTemplate() {
		String host = "localhost";
		int port = 6379;
		int maxTotal = 128;
		int maxIdle = 50;
		RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration(host, port);
		JedisConnectionFactory jedisConnectionFactory = null;
		jedisConnectionFactory = new JedisConnectionFactory(redisStandaloneConfiguration);
		jedisConnectionFactory.getPoolConfig().setMaxTotal(maxTotal);
		jedisConnectionFactory.getPoolConfig().setMaxIdle(maxIdle);

		final RedisTemplate<K, V> redisTemplate = new RedisTemplate<K, V>();
		redisTemplate.setKeySerializer(new GenericToStringSerializer<>(Object.class));
		redisTemplate.setHashKeySerializer(new GenericToStringSerializer<>(Object.class));
//		redisTemplate.setHashValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
		redisTemplate.setHashValueSerializer(new GenericToStringSerializer<>(Object.class));
		redisTemplate.setValueSerializer(new GenericToStringSerializer<>(Object.class));
		redisTemplate.setConnectionFactory(jedisConnectionFactory);
		redisTemplate.afterPropertiesSet();
		return redisTemplate;
	}
	
	private static <K, V> RedisTemplate<K, V> getRedisTemplate(Class<K> kClass, Class<V> vClass) {
		String host = "localhost";
		int port = 6379;
		int maxTotal = 128;
		int maxIdle = 50;
		RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration(host, port);
		JedisConnectionFactory jedisConnectionFactory = null;
		jedisConnectionFactory = new JedisConnectionFactory(redisStandaloneConfiguration);
		jedisConnectionFactory.getPoolConfig().setMaxTotal(maxTotal);
		jedisConnectionFactory.getPoolConfig().setMaxIdle(maxIdle);

		final RedisTemplate<K, V> redisTemplate = new RedisTemplate<K, V>();
		redisTemplate.setKeySerializer(new GenericToStringSerializer<>(kClass));
		redisTemplate.setHashKeySerializer(new GenericToStringSerializer<>(kClass));
		// redisTemplate.setHashValueSerializer(new StringRedisSerializer());
//		redisTemplate.setHashValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
		redisTemplate.setHashValueSerializer(new GenericToStringSerializer<>(vClass));
//		 redisTemplate.setValueSerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(new GenericToStringSerializer<>(vClass));
		redisTemplate.setConnectionFactory(jedisConnectionFactory);
		redisTemplate.afterPropertiesSet();
		return redisTemplate;
	}
}
