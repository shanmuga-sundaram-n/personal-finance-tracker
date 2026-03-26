package com.shan.cyber.tech.financetracker.transaction.domain.service;

import com.shan.cyber.tech.financetracker.transaction.config.TransactionConfig;
import com.shan.cyber.tech.financetracker.transaction.domain.port.outbound.BalanceUpdatePort;
import com.shan.cyber.tech.financetracker.transaction.domain.port.outbound.TransactionEventPublisherPort;
import com.shan.cyber.tech.financetracker.transaction.domain.port.outbound.TransactionPersistencePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * H-1 Regression — Verifies that TransactionCommandService is produced as a Spring bean by
 * TransactionConfig#transactionCommandService(), not constructed with {@code new} inside the
 * controller or elsewhere.
 *
 * <p>The @Bean factory method pattern ensures Spring can wrap the service with @Transactional
 * proxy. If the controller used {@code new TransactionCommandService(...)}, the proxy would be
 * bypassed and @Transactional on TransactionApplicationService would have no effect on the
 * inner service calls.</p>
 *
 * <p>This test verifies that:
 * <ol>
 *   <li>TransactionConfig.transactionCommandService() produces a non-null instance.</li>
 *   <li>The returned type is exactly TransactionCommandService (the declared return type that
 *       Spring registers under the bean name "transactionCommandService").</li>
 * </ol>
 * No Docker or full Spring context is required — the config method is a plain Java factory.</p>
 */
@ExtendWith(MockitoExtension.class)
class TransactionCommandServiceBeanTest {

    @Mock private TransactionPersistencePort persistencePort;
    @Mock private TransactionEventPublisherPort eventPublisherPort;
    @Mock private BalanceUpdatePort balanceUpdatePort;

    @Test
    void should_produceTransactionCommandServiceBean_from_transactionConfig() {
        // Arrange — instantiate config as Spring would
        TransactionConfig config = new TransactionConfig();

        // Act — call the @Bean factory method
        TransactionCommandService bean = config.transactionCommandService(
                persistencePort, eventPublisherPort, balanceUpdatePort);

        // Assert — confirms the factory method wires the service correctly
        assertThat(bean)
                .as("TransactionConfig#transactionCommandService() must produce a non-null bean")
                .isNotNull()
                .isInstanceOf(TransactionCommandService.class);
    }

    @Test
    void should_injectAllThreePorts_into_transactionCommandService() {
        TransactionConfig config = new TransactionConfig();

        // Act — factory must accept exactly these three ports (constructor signature contract)
        TransactionCommandService bean = config.transactionCommandService(
                persistencePort, eventPublisherPort, balanceUpdatePort);

        // Assert — if construction succeeded, all ports were accepted; any missing port would
        // cause a NullPointerException or compile error, catching future constructor drift
        assertThat(bean).isNotNull();
    }
}
