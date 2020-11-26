// Copyright (c) 2020 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/runtime;
import ballerina/test;

SimpleTriggerConfiguration config = {
    intervalInMillis: 1000,
    initialDelayInMillis: 1000
};

int counter1 = 0;
int counter2 = 0;

function pauseResumeJob1() {
    counter1 = counter1 + 1;
}

function pauseResumeJob2() {
    counter2 = counter2 + 1;
}

@test:Config {}
function testTaskPauseAndResume() {
    Scheduler timer1 = scheduler();
    Scheduler timer2 = scheduler();
    var attachResult = timer1.scheduleJob(pauseResumeJob1, config);
    var attachResult1 = timer2.scheduleJob(pauseResumeJob2, config);
    runtime:sleep(3500);
    var result = timer1.pauseAllJobs();
    if (result is error) {
        test:assertFail("An error occurred when pausing the scheduler");
    }
    runtime:sleep(4000);
    test:assertEquals(counter1, 3, msg = "Values are mismatched");
    result = timer1.resumeAllJobs();
    if (result is error) {
        return;
    }
    checkpanic timer1.stop();
    checkpanic timer2.stop();
    test:assertTrue(counter2 - counter1 == 0, msg = "Test failred");
}

int jobCount1 = 0;
int jobCount2 = 0;

function job1() {
    jobCount1 = jobCount1 + 1;
}

function job2() {
    jobCount2 = jobCount2 + 1;
}

@test:Config {}
function testSchedulerPauseJob() {
    Scheduler sch = scheduler();
    var id1 =  sch.scheduleJob(job1, {intervalInMillis: 1000});
    var id2 =  sch.scheduleJob(job2, {intervalInMillis: 1000});
    runtime:sleep(3500);
    test:assertEquals(jobCount1, 3, msg = "Expected value mismatched");
    test:assertEquals(jobCount2, 3, msg = "Expected value mismatched");
    if (id1 is int) {
        checkpanic sch.pauseJob(id1);
        runtime:sleep(4500);
    }
    checkpanic sch.stop();
    test:assertTrue(jobCount2 - jobCount1 > 3, msg = "Expected value mismatched");
}

int jobCount3 = 0;
int jobCount4 = 0;

function job3() {
    jobCount3 = jobCount3 + 1;
}

function job4() {
    jobCount4 = jobCount4 + 1;
}

@test:Config {}
function testSchedulerPauseResumeJob() {
    Scheduler sch = scheduler();
    var id3 =  sch.scheduleJob(job3, {intervalInMillis: 1000});
    var id4 =  sch.scheduleJob(job4, {intervalInMillis: 1000});
    runtime:sleep(3500);
    test:assertEquals(jobCount3, 3, msg = "Output mismatched");
    test:assertEquals(jobCount4, 3, msg = "Expected count mismatched");
    if (id3 is int) {
        checkpanic sch.pauseJob(id3);
        runtime:sleep(3500);
        test:assertEquals(jobCount3, 3, msg = "Output mismatched");
        test:assertTrue(jobCount4 > 5, msg = "Expected count mismatched");
        checkpanic sch.resumeJob(id3);
        runtime:sleep(1500);
    }
    checkpanic sch.stop();
    test:assertEquals(jobCount4 - jobCount3, 0, msg = "Expected value mismatched");
}

int pauseCount = 0;

service pauseResumeService = service {
    resource function onTrigger() {
        pauseCount = pauseCount + 1;
    }
};

listener Listener pauseListner = new({intervalInMillis: 1000});

@test:Config {}
function testListenerPauseResume() {
    var attachResult = trap pauseListner.__attach(pauseResumeService);
    checkpanic pauseListner.__start();
    runtime:sleep(2200);
    test:assertEquals(pauseCount, 2, msg = "Output mismatched");
    checkpanic pauseListner.pause();
    runtime:sleep(4500);
    test:assertEquals(pauseCount, 2, msg = "Output mismatched");
    checkpanic pauseListner.resume();
    runtime:sleep(1500);
    test:assertTrue(pauseCount >= 3, msg = "Output mismatched");
    checkpanic pauseListner.__gracefulStop();
}