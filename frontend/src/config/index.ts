const config = {
  apiGateway: process.env.REACT_APP_API_GATEWAY || 'http://localhost:18080',
  keycloak: {
    url: process.env.REACT_APP_KEYCLOAK_URL || 'http://localhost:18180',
    realm: process.env.REACT_APP_KEYCLOAK_REALM || 'enterprise-platform',
    clientId: process.env.REACT_APP_KEYCLOAK_CLIENT_ID || 'enterprise-order-platform'
  }
};

export default config;
