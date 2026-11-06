<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ include file="../common/header.jspf" %>
<h1 class="page-title">My attendance</h1>
<p class="muted"><c:out value="${profile.fullName}"/> &middot; Roll no <strong><c:out value="${profile.rollNo}"/></strong> &middot;
  <c:out value="${profile.department}"/>, semester ${profile.semester}</p>

<section class="tiles">
  <div class="tile tile-accent"><span class="tile-num"><fmt:formatNumber value="${overall}" maxFractionDigits="1"/>%</span><span class="tile-lbl">Overall attendance</span></div>
  <div class="tile"><span class="tile-num">${rows.size()}</span><span class="tile-lbl">Subjects</span></div>
  <div class="tile ${lowCount > 0 ? 'tile-warn' : ''}"><span class="tile-num">${lowCount}</span><span class="tile-lbl">Subjects with shortage</span></div>
</section>

<c:if test="${lowCount > 0}">
  <div class="alert alert-error">Warning: your attendance is below ${threshold}% in ${lowCount} subject(s). See the details below to know how many classes you must attend.</div>
</c:if>

<div class="card">
  <h2>Subject-wise attendance</h2>
  <c:if test="${empty rows}"><p class="muted">You are not enrolled in any course yet.</p></c:if>
  <div class="table-wrap">
  <table class="table">
    <thead><tr><th>Course</th><th class="num">Classes</th><th class="num">Present</th><th class="num">Late</th><th class="num">Absent</th><th>Attendance</th><th></th></tr></thead>
    <tbody>
    <c:forEach items="${rows}" var="s">
      <fmt:formatNumber var="pw" value="${s.percentage}" maxFractionDigits="0" groupingUsed="false"/>
      <tr>
        <td><strong><c:out value="${s.courseCode}"/></strong><br><span class="muted small"><c:out value="${s.courseName}"/></span></td>
        <td class="num">${s.total}</td><td class="num">${s.present}</td><td class="num">${s.late}</td><td class="num">${s.absent}</td>
        <td class="wide">
          <div class="bar"><div class="bar-fill ${s.level} w-${pw}"></div></div>
          <span class="pct ${s.low ? 'text-low' : ''}"><fmt:formatNumber value="${s.percentage}" maxFractionDigits="1"/>%</span>
          <c:if test="${s.low}"><span class="small text-low"> &mdash; attend the next ${s.classesNeeded} classes in a row to reach ${threshold}%</span></c:if>
        </td>
        <td><a class="btn btn-sm" href="${ctx}/student/history?courseId=${s.courseId}">Details</a></td>
      </tr>
    </c:forEach>
    </tbody>
  </table>
  </div>
</div>
<%@ include file="../common/footer.jspf" %>
