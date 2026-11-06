<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Sign in | SAMS</title>
  <link rel="icon" type="image/svg+xml" href="${ctx}/images/favicon.svg">
  <link rel="stylesheet" href="${ctx}/css/style.css">
</head>
<body class="login-body">
  <div class="login-card">
    <div class="login-side">
      <div class="logo logo-lg">&#10003;</div>
      <h1>Smart Attendance<br>Management System</h1>
      <p>Take attendance in seconds, track it in real time and never miss a shortage again.</p>
      <ul>
        <li>One-click class attendance for teachers</li>
        <li>Live percentage &amp; shortage alerts for students</li>
        <li>Reports, CSV export and device API for admins</li>
      </ul>
    </div>
    <form class="login-form" method="post" action="${ctx}/login" autocomplete="off">
      <h2>Welcome back</h2>
      <p class="muted">Sign in to continue</p>
      <c:if test="${not empty error}"><div class="alert alert-error"><c:out value="${error}"/></div></c:if>
      <c:if test="${not empty info}"><div class="alert alert-info"><c:out value="${info}"/></div></c:if>
      <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
      <label for="username">Username</label>
      <input id="username" name="username" type="text" value="<c:out value='${username}'/>" required autofocus>
      <label for="password">Password</label>
      <input id="password" name="password" type="password" required>
      <button class="btn btn-primary btn-block" type="submit">Sign in</button>
      <p class="hint">Demo: <code>admin</code> / <code>Admin@123</code> &nbsp;|&nbsp; <code>teacher1</code> / <code>Teacher@123</code> &nbsp;|&nbsp; <code>student01</code> / <code>Student@123</code></p>
    </form>
  </div>
</body>
</html>
