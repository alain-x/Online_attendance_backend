# Attendance Management System - Backend

Spring Boot backend for the Online Attendance Management System.

## Quick Start

1. **Prerequisites:**
   - Java 17+
   - MySQL 8.0+
   - Maven 3.6+

2. **Database Setup:**
   ```sql
   CREATE DATABASE attendance_db;
   ```

3. **Configuration:**
   Edit `src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/attendance_db
   spring.datasource.username=root
   spring.datasource.password=your_password
   ```

4. **Run:**
   ```bash
   ./mvnw spring-boot:run
   ```

## Environment Variables

Set these environment variables for production:

- `DB_HOST` - Database host
- `DB_PORT` - Database port  
- `DB_NAME` - Database name
- `DB_USER` - Database username
- `DB_PASSWORD` - Database password
- `JWT_SECRET` - JWT signing secret (minimum 256 bits)
- `JWT_EXP_MINUTES` - JWT expiration time
- `BOOTSTRAP_ADMIN_USERNAME` - First admin username
- `BOOTSTRAP_ADMIN_PASSWORD` - First admin password

## API Documentation

The API runs on `http://localhost:8080` by default.

### Authentication Required
Most endpoints require JWT authentication. Include the token in the Authorization header:
```
Authorization: Bearer <token>
```

## Security Notes

⚠️ **IMPORTANT:** 
- Change default passwords in production
- Use strong JWT secrets (minimum 256 bits)
- Configure HTTPS in production
- Regularly update dependencies
- Use environment variables for sensitive data
