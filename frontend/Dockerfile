FROM node:22-alpine AS build
WORKDIR /app
RUN corepack enable && corepack prepare pnpm@11.9.0 --activate
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml .npmrc ./
RUN pnpm install --frozen-lockfile --config.node-linker=hoisted
COPY . .
ARG REACT_APP_ORDER_SERVICE_URL=http://localhost:8090/api
ARG REACT_APP_ANALYTICS_SERVICE_URL=http://localhost:8091
ARG REACT_APP_ANALYTICS_WS_URL=ws://localhost:8091/ws/realtime
ENV REACT_APP_ORDER_SERVICE_URL=$REACT_APP_ORDER_SERVICE_URL \
    REACT_APP_ANALYTICS_SERVICE_URL=$REACT_APP_ANALYTICS_SERVICE_URL \
    REACT_APP_ANALYTICS_WS_URL=$REACT_APP_ANALYTICS_WS_URL
RUN CI=true node node_modules/react-scripts/bin/react-scripts.js build

FROM nginx:1.27-alpine
RUN apk add --no-cache curl
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/build /usr/share/nginx/html
EXPOSE 80
