# Imagem única: build do front + jar do Spring + base em /app/data.
# A base já vem no repositório (pasta data/).
#   docker build -t pauta .
#   docker run -p 8080:8080 pauta

FROM node:22-alpine AS front
WORKDIR /src/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build -- --outDir /out/static

FROM eclipse-temurin:21-jdk-alpine AS back
WORKDIR /src/backend
COPY backend/ ./
COPY --from=front /out/static ./src/main/resources/static
RUN chmod +x mvnw && ./mvnw -B -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=back /src/backend/target/pauta.jar ./pauta.jar
COPY data/ ./data/
ENV PAUTA_DATA_DIR=/app/data
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "pauta.jar"]
