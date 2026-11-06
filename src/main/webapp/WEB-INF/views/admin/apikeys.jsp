<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ include file="../common/header.jspf" %>
<h1 class="page-title">API keys</h1>
<p class="muted">Keys let RFID readers, biometric terminals or mobile apps push attendance through the REST API
  (<code>POST /api/v1/attendance/mark</code>). Send the key in the <code>X-API-KEY</code> header. Only a hash is stored.</p>

<c:if test="${not empty newApiKey}">
  <div class="card keybox">
    <h2>Your new API key</h2>
    <p>Copy it now &ndash; for security it will <strong>never be shown again</strong>.</p>
    <div class="keyline"><code id="newKey"><c:out value="${newApiKey}"/></code>
      <button class="btn btn-sm" type="button" data-copy="#newKey">Copy</button></div>
  </div>
</c:if>

<div class="card">
  <h2>Generate a key</h2>
  <form method="post" action="${ctx}/admin/apikeys" class="form inline-form">
    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
    <input type="hidden" name="action" value="create">
    <input name="label" required maxlength="80" placeholder="e.g. Room 204 RFID reader" class="grow">
    <button class="btn btn-primary" type="submit">Generate</button>
  </form>
</div>

<div class="card">
  <h2>Issued keys</h2>
  <c:if test="${empty keys}"><p class="muted">No API keys yet.</p></c:if>
  <c:if test="${not empty keys}">
  <table class="table">
    <thead><tr><th>Label</th><th>Key prefix</th><th>Created</th><th>Last used</th><th>Status</th><th></th></tr></thead>
    <tbody>
    <c:forEach items="${keys}" var="k">
      <tr>
        <td><c:out value="${k.label}"/></td>
        <td><code><c:out value="${k.keyPrefix}"/>&hellip;</code></td>
        <td><fmt:formatDate value="${k.createdAt}" pattern="dd MMM yyyy"/></td>
        <td><c:choose><c:when test="${empty k.lastUsedAt}"><span class="muted">never</span></c:when>
            <c:otherwise><fmt:formatDate value="${k.lastUsedAt}" pattern="dd MMM, HH:mm"/></c:otherwise></c:choose></td>
        <td><span class="badge ${k.active ? 'badge-good' : 'badge-low'}">${k.active ? 'Active' : 'Revoked'}</span></td>
        <td class="nowrap">
          <form method="post" action="${ctx}/admin/apikeys" class="inline">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <input type="hidden" name="action" value="toggle">
            <input type="hidden" name="id" value="${k.id}">
            <input type="hidden" name="active" value="${k.active ? 0 : 1}">
            <button class="btn btn-sm" type="submit">${k.active ? 'Revoke' : 'Re-activate'}</button>
          </form>
          <form method="post" action="${ctx}/admin/apikeys" class="inline" data-confirm="Delete this key permanently?">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <input type="hidden" name="action" value="delete">
            <input type="hidden" name="id" value="${k.id}">
            <button class="btn btn-sm btn-danger" type="submit">Delete</button>
          </form>
        </td>
      </tr>
    </c:forEach>
    </tbody>
  </table>
  </c:if>
</div>
<%@ include file="../common/footer.jspf" %>
