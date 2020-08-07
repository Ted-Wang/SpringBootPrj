package psn.ted.tool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.serializer.GenericToStringSerializer;

public class RedisOrderNumGenerator {

	@Value("${lykos.ordermanagement.orderNumberStart:1}")
    private int orderNumberStart=1;

    @Value("${lykos.ordermanagement.orderNumberLimit:9999}")
	private int orderNumberLimit=9999;
    
    @Value("${lykos.ordermanagement.orderNumber.pop.waiting.seconds:3}")
	private int orderNumberPopWaiting=3;
    
    @Value("${lykos.ordermanagement.orderNumber.datePartFormat:yyMMdd}")
	private String orderNumberDatePartFormat="yyMMdd";

	private static final Logger LOG = LoggerFactory.getLogger(RedisOrderNumGenerator.class);
	
//	private static final String ORDER_NUM_GEN_LOCK_KEY_PREFIX = RedisOrderNumGenerator.class.getSimpleName() + "_Key_";
//	private static final String ORDER_NUM_LIST_PREFIX = RedisOrderNumGenerator.class.getSimpleName() + "_List";
	private static final String ORDER_NUM_GEN_LOCK_KEY_PREFIX = "key";
	private static final String ORDER_NUM_LIST_PREFIX = "list";
	
	private volatile String lastGeneratedDateChecked;

	private RedisTemplate<String, String> redisTemplate;
	
	public void clearLock() {
		Date today = DateUtil.today();
		final String formattedDate = DateUtil.convertDateTimeWithTimezone(today, orderNumberDatePartFormat);
		String redisLockKey = ORDER_NUM_GEN_LOCK_KEY_PREFIX + formattedDate;
		String redisOrderNumberListDaily = ORDER_NUM_LIST_PREFIX + formattedDate;
		redisTemplate.delete(redisLockKey);
		redisTemplate.delete(redisOrderNumberListDaily);
	}

    public RedisOrderNumGenerator() {
//    	String host = "localhost";
    	String host = "172.17.1.100";
		int port = 6379;
		int maxTotal = 128;
		int maxIdle = 50;
		RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration(host, port);
		JedisConnectionFactory jedisConnectionFactory = null;
		jedisConnectionFactory = new JedisConnectionFactory(redisStandaloneConfiguration);
		jedisConnectionFactory.getPoolConfig().setMaxTotal(maxTotal);
		jedisConnectionFactory.getPoolConfig().setMaxIdle(maxIdle);

		redisTemplate = new RedisTemplate<>();
		redisTemplate.setKeySerializer(new GenericToStringSerializer<>(Object.class));
		redisTemplate.setHashKeySerializer(new GenericToStringSerializer<>(Object.class));
		redisTemplate.setHashValueSerializer(new GenericToStringSerializer<>(Object.class));
		redisTemplate.setValueSerializer(new GenericToStringSerializer<>(Object.class));
		redisTemplate.setConnectionFactory(jedisConnectionFactory);
		redisTemplate.afterPropertiesSet();
	}

    public void init() {
    	initOrderNumbersOnRedis();
    }

