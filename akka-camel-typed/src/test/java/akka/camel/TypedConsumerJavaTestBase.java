package akka.camel;

import akka.actor.TypedActor;
import akka.japi.SideEffect;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static akka.actor.Actors.*;
import static akka.camel.CamelContextManager.*;
import static akka.camel.CamelServiceManager.*;

import static org.junit.Assert.*;

/**
 * @author Martin Krasser
 */
public class TypedConsumerJavaTestBase {

    private SampleErrorHandlingTypedConsumer consumer;

    @BeforeClass
    public static void setUpBeforeClass() {
        startCamelService();
    }

    @AfterClass
    public static void tearDownAfterClass() {
        stopCamelService();
        registry().shutdownAll();
    }

    @Test
    public void shouldHandleExceptionThrownByTypedActorAndGenerateCustomResponse() {
        getMandatoryService().awaitEndpointActivation(1, new SideEffect() {
            public void apply() {
                consumer = TypedActor.newInstance(
                        SampleErrorHandlingTypedConsumer.class,
                        SampleErrorHandlingTypedConsumerImpl.class);
            }
        });
        String result = getMandatoryTemplate().requestBody("direct:error-handler-test-java-typed", "hello", String.class);
        assertEquals("error: hello", result);
    }
}
