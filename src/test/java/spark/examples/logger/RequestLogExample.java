/*
 * Copyright 2011- Per Wendel
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package spark.examples.logger;

import org.eclipse.jetty.server.Slf4jRequestLog;

import static spark.Spark.get;
import static spark.Spark.requestLog;

public class RequestLogExample {

    public static void main(String[] args) {
        requestLog(Slf4jRequestLog.class);
        get("/log", (request, response) -> "Logged!");
    }
}
