FROM node:26-alpine
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN chown -R node:node /app
USER node
EXPOSE 5173
CMD ["sh", "./scripts/dev.sh"]
