/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.ribbon;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import rx.Observable;

import com.netflix.client.DefaultLoadBalancerRetryHandler;
import com.netflix.client.RetryHandler;
import com.netflix.loadbalancer.BaseLoadBalancer;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.LoadBalancerBuilder;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.reactive.LoadBalancerCommand;
import com.netflix.loadbalancer.reactive.ServerOperation;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultProducer;


/**
 * The Ribbon producer.
 */
public class RibbonProducer extends DefaultProducer {
    private RibbonConfiguration configuration;
    private ILoadBalancer loadBalancer;
    private RetryHandler retryHandler;
    private final LoadBalancerCommand<Exchange> loadBalancerCommand;

    public RibbonProducer(RibbonEndpoint endpoint, RibbonConfiguration configuration) {
        super(endpoint);
        this.configuration = configuration;

        List servers = new ArrayList<>();
        servers.add(new Server("www.google.com", 80));
        servers.add(new Server("www.linkedin.com", 80));
        servers.add(new Server("www.yandex.com", 80));

        loadBalancer = LoadBalancerBuilder.newBuilder().buildFixedServerListLoadBalancer(servers);
        retryHandler = new DefaultLoadBalancerRetryHandler(0, 1, true);
        loadBalancerCommand = LoadBalancerCommand.<Exchange>builder()
                .withLoadBalancer(loadBalancer)
                .withRetryHandler(retryHandler)
                .build();

    }

    public void process(final Exchange exchange) throws Exception {
        loadBalancerCommand.submit(new ServerOperation<Exchange>() {
            @Override
            public Observable<Exchange> call(Server server) {
                exchange.getIn().setHeader("HOST", server.getHost());
                exchange.getIn().setHeader("POST", server.getPort());
                URL url;
                try {
                    url = new URL("http://" + server.getHost() + ":" + server.getPort() + "/");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    exchange.getIn().setBody(conn.getResponseMessage());
                    return Observable.just(exchange);
                } catch (Exception e) {
                    return Observable.error(e);
                }
            }
        }).toBlocking().first();
        System.out.println("=== Load balancer stats ===");
        System.out.println( ((BaseLoadBalancer) loadBalancer).getLoadBalancerStats());
    }

//
//    public void process(final Exchange exchange) throws Exception {
//        loadBalancerCommand.submit(new ServerOperation<Exchange>() {
//            @Override
//            public Observable<Exchange> call(Server server) {
//                URL url;
//                try {
//                    url = new URL("http://" + server.getHost() + ":" + server.getPort() + "/");
//                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//                    exchange.getIn().setBody(conn.getResponseMessage());
//                    return Observable.just(exchange);
//                } catch (Exception e) {
//                    return Observable.error(e);
//                }
//            }
//        }).toBlocking().first();
//        System.out.println("=== Load balancer stats ===");
//        System.out.println( ((BaseLoadBalancer) loadBalancer).getLoadBalancerStats());
//    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
    }

}
