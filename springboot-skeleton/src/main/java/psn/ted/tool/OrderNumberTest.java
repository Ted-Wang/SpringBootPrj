package psn.ted.tool;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class OrderNumberTest {
	public static void main(String[] args) throws InterruptedException {
		AtomicInteger total1 = new AtomicInteger();
		AtomicInteger total2 = new AtomicInteger();
		AtomicInteger total3 = new AtomicInteger();
		AtomicInteger total4 = new AtomicInteger();
		RedisOrderNumGenerator generator = new RedisOrderNumGenerator();
		generator.clearLock();
		int tCount = 80;
		CountDownLatch signal = new CountDownLatch(tCount);
		
		for (int i = 0; i < tCount; i++) {
			Runnable test = new Runnable() {
				public void run() {
					while(true) {
						String orderNum = generator.generateAndSetOrderNumberOn();
						total1.incrementAndGet();
						if(orderNum != null && !orderNum.equals("-1")) {
							total2.incrementAndGet();
						} else if(orderNum.equals("-1")) {
							total3.incrementAndGet();
							break;
						} else {
							total4.incrementAndGet();
							break;
						}
					}
					signal.countDown();
				}
			};
			new Thread(test).start();
		}
		signal.await();
		System.out.println("[Main_1]"+total1);
		System.out.println("[Main_2]"+total2);
		System.out.println("[Main_-1]"+total3);
		System.out.println("[Main_N]"+total4);
	}
}
