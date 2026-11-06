# SAMS REST API v1

Base URL: `http://<server>:8080/AttendanceManagementSystem/api/v1`
Authentication: HTTP header `X-API-KEY: <key>` (or `Authorization: Bearer <key>`).
All responses are JSON: `{"success": true, "data": ...}` or `{"success": false, "error": "..."}`.
Rate limit: 120 requests / minute / key (HTTP 429 afterwards).

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET  | `/health` | Public heartbeat. |
| GET  | `/courses` | All courses with teacher and enrolled count. |
| GET  | `/courses/{code}/students` | Students enrolled in a course. |
| GET  | `/attendance?course=CS501&roll=CS2026001&from=2026-10-01&to=2026-10-31&limit=200` | Search attendance rows (all filters optional). |
| GET  | `/students/{roll}/summary` | Attendance percentage of a student in every course. |
| POST | `/attendance/mark` | Mark attendance (single scan or bulk). |

## POST /attendance/mark
Single scan (date defaults to today):
```json
{ "course": "CS501", "roll": "CS2026001", "status": "PRESENT" }
```
Bulk:
```json
{ "course": "CS501", "date": "2026-11-04",
  "records": [ {"roll":"CS2026001","status":"PRESENT"},
               {"roll":"CS2026002","status":"LATE"},
               {"roll":"CS2026003","status":"ABSENT"} ] }
```
`status` is `PRESENT`, `LATE` or `ABSENT`. Calling it again for the same student/date **updates** the record.

Response codes: `200` all saved, `207` some rejected (listed in `data.rejected`), `422` nothing saved,
`400` bad JSON / date, `401` missing or invalid key, `404` unknown course, `429` rate limit.
