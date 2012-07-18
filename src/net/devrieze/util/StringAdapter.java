package net.devrieze.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;


public class StringAdapter extends ResultSetAdapter<String> {

  
  public class StringAdapterIterator extends SingletonAdapterIterator<String> {

    public StringAdapterIterator(ResultSet pResultSet) {
      super(pResultSet);
    }

    @Override
    protected String doCreateElem(ResultSet pResultSet) throws SQLException {
      return pResultSet.getString(1);
    }

  }

  public StringAdapter(ResultSet pResultSet) {
    super(pResultSet);
  }

  @Override
  public Iterator<String> iterator() {
    
    return new StringAdapterIterator(aResultSet);
  }

}
