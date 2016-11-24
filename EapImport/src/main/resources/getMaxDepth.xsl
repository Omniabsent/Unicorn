<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
    <xsl:output method="text"/>

    <xsl:template match="/">
        <xsl:sequence select="max(//*/count(ancestor-or-self::node()))"/>
    </xsl:template>
</xsl:stylesheet>