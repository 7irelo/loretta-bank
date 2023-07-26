# Configuration Guide

Loretta Bank uses a centralized configuration server to manage service configurations. This guide explains how to set up and manage configurations.

## Config Server Setup

1. Clone the configuration repository.
2. Add configurations for each service under their respective directories.
3. Push changes to the Git repository.
4. The Config Server will automatically pull updates.

## Configuration Structure

- `application.yml`: General configurations.
- `bootstrap.yml`: Config server and profiles settings.
- `logback-spring.xml`: Logging configurations.
