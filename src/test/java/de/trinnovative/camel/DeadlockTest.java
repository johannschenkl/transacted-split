package de.trinnovative.camel;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.javaconfig.CamelConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

@CamelSpringTest
@ContextConfiguration(classes = {App.class, DeadlockTest.ContextConfig.class})
@DirtiesContext
public class DeadlockTest {

    @EndpointInject("mock:result")
    private MockEndpoint resultEndpoint;

    @Produce("direct:start")
    private ProducerTemplate template;

    @Test
    @Timeout(value = 10)
    public void testSendMatchingMessage() throws Exception {

        var input = IntStream.range(0, 10_000).boxed().collect(Collectors.toList());

        resultEndpoint.expectedBodiesReceived(input);

        template.sendBody(input);

        resultEndpoint.assertIsSatisfied();
    }

    @Configuration
    public static class ContextConfig extends CamelConfiguration {
        @Bean
        public RouteBuilder route1() {
            return new RouteBuilder() {

                @Override
                public void configure() throws Exception {
                    from("direct:start")
                            .split(body())
                            .to("direct:2");
                }
            };
        }

        @Bean
        public RouteBuilder route2() {
            return new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("direct:2")
                            .transacted()
                            .to("mock:result");
                }
            };
        }
    }
}