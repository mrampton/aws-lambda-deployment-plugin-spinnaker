/*
 * Copyright 2018 Amazon.com, Inc. or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.amazon.aws.spinnaker.plugin.lambda.delete;

import com.amazon.aws.spinnaker.plugin.lambda.LambdaCloudOperationOutput;
import com.amazon.aws.spinnaker.plugin.lambda.LambdaStageBaseTask;
import com.amazon.aws.spinnaker.plugin.lambda.delete.model.LambdaDeleteStageInput;
import com.amazon.aws.spinnaker.plugin.lambda.utils.LambdaCloudDriverResponse;
import com.amazon.aws.spinnaker.plugin.lambda.utils.LambdaCloudDriverUtils;
import com.amazon.aws.spinnaker.plugin.lambda.utils.LambdaDefinition;
import com.netflix.spinnaker.orca.api.pipeline.TaskResult;
import com.netflix.spinnaker.orca.api.pipeline.models.StageExecution;
import com.netflix.spinnaker.orca.clouddriver.config.CloudDriverConfigurationProperties;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class LambdaDeleteTask  implements LambdaStageBaseTask {
    private static Logger logger = LoggerFactory.getLogger(LambdaDeleteTask.class);

    private String cloudDriverUrl;

    @Autowired
    CloudDriverConfigurationProperties props;

    @Autowired
    private LambdaCloudDriverUtils utils;

    private static String CLOUDDRIVER_DELETE_LAMBDA_PATH = "/aws/ops/deleteLambdaFunction";

    @NotNull
    @Override
    public TaskResult execute(@NotNull StageExecution stage) {
        logger.debug("Executing LambdaDeletionTask...");
        cloudDriverUrl = props.getCloudDriverBaseUrl();
        prepareTask(stage);
        LambdaDeleteStageInput ldi = utils.getInput(stage, LambdaDeleteStageInput.class);
        ldi.setAppName(stage.getExecution().getApplication());

        if (ldi.getVersion().equals("$ALL")) {
            addToTaskContext(stage, "deleteTask:deleteVersion", ldi.getVersion());
            return formTaskResult(stage, deleteLambda(ldi), stage.getOutputs());
        }

        String versionToDelete = getVersion(stage, ldi);
        if (versionToDelete == null) {
            addErrorMessage(stage, "No version found for Lambda function. Unable to perform delete operation.");
            return formSuccessTaskResult(stage, "LambdaDeleteTask",  "Found no version of function to delete");
        }

        addToTaskContext(stage, "deleteTask:deleteVersion", versionToDelete);

        if (!versionToDelete.contains(",")) {
            ldi.setQualifier(versionToDelete);
            return formTaskResult(stage, deleteLambda(ldi), stage.getOutputs());
        }

        String[] allVersionsList = versionToDelete.split(",");
        List<String> urlList = new ArrayList<String>();

        for (String currVersion : allVersionsList) {
            ldi.setQualifier((String) currVersion);
            LambdaCloudOperationOutput ldso = deleteLambda(ldi);
            urlList.add(ldso.getUrl());
        }
        addToTaskContext(stage, "urlList", urlList);
        return taskComplete(stage);
    }

    private String getVersion(StageExecution stage, LambdaDeleteStageInput ldi) {
        if (ldi.getVersion() == null) {
            return null;
        }
        if (!ldi.getVersion().startsWith("$")) { // actual version number
            return ldi.getVersion();
        }

        if (ldi.getVersion().startsWith("$PROVIDED")) { // actual version number
            return ldi.getVersionNumber();
        }

        LambdaDefinition lf = utils.retrieveLambdaFromCache(stage, true);
        if (lf != null) {
            return utils.getCanonicalVersion(lf, ldi.getVersion(), ldi.getVersionNumber(), ldi.getRetentionNumber());
        }
        return null;
    }

    private LambdaCloudOperationOutput deleteLambda(LambdaDeleteStageInput inp) {
        inp.setCredentials(inp.getAccount());
        String endPoint = cloudDriverUrl + CLOUDDRIVER_DELETE_LAMBDA_PATH;
        String rawString = utils.asString(inp);
        LambdaCloudDriverResponse respObj = utils.postToCloudDriver(endPoint, rawString);
        String url = cloudDriverUrl + respObj.getResourceUri();
        logger.debug("Posted to cloudDriver for deleteLambda: " + url);
        return LambdaCloudOperationOutput.builder().url(url).build();
    }
}

