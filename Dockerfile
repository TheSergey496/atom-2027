FROM eclipse-temurin:25-jdk-alpine as rest_build
WORKDIR /workspace/app

COPY backend/mvnw .
COPY backend/.mvn .mvn
COPY backend/pom.xml .
COPY backend/src src

RUN chmod +x ./mvnw
RUN ./mvnw install -DskipTests
RUN mkdir -p target/dependency && (cd target/dependency; jar -xf ../*.jar)

FROM node:latest AS front_build
WORKDIR /dist/src/app
RUN npm cache clean --force
COPY frontend/ .
COPY --from=rest_build /workspace/frontend/src/app/gen src/app/gen
RUN npm install --force --legacy-peer-deps
RUN npm run build --prod

FROM eclipse-temurin:25-jdk-alpine as backend
ARG DEPENDENCY=/workspace/app/target/dependency
COPY --from=rest_build ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=rest_build ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=rest_build ${DEPENDENCY}/BOOT-INF/classes /app
RUN apk --no-cache add msttcorefonts-installer fontconfig && update-ms-fonts && fc-cache -f
ENTRYPOINT ["java","-cp","app:app/lib/*","org.prolegacy.atom2027.Atom2027Application"]
EXPOSE 8085

FROM nginx:latest as frontend
RUN rm -rf /usr/share/nginx/html
COPY --from=front_build /dist/src/app/dist/frontend /usr/share/nginx/html
COPY --from=front_build /dist/src/app/nginx.conf  /etc/nginx/conf.d/default.conf
COPY --from=front_build /dist/src/app/front-run.sh /front-run.sh
ENTRYPOINT ["/front-run.sh"]
EXPOSE 80