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
package org.apache.camel.component.timer;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

import org.apache.camel.Endpoint;
import org.apache.camel.impl.DefaultComponent;

/**
 * Represents the component that manages {@link TimerEndpoint}.  It holds the
 * list of {@link TimerConsumer} objects that are started.
 *
 * @version 
 */
public class TimerComponent extends DefaultComponent {
    private final Map<String, Timer> timers = new HashMap<String, Timer>();

    public Timer getTimer(TimerEndpoint endpoint) {
        String key = endpoint.getTimerName();
        if (!endpoint.isDaemon()) {
            key = "nonDaemon:" + key;
        }

        Timer answer;
        synchronized (timers) {
            answer = timers.get(key);
            if (answer == null) {
                // the timer name is also the thread name, so lets resolve a name to be used
                String name = endpoint.getCamelContext().getExecutorServiceManager().resolveThreadName(endpoint.getTimerName());
                answer = new Timer(name, endpoint.isDaemon());
                timers.put(key, answer);
            }
        }
        return answer;
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        TimerEndpoint answer = new TimerEndpoint(uri, this, remaining);

        // convert time from String to a java.util.Date using the supported patterns
        String time = getAndRemoveParameter(parameters, "time", String.class);
        String pattern = getAndRemoveParameter(parameters, "pattern", String.class);
        if (time != null) {
            SimpleDateFormat sdf;
            if (pattern != null) {
                sdf = new SimpleDateFormat(pattern);
            } else if (time.contains("T")) {
                sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            } else {
                sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            }
            Date date = sdf.parse(time);
            answer.setTime(date);
        }

        setProperties(answer, parameters);
        return answer;
    }

    @Override
    protected void doStop() throws Exception {
        Collection<Timer> collection = timers.values();
        for (Timer timer : collection) {
            timer.cancel();
        }
        timers.clear();
    }
}
