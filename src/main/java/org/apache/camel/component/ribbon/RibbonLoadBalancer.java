package org.apache.camel.component.ribbon;

import org.apache.camel.Exchange;
import org.apache.camel.processor.loadbalancer.SimpleLoadBalancerSupport;

/**
 * Created by bibryam on 18/04/2016.
 */
public class RibbonLoadBalancer extends SimpleLoadBalancerSupport {

    @Override
    public void process(Exchange exchange) throws Exception {
        String body = exchange.getIn().getBody(String.class);
        try {
            if ("x".equals(body)) {
                getProcessors().get(0).process(exchange);
            } else if ("y".equals(body)) {
                getProcessors().get(1).process(exchange);
            } else {
                getProcessors().get(2).process(exchange);
            }
        } catch (Throwable e) {
            exchange.setException(e);
        }
    }
}
