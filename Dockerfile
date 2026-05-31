# =============================================================
# STAGE 1 — BUILD
# Usa a imagem oficial do Maven com JDK 17 para compilar
# Não depende do Maven Wrapper local (evita problemas de SHA-256)
# =============================================================
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /build

# Copia o pom.xml primeiro para cache eficiente de dependências
COPY pom.xml ./

# Baixa as dependências (camada cacheável separada do código-fonte)
RUN mvn dependency:go-offline -q

# Copia o restante do código-fonte
COPY src/ src/

# Build de produção sem testes (testes rodam na CI)
RUN mvn clean package -DskipTests -q

# =============================================================
# STAGE 2 — RUNTIME
# Imagem final enxuta (JRE apenas), sem Maven nem código-fonte
# =============================================================
FROM eclipse-temurin:17-jre

WORKDIR /work/

# Copia apenas os artefatos compilados do stage de build
COPY --from=build /build/target/quarkus-app/lib/     /work/lib/
COPY --from=build /build/target/quarkus-app/*.jar    /work/
COPY --from=build /build/target/quarkus-app/app/     /work/app/
COPY --from=build /build/target/quarkus-app/quarkus/ /work/quarkus/

EXPOSE 8080

CMD ["java", "-jar", "/work/quarkus-run.jar"]