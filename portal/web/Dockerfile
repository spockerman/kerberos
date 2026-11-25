# Build (contexto = raiz do repositório, para acessar design-system/)
FROM node:22-alpine AS build
WORKDIR /repo
COPY design-system ./design-system
COPY portal/web/package.json portal/web/package-lock.json ./portal/web/
RUN cd portal/web && npm ci --no-audit --no-fund
COPY portal/web ./portal/web
RUN cd portal/web && npm run build

# Runtime: servidor standalone do Next.js
FROM node:22-alpine
WORKDIR /app
ENV NODE_ENV=production PORT=3001 HOSTNAME=0.0.0.0 NEXT_TELEMETRY_DISABLED=1
COPY --from=build /repo/portal/web/.next/standalone ./
COPY --from=build /repo/portal/web/.next/static ./.next/static
COPY --from=build /repo/portal/web/public ./public
USER node
EXPOSE 3001
CMD ["node", "server.js"]
