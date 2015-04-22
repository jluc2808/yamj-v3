<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
    <head>
        <title>YAMJ v3</title>
        <!--Import the header details-->
        <c:import url="template.jsp">
            <c:param name="sectionName" value="HEAD" />
        </c:import>
    </head>
    <body background="${pageContext.request.contextPath}/images/yamj-configbg.jpg">
        <!--Import the navigation header-->
        <c:import url="template.jsp">
            <c:param name="sectionName" value="NAV" />
        </c:import>

        <div id="logo">
            <h2>Player Path Entries</h2>
            <p><a href="${pageContext.request.contextPath}/player/add-path/${player.name}.html" class="btn info">Add new path &raquo;</a></p>
        </div>

        <table id="headertable" style="width:75%;">
            <tr>
                <td class="right">Player Name:</td>
                <td>${player.name}</td>
            </tr>
            <tr>
                <td class="right">Device Type:</td>
                <td>${player.deviceType}</td>
            </tr>
            <tr>
                <td class="right">IP Address:</td>
                <td>${player.ipAddress}</td>
            </tr>
        </table>

        <table id="tablelist">
            <tr>
                <th>Source Path</th>
                <th>Target Path</th>
                <th>Actions</th>
            </tr>
            <tbody>
                <c:forEach items="${pathlist}" var="entry" varStatus="row">
                    <tr>
                        <td>${entry.sourcePath}</td>
                        <td>${entry.targetPath}</td>
                        <td class="center">
                            <a href="${pageContext.request.contextPath}/player/edit-path/${entry.id}.html">Edit</a> or
                            <a href="${pageContext.request.contextPath}/player/delete-path/${entry.id}.html">Delete</a>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>

        <!-- Import the footer -->
        <c:import url="template.jsp">
            <c:param name="sectionName" value="FOOTER" />
        </c:import>
    </body>
</html>
