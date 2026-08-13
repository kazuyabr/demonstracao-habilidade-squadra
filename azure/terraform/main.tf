terraform {
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 3.0"
    }
  }

  backend "azurerm" {
    resource_group_name  = "terraform-state-rg"
    storage_account_name = "terraformstateenterprise"
    container_name       = "tfstate"
    key                  = "enterprise-order-platform.tfstate"
  }
}

provider "azurerm" {
  features {}
}

# Resource Group
resource "azurerm_resource_group" "main" {
  name     = "enterprise-order-platform-rg"
  location = "East US"
}

# AKS Cluster
resource "azurerm_kubernetes_cluster" "main" {
  name                = "enterprise-order-aks"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  dns_prefix          = "enterprise-order"

  default_node_pool {
    name       = "default"
    node_count = 3
    vm_size    = "Standard_D2s_v3"
  }

  identity {
    type = "SystemAssigned"
  }

  tags = {
    environment = "production"
  }
}

# SQL Server
resource "azurerm_mssql_server" "main" {
  name                         = "enterprise-order-sql"
  resource_group_name          = azurerm_resource_group.main.name
  location                     = azurerm_resource_group.main.location
  version                      = "12.0"
  administrator_login          = "sqladmin"
  administrator_login_password = var.sql_password
}

# SQL Database
resource "azurerm_mssql_database" "order" {
  name      = "order_db"
  server_id = azurerm_mssql_server.main.id
}

resource "azurerm_mssql_database" "payment" {
  name      = "payment_db"
  server_id = azurerm_mssql_server.main.id
}

resource "azurerm_mssql_database" "saga" {
  name      = "saga_db"
  server_id = azurerm_mssql_server.main.id
}

# Cosmos DB (MongoDB API)
resource "azurerm_cosmosdb_account" "main" {
  name                = "enterprise-order-cosmos"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  offer_type          = "Standard"
  kind                = "MongoDB"

  consistency_policy {
    consistency_level = "Session"
  }

  geo_location {
    location          = azurerm_resource_group.main.location
    failover_priority = 0
  }
}

# Service Bus (替代 Pulsar)
resource "azurerm_servicebus_namespace" "main" {
  name                = "enterprise-order-servicebus"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  sku                 = "Standard"
}

# Key Vault
resource "azurerm_key_vault" "main" {
  name                = "enterprise-order-kv"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  tenant_id           = data.azurerm_client_config.current.tenant_id
  sku_name            = "standard"
}

data "azurerm_client_config" "current" {}

# Container Registry
resource "azurerm_container_registry" "main" {
  name                = "enterpriseorderregistry"
  resource_group_name = azurerm_resource_group.main.name
  location            = azurerm_resource_group.main.location
  sku                 = "Standard"
  admin_enabled       = true
}

# Application Insights
resource "azurerm_application_insights" "main" {
  name                = "enterprise-order-insights"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  application_type    = "web"
}

# Variables
variable "sql_password" {
  description = "SQL Server administrator password"
  type        = string
  sensitive   = true
}

# Outputs
output "aks_cluster_name" {
  value = azurerm_kubernetes_cluster.main.name
}

output "sql_server_fqdn" {
  value = azurerm_mssql_server.main.fully_qualified_domain_name
}

output "cosmosdb_connection_string" {
  value     = azurerm_cosmosdb_account.main.connection_strings[0]
  sensitive = true
}

output "container_registry_login_server" {
  value = azurerm_container_registry.main.login_server
}
