/*
 *  Copyright (c) 2019 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.ballerinalang.stdlib.task.listener.objects;

import org.ballerinalang.bre.Context;
import org.ballerinalang.connector.api.Service;
import org.ballerinalang.stdlib.task.SchedulingException;

import java.util.ArrayList;

/**
 * Ballerina task interface.
 */
public interface Task {

    /**
     * Get the registered ID of the task.
     *
     * @return  taskId
     */
    String getId();

    /**
     * Stop the task.
     */
    void stop();

    /**
     * Get list of attached services of the task.
     *
     * @return  Services List
     */
    ArrayList<Service> getServices();

    /**
     * Add particular service to the registry of the Task.
     *
     * @param service   Service which needs to be attached to the task.
     */
    void addService(Service service);

    /**
     * Pause the task, if running.
     *
     * @throws SchedulingException  when task is not running or any other error occurs.
     */
    void pause() throws SchedulingException;

    /**
     * Resume the task if paused.
     *
     * @throws SchedulingException  when the task is not already paused or any other error occurs.
     */
    void resume() throws SchedulingException;

    /**
     * Run all the services attached to the task.
     *
     * @param context - Ballerina context which runs the services.
     */
    void runServices(Context context);
}
