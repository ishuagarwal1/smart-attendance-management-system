# Smart Attendance Management System (SAMS)

A complete, real-life **student attendance management web application** written in
**Java (Servlets + JSP + JDBC)** with a **MySQL** database and a **REST API protected by API keys**.

| Layer      | Technology                                                                 |
|------------|----------------------------------------------------------------------------|
| Front-end  | JSP 2.3, JSTL 1.2, HTML5, CSS3 (responsive), vanilla JavaScript            |
| Back-end   | Java 11+, Servlet 4.0 (filters, listeners, servlets), JDBC, DAO pattern    |
| Database   | MySQL 8 / MariaDB 10 (7 tables + 1 view, foreign keys, unique keys)        |
| Security   | PBKDF2 password hashing, CSRF tokens, role-based access, API keys (SHA-256), CSP headers, login lock-out |
| Server     | Apache Tomcat **9.x**                                                      |

## Features
* **Three roles** - Admin, Teacher, Student (role based menus and URL protection)
* **Teacher**: take attendance in one click (Present / Late / Absent + remark), edit past days, course report, CSV export
* **Student**: subject-wise percentage, shortage warning, "classes needed to reach 75 %", date-wise history
* **Admin**: manage users, courses, teacher assignment, enrollments, defaulter report, audit log, dashboard with 14-day trend
* **REST API + API keys** so RFID / biometric devices or mobile apps can mark attendance automatically
* Safe by design: prepared statements, transactions, UNIQUE (course, student, date) so no duplicate marking

## 1. Requirements
1. **JDK 11 or newer** (to build) - the JRE is enough to run Tomcat
2. **Apache Tomcat 9.x** (NOT 10/11 - they use the `jakarta.*` namespace)
3. **MySQL 8** (or MariaDB 10.5+)
4. **Apache Maven 3.8+** (or import the project into Eclipse / IntelliJ / NetBeans as a Maven project)

## 2. Set up the database
```bash
mysql -u root -p < database/schema.sql          # creates sams_db, tables, view and user sams_user
mysql -u root -p < database/sample_data.sql     # optional demo data (25 users, ~2400 attendance rows)
```
If you change the DB user / password, edit `src/main/resources/db.properties`.

## 3. Build and deploy

### Option A - prebuilt WAR (no Maven needed, works with XAMPP / MariaDB)
`dist/AttendanceManagementSystem.war` already contains all libraries and the MariaDB JDBC driver.
Import `database/schema.sql` (+ `sample_data.sql`) with phpMyAdmin or the `mysql` client, copy the WAR into Tomcat 9's
`webapps` folder and start Tomcat. To change the database password without touching the WAR, create
`<TOMCAT_HOME>/conf/sams.properties` containing e.g. `db.password=MyPassword`.

### Option B - build from source with Maven (MySQL 8)
```bash
mvn clean package                               # creates target/AttendanceManagementSystem.war
copy target\AttendanceManagementSystem.war  <TOMCAT_HOME>\webapps\      (Windows)
cp   target/AttendanceManagementSystem.war  $TOMCAT_HOME/webapps/       (Linux / macOS)
<TOMCAT_HOME>/bin/startup.bat | startup.sh
```
Open **http://localhost:8080/AttendanceManagementSystem/**

### Demo accounts (from `sample_data.sql`)
| Role    | Username            | Password      |
|---------|---------------------|---------------|
| Admin   | `admin`             | `Admin@123`   |
| Teacher | `teacher1` ... `teacher4` | `Teacher@123` |
| Student | `student01` ... `student20` | `Student@123` |

> Change these passwords before using the system for real (menu: *Password*).

## 4. REST API (for devices and integrations)
1. Sign in as admin -> **API keys** -> *Generate*. Copy the key (shown only once).
2. Call the API with the header `X-API-KEY: <your key>`.

```bash
# heartbeat (no key needed)
curl http://localhost:8080/AttendanceManagementSystem/api/v1/health

# list courses
curl -H "X-API-KEY: sams_xxx" http://localhost:8080/AttendanceManagementSystem/api/v1/courses

# an RFID reader marks one student present today
curl -X POST -H "X-API-KEY: sams_xxx" -H "Content-Type: application/json" \
     -d '{"course":"CS501","roll":"CS2026001","status":"PRESENT"}' \
     http://localhost:8080/AttendanceManagementSystem/api/v1/attendance/mark
```
Full reference: [`docs/API.md`](docs/API.md)

## 5. Project structure
```
AttendanceManagementSystem
|-- pom.xml
|-- database/            schema.sql, sample_data.sql
|-- dist/                AttendanceManagementSystem.war  (prebuilt, MariaDB driver)
|-- docs/                API.md, project report (docx/pdf), diagrams/, screenshots/
|-- tests/               smoke_test.sh  (curl based end-to-end test)
`-- src/main
    |-- java/com/sams
    |   |-- model/       User, StudentInfo, Course, AttendanceRecord, AttendanceSummary, ApiKey ...
    |   |-- dao/         UserDAO, StudentDAO, CourseDAO, AttendanceDAO, ApiKeyDAO, AuditDAO
    |   |-- servlet/     Login, Logout, Admin*, Teacher*, Student*, Account, ApiServlet
    |   |-- filter/      SecurityFilter (CSRF + headers), AuthFilter (roles), ApiKeyFilter (+rate limit)
    |   |-- listener/    AppContextListener (starts / stops the connection pool)
    |   `-- util/        DBUtil (pool), PasswordUtil, ApiKeyUtil, JsonUtil, WebUtil, Config
    |-- resources/       db.properties
    `-- webapp/          WEB-INF/web.xml, WEB-INF/views/**.jsp, css/, js/, images/
```

## 6. Troubleshooting
| Problem | Fix |
|---------|-----|
| `HTTP 404` after deploy | Check the URL context path equals the WAR name; look at `logs/catalina.out` |
| `JDBC driver not found` | Run `mvn clean package` again so the driver is inside `WEB-INF/lib` |
| `Access denied for user 'sams_user'` | Re-run `schema.sql` or fix user / password in `db.properties` |
| Page shows `javax.servlet` errors on Tomcat 10+ | Use Tomcat 9.x (this project uses the `javax.*` API) |
| Time zone error from MySQL | Edit `serverTimezone=` in the JDBC URL in `db.properties` |
