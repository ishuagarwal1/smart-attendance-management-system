<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ include file="../common/header.jspf" %>
<h1 class="page-title">My courses</h1>
<p class="muted">Welcome, <c:out value="${sessionScope.user.fullName}"/>. Pick a course to take attendance or view its report.</p>

<c:if test="${empty courses}"><div class="card"><p class="muted">No course has been assigned to you yet. Please contact the administrator.</p></div></c:if>

<section class="cards">
<c:forEach items="${courses}" var="c">
  <fmt:formatNumber var="pw" value="${c.avgPercentage}" maxFractionDigits="0" groupingUsed="false"/>
  <div class="card course-card">
    <span class="code"><c:out value="${c.code}"/></span>
    <h3><c:out value="${c.name}"/></h3>
    <p class="muted small">Semester ${c.semester} &middot; ${c.credits} credits &middot; ${c.enrolledCount} students</p>
    <div class="mini-stats">
      <div><strong>${c.classesHeld}</strong><span>classes held</span></div>
      <div><strong><c:choose><c:when test="${c.classesHeld > 0}"><fmt:formatNumber value="${c.avgPercentage}" maxFractionDigits="1"/>%</c:when><c:otherwise>&ndash;</c:otherwise></c:choose></strong><span>average</span></div>
      <div><strong><c:choose><c:when test="${empty c.lastMarked}">&ndash;</c:when><c:otherwise><fmt:formatDate value="${c.lastMarked}" pattern="dd MMM"/></c:otherwise></c:choose></strong><span>last marked</span></div>
    </div>
    <div class="bar"><div class="bar-fill ok w-${c.classesHeld > 0 ? pw : 0}"></div></div>
    <div class="actions">
      <a class="btn btn-primary" href="${ctx}/teacher/attendance?courseId=${c.id}">Take attendance</a>
      <a class="btn" href="${ctx}/teacher/report?courseId=${c.id}">Report</a>
    </div>
  </div>
</c:forEach>
</section>
<%@ include file="../common/footer.jspf" %>
