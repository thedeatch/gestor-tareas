FROM node:22-alpine AS build

WORKDIR /app

COPY package.json package-lock.json ./

RUN npm ci

COPY angular.json ./
COPY tsconfig.json tsconfig.app.json ./
COPY public public
COPY src src

RUN npm run build


FROM nginx:alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf

COPY --from=build \
    /app/dist/gestor-tareas/browser \
    /usr/share/nginx/html

EXPOSE 80