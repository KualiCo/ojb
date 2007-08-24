<?xml version="1.0" ?> 
<!--
#/* Copyright 2004 Apache Software Foundation
# *
# * Licensed under the Apache License, Version 2.0 (the "License");
# * you may not use this file except in compliance with the License.
# * You may obtain a copy of the License at
# *
# *     http://www.apache.org/licenses/LICENSE-2.0
# *
# * Unless required by applicable law or agreed to in writing, software
# * distributed under the License is distributed on an "AS IS" BASIS,
# * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# * See the License for the specific language governing permissions and
# * limitations under the License.
# */
-->
<!--
	ObJectRelationalBridge - Bridging Java objects and relational dabatases

	This stylesheets upgrades 0.8.400 repository format to 0.9 format.
	
	It is of beta quality only. But should be enough to get you started.
	
	Usage:
		Make sure that Xalan.jar is in the classpath
		java org.apache.xalan.xslt.Process -IN *oldrepository.xml* -XSL 0.8to0.9.xsl -OUT *newrepository.xml*
		
		The current split into more files is not supported. You will have to do that manually when you have upgraded.

	Author: Lasse Lindgaard, June 2nd, 2002

-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:output
		method="xml"
		indent="yes"
		doctype-system="repository.dtd"
	/>
	
	<xsl:strip-space elements="*"/>
	
	<!-- default templates -->
	<xsl:template match="/">
		<xsl:apply-templates select="@*|*" mode="attribute"/>
		<xsl:apply-templates select="@*|node()" mode="elements"/>
	</xsl:template>

	<!--
		make an error for unmatched nodes
		that will provide a simple way of change detection
		and provide feedback on when to write the next
		migration stylesheet
	-->
	<xsl:template match="@*|node()">
		<xsl:message terminate="yes">Sorry, <xsl:copy><xsl:value-of select="local-name()"/></xsl:copy> with content <xsl:value-of select="."/> was not matched. Something went wrong. Migration failed.</xsl:message>
	</xsl:template>
	
	<xsl:template match="@*|node()" mode="attribute"></xsl:template>
	
	<xsl:template match="@*|node()" mode="elements"></xsl:template>
	
	<xsl:template match="text()" priority="1" mode="attribute"></xsl:template>
	
	<xsl:template match="text()" priority="1" mode="elements"><xsl:copy/></xsl:template>
	
	<xsl:template match="comment()" priority="1" mode="attribute"><xsl:copy/></xsl:template>
	
	<xsl:template match="comment()" priority="1" mode="elements"><xsl:copy/></xsl:template>
	
	<!-- copy all ID attributes to lowercase id -->	
	<xsl:template match="@ID" mode="attribute">
		<xsl:attribute name="id"><xsl:value-of select="."/></xsl:attribute>
	</xsl:template>

	<xsl:template match="@id" mode="attribute">
		<xsl:attribute name="id"><xsl:value-of select="."/></xsl:attribute>
	</xsl:template>

	<!-- begin MappingRepository to descriptor-repository -->
	<xsl:template match="MappingRepository" mode="elements">
		<descriptor-repository>
			<xsl:apply-templates select="@*|*" mode="attribute"/>
			<xsl:apply-templates select="@*|node()" mode="elements"/>
		</descriptor-repository>
	</xsl:template>
	<!-- end MappingRepository to descriptor-repository -->

	<!-- begin JdbcConnectionDescriptor to jdbc-connection-descriptor -->
	<xsl:template match="JdbcConnectionDescriptor" mode="elements">
		<jdbc-connection-descriptor>
			<xsl:apply-templates select="@*|*" mode="attribute"/>
		</jdbc-connection-descriptor>
	</xsl:template>

	<!-- no id here -->
	<xsl:template match="JdbcConnectionDescriptor/@id" mode="attribute"></xsl:template>

	<xsl:template match="dbms.name" mode="attribute">
		<xsl:attribute name="platform"><xsl:value-of select="."/></xsl:attribute>
	</xsl:template>
	
	<xsl:template match="jdbc.level" mode="attribute">
		<xsl:attribute name="jdbc-level"><xsl:value-of select="."/></xsl:attribute>
	</xsl:template>

	<xsl:template match="driver.name" mode="attribute">
		<xsl:attribute name="driver"><xsl:value-of select="."/></xsl:attribute>
	</xsl:template>
	
	<xsl:template match="url.protocol" mode="attribute">
		<xsl:attribute name="protocol"><xsl:value-of select="."/></xsl:attribute>
	</xsl:template>
	
	<xsl:template match="url.subprotocol" mode="attribute">
		<xsl:attribute name="subprotocol"><xsl:value-of select="."/></xsl:attribute>
	</xsl:template>
	
	<xsl:template match="url.dbalias" mode="attribute">
		<xsl:attribute name="dbalias"><xsl:value-of select="."/></xsl:attribute>
	</xsl:template>
	
	<xsl:template match="datasource.name" mode="attribute">
		<xsl:attribute name="jndi-datasource-name"><xsl:value-of select="."/></xsl:attribute>
	</xsl:template>
	
	<xsl:template match="user.name" mode="attribute">
		<xsl:attribute name="username"><xsl:value-of select="."/></xsl:attribute>
	</xsl:template>

	<xsl:template match="user.passwd" mode="attribute">
		<xsl:attribute name="password"><xsl:value-of select="."/></xsl:attribute>
	</xsl:template>
	<!-- end JdbcConnectionDescriptor to jdbc-connection-descriptor -->

	<!-- begin ClassDescriptor to class-descriptor -->
	<xsl:template match="ClassDescriptor" mode="elements">
		<class-descriptor>
			<xsl:apply-templates select="@*|*" mode="attribute"/>
			<xsl:apply-templates select="JdbcConnectionDescriptor" mode="elements"/>
			<xsl:apply-templates select="ExtentDescriptor" mode="elements"/>
			<xsl:apply-templates select="FieldDescriptor" mode="elements"/>
			<xsl:apply-templates select="ReferenceDescriptor" mode="elements"/>
			<xsl:apply-templates select="CollectionDescriptor" mode="elements"/>
		</class-descriptor>
	</xsl:template>

	<!-- no id here -->
	<xsl:template match="ClassDescriptor/@id" mode="attribute"></xsl:template>
	<!-- no conversionStrategy on ClassDescriptors -->
	<xsl:template match="ClassDescriptor/conversionStrategy" mode="attribute"></xsl:template>
