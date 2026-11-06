<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="../common/header.jspf" %>
<h1 class="page-title">Admin dashboard</h1>

<section class="tiles">
  <div class="tile"><span class="tile-num"><c:out value="${studentCount}"/></span><span class="tile-lbl">Active students</span></div>
  <div class="tile"><span class="tile-num"><c:out value="${teacherCount}"/></span><span class="tile-lbl">Teachers</span></div>
  <div class="tile"><span class="tile-num"><c:out value="${courseCount}"/></span><span class="tile-lbl">Courses</span></div>
  <div class="tile tile-accent"><span class="tile-num"><fmt:formatNumber value="${overall}" maxFractionDigits="1"/>%</span><span class="tile-lbl">Overall attendance</span></div>
</section>

<section class="split">
  <div class="card">
    <h2>Attendance trend <small class="muted">(last 14 days, % present)</small></h2>
    <c:choose>
      <c:when test="${empty trend}"><p class="muted">No attendance has been recorded yet.</p></c:when>
      <c:otherwise>
        <div class="chart">
          <c:forEach items="${trend}" var="d">
            <div class="col" title="${d.present} of ${d.total} present">
              <span class="val">${d.percent}</span>
              <div class="colbar h-${d.percent}"></div>
              <span class="lbl"><fmt:formatDate value="${d.date}" pattern="dd/MM"/></span>
            </div>
          </c:forEach>
        </div>
      </c:otherwise>
    </c:choose>
  </div>

  <div class="card">
    <h2>Quick actions</h2>
    <div class="actions">
      <a class="btn btn-primary" href="${ctx}/admin/users">Add user</a>
      <a class="btn btn-primary" href="${ctx}/admin/courses">Manage courses</a>
      <a class="btn" href="${ctx}/admin/reports">View reports</a>
      <a class="btn" href="${ctx}/admin/apikeys">Device API keys</a>
    </div>
    <p class="muted small"><c:out value="${recordCount}"/> attendance records stored in total.</p>
  </div>
</section>

<div class="card">
  <h2>Attendance shortage <small class="muted">(below ${threshold}%)</small>
    <a class="link right" href="${ctx}/admin/reports">See all</a></h2>
  <c:choose>
    <c:when test="${empty defaulters}"><p class="muted">Great news - nobody is below the minimum.</p></c:when>
    <c:otherwise>
      <table class="table">
        <thead><tr><th>Roll no</th><th>Student</th><th>Course</th><th class="num">Classes</th><th class="num">Attended</th><th>%</th></tr></thead>
        <tbody>
        <c:forEach items="${defaulters}" var="s" end="7">
          <tr>
            <td><c:out value="${s.rollNo}"/></td>
            <td><c:out value="${s.studentName}"/></td>
            <td><c:out value="${s.courseCode}"/></td>
            <td class="num">${s.total}</td>
            <td class="num">${s.present + s.late}</td>
            <td><span class="badge badge-low"><fmt:formatNumber value="${s.percentage}" maxFractionDigits="1"/>%</span></td>
          </tr>
        </c:forEach>
        </tbody>
      </table>
    </c:otherwise>
  </c:choose>
</div>
<%@ include file="../common/footer.jspf" %>
