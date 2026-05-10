FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
ARG VITE_API_BASE_URL=
ARG NITRO_API_PROXY_TARGET=http://api-gateway:8080
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
ENV NITRO_API_PROXY_TARGET=${NITRO_API_PROXY_TARGET}
RUN npm run build

FROM node:20-alpine
WORKDIR /app
ENV NODE_ENV=production
ENV HOST=0.0.0.0
ENV PORT=80
ENV NITRO_API_PROXY_TARGET=http://api-gateway:8080
COPY --from=build /app/.output ./.output
EXPOSE 80
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 CMD wget -qO- http://127.0.0.1/ >/dev/null || exit 1
CMD ["node", ".output/server/index.mjs"]
