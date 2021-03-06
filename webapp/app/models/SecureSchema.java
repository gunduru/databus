package models;

import java.util.ArrayList;
import java.util.List;

import com.alvazan.orm.api.base.CursorToMany;
import com.alvazan.orm.api.base.CursorToManyImpl;
import com.alvazan.orm.api.base.NoSqlEntityManager;
import com.alvazan.orm.api.base.Query;
import com.alvazan.orm.api.base.anno.NoSqlDiscriminatorColumn;
import com.alvazan.orm.api.base.anno.NoSqlEmbedded;
import com.alvazan.orm.api.base.anno.NoSqlIndexed;
import com.alvazan.orm.api.base.anno.NoSqlManyToMany;
import com.alvazan.orm.api.base.anno.NoSqlOneToMany;
import com.alvazan.orm.api.base.anno.NoSqlQueries;
import com.alvazan.orm.api.base.anno.NoSqlQuery;
import com.alvazan.orm.api.z8spi.KeyValue;
import com.alvazan.orm.api.z8spi.iter.Cursor;
import com.alvazan.play.NoSql;

@NoSqlDiscriminatorColumn("schema")
@NoSqlQueries({
	@NoSqlQuery(name="findAll", query="select e from TABLE as e"),
	@NoSqlQuery(name="findByName", query="select u from TABLE as u where u.schemaName = :schemaName")
})
public class SecureSchema extends SecureResource {

	private String description;
	
	private Integer tableCount = null;
	
	@NoSqlIndexed
	private String schemaName;
	
	@NoSqlManyToMany
	private CursorToMany<SecureTable> tablesCursor = new CursorToManyImpl<SecureTable>();

	@NoSqlOneToMany
	private List<StreamAggregation> aggregations = new ArrayList<StreamAggregation>();
	
	@NoSqlEmbedded
	private List<String> monitorIds = new ArrayList();

	@NoSqlEmbedded
	private List<String> triggerIds = new ArrayList();
	
	@NoSqlEmbedded
	private List<String> postTriggersIds = new ArrayList<String>();
	
	public String getSchemaName() {
		return schemaName;
	}

	public void setSchemaName(String name) {
		this.schemaName = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public CursorToMany<SecureTable> getTablesCursor() {
		if (tablesCursor == null)
			tablesCursor = new CursorToManyImpl<SecureTable>();
		return new RemoveSafeCursor(tablesCursor) {
			protected boolean isDeleted(Object next) {
				return ((SecureTable)next).getPrimaryKey() == null?true:false;
			}
		};
	}
	
	public List<SecureTable> getTables(int firstRow, int maxResults) {
		List<SecureTable> tables = new ArrayList<SecureTable>();
		CursorToMany<SecureTable> cursor = getTablesCursor();
		for(int i = 0; i < firstRow && cursor.next(); i++) {
		}
		
		for(int i = 0; i < maxResults && cursor.next(); i++) {
			SecureTable t = cursor.getCurrent();
			//trigger a cache load as playframework is NOT going through getters/setters(if it was, we would NOT need to do this)
			t.getTableName();
			tables.add(t);
		}
		NoSql.em().flush();
		return tables;
	}

	public void setTablesCursor(CursorToMany<SecureTable> tablesCursor) {
		this.tablesCursor = tablesCursor;
	}

	public static List<SecureSchema> findAll(NoSqlEntityManager em) {
		Query<SecureSchema> query = em.createNamedQuery(SecureSchema.class, "findAll");
		Cursor<KeyValue<SecureSchema>> cursor = query.getResults("schemaName");
		List<SecureSchema> groups = new ArrayList<SecureSchema>();
		while(cursor.next()) {
			KeyValue<SecureSchema> kv = cursor.getCurrent();
			if(kv.getValue() == null)
				continue;
			groups.add(kv.getValue());
		}
		return groups;
	}
	
	public static SecureSchema findByName(NoSqlEntityManager mgr, String schemaName) {
		Query<SecureSchema> query = mgr.createNamedQuery(SecureSchema.class, "findByName");
		query.setParameter("schemaName", schemaName);
		return query.getSingleObject();
	}
	
	public void addTable(SecureTable t) {
		if(tableCount == null) {
			tableCount = 0;
		}
		
		tableCount++;
		t.setSchema(this);
		getTablesCursor().addElement(t);
	}
	
	public Integer getTableCount() {
		return tableCount;
	}
	
	public void populateTableCount(NoSqlEntityManager mgr) {
		if(tableCount == null) {
			CursorToMany<SecureTable> c = getTablesCursor();
			c.beforeFirst();
			tableCount = 0;
			while(c.next()) {
				tableCount++;
			}
			c.beforeFirst();
			
			mgr.put(this);
			mgr.flush();
		}
	}

	@Override
	public String getName() {
		return schemaName;
	}

	protected void addAggregation(StreamAggregation agg) {
		aggregations.add(agg);
	}

	public List<StreamAggregation> getAggregations() {
		return aggregations;
	}

	public List<String> getMonitorIds() {
		return monitorIds;
	}

	public void setMonitorIds(List<String> monitorIds) {
		this.monitorIds = monitorIds;
	}

	public List<String> getTriggerIds() {
		return triggerIds;
	}

	public List<String> getPostTriggerIds() {
		return postTriggersIds;
	}
	
	
}
