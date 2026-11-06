#!/bin/bash
# End-to-end smoke test (curl). Needs the app on http://localhost:8080/sams and the demo data loaded.
# Uses the local "mysql -uroot" client only to verify rows in the database.
B=http://localhost:8080/sams
pass=0; fail=0
chk(){ if [ "$2" = "$3" ]; then pass=$((pass+1)); echo "PASS  $1"; else fail=$((fail+1)); echo "FAIL  $1 (got '$2', want '$3')"; fi; }
tok(){ curl -s -b $1 -c $1 $B/login | grep -o 'name="_csrf" value="[^"]*"' | head -1 | sed 's/.*value="//;s/"//'; }
login(){ J=/tmp/j_$1; rm -f $J; T=$(tok $J); curl -s -o /dev/null -w "%{http_code}" -b $J -c $J -d "_csrf=$T&username=$1&password=$2" $B/login; }
csrf(){ curl -s -b $1 -c $1 $B$2 | grep -o 'name="_csrf" value="[^"]*"' | head -1 | sed 's/.*value="//;s/"//'; }
code(){ curl -s -o /dev/null -w "%{http_code}" -b $1 -c $1 "$B$2"; }

chk "login page"            "$(code /tmp/j0 /login)" 200
chk "POST without CSRF"     "$(curl -s -o /dev/null -w '%{http_code}' -d 'username=admin&password=x' $B/login)" 403
chk "bad password"          "$(login admin wrong | tr -d '\n')" 200
chk "admin login redirect"  "$(login admin 'Admin@123')" 302
for p in /admin/dashboard /admin/users /admin/courses /admin/reports /admin/apikeys "/admin/enrollments?courseId=1"; do chk "admin GET $p" "$(code /tmp/j_admin $p)" 200; done
chk "admin blocked from teacher area" "$(code /tmp/j_admin /teacher/dashboard)" 403
chk "no session -> redirect"          "$(curl -s -o /dev/null -w '%{http_code}' $B/admin/dashboard)" 302

chk "teacher login"         "$(login teacher1 'Teacher@123')" 302
for p in /teacher/dashboard "/teacher/attendance?courseId=1" "/teacher/report?courseId=1" "/teacher/export?courseId=1"; do chk "teacher GET $p" "$(code /tmp/j_teacher1 $p)" 200; done
chk "teacher other's course = 403"  "$(code /tmp/j_teacher1 '/teacher/attendance?courseId=2')" 403
chk "teacher -> admin = 403"        "$(code /tmp/j_teacher1 /admin/users)" 403

# save attendance for course 1 (teacher1): everyone present except student01 absent
T=$(csrf /tmp/j_teacher1 "/teacher/attendance?courseId=1")
D=$(date +%F)
DATA="_csrf=$T&courseId=1&date=$D"
for i in $(seq 6 25); do S=PRESENT; [ $i = 6 ] && S=ABSENT; DATA="$DATA&status_$i=$S&remark_$i="; done
chk "teacher saves attendance" "$(curl -s -o /dev/null -w '%{http_code}' -b /tmp/j_teacher1 -c /tmp/j_teacher1 -d "$DATA" $B/teacher/attendance)" 302
chk "DB has today's rows"      "$(mysql -N -uroot sams_db -e "select count(*) from attendance where course_id=1 and att_date=CURDATE()")" 20
# re-save (edit) must not duplicate
curl -s -o /dev/null -b /tmp/j_teacher1 -c /tmp/j_teacher1 -d "$DATA" $B/teacher/attendance
chk "re-save does not duplicate" "$(mysql -N -uroot sams_db -e "select count(*) from attendance where course_id=1 and att_date=CURDATE()")" 20
chk "student01 absent today"   "$(mysql -N -uroot sams_db -e "select status from attendance where course_id=1 and student_id=6 and att_date=CURDATE()")" ABSENT
chk "CSV export has header"    "$(curl -s -b /tmp/j_teacher1 "$B/teacher/export?courseId=1" | head -1 | tr -d '\r' | sed 's/^\xEF\xBB\xBF//')" "Roll No,Student,Total Classes,Present,Late,Absent,Percentage,Status"

