# Optimized Dockerfile for Google Cloud Run
# Multi-stage build for P-Card Platform

# Stage 1: Build the application
FROM maven:3.8.6-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy pom.xml and download dependencies (layer caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime environment optimized for Cloud Run
FROM tomcat:9.0.93-jdk17-temurin-jammy

# Remove default Tomcat webapps
RUN rm -rf /usr/local/tomcat/webapps/*

# Copy the built WAR file
COPY --from=builder /app/target/p-card-platform-1.0-SNAPSHOT.war /usr/local/tomcat/webapps/ROOT.war

# Create SSL directory and copy certificates (for Aiven MySQL)
RUN mkdir -p /app/ssl && chmod 755 /app/ssl
# Uncomment one of the following based on your certificate format:
# For PEM format (requires MySQL Connector/J 8.0.13+):
# COPY ssl/aiven-ca.pem /app/ssl/aiven-ca.pem
# For JKS format (traditional Java truststore):
# COPY ssl/aiven-truststore.jks /app/ssl/aiven-truststore.jks

# Create uploads directory (Cloud Run uses Cloud Storage, but keep for local testing)
RUN mkdir -p /uploads && chmod 755 /uploads

# Configure Tomcat for Cloud Run production
# Cloud Run will inject PORT env var, but Tomcat uses 8080 by default
RUN sed -i 's/8080/${PORT:-8080}/g' /usr/local/tomcat/conf/server.xml && \
    echo 'JAVA_OPTS="$JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -Xms256m -Xmx768m"' > /usr/local/tomcat/bin/setenv.sh && \
    chmod +x /usr/local/tomcat/bin/setenv.sh

# Environment variables (will be overridden by Cloud Run)
ENV DB_URL=jdbc:mysql://localhost:3306/p_card_db?useSSL=false&serverTimezone=Asia/Shanghai
ENV DB_USERNAME=pcard_user
ENV DB_PASSWORD=changeme
ENV APP_TIMEZONE=Asia/Shanghai

# Cloud Run health checks
HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=3 \
  CMD curl -f http://localhost:${PORT:-8080}/ || exit 1

# Expose port (Cloud Run will use PORT env var)
EXPOSE 8080

# Signal handling for graceful shutdown
STOPSIGNAL SIGTERM

# Start Tomcat
CMD ["catalina.sh", "run"]
