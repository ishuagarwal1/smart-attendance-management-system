<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ include file="../common/header.jspf" %>
<p><a class="link" href="${ctx}/student/dashboard">&larr; My attendance</a></p>
<h1 class="page-title"><c:out value="${course.code}"/> &ndash; <c:out value="${course.name}"/></h1>
<c:if test="${not empty summary}">
  <p class="muted">Attendance: <strong><fmt:formatNumber value="${summary.percentage}" maxFractionDigits="1"/>%</strong>
    (${summary.present} present, ${summary.late} late, ${summary.absent} absent out of ${summary.total} classes)</p>
</c:if>
<div class="card">
  <c:if test="${empty records}"><p class="muted">No attendance has been recorded for this course yet.</p></c:if>
  <c:if test="${not empty records}">
  <table class="table">
    <thead><tr><th>Date</th><th>Day</th><th>Status</th><th>Remark</th><th>Marked by</th></tr></thead>
    <tbody>
    <c:forEach items="${records}" var="r">
      <tr>
        <td><fmt:formatDate value="${r.date}" pattern="dd MMM yyyy"/></td>
        <td><fmt:formatDate value="${r.date}" pattern="EEEE"/></td>
        <td><span class="badge ${r.status == 'PRESENT' ? 'badge-good' : r.status == 'LATE' ? 'badge-ok' : 'badge-low'}"><c:out value="${r.status}"/></span></td>
        <td><c:out value="${r.remarks}"/></td>
        <td><c:out value="${empty r.markedByName ? (r.markedVia == 'API' ? 'Device (API)' : '-') : r.markedByName}"/></td>
      </tr>
    </c:forEach>
    </tbody>
  </table>
  </c:if>
</div>
<%@ include file="../common/footer.jspf" %>
