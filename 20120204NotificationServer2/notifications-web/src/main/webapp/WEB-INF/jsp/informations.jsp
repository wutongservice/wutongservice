<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
	<title>Informations</title>
</head>
<body>
<table border=1>
	<thead><tr>
		<th>ID</th>
		<th>Message</th>
	</tr></thead>
	<c:forEach var="information" items="${result.informations}">
	<tr>
		<td>${information.id}</td>
		<td>${information.title}</td>
	</tr>
	</c:forEach>
</table>
</body>
</html>