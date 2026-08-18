const { createProxyMiddleware } = require('http-proxy-middleware');

// In development the CRA dev server proxies API calls to the gateway,
// mirroring what nginx does in the production image.
module.exports = function (app) {
  app.use(
    '/api',
    createProxyMiddleware({
      target: 'http://api-gateway:8080',
      changeOrigin: true
    })
  );
};