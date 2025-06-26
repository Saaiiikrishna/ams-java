# Docker Deployment Guide

This guide explains how to deploy the Attendance Management System using Docker containers.

## Architecture Overview

The system consists of the following services:

### Backend Services
- **ams-backend**: Spring Boot application with gRPC services (Port: 8080, 9090)
- **postgres**: PostgreSQL database (Port: 5432)
- **redis**: Redis cache (Production only, Port: 6379)

### Frontend Services
- **admin-panel**: React-based admin interface (Port: 3001)
- **entity-dashboard**: React-based entity management (Port: 3002)
- **public-menu**: React-based public menu display (Port: 3003)

### Monitoring Services
- **prometheus**: Metrics collection (Port: 9090)
- **grafana**: Monitoring dashboards (Port: 3000)
- **zipkin**: Distributed tracing (Port: 9411)
- **elasticsearch**: Log storage (Production only)

### Load Balancing
- **nginx-lb**: Load balancer and reverse proxy (Production only, Ports: 80, 443)

## Prerequisites

1. **Docker Desktop**: Install Docker Desktop for Windows
2. **Git**: For cloning the repository
3. **Minimum System Requirements**:
   - RAM: 8GB (16GB recommended for production)
   - CPU: 4 cores
   - Disk: 20GB free space

## Quick Start

### Development Environment

1. **Clone the repository**:
   ```bash
   git clone <repository-url>
   cd ams-java
   ```

2. **Deploy using the script**:
   ```bash
   # Windows
   scripts\deploy-docker.bat dev deploy
   
   # Linux/Mac
   scripts/deploy-docker.sh dev deploy
   ```

3. **Access the applications**:
   - Backend API: http://localhost:8080
   - Admin Panel: http://localhost:3001
   - Entity Dashboard: http://localhost:3002
   - Public Menu: http://localhost:3003
   - Grafana: http://localhost:3000 (admin/admin123)

### Production Environment

1. **Configure environment**:
   ```bash
   # Copy and edit production environment file
   copy .env.prod .env
   # Edit .env with your production values
   ```

2. **Deploy to production**:
   ```bash
   # Windows
   scripts\deploy-docker.bat prod deploy
   
   # Linux/Mac
   scripts/deploy-docker.sh prod deploy
   ```

## Manual Deployment

### Development
```bash
# Build and start development environment
docker-compose up -d --build

# Check status
docker-compose ps

# View logs
docker-compose logs -f ams-backend
```

### Production
```bash
# Build and start production environment
docker-compose -f docker-compose.prod.yml up -d --build

# Check status
docker-compose -f docker-compose.prod.yml ps

# View logs
docker-compose -f docker-compose.prod.yml logs -f ams-backend
```

## Environment Configuration

### Development (.env.dev)
- Uses default passwords
- Debug logging enabled
- Smaller resource limits
- In-memory storage for some services

### Production (.env.prod)
- Requires secure passwords
- Info-level logging
- Higher resource limits
- Persistent storage for all services
- SSL/TLS configuration

## Service Management

### Available Commands

```bash
# Build images only
scripts\deploy-docker.bat dev build

# Start services
scripts\deploy-docker.bat dev start

# Stop services
scripts\deploy-docker.bat dev stop

# Restart services
scripts\deploy-docker.bat dev restart

# Check status
scripts\deploy-docker.bat dev status

# Clean up resources
scripts\deploy-docker.bat dev cleanup
```

### Health Checks

All services include health checks:
- **Backend**: HTTP endpoint `/actuator/health`
- **Frontend**: HTTP endpoint `/`
- **Database**: PostgreSQL connection test
- **Monitoring**: Service-specific health endpoints

### Scaling Services

```bash
# Scale backend instances (production)
docker-compose -f docker-compose.prod.yml up -d --scale ams-backend=3

# Scale frontend instances
docker-compose -f docker-compose.prod.yml up -d --scale admin-panel=2
```

## Monitoring and Observability

### Grafana Dashboards
- Access: http://localhost:3000
- Username: admin
- Password: admin123 (dev) / configured password (prod)

### Prometheus Metrics
- Access: http://localhost:9090
- Monitors all application and system metrics

### Zipkin Tracing
- Access: http://localhost:9411
- Distributed tracing for gRPC and HTTP requests

### Log Management
- Development: Docker logs
- Production: Centralized logging with Elasticsearch

## Troubleshooting

### Common Issues

1. **Port conflicts**:
   ```bash
   # Check what's using the port
   netstat -ano | findstr :8080
   
   # Stop conflicting services or change ports in docker-compose.yml
   ```

2. **Memory issues**:
   ```bash
   # Increase Docker Desktop memory allocation
   # Settings > Resources > Advanced > Memory
   ```

3. **Build failures**:
   ```bash
   # Clean Docker cache
   docker system prune -a
   
   # Rebuild without cache
   docker-compose build --no-cache
   ```

4. **Service startup failures**:
   ```bash
   # Check logs
   docker-compose logs service-name
   
   # Check health status
   docker-compose ps
   ```

### Performance Tuning

1. **Database optimization**:
   - Adjust PostgreSQL settings in `docker/postgres/postgresql.conf`
   - Monitor connection pools

2. **JVM tuning**:
   - Modify `JAVA_OPTS` in docker-compose files
   - Monitor heap usage in Grafana

3. **Network optimization**:
   - Use custom networks for service isolation
   - Configure DNS resolution

## Security Considerations

### Production Security
1. **Change default passwords** in `.env`
2. **Use HTTPS** with proper SSL certificates
3. **Configure firewall** rules
4. **Regular security updates** for base images
5. **Backup strategy** for data volumes

### Network Security
- Services communicate on isolated Docker networks
- Only necessary ports are exposed
- Load balancer handles external traffic

## Backup and Recovery

### Database Backup
```bash
# Create backup
docker exec ams-postgres-prod pg_dump -U postgres attendance_db > backup.sql

# Restore backup
docker exec -i ams-postgres-prod psql -U postgres attendance_db < backup.sql
```

### Volume Backup
```bash
# Backup volumes
docker run --rm -v ams_postgres_data_prod:/data -v $(pwd):/backup alpine tar czf /backup/postgres_backup.tar.gz /data
```

## Support

For issues and questions:
1. Check the logs: `docker-compose logs`
2. Review this documentation
3. Check Docker Desktop status
4. Verify system requirements