<!-- 
	<xsl:template match="conversionStrategy" mode="attribute">
		<xsl:attribute name="conversion"><xsl:value-of select="."/></xsl:attribute>
	</xsl:template>
 -->
	<xsl:template match="class.name" mode="attribute">
		<xsl:attribute name="class"><xsl:value-of select="."/></xsl:attribute>
	</xsl:template>

	<xsl:template match="ExtentDescriptor/class.name" mode="attribute">
		<xsl:attribute name="class"><xsl:value-of select="."/></xsl:attribute>
	</xsl:template>

	<xsl:template match="class.proxy" mode="attribute">
		<xsl:attribute name="proxy"><xsl:value-of select="."/></xsl:attribute>
	</xsl:template>

	<xsl:template match="schema.name" mode="attribute">
		<xsl:attribute name="schema"><xsl:value-of select="."/></xsl:attribute>
	</xsl:template>

	<xsl:template match="table.name" mode="attribute">
		<xsl:attribute name="table"><xsl:value-of select="."/></xsl:attribute>
	</xsl:template>

	<xsl:template match="orderby" mode="attribute">
		<xsl:attribute name="orderby"><xsl:value-of select="."/></xsl:attribute>
		<xsl:apply-templates select="@*|node()" mode="attribute"/>
	</xsl:template>

	<xsl:template match="@sort" mode="attribute">
		<xsl:attribute name="sort">
			<xsl:choose>
				<xsl:when test=".='asc'">ASC</xsl:when>
				<xsl:when test=".='desc'">DESC</xsl:when>
				<xsl:when test=".='ASC'">ASC</xsl:when>
				<xsl:when test=".='DESC'">DESC</xsl:when>
				<xsl:otherwise><xsl:message terminate="yes">Illegal sorting method <xsl:value-of select="."/></xsl:message></xsl:otherwise>
			</xsl:choose>
		</xsl:attribute>
	</xsl:template>

	<xsl:template match="rowReader" mode="attribute">
		<xsl:attribute name="row-reader"><xsl:value-of select="."/></xsl:attribute>
	</xsl:template>

	<xsl:template match="@isolation" mode="attribute">
		<xsl:attribute name="isolation-level"><xsl:value-of select="."/></xsl:attribute>
	</xsl:template>
	<!-- end ClassDescriptor to class-descriptor -->

	<!-- begin ClassDescriptor/ExtentDescriptor  -->
	<xsl:template match="ClassDescriptor[ExtentDescriptor]" mode="elements">
		<class-descriptor class="{ExtentDescriptor/class.name}">
			<xsl:apply-templates select="ExtentDescriptor/@*|ExtentDescriptor/node()" mode="elements"/>
		</class-descriptor>
	</xsl:template>
	
	<!-- class.name is already defined -->
	
	<xsl:template match="class.extent" mode="elements">
		<extent-class class-ref="{.}"/>
	</xsl:template>
	<!-- end ExtentDescriptor to extends-descriptor -->

	<!-- begin FieldDescriptor to field-descriptor -->
	<xsl:template match="FieldDescriptor" mode="elements">
		<field-descriptor>
			<xsl:apply-templates select="@*|*" mode="attribute"/>
			<xsl:apply-templates select="@*|node()" mode="elements"/>
		</field-descriptor>
	</xsl:template>
	
	<xsl:template match="field.name" mode="attribute">
		<xsl:attribute name="name"><xsl:value-of select="."/></xsl:attribute>
	</xsl:template>
	
	<!-- table.name is already defined -->
	
	<xsl:template match="column.name" mode="attribute">
		<xsl:attribute name="column"><xsl:value-of select="."/></xsl:attribute>
	</xsl:template>
	
	<xsl:template match="jdbc_type" mode="attribute">
		<xsl:attribute name="jdbc-type"><xsl:value-of select="."/></xsl:attribute>
	</xsl:template>
	
	<xsl:template match="PrimaryKey" mode="attribute">
		<xsl:attribute name="primarykey"><xsl:value-of select="."/></xsl:attribute>
	</xsl:template>
	
	<xsl:template match="autoincrement" mode="attribute">
		<xsl:attribute name="autoincrement"><xsl:value-of select="."/></xsl:attribute>
	</xsl:template>
	
	<xsl:template match="locking" mode="attribute">
		<xsl:attribute name="locking"><xsl:value-of select="."/></xsl:attribute>
	</xsl:template>
	
	<!-- end FieldDescriptor to field-descriptor -->

	<!-- begin CollectionDescriptor to collection-descriptor -->
	<xsl:template match="CollectionDescriptor" mode="elements">
		<collection-descriptor>
			<xsl:apply-templates select="@*|*" mode="attribute"/>
			<xsl:apply-templates select="@*|node()" mode="elements"/>
		</collection-descriptor>
	</xsl:template>

	<!-- no id here -->
	<xsl:template match="CollectionDescriptor/@id" mode="attribute"></xsl:template>
	
	<xsl:template match="cdfield.name" mode="attribute">
		<xsl:attribute name="name"><xsl:value-of select="."/></xsl:attribute>
	</xsl:template>
	
	<xsl:template match="items.class" mode="attribute">
		<xsl:attribute name="element-class-ref"><xsl:value-of select="."/></xsl:attribute>
	</xsl:template>
	
	<xsl:template match="collection.class" mode="attribute">
		<xsl:attribute name="collection-class"><xsl:value-of select="."/></xsl:attribute>
	</xsl:template>
	
	<xsl:template match="auto.retrieve" mode="attribute">
		<xsl:attribute name="auto-retrieve"><xsl:value-of select="."/></xsl:attribute>
	</xsl:template>
	
	<xsl:template match="auto.update" mode="attribute">
		<xsl:attribute name="auto-update"><xsl:value-of select="."/></xsl:attribute>
	</xsl:template>
	
	<xsl:template match="auto.delete" mode="attribute">
		<xsl:attribute name="auto-delete"><xsl:value-of select="."/></xsl:attribute>
	</xsl:template>

	<xsl:template match="proxyCollection" mode="attribute">
		<xsl:attribute name="proxy"><xsl:value-of select="."/></xsl:attribute>
	</xsl:template>

	<xsl:template match="refreshCollection" mode="attribute">
		<xsl:attribute name="refresh"><xsl:value-of select="."/></xsl:attribute>
	</xsl:template>

	<xsl:template match="indirection_table" mode="attribute">
		<xsl:attribute name="indirection-table"><xsl:value-of select="."/></xsl:attribute>
	</xsl:template>
	
	<!-- orderby and sort is defined earlier in the stylesheet so no need to repeat them here -->
	
	<xsl:template match="fks_pointing_to_this_class" mode="elements">
		<fk-pointing-to-this-class column="{.}"/>
	</xsl:template>

	<xsl:template match="fks_pointing_to_items_class" mode="elements">
		<fk-pointing-to-element-class column="{.}"/>
	</xsl:template>

	<xsl:template match="inverse_fk_descriptor_ids" mode="elements">
		<inverse-foreignkey field-id-ref="{.}"/>
	</xsl:template>
	<!-- end CollectionDescriptor to collection-descriptor -->

	<!-- begin ReferenceDescriptor to reference-descriptor -->
	<xsl:template match="ReferenceDescriptor" mode="elements">
		<reference-descriptor>
			<xsl:apply-templates select="@*|*" mode="attribute"/>
			<xsl:apply-templates select="@*|node()" mode="elements"/>
		</reference-descriptor>
	</xsl:template>

	<!-- no id here -->
	<xsl:template match="ReferenceDescriptor/@id" mode="attribute"></xsl:template>

	<xsl:template match="rdfield.name" mode="attribute">
		<xsl:attribute name="name"><xsl:value-of select="."/></xsl:attribute>
	</xsl:template>

	<xsl:template match="referenced.class" mode="attribute">
		<xsl:attribute name="class-ref"><xsl:value-of select="."/></xsl:attribute>
	</xsl:template>

	<xsl:template match="refreshReference" mode="attribute">
		<xsl:attribute name="refresh"><xsl:value-of select="."/></xsl:attribute>
	</xsl:template>

	<xsl:template match="proxyReference" mode="attribute">
		<xsl:attribute name="proxy"><xsl:value-of select="."/></xsl:attribute>
	</xsl:template>

	<xsl:template match="fk_descriptor_ids" mode="elements">
		<foreignkey field-id-ref="{.}"/>
	</xsl:template>
	<!-- end ReferenceDescription to reference-descriptor -->
</xsl:stylesheet>

