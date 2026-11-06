<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="../common/header.jspf" %>
<p><a class="link" href="${ctx}/teacher/dashboard">&larr; My courses</a></p>
<h1 class="page-title">Mark attendance &ndash; <c:out value="${course.code}"/></h1>
<p class="muted"><c:out value="${course.name}"/> &middot; ${students.size()} students</p>

<div class="card">
  <form method="get" action="${ctx}/teacher/attendance" class="inline-form">
    <input type="hidden" name="courseId" value="${course.id}">
    <label for="date">Date</label>
    <input id="date" type="date" name="date" value="${date}" max="${today}" data-autosubmit>
    <c:if test="${not empty existing}"><span class="badge badge-ok">Already marked &ndash; editing</span></c:if>
  </form>
</div>

<c:choose>
<c:when test="${empty students}"><div class="card"><p class="muted">No students are enrolled in this course.</p></div></c:when>
<c:otherwise>
<form method="post" action="${ctx}/teacher/attendance" id="attForm">
  <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
  <input type="hidden" name="courseId" value="${course.id}">
  <input type="hidden" name="date" value="${date}">
  <div class="card">
    <div class="toolbar">
      <button class="btn btn-sm" type="button" data-markall="PRESENT">Mark all present</button>
      <button class="btn btn-sm" type="button" data-markall="ABSENT">Mark all absent</button>
      <span class="counter" id="counter">
        <span class="badge badge-good">Present <b id="cP">0</b></span>
        <span class="badge badge-ok">Late <b id="cL">0</b></span>
        <span class="badge badge-low">Absent <b id="cA">0</b></span>
      </span>
    </div>
    <div class="table-wrap">
    <table class="table att-table">
      <thead><tr><th>Roll no</th><th>Student</th><th>Status</th><th>Remark (optional)</th></tr></thead>
      <tbody>
      <c:forEach items="${students}" var="s">
        <c:set var="cur" value="${empty existing[s.userId] ? 'PRESENT' : existing[s.userId].status}"/>
        <tr>
          <td><c:out value="${s.rollNo}"/></td>
          <td><c:out value="${s.fullName}"/></td>
          <td>
            <div class="seg">
              <input type="radio" id="p${s.userId}" name="status_${s.userId}" value="PRESENT" ${cur == 'PRESENT' ? 'checked' : ''}><label class="seg-p" for="p${s.userId}">P</label>
              <input type="radio" id="l${s.userId}" name="status_${s.userId}" value="LATE" ${cur == 'LATE' ? 'checked' : ''}><label class="seg-l" for="l${s.userId}">L</label>
              <input type="radio" id="a${s.userId}" name="status_${s.userId}" value="ABSENT" ${cur == 'ABSENT' ? 'checked' : ''}><label class="seg-a" for="a${s.userId}">A</label>
            </div>
          </td>
          <td><input class="remark" name="remark_${s.userId}" maxlength="200" value="<c:out value='${existing[s.userId].remarks}'/>" placeholder="e.g. medical leave"></td>
        </tr>
      </c:forEach>
      </tbody>
    </table>
    </div>
    <div class="form-actions"><button class="btn btn-primary btn-lg" type="submit">Save attendance</button></div>
  </div>
</form>
</c:otherwise>
</c:choose>
<%@ include file="../common/footer.jspf" %>
