package org.apereo.cas.trusted.config;

import org.apereo.cas.CipherExecutor;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.trusted.authentication.impl.InMemoryMultifactorAuthenticationTrustStorage;
import org.apereo.cas.trusted.authentication.MultifactorAuthenticationTrustCipherExecutor;
import org.apereo.cas.trusted.authentication.MultifactorAuthenticationTrustStorage;
import org.apereo.cas.trusted.web.flow.MultifactorAuthenticationSetTrustAction;
import org.apereo.cas.trusted.web.flow.MultifactorAuthenticationVerifyTrustAction;
import org.apereo.cas.util.cipher.NoOpCipherExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.webflow.execution.Action;

/**
 * This is {@link MultifactorAuthnTrustConfiguration}.
 *
 * @author Misagh Moayyed
 * @since 5.1.0
 */
@Configuration("multifactorAuthnTrustConfiguration")
@EnableConfigurationProperties(CasConfigurationProperties.class)
public class MultifactorAuthnTrustConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(MultifactorAuthnTrustConfiguration.class);

    @Autowired
    private CasConfigurationProperties casProperties;

    @Bean
    @RefreshScope
    public Action mfaSetTrustAction(@Qualifier("mfaTrustEngine") final MultifactorAuthenticationTrustStorage storage) {
        final MultifactorAuthenticationSetTrustAction a = new MultifactorAuthenticationSetTrustAction();
        a.setStorage(storage);
        return a;
    }

    @Bean
    @RefreshScope
    public Action mfaVerifyTrustAction(@Qualifier("mfaTrustEngine") final MultifactorAuthenticationTrustStorage storage) {
        final MultifactorAuthenticationVerifyTrustAction a = new MultifactorAuthenticationVerifyTrustAction();
        a.setStorage(storage);
        a.setNumberOfDays(casProperties.getAuthn().getMfa().getTrusted().getValidNumberOfDays());
        return a;
    }

    @ConditionalOnMissingBean(name = "mfaTrustEngine")
    @Bean
    @RefreshScope
    public MultifactorAuthenticationTrustStorage mfaTrustEngine() {
        final InMemoryMultifactorAuthenticationTrustStorage m = new InMemoryMultifactorAuthenticationTrustStorage();
        m.setCipherExecutor(mfaTrustCipherExecutor());
        return m;
    }

    @Bean
    @RefreshScope
    public CipherExecutor<String, String> mfaTrustCipherExecutor() {
        if (casProperties.getAuthn().getMfa().getTrusted().isCipherEnabled()) {
            return new MultifactorAuthenticationTrustCipherExecutor(
                    casProperties.getAuthn().getMfa().getTrusted().getEncryptionKey(),
                    casProperties.getAuthn().getMfa().getTrusted().getSigningKey());
        }
        LOGGER.info("Multifactor trusted authentication record encryption/signing is turned off and "
                + "may NOT be safe in a production environment. "
                + "Consider using other choices to handle encryption, signing and verification of "
                + "trusted authentication records for MFA");
        return new NoOpCipherExecutor();
    }
}
