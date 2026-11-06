<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ include file="../common/header.jspf" %>
<p><a class="link" href="${ctx}/teacher/dashboard">&larr; My courses</a></p>
<h1 class="page-title">Attendance report &ndash; <c:out value="${course.code}"/></h1>
<p class="muted"><c:out value="${course.name}"/> &middot; ${course.classesHeld} classes held &middot;
  <c:choose><c:when test="${lowCount > 0}"><span class="text-low">${lowCount} student(s) below ${threshold}%</span></c:when>
  <c:otherwise>nobody below ${threshold}%</c:otherwise></c:choose></p>

<div class="card">
  <div class="toolbar">
    <input class="search" type="search" placeholder="Search student..." data-filter="#repTable">
    <a class="btn btn-sm" href="${ctx}/teacher/export?courseId=${course.id}">Download CSV</a>
    <a class="btn btn-sm btn-primary" href="${ctx}/teacher/attendance?courseId=${course.id}">Take attendance</a>
    <button class="btn btn-sm" type="button" data-print>Print</button>
  </div>
  <div class="table-wrap">
  <table class="table" id="repTable">
    <thead><tr><th>Roll no</th><th>Student</th><th class="num">Classes</th><th class="num">Present</th><th class="num">Late</th><th class="num">Absent</th><th>Attendance</th></tr></thead>
    <tbody>
    <c:forEach items="${rows}" var="s">
      <fmt:formatNumber var="pw" value="${s.percentage}" maxFractionDigits="0" groupingUsed="false"/>
      <tr>
        <td><c:out value="${s.rollNo}"/></td>
        <td><c:out value="${s.studentName}"/></td>
        <td class="num">${s.total}</td><td class="num">${s.present}</td><td class="num">${s.late}</td><td class="num">${s.absent}</td>
        <td><div class="bar"><div class="bar-fill ${s.level} w-${pw}"></div></div>
            <span class="pct ${s.low ? 'text-low' : ''}"><fmt:formatNumber value="${s.percentage}" maxFractionDigits="1"/>%</span></td>
      </tr>
    </c:forEach>
    </tbody>
  </table>
  </div>
</div>
<%@ include file="../common/footer.jspf" %>
