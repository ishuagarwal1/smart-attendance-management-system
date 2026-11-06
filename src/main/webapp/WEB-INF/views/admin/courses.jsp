<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ include file="../common/header.jspf" %>
<h1 class="page-title">Courses</h1>

<div class="card">
  <h2>Add a course</h2>
  <form method="post" action="${ctx}/admin/courses" class="form grid2">
    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
    <input type="hidden" name="action" value="create">
    <div><label for="code">Course code</label><input id="code" name="code" required maxlength="20" placeholder="CS505"></div>
    <div><label for="name">Course name</label><input id="name" name="name" required maxlength="120"></div>
    <div><label for="department">Department</label><input id="department" name="department" value="Computer Science" required></div>
    <div><label for="semester">Semester</label><input id="semester" name="semester" type="number" min="1" max="12" value="5"></div>
    <div><label for="credits">Credits</label><input id="credits" name="credits" type="number" min="1" max="10" value="3"></div>
    <div><label for="teacherId">Teacher</label>
      <select id="teacherId" name="teacherId">
        <option value="0">-- not assigned --</option>
        <c:forEach items="${teachers}" var="t"><option value="${t.id}"><c:out value="${t.fullName}"/></option></c:forEach>
      </select></div>
    <div class="form-actions"><button class="btn btn-primary" type="submit">Create course</button></div>
  </form>
</div>

<div class="card">
  <h2>All courses</h2>
  <div class="table-wrap">
  <table class="table">
    <thead><tr><th>Code</th><th>Course</th><th>Sem</th><th>Teacher</th><th class="num">Students</th><th class="num">Classes held</th><th class="num">Avg %</th><th></th></tr></thead>
    <tbody>
    <c:forEach items="${courses}" var="c">
      <tr>
        <td><strong><c:out value="${c.code}"/></strong></td>
        <td><c:out value="${c.name}"/></td>
        <td>${c.semester}</td>
        <td>
          <form method="post" action="${ctx}/admin/courses" class="inline-form">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <input type="hidden" name="action" value="assign">
            <input type="hidden" name="courseId" value="${c.id}">
            <select name="teacherId" data-autosubmit>
              <option value="0">-- none --</option>
              <c:forEach items="${teachers}" var="t"><option value="${t.id}" ${t.id == c.teacherId ? 'selected' : ''}><c:out value="${t.fullName}"/></option></c:forEach>
            </select>
          </form>
        </td>
        <td class="num">${c.enrolledCount}</td>
        <td class="num">${c.classesHeld}</td>
        <td class="num"><c:if test="${c.classesHeld > 0}"><fmt:formatNumber value="${c.avgPercentage}" maxFractionDigits="1"/>%</c:if></td>
        <td class="nowrap">
          <a class="btn btn-sm" href="${ctx}/admin/enrollments?courseId=${c.id}">Students</a>
          <form method="post" action="${ctx}/admin/courses" class="inline" data-confirm="Delete this course and ALL its attendance records?">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <input type="hidden" name="action" value="delete">
            <input type="hidden" name="courseId" value="${c.id}">
            <button class="btn btn-sm btn-danger" type="submit">Delete</button>
          </form>
        </td>
      </tr>
    </c:forEach>
    </tbody>
  </table>
  </div>
</div>
<%@ include file="../common/footer.jspf" %>
