package org.springframework.orm.ibatis;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import com.ibatis.db.sqlmap.SqlMap;
import com.ibatis.db.sqlmap.XmlSqlMapBuilder;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;

/**
 * FactoryBean that creates an iBATIS Database Layer SqlMap as singleton in the
 * current bean factory, possibly for use with SqlMapTemplate.
 *
 * <p>NOTE: The SqlMap/MappedStatement API is the one to use with iBATIS SQL Maps 1.x.
 * The SqlMapClient/SqlMapSession is only available with SQL Maps 2.
 *
 * @author Juergen Hoeller
 * @since 28.11.2003
 * @see SqlMapTemplate#setSqlMap
 */
public class SqlMapFactoryBean implements FactoryBean, InitializingBean {

	private Resource configLocation;

	private Properties sqlMapProperties;

	private SqlMap sqlMap;

	/**
	 * Set the location of the iBATIS SqlMap config file.
	 * A typical value is "WEB-INF/sql-map-config.xml".
	 */
	public void setConfigLocation(Resource configLocation) {
		this.configLocation = configLocation;
	}

	/**
	 * Set optional properties to be passed into the XmlSqlMapBuilder.
	 * @see com.ibatis.db.sqlmap.XmlSqlMapBuilder#buildSqlMap(java.io.Reader, java.util.Properties)
	 */
	public void setSqlMapProperties(Properties sqlMapProperties) {
		this.sqlMapProperties = sqlMapProperties;
	}

	public void afterPropertiesSet() throws IOException {
		if (this.configLocation == null) {
			throw new IllegalArgumentException("configLocation must be set");
		}
		InputStream is = this.configLocation.getInputStream();
		this.sqlMap = (this.sqlMapProperties != null) ?
				XmlSqlMapBuilder.buildSqlMap(new InputStreamReader(is), this.sqlMapProperties) :
				XmlSqlMapBuilder.buildSqlMap(new InputStreamReader(is));
	}

	public Object getObject() {
		return this.sqlMap;
	}

	public Class getObjectType() {
		return (this.sqlMap != null ? this.sqlMap.getClass() : SqlMap.class);
	}

	public boolean isSingleton() {
		return true;
	}

}
