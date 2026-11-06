<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ include file="../common/header.jspf" %>
<h1 class="page-title">Reports &amp; audit log</h1>

<div class="card">
  <h2>Students below
    <form method="get" action="${ctx}/admin/reports" class="inline-form">
      <input type="number" name="below" min="1" max="100" value="${below}" class="tiny"> %
      <button class="btn btn-sm" type="submit">Apply</button>
    </form>
  </h2>
  <p class="muted">${defaulters.size()} record(s) found.</p>
  <c:if test="${not empty defaulters}">
  <input class="search" type="search" placeholder="Filter by roll no, name or course..." data-filter="#defTable">
  <div class="table-wrap">
  <table class="table" id="defTable">
    <thead><tr><th>Roll no</th><th>Student</th><th>Course</th><th class="num">Classes</th><th class="num">Present</th><th class="num">Late</th><th class="num">Absent</th><th>Attendance</th><th class="num">Needs</th></tr></thead>
    <tbody>
    <c:forEach items="${defaulters}" var="s">
      <fmt:formatNumber var="pw" value="${s.percentage}" maxFractionDigits="0" groupingUsed="false"/>
      <tr>
        <td><c:out value="${s.rollNo}"/></td>
        <td><c:out value="${s.studentName}"/></td>
        <td><c:out value="${s.courseCode}"/></td>
        <td class="num">${s.total}</td><td class="num">${s.present}</td><td class="num">${s.late}</td><td class="num">${s.absent}</td>
        <td><div class="bar"><div class="bar-fill ${s.level} w-${pw}"></div></div>
            <span class="pct"><fmt:formatNumber value="${s.percentage}" maxFractionDigits="1"/>%</span></td>
        <td class="num">${s.classesNeeded} classes</td>
      </tr>
    </c:forEach>
    </tbody>
  </table>
  </div>
  </c:if>
</div>

<div class="card">
  <h2>Audit log <small class="muted">(latest 25 events)</small></h2>
  <div class="table-wrap">
  <table class="table small">
    <thead><tr><th>When</th><th>User</th><th>Action</th><th>Details</th><th>IP</th></tr></thead>
    <tbody>
    <c:forEach items="${audit}" var="a">
      <tr>
        <td class="nowrap"><fmt:formatDate value="${a.createdAt}" pattern="dd MMM HH:mm:ss"/></td>
        <td><c:out value="${empty a.userName ? '-' : a.userName}"/></td>
        <td><span class="badge badge-role"><c:out value="${a.action}"/></span></td>
        <td><c:out value="${a.details}"/></td>
        <td><c:out value="${a.ip}"/></td>
      </tr>
    </c:forEach>
    </tbody>
  </table>
  </div>
</div>
<%@ include file="../common/footer.jspf" %>
