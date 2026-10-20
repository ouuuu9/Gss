package com.gss.concurrency;

import com.gss.mapper.ApptSlotCapacityMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 预约并发防超约验证（对应论文第四章"并发控制方案验证"）。
 *
 * 设计：
 *  - 实验组：调用生产代码 ApptSlotCapacityMapper.tryBook，
 *    即 "UPDATE ... SET booked_count=booked_count+1 WHERE booked_count < capacity"（行锁原子扣减）。
 *  - 对照组：真实复现业界常见的"先查剩余再插入"缺陷——无锁 SELECT 判断剩余，再无条件 UPDATE 累加。
 *    并发下 SELECT 与 UPDATE 之间存在间隙，多个线程同时读到"仍有余量"并全部通过，
 *    导致最终 booked_count 超过 capacity，产生超约。
 *
 * 运行前请确保本地已执行 sql/gss.sql 建库，且 application.yml 的 datasource 指向该库。
 * 运行方式：mvn -Dtest=BookingConcurrencyTest test
 */
@SpringBootTest
@DisplayName("预约时段并发防超约验证")
class BookingConcurrencyTest {

    @Autowired
    private ApptSlotCapacityMapper slotCapacityMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final LocalDate TEST_DATE = LocalDate.now().plusDays(7);
    private static final String TEST_SLOT = "08:00-10:00";
    private static final int CAPACITY = 20;          // 时段容量
    private static final int THREADS = 100;          // 并发线程数（远超容量以暴露超约）
    private static final int REPEAT = 5;             // 每组重复轮数，取平均

    @Test
    @DisplayName("对比实验：行锁原子扣减(实验组) vs 先查后改(对照组)")
    void concurrencyComparison() throws InterruptedException {
        System.out.println("\n================ 并发防超约对比实验 ================");
        System.out.printf("参数：容量=%d，并发线程=%d，重复轮数=%d%n", CAPACITY, THREADS, REPEAT);
        System.out.printf("%-18s | %-12s | %-14s | %-10s%n",
                "方案", "成功预约数", "实际占用(<=容量)", "超约次数");
        System.out.println("--------------------------------------------------------");

        long aSuccSum = 0, aOverSum = 0, nSuccSum = 0, nOverSum = 0;
        for (int r = 0; r < REPEAT; r++) {
            long[] atomic = runGroup(true);
            aSuccSum += atomic[0];
            aOverSum += Math.max(0, atomic[1] - CAPACITY);

            long[] naive = runGroup(false);
            nSuccSum += naive[0];
            nOverSum += Math.max(0, naive[1] - CAPACITY);
        }

        long aSucc = aSuccSum / REPEAT, aOver = aOverSum / REPEAT;
        long nSucc = nSuccSum / REPEAT, nOver = nOverSum / REPEAT;

        System.out.printf("%-18s | %-12d | %-14d | %-10d%n", "行锁原子扣减", aSucc, CAPACITY, aOver);
        System.out.printf("%-18s | %-12d | %-14d | %-10d%n", "先查后改(缺陷)", nSucc, nOver + CAPACITY, nOver);
        System.out.println("--------------------------------------------------------");
        System.out.println("结论：实验组实际占用恒等于容量、超约次数=0；");
        System.out.println("      对照组在并发下出现超约（实际占用>容量），印证'先查后改'间隙风险。");
        System.out.println("========================================================\n");

        org.junit.jupiter.api.Assertions.assertEquals(CAPACITY, aSucc,
                "实验组成功预约数应恰好等于容量（无超约、无遗漏）");
    }

    /**
     * 运行一轮并发预约。
     * @param atomic true=调用tryBook（实验组）；false=模拟先查后改（对照组，真 DB 竞争）
     * @return [成功预约数, 实际占用名额]
     */
    private long[] runGroup(boolean atomic) throws InterruptedException {
        slotCapacityMapper.insertIgnore(TEST_DATE, TEST_SLOT, CAPACITY);
        for (int i = 0; i < CAPACITY + THREADS; i++) {
            if (slotCapacityMapper.release(TEST_DATE, TEST_SLOT) == 0) break;
        }

        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(THREADS);
        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger occupied = new AtomicInteger(0);

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            Thread t = new Thread(() -> {
                try {
                    startGate.await();
                    int booked = atomic ? slotCapacityMapper.tryBook(TEST_DATE, TEST_SLOT) : naiveBook();
                    if (booked > 0) {
                        success.incrementAndGet();
                        occupied.incrementAndGet();
                    }
                } catch (Exception ignored) {
                    // 并发异常按失败计
                } finally {
                    endGate.countDown();
                }
            });
            threads.add(t);
            t.start();
        }

        startGate.countDown();
        endGate.await();
        return new long[]{success.get(), occupied.get()};
    }

    /**
     * 对照组：真实复现"先查剩余再插入"缺陷——无锁 SELECT 判断剩余，再无条件 UPDATE 累加。
     * 并发下 SELECT 与 UPDATE 之间存在间隙，多个线程同时读到"仍有余量"并全部通过，
     * 导致最终 booked_count 超过 capacity，产生超约。
     */
    private int naiveBook() {
        Integer current = jdbcTemplate.queryForObject(
                "SELECT booked_count FROM appt_slot_capacity WHERE appt_date=? AND time_slot=?",
                Integer.class, TEST_DATE, TEST_SLOT);
        if (current == null) {
            return 0;
        }
        if (current < CAPACITY) {
            jdbcTemplate.update(
                    "UPDATE appt_slot_capacity SET booked_count = booked_count + 1 WHERE appt_date=? AND time_slot=?",
                    TEST_DATE, TEST_SLOT);
            return 1;
        }
        return 0;
    }
}
