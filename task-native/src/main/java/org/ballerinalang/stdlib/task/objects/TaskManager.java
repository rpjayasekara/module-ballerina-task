/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.stdlib.task.objects;

import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.Runtime;
import org.ballerinalang.stdlib.task.exceptions.SchedulingException;
import org.ballerinalang.stdlib.task.utils.TaskConstants;
import org.ballerinalang.stdlib.task.utils.Utils;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Task manager to handle schedulers in ballerina tasks.
 */
public class TaskManager {
    private Scheduler scheduler;
    private Runtime runtime = null;
    Map<Integer, JobDetail> jobInfoMap = new HashMap<>();
    Map<Integer, Trigger> triggerInfoMap = new ConcurrentHashMap<>();
    Properties properties;
    boolean isConfiguredSchFactory = false;

    private static class TaskManagerHelper {
        private static final TaskManager INSTANCE = new TaskManager();
    }

    private TaskManager() {}

    public static TaskManager getInstance() {
        return TaskManagerHelper.INSTANCE;
    }

    public void initializeScheduler(Properties properties, Environment env) throws SchedulingException,
            SchedulerException {
        getAllRunningJobs();
        if (this.scheduler != null) {
            this.scheduler.shutdown();
        }
        if (!triggerInfoMap.isEmpty()) {
            configureScheduler(properties, env);
        } else {
            this.properties = properties;
            isConfiguredSchFactory = true;
        }
    }

    public void rescheduleJobs() throws SchedulerException {
        startScheduler();
        for (Map.Entry<Integer, Trigger> entry : triggerInfoMap.entrySet()) {
            this.scheduler.scheduleJob(jobInfoMap.get(entry.getKey()), entry.getValue());
        }
    }

    public Scheduler getScheduler(Properties properties, Environment env) throws SchedulingException,
            SchedulerException {
        if (isConfiguredSchFactory) {
            this.scheduler = Utils.initializeScheduler(this.properties);
            isConfiguredSchFactory = false;
            setRuntime(env.getRuntime());
        } else {
            if (this.scheduler == null || this.scheduler.isShutdown()) {
                this.scheduler = Utils.initializeScheduler(properties);
                setRuntime(env.getRuntime());
            }
        }
        return this.scheduler;
    }

    private void setRuntime(Runtime runtime) {
        this.runtime = runtime;
    }

    public Runtime getRuntime() {
        return this.runtime;
    }

    public Map<Integer, Trigger>  getTriggerInfoMap() {
        return this.triggerInfoMap;
    }

    public Set<Integer> getAllRunningJobs() throws SchedulerException {
        for (Map.Entry<Integer, Trigger> entry : triggerInfoMap.entrySet()) {
            Trigger.TriggerState triggerState = scheduler.getTriggerState(entry.getValue().getKey());
            if (triggerState != null && isTriggerCompleted(triggerState)) {
                this.triggerInfoMap.remove(entry.getKey());
            }
        }
        return this.triggerInfoMap.keySet();
    }

    public void scheduleOneTimeJob(JobDataMap jobDataMap, long time, Integer jobId) throws SchedulerException {
        scheduleJob(Utils.createJob(jobDataMap, jobId.toString()),
                Utils.getOneTimeTrigger(time, TaskConstants.TRIGGER_ID), jobId);
    }

    public void scheduleIntervalJob(JobDataMap jobDataMap, long interval, long maxCount, Object startTime,
                                    Object endTime, String waitingPolicy, Integer jobId) throws SchedulerException {
        JobDetail job = Utils.createJob(jobDataMap, jobId.toString());
        Trigger trigger = Utils.getIntervalTrigger(interval, maxCount, startTime, endTime, waitingPolicy,
                TaskConstants.TRIGGER_ID);
        scheduleJob(job, trigger, jobId);
    }

    private void scheduleJob(JobDetail job, Trigger trigger, Integer jobId) throws SchedulerException {
        this.scheduler.scheduleJob(job, trigger);
        this.triggerInfoMap.put(jobId, trigger);
        this.jobInfoMap.put(jobId, job);
        startScheduler();
    }

    private void startScheduler () throws SchedulerException {
        if (!this.scheduler.isStarted()) {
            this.scheduler.start();
        }
    }

    public void unScheduleJob(Integer jobId) throws SchedulerException, SchedulingException {
        this.scheduler.unscheduleJob(getTrigger(jobId).getKey());
        if (getAllRunningJobs().isEmpty()) {
            this.scheduler.shutdown();
        }
    }

    public void pause() throws SchedulerException {
        this.scheduler.pauseAll();
    }

    public void resume() throws SchedulerException {
        this.scheduler.resumeAll();
    }

    public void pauseJob(Integer jobId) throws SchedulerException, SchedulingException {
        this.scheduler.pauseJob(getTrigger(jobId).getJobKey());
    }

    public void resumeJob(Integer jobId) throws SchedulerException, SchedulingException {
        this.scheduler.resumeJob(getTrigger(jobId).getJobKey());
    }

    private boolean isTriggerCompleted(Trigger.TriggerState triggerState) {
        return triggerState.equals(Trigger.TriggerState.COMPLETE) || triggerState.equals(Trigger.TriggerState.NONE);
    }

    private void configureScheduler(Properties properties, Environment env) throws SchedulerException,
            SchedulingException {
        this.scheduler = Utils.initializeScheduler(properties);
        setRuntime(env.getRuntime());
        if (!triggerInfoMap.isEmpty()) {
            rescheduleJobs();
        }
    }

    private Trigger getTrigger(Integer jobId) throws SchedulingException {
        if (this.triggerInfoMap.get(jobId) == null) {
            throw new SchedulingException("Invalid job id: " + jobId);
        } else {
            return this.triggerInfoMap.get(jobId);
        }
    }
}
