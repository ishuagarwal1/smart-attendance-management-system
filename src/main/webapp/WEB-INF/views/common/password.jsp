<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="header.jspf" %>
<div class="card narrow">
  <h2>Change password</h2>
  <c:if test="${not empty error}"><div class="alert alert-error"><c:out value="${error}"/></div></c:if>
  <form method="post" action="${ctx}/account/password" class="form">
    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
    <label for="current">Current password</label>
    <input id="current" name="current" type="password" required>
    <label for="next">New password</label>
    <input id="next" name="next" type="password" required minlength="8">
    <small class="muted">At least 8 characters, with a letter and a digit.</small>
    <label for="confirm">Confirm new password</label>
    <input id="confirm" name="confirm" type="password" required>
    <button class="btn btn-primary" type="submit">Update password</button>
  </form>
</div>
<%@ include file="footer.jspf" %>
