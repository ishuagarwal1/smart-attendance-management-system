<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ include file="../common/header.jspf" %>
<h1 class="page-title">Users</h1>

<div class="card">
  <h2>Add a new user</h2>
  <form method="post" action="${ctx}/admin/users" class="form grid2">
    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
    <input type="hidden" name="action" value="create">
    <div><label for="role">Role</label>
      <select id="role" name="role" data-role-select required>
        <option value="STUDENT">Student</option>
        <option value="TEACHER">Teacher</option>
        <option value="ADMIN">Administrator</option>
      </select></div>
    <div><label for="fullName">Full name</label><input id="fullName" name="fullName" required maxlength="100"></div>
    <div><label for="username">Username</label><input id="username" name="username" required pattern="[A-Za-z0-9._-]{3,30}"></div>
    <div><label for="email">E-mail</label><input id="email" name="email" type="email" required></div>
    <div><label for="password">Temporary password</label><input id="password" name="password" type="password" required minlength="8"></div>
    <div class="student-only"><label for="rollNo">Roll number</label><input id="rollNo" name="rollNo" maxlength="30"></div>
    <div class="student-only"><label for="department">Department</label><input id="department" name="department" value="Computer Science"></div>
    <div class="student-only"><label for="semester">Semester</label><input id="semester" name="semester" type="number" min="1" max="12" value="1"></div>
    <div class="student-only"><label for="phone">Phone</label><input id="phone" name="phone" maxlength="20"></div>
    <div class="form-actions"><button class="btn btn-primary" type="submit">Create account</button></div>
  </form>
</div>

<div class="card">
  <h2>All accounts <small class="muted">(<c:out value="${users.size()}"/>)</small></h2>
  <input class="search" type="search" placeholder="Search by name, username or e-mail..." data-filter="#userTable">
  <div class="table-wrap">
  <table class="table" id="userTable">
    <thead><tr><th>Name</th><th>Username</th><th>E-mail</th><th>Role</th><th>Last login</th><th>Status</th><th></th></tr></thead>
    <tbody>
    <c:forEach items="${users}" var="u">
      <tr>
        <td><c:out value="${u.fullName}"/></td>
        <td><c:out value="${u.username}"/></td>
        <td><c:out value="${u.email}"/></td>
        <td><span class="badge badge-role"><c:out value="${u.role}"/></span></td>
        <td><c:choose><c:when test="${empty u.lastLogin}"><span class="muted">never</span></c:when>
            <c:otherwise><fmt:formatDate value="${u.lastLogin}" pattern="dd MMM yyyy, HH:mm"/></c:otherwise></c:choose></td>
        <td><c:choose><c:when test="${u.active}"><span class="badge badge-good">Active</span></c:when>
            <c:otherwise><span class="badge badge-low">Disabled</span></c:otherwise></c:choose></td>
        <td>
          <c:if test="${u.id != sessionScope.user.id}">
          <form method="post" action="${ctx}/admin/users" class="inline" data-confirm="${u.active ? 'Deactivate' : 'Activate'} this account?">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <input type="hidden" name="action" value="toggle">
            <input type="hidden" name="id" value="${u.id}">
            <button class="btn btn-sm" type="submit">${u.active ? 'Deactivate' : 'Activate'}</button>
          </form>
          </c:if>
        </td>
      </tr>
    </c:forEach>
    </tbody>
  </table>
  </div>
</div>
<%@ include file="../common/footer.jspf" %>
