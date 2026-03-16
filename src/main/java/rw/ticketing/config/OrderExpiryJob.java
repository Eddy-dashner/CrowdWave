package rw.ticketing.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rw.ticketing.enums.OrderStatus;
import rw.ticketing.repository.OrderRepository;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderExpiryJob {

    private final OrderRepository orderRepository;

    /**
     * Runs every 2 minutes. Marks orders EXPIRED if their 15-min payment window passed.
     */
    @Scheduled(fixedDelay = 120_000)
    @Transactional
    public void expireStaleOrders() {
        var stale = orderRepository.findByStatusAndExpiresAtBefore(OrderStatus.PENDING, LocalDateTime.now());
        if (stale.isEmpty()) return;
        stale.forEach(o -> o.setStatus(OrderStatus.EXPIRED));
        orderRepository.saveAll(stale);
        log.info("Expired {} stale orders", stale.size());
    }
}
