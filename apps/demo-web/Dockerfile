# Build (contexto = raiz do repositório, para acessar design-system/)
FROM node:22-alpine AS build
WORKDIR /repo
COPY design-system ./design-system
COPY apps/demo-web/package.json apps/demo-web/package-lock.json ./apps/demo-web/
RUN cd apps/demo-web && npm ci --no-audit --no-fund
COPY apps/demo-web ./apps/demo-web
RUN cd apps/demo-web && npm run build

# Runtime: servidor standalone do Next.js
FROM node:22-alpine
WORKDIR /app
ENV NODE_ENV=production PORT=3000 HOSTNAME=0.0.0.0 NEXT_TELEMETRY_DISABLED=1
COPY --from=build /repo/apps/demo-web/.next/standalone ./
COPY --from=build /repo/apps/demo-web/.next/static ./.next/static
COPY --from=build /repo/apps/demo-web/public ./public
USER node
EXPOSE 3000
CMD ["node", "server.js"]