chk "student login"         "$(login student01 'Student@123')" 302
for p in /student/dashboard "/student/history?courseId=1"; do chk "student GET $p" "$(code /tmp/j_student01 $p)" 200; done
chk "student -> teacher = 403" "$(code /tmp/j_student01 /teacher/dashboard)" 403
chk "student sees own %"       "$(curl -s -b /tmp/j_student01 $B/student/dashboard | grep -c 'CS501')" 1

# admin creates a key via the UI, then use the API
T=$(csrf /tmp/j_admin /admin/apikeys)
curl -s -o /dev/null -b /tmp/j_admin -c /tmp/j_admin -d "_csrf=$T&action=create&label=Test+RFID" $B/admin/apikeys
KEY=$(curl -s -b /tmp/j_admin -c /tmp/j_admin $B/admin/apikeys | grep -o 'id="newKey">[^<]*' | sed 's/.*>//')
echo "key=${KEY:0:12}..."
A="$B/api/v1"
chk "API health (public)"      "$(curl -s -o /dev/null -w '%{http_code}' $A/health)" 200
chk "API no key = 401"         "$(curl -s -o /dev/null -w '%{http_code}' $A/courses)" 401
chk "API bad key = 401"        "$(curl -s -o /dev/null -w '%{http_code}' -H 'X-API-KEY: sams_bad' $A/courses)" 401
chk "API courses"              "$(curl -s -o /dev/null -w '%{http_code}' -H "X-API-KEY: $KEY" $A/courses)" 200
chk "API student summary"      "$(curl -s -H "X-API-KEY: $KEY" $A/students/CS2026001/summary | grep -c '"courseCode":"CS501"')" 1
chk "API mark single scan"     "$(curl -s -o /dev/null -w '%{http_code}' -H "X-API-KEY: $KEY" -H 'Content-Type: application/json' -d '{"course":"CS502","roll":"CS2026002","status":"present"}' $A/attendance/mark)" 200
chk "API stored via=API"       "$(mysql -N -uroot sams_db -e "select marked_via from attendance where course_id=2 and student_id=7 and att_date=CURDATE()")" API
echo "bulk partial result:"; curl -s -w ' [%{http_code}]\n' -H "X-API-KEY: $KEY" -H 'Content-Type: application/json' -d '{"course":"CS502","date":"'$D'","records":[{"roll":"CS2026003","status":"LATE"},{"roll":"NOPE","status":"PRESENT"}]}' $A/attendance/mark
chk "API bad JSON = 400"       "$(curl -s -o /dev/null -w '%{http_code}' -H "X-API-KEY: $KEY" -d '{bad' $A/attendance/mark)" 400
chk "API future date = 400"    "$(curl -s -o /dev/null -w '%{http_code}' -H "X-API-KEY: $KEY" -d '{"course":"CS502","date":"2099-01-01","roll":"CS2026002","status":"PRESENT"}' $A/attendance/mark)" 400
# revoke key
ID=$(mysql -N -uroot sams_db -e "select id from api_keys order by id desc limit 1")
T=$(csrf /tmp/j_admin /admin/apikeys)
curl -s -o /dev/null -b /tmp/j_admin -c /tmp/j_admin -d "_csrf=$T&action=toggle&id=$ID&active=0" $B/admin/apikeys
chk "revoked key = 401"        "$(curl -s -o /dev/null -w '%{http_code}' -H "X-API-KEY: $KEY" $A/courses)" 401
# admin creates a student, duplicate username rejected
T=$(csrf /tmp/j_admin /admin/users)
curl -s -o /dev/null -b /tmp/j_admin -c /tmp/j_admin -d "_csrf=$T&action=create&role=STUDENT&fullName=Test+Student&username=student99&email=s99@sams.edu&password=Passw0rd99&rollNo=CS2026099&department=Computer+Science&semester=5" $B/admin/users
chk "student99 created"        "$(mysql -N -uroot sams_db -e "select count(*) from students s join users u on u.id=s.user_id where u.username='student99'")" 1
chk "password stored hashed"   "$(mysql -N -uroot sams_db -e "select left(password_hash,7) from users where username='student99'")" "pbkdf2$"
echo; echo "PASS=$pass FAIL=$fail"
grep -i -E "exception|error" /tmp/tomcat.log | grep -v -E "^INFO" | head -5
