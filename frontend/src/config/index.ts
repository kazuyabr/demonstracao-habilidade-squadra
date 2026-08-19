const config = {
  apiGateway: process.env.REACT_APP_API_GATEWAY || '',
  keycloak: {
    url: process.env.REACT_APP_KEYCLOAK_URL || 'http://localhost:18180',
    realm: process.env.REACT_APP_KEYCLOAK_REALM || 'enterprise-platform',
    clientId: process.env.REACT_APP_KEYCLOAK_CLIENT_ID || 'enterprise-order-platform',
    redirectUri: process.env.REACT_APP_REDIRECT_URI || 'http://localhost:13003/callback'
  },
  demo: {
    username: process.env.REACT_APP_DEMO_USERNAME || 'demouser',
    password: process.env.REACT_APP_DEMO_PASSWORD || '',
    // If a password is not baked into the build, point users at the README.
    hint: process.env.REACT_APP_DEMO_PASSWORD ? undefined : 'See README for the demo password'
  }
};

export default config;