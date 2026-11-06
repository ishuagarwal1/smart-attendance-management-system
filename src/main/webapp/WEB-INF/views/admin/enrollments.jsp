<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="../common/header.jspf" %>
<p><a class="link" href="${ctx}/admin/courses">&larr; All courses</a></p>
<h1 class="page-title"><c:out value="${course.code}"/> &ndash; <c:out value="${course.name}"/></h1>
<p class="muted">Teacher: <strong><c:out value="${empty course.teacherName ? 'not assigned' : course.teacherName}"/></strong></p>

<section class="split">
  <div class="card">
    <h2>Enrolled students <small class="muted">(${enrolled.size()})</small></h2>
    <c:if test="${empty enrolled}"><p class="muted">Nobody is enrolled yet.</p></c:if>
    <table class="table">
      <tbody>
      <c:forEach items="${enrolled}" var="s">
        <tr>
          <td><c:out value="${s.rollNo}"/></td>
          <td><c:out value="${s.fullName}"/></td>
          <td class="num">
            <form method="post" action="${ctx}/admin/enrollments" class="inline" data-confirm="Remove this student from the course?">
              <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
              <input type="hidden" name="action" value="remove">
              <input type="hidden" name="courseId" value="${course.id}">
              <input type="hidden" name="studentId" value="${s.userId}">
              <button class="btn btn-sm btn-danger" type="submit">Remove</button>
            </form>
          </td>
        </tr>
      </c:forEach>
      </tbody>
    </table>
  </div>

  <div class="card">
    <h2>Add students</h2>
    <c:choose>
      <c:when test="${empty available}"><p class="muted">All active students are already enrolled.</p></c:when>
      <c:otherwise>
        <form method="post" action="${ctx}/admin/enrollments">
          <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
          <input type="hidden" name="action" value="enroll">
          <input type="hidden" name="courseId" value="${course.id}">
          <div class="checklist">
            <c:forEach items="${available}" var="s">
              <label class="check"><input type="checkbox" name="studentId" value="${s.userId}">
                <c:out value="${s.rollNo}"/> &ndash; <c:out value="${s.fullName}"/></label>
            </c:forEach>
          </div>
          <button class="btn btn-primary" type="submit">Enroll selected</button>
        </form>
      </c:otherwise>
    </c:choose>
  </div>
</section>
<%@ include file="../common/footer.jspf" %>
