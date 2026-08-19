# Build stage
FROM node:18-alpine AS build
WORKDIR /app
COPY package.json .
RUN npm install
COPY . .

# Demo credentials are baked in only when provided at build time (local .env).
# CI builds leave them empty; the login screen shows a README hint instead.
ARG REACT_APP_DEMO_USERNAME
ARG REACT_APP_DEMO_PASSWORD
ENV REACT_APP_DEMO_USERNAME=${REACT_APP_DEMO_USERNAME}
ENV REACT_APP_DEMO_PASSWORD=${REACT_APP_DEMO_PASSWORD}

RUN npm run build

# Production stage
FROM nginx:alpine
COPY --from=build /app/build /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]