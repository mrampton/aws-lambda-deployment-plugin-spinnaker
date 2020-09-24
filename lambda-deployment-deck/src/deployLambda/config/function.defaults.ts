// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0
import {
  IAmazonFunctionUpsertCommand,
} from '@spinnaker/amazon';

export function constructNewAwsFunctionTemplate(): IAmazonFunctionUpsertCommand {
  const defaultCredentials = ''; 
  const defaultRegion = '';
  return {
    role: '',
    runtime: '',
    s3bucket: '',
    s3key: '',
    handler: '',
    functionName: '',
    publish: false,
    tags: {},
    memorySize: 128,
    description: '',

    credentials: defaultCredentials, 
    cloudProvider: 'aws',  
    region: defaultRegion,
    envVariables: {},

    tracingConfig: {
      mode: 'PassThrough',
    },
    KMSKeyArn: '',
    vpcId: '',
    subnetIds: [],
    securityGroupIds: [],
    timeout: 3,
    deadLetterConfig: {
      targetArn: '',
    },
    operation: '',
    targetGroups: '',
  };
}
