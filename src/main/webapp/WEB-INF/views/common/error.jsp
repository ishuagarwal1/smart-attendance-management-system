<%@ page isErrorPage="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<%
    Integer code = (Integer) request.getAttribute("javax.servlet.error.status_code");
    String msg = (String) request.getAttribute("javax.servlet.error.message");
    int status = code == null ? 500 : code;
    response.setStatus(status);
    request.setAttribute("errCode", status);
    String friendly;
    switch (status) {
        case 403: friendly = (msg != null && !msg.isEmpty()) ? msg : "You do not have permission to open this page."; break;
        case 404: friendly = (msg != null && !msg.isEmpty() && !msg.startsWith("/")) ? msg : "The page you are looking for does not exist."; break;
        case 405: friendly = "That action is not allowed here."; break;
        default:  friendly = "Something went wrong on our side. Please try again; if it keeps happening contact the administrator.";
    }
    request.setAttribute("errMsg", friendly);
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Error <c:out value="${errCode}"/> | SAMS</title>
  <link rel="stylesheet" href="${ctx}/css/style.css">
</head>
<body class="login-body">
  <div class="card narrow center">
    <div class="errcode"><c:out value="${errCode}"/></div>
    <p><c:out value="${errMsg}"/></p>
    <a class="btn btn-primary" href="${ctx}/dashboard">Back to dashboard</a>
  </div>
</body>
</html>
