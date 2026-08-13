@description('Enterprise Order Platform - Main Bicep Template')
@minLength(1)
@maxLength(24)
param location string = resourceGroup().location
param environment string = 'production'

// AKS Cluster
module aks './modules/aks.bicep' = {
  name: 'aks-deployment'
  params: {
    location: location
    environment: environment
  }
}

// SQL Server
module sql './modules/sql.bicep' = {
  name: 'sql-deployment'
  params: {
    location: location
    environment: environment
  }
}

// Cosmos DB
module cosmos './modules/cosmos.bicep' = {
  name: 'cosmos-deployment'
  params: {
    location: location
    environment: environment
  }
}

// Service Bus
module servicebus './modules/servicebus.bicep' = {
  name: 'servicebus-deployment'
  params: {
    location: location
    environment: environment
  }
}

// Key Vault
module keyvault './modules/keyvault.bicep' = {
  name: 'keyvault-deployment'
  params: {
    location: location
    environment: environment
  }
}

// Container Registry
module acr './modules/acr.bicep' = {
  name: 'acr-deployment'
  params: {
    location: location
    environment: environment
  }
}

// Application Insights
module appinsights './modules/appinsights.bicep' = {
  name: 'appinsights-deployment'
  params: {
    location: location
    environment: environment
  }
}

// Outputs
output aksClusterName value=aks.outputs.clusterName
output sqlServerFqdn value=sql.outputs.fqdn
output cosmosConnectionString value=cosmos.outputs.connectionString
output containerRegistryLoginServer value=acr.outputs.loginServer
