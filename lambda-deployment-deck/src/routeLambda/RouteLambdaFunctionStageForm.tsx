// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

import React from 'react';

import {
  AccountService, 
  FormikFormField,
  IAccount,
  IAccountDetails, 
  IFormInputProps,
  IFormikStageConfigInjectedProps,
  IFunction,
  IRegion,    
  ReactSelectInput,
  TextInput,
  useData, 
} from '@spinnaker/core';

import {
  DeploymentStrategyList,
  DeploymentStrategyPicker,
} from './constants';

import {
  DeploymentStrategyForm,
} from './components/DeploymentStrategyForm';

export function RouteLambdaFunctionStageForm(props: IFormikStageConfigInjectedProps) {
  const { values, errors } = props.formik; 
  const { functions } = props.application;
  
  const { result: fetchAccountsResult, status: fetchAccountsStatus } = useData(
    () => AccountService.listAccounts('aws'),
    [],
    [],
  );

  const onAccountChange = (fieldValue: any): void => {
    props.formik.setFieldValue("region", null);
    props.formik.setFieldValue("functionName", null);

    props.formik.setFieldValue("account", fieldValue);
  };

  const onRegionChange = (fieldValue: any): void => {
    props.formik.setFieldValue("functionName", null);
    props.formik.setFieldValue("region", fieldValue);
  };


  const availableFunctions = values.account && values.region ?
    functions.data
      .filter((f: IFunction) => f.account === values.account)
      .filter((f: IFunction) => f.region === values.region)
      .map((f: IFunction) => f.functionName) :
    [];

  return (
    <div className="form-horizontal">
      <h4> Basic Settings </h4> 
      <FormikFormField 
        label="Account"
        name="account"
        onChange={onAccountChange}
        required={true}
        input={(inputProps: IFormInputProps) => (
          <ReactSelectInput
            {...inputProps}
            clearable={false} 
            isLoading={fetchAccountsStatus === 'PENDING'}
            stringOptions={fetchAccountsResult.map((acc: IAccount) => acc.name)}
          />
        )}
      />
      <FormikFormField
        label="Region"
        name="region"
        onChange={onRegionChange}
        input={(inputProps: IFormInputProps) => (
          <ReactSelectInput
            clearable={false}
            disabled={ !(values.account) }
            placeholder={
              values.account ?
              "Select..." :
              "Select an Account..."
            }
            {...inputProps} 
            isLoading={fetchAccountsStatus === 'PENDING'}
            stringOptions={fetchAccountsResult
              .filter((acc: IAccountDetails) => acc.name === values.account)
              .flatMap((acc: IAccountDetails) => acc.regions)
              .map((reg: IRegion) => reg.name)  
            }
          />
        )}
      />
      <FormikFormField
        label="Function Name"
        name="functionName" 
        input={(inputProps: IFormInputProps) => (
          <ReactSelectInput
            clearable={false}
            disabled={ !(values.account && values.region) }
            placeholder={
              values.account && values.region ?
              "Select..." :
              "Select an Account and Region..."
            }
            {...inputProps}
            stringOptions={ availableFunctions }
          />
        )}
      /> 
      
      < FormikFormField
        label="Alias"
        name="aliasName"
        input={( inputProps: IFormInputProps) => (
          < TextInput {...inputProps} />
        )}
      />
      <FormikFormField
        label="Strategy"
        name="deploymentStrategy"
        input={(inputProps: IFormInputProps) => (
          <ReactSelectInput
            {...inputProps}
            clearable={false}
            options={DeploymentStrategyList}
            optionRenderer={option => (
              < DeploymentStrategyPicker
                config={props}
                value={(option.value as any)}
                showingDetails={true}
              />
            )}
          />
        )}
      />
      { values.deploymentStrategy ?
        <div>
          <h4> Strategy Settings </h4>
          < DeploymentStrategyForm {...props} /> 
        </div> : null
      }
    </div>
  );
}
