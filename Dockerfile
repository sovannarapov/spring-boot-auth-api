# Use an official OpenJDK runtime as a parent image
FROM openjdk:21-jdk

# Set the working directory in the container
WORKDIR /app

# Copy the packaged jar file into the container
COPY target/spring-boot-auth.jar /app/spring-boot-auth.jar

# Make port 9090 available to the world outside this container
EXPOSE 9090

# Run the jar file
ENTRYPOINT ["java", "-jar", "/app/spring-boot-auth.jar"]
