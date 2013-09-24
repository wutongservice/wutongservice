<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:template match="/document">
    <html>
      <head>
        <meta http-equiv="content-type" content="text/html; charset=UTF-8"/>
        <title>
          <xsl:value-of select="title"/>
        </title>
        <style type="text/css">
          #main {background-color: white;padding:0px; margin:0; width:950px; margin:0 auto; border-width:0 1;
          border-style:none solid;}
          body {background-color: #EEEEEE; padding:0; margin:0; font-family: Helvetica, Tahoma, Arial, sans-serif;
          font-size: 12px; line-height:
          1.5; color: black;}
          div.section { padding: 0 10px; border-bottom:1px solid black;}
          div.sub {padding: 5px 0; border-bottom:1px dotted gray;}
          div.sub_last {padding: 10px 0; }
          #navigator { margin:0; padding:0; position:fixed !important; top:0px; position:absolute; z-index:100;
          top:400px; left:5px;}
          table {font-size:12px;border-collapse:collapse;padding:0;}
          table.navigator_table {text-align:left;}
          table.content_table {width:930px; border-style:solid; border-width:1px;}
          table.content_table > thead td {font-weight:bold; background-color:#EEEEEE;padding:5px;}
          table.content_table td {border-style:solid; border-width:1px; border-color:#EEEEEE;padding:5px;}
          a {color:black; text-decoration:none; font-weight:bold;}
          a:hover {text-decoration:underline;}
          td.column_name {width:20%;}
          td.column_type {width:5%;}
          td.column_readonly {width:5%;}
          td.column_description {width:70%;}
          td.arg_name {width:20%;}
          td.arg_optional {width:5%;}
          td.arg_description {width:75%;}
        </style>
      </head>
      <body>
        <div id="navigator">
          <table class="navigator_table">
            <tr>
              <td>
                <a href="#top">TOP</a>
              </td>
            </tr>
            <tr>
              <td>
                <a href="#schemas">Schemas</a>
              </td>
            </tr>
            <tr>
              <td>
                <a href="#groups">Groups</a>
              </td>
            </tr>
            <tr>
              <td>
                <a href="#methods">Methods</a>
              </td>
            </tr>
          </table>
        </div>

        <div id="main">
          <!--Title-->
          <div class="section">
            <a name="top"/>
            <h1>
              <xsl:value-of select="title"/>
            </h1>
          </div>


          <!--Description-->
          <xsl:if test="description">
            <div class="section">
              <h3>Description</h3>
              <p>
                <xsl:value-of select="description"/>
              </p>
            </div>
          </xsl:if>

          <!--Schemas-->
          <xsl:if test="schemas">
            <div class="section">
              <a name="schemas"/>
              <h3>Schemas</h3>
              <xsl:for-each select="schemas/schema">
                <div>
                  <xsl:attribute name="class">
                    <xsl:choose>
                      <xsl:when test="position() &lt; last()">sub</xsl:when>
                      <xsl:otherwise>sub_last</xsl:otherwise>
                    </xsl:choose>
                  </xsl:attribute>
                  <a>
                    <xsl:attribute name="name">
                      schema_<xsl:value-of select="@name"/>
                    </xsl:attribute>
                  </a>
                  <h3>Description</h3>
                  <p>
                    <xsl:value-of select="description"/>
                  </p>
                  <h4>
                    <xsl:value-of select="@name"/>
                  </h4>
                  <p>
                    <xsl:value-of select="description"/>
                  </p>
                  <table class="content_table">
                    <thead>
                      <tr>
                        <td class="column_name">Name</td>
                        <td class="column_type">Type</td>
                        <td class="column_readonly">Readonly</td>
                        <td class="column_description">Description</td>
                      </tr>
                    </thead>
                    <tbody>
                      <xsl:for-each select="columns/column">
                        <tr>
                          <td>
                            <xsl:value-of select="@name"/>
                          </td>
                          <td>
                            <xsl:value-of select="@type"/>
                          </td>
                          <td>
                            <xsl:choose>
                              <xsl:when test="@readonly='true'">YES</xsl:when>
                              <xsl:otherwise>&#160;</xsl:otherwise>
                            </xsl:choose>
                          </td>
                          <td>
                            <xsl:value-of select="."/>
                          </td>
                        </tr>
                      </xsl:for-each>
                    </tbody>
                  </table>
                </div>
              </xsl:for-each>
            </div>
          </xsl:if>


          <!--Method groups-->
          <xsl:if test="groups">
            <div class="section">
              <a name="groups"/>
              <h3>Method groups</h3>
              <xsl:for-each select="groups/group">
                <div>
                  <xsl:attribute name="class">
                    <xsl:choose>
                      <xsl:when test="position() &lt; last()">sub</xsl:when>
                      <xsl:otherwise>sub_last</xsl:otherwise>
                    </xsl:choose>
                  </xsl:attribute>
                  <h4>
                    <xsl:value-of select="@name"/>
                  </h4>
                  <p>
                    <xsl:value-of select="description"/>
                  </p>
                  <ul>
                    <xsl:for-each select="methods/method">
                      <li>
                        <a>
                          <xsl:attribute name="href">
                            #method_<xsl:value-of select="."/>
                          </xsl:attribute>
                          <xsl:value-of select="."/>
                        </a>
                      </li>
                    </xsl:for-each>
                  </ul>
                </div>
              </xsl:for-each>
            </div>
          </xsl:if>

          <!--Methods-->
          <xsl:if test="methods">
            <div class="section">
              <a name="methods"/>
              <h3>Methods</h3>
              <xsl:for-each select="methods/method">
                <div>
                  <xsl:attribute name="class">
                    <xsl:choose>
                      <xsl:when test="position() &lt; last()">sub</xsl:when>
                      <xsl:otherwise>sub_last</xsl:otherwise>
                    </xsl:choose>
                  </xsl:attribute>
                  <a>
                    <xsl:attribute name="name">
                      method_<xsl:value-of select="@name"/>
                    </xsl:attribute>
                  </a>
                  <h4>
                    <xsl:value-of select="@name"/>
                  </h4>
                  <p>
                    <xsl:value-of select="description"/>
                  </p>
                  <p>Need Login:
                    <xsl:choose>
                      <xsl:when test="@login='false'">NO</xsl:when>
                      <xsl:otherwise>YES</xsl:otherwise>
                    </xsl:choose>
                  </p>
                  <p>Need AppId:
                    <xsl:choose>
                      <xsl:when test="@appId='false'">NO</xsl:when>
                      <xsl:otherwise>YES</xsl:otherwise>
                    </xsl:choose>
                  </p>
                  <p>
                    return
                    <xsl:if test="return/@schema">
                      <!--(schema <xsl:value-of select="return/@schema"/>)-->
                      <a>
                        <xsl:attribute name="href">
                          #schema_<xsl:value-of select="return/@schema"/>
                        </xsl:attribute>
                        <xsl:value-of select="return/@schema"/>
                      </a>
                    </xsl:if>
                    :
                    <xsl:value-of select="return"/>
                  </p>
                  <table class="content_table">
                    <thead>
                      <tr>
                        <td class="arg_name">Name</td>
                        <td class="arg_optional">Optional</td>
                        <td class="arg_description">Description</td>
                      </tr>
                    </thead>
                    <tbody>
                      <xsl:for-each select="arguments/argument">
                        <tr>
                          <td>
                            <xsl:value-of select="@name"/>
                          </td>
                          <td>
                            <xsl:choose>
                              <xsl:when test="@optional='true'">YES</xsl:when>
                              <xsl:otherwise>&#160;</xsl:otherwise>
                            </xsl:choose>
                          </td>
                          <td>
                            <xsl:value-of select="."/>
                          </td>
                        </tr>
                      </xsl:for-each>
                    </tbody>
                  </table>
                </div>
              </xsl:for-each>
            </div>
          </xsl:if>
        </div>
      </body>
    </html>
  </xsl:template>
</xsl:stylesheet>