	public void initOrderNumbersOnRedis() {
		long start = System.currentTimeMillis();
		Date today = DateUtil.today();
		final String formattedDate = DateUtil.convertDateTimeWithTimezone(today, orderNumberDatePartFormat);

		String redisLockKey = ORDER_NUM_GEN_LOCK_KEY_PREFIX + formattedDate;
		String redisOrderNumberListDaily = ORDER_NUM_LIST_PREFIX + formattedDate;
		String redisLockValue = Thread.currentThread().toString() + start;

		if (!redisTemplate.hasKey(redisOrderNumberListDaily)) {
			LOG.error("check listKey1, {}, hasKey, {}", redisOrderNumberListDaily, redisTemplate.hasKey(redisOrderNumberListDaily));
			ValueOperations<String, String> valOps = redisTemplate.opsForValue();
			boolean locked = valOps.setIfAbsent(redisLockKey, redisLockValue);
			LOG.error("lock key1, {}, locked, {}", redisOrderNumberListDaily, locked);
			if (locked) {
				redisTemplate.expire(redisLockKey, 86400, TimeUnit.SECONDS);
				LOG.error("lock key2, {}, locked, {}", redisOrderNumberListDaily, locked);
				if (!redisTemplate.hasKey(redisOrderNumberListDaily)) {
					List<String> orderNumbers = generateNumbers(formattedDate);
					LOG.error("check listKey2, {}, hasKey, {}", redisOrderNumberListDaily, redisTemplate.hasKey(redisOrderNumberListDaily));
					ListOperations<String, String> listOps = redisTemplate.opsForList();
					LOG.error("check listValue1, {}, hasKey, {}", orderNumbers, redisTemplate.hasKey(redisOrderNumberListDaily));
					listOps.rightPushAll(redisOrderNumberListDaily, orderNumbers);
					redisTemplate.expire(redisOrderNumberListDaily, 86400, TimeUnit.SECONDS);
					LOG.info("Generate {} order numbers in {} miliseconds at [Time: {}], actually generated numbers of orderNum: {}.",
					        orderNumberLimit, System.currentTimeMillis() - start, today, orderNumbers.size());
				}
			}
		}

		lastGeneratedDateChecked = formattedDate;
	}
    
    private List<String> generateNumbers(String formattedDate){
    	List<String> orderNumbers = new ArrayList<>(orderNumberLimit);
		int length = (int) (Math.log10(orderNumberLimit) + 1);
    	for (int i = orderNumberStart; i <= orderNumberLimit; i++) {
    		String format = "%s%s%0" + length + "d";
    		String orderNum = String.format(format, "S", formattedDate, i);
    		orderNumbers.add(orderNum);
    	}
    	Collections.shuffle(orderNumbers);
    	return orderNumbers;
    }

    //order number: S1907105229 : S-yyMMdd-4digitRandomNumber.
    public String generateAndSetOrderNumberOn() {
    	Date today = DateUtil.today();
    	final String formattedDate = DateUtil.convertDateTimeWithTimezone(today, orderNumberDatePartFormat);
		String redisOrderNumberListDaily = ORDER_NUM_LIST_PREFIX + formattedDate;
    	
    	ListOperations<String, String> listOps = redisTemplate.opsForList();
    	String orderNum = listOps.leftPop(redisOrderNumberListDaily);
//    	String orderNum = null;
    	boolean orderNumberOutOfStock = false;
		if (orderNum == null) {
			if (isInitializedForToday(formattedDate)) {
				orderNumberOutOfStock = true;
				LOG.error("Failed to get order number due to orderNumber is Out Of Stock1. {}", orderNum);
			} else {
				initOrderNumbersOnRedis();
				orderNum = listOps.leftPop(redisOrderNumberListDaily);
				if(orderNum == null) {
					orderNum = listOps.leftPop(redisOrderNumberListDaily, orderNumberPopWaiting*1000, TimeUnit.MILLISECONDS);
				}
				LOG.error("Failed to get order number due to orderNumber is Out Of Stock2. {}", orderNum);
			}
		}
		
		if(orderNumberOutOfStock) {
//			LOG.error("Failed to get order number due to orderNumber is Out Of Stock3. {}", orderNum);
//			throw new RuntimeException();
			orderNum = "-1";
			System.out.println("[Main_OUTOFSTOCK]");
		}
		
		if(orderNum == null) {
//			LOG.error("Failed to get order number due to orderNumber is Out Of Stock3. {}", orderNum);
//			throw new RuntimeException();
			orderNum = "-1";
			System.out.println("[Main_NULL]");
		}
		
//		System.out.println(orderNum);
		return orderNum;
//		orderTxn.setOrderNum(orderNum);
    }

    public void release(String orderNumber) {
    	Date today = DateUtil.today();
    	final String formattedDate = DateUtil.convertDateTimeWithTimezone(today, orderNumberDatePartFormat);
		String redisOrderNumberListDaily = ORDER_NUM_LIST_PREFIX + formattedDate;
		
    	ListOperations<String, String> listOps = redisTemplate.opsForList();
    	listOps.leftPush(redisOrderNumberListDaily, orderNumber);
    }

    private boolean isInitializedForToday(String today) {
    	return today.equals(lastGeneratedDateChecked);
    }

}
