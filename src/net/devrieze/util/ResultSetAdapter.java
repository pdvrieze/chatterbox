package net.devrieze.util;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Iterator;


public abstract class ResultSetAdapter<T> implements Iterable<T> {


  public abstract static class ResultSetAdapterIterator<T> implements Iterator<T> {

    private final ResultSet aResultSet;

    private boolean aPeeked = false;

    public ResultSetAdapterIterator(ResultSet pResultSet) {
      aResultSet = pResultSet;
      try {
        aResultSet.beforeFirst();
        ResultSetMetaData metadata = aResultSet.getMetaData();
        for (int i = 0; i < metadata.getColumnCount(); ++i) {
          doRegisterColumn(i, metadata.getColumnName(i));
        }
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }

    protected abstract void doRegisterColumn(int pI, String pColumnName);

    abstract protected T doCreateElem(ResultSet pResultSet) throws SQLException;

    @Override
    public final boolean hasNext() {
      aPeeked = true;
      try {
        return aResultSet.next();
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public final T next() {
      try {
        if (!aPeeked) {
          if (!aResultSet.next()) {
            throw new IllegalStateException("Trying to go beyond the last element");
          }
        }
        aPeeked = false;

        return doCreateElem(aResultSet);

      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public final void remove() {
      try {
        aResultSet.deleteRow();
      } catch (SQLFeatureNotSupportedException e) {
        throw new UnsupportedOperationException(e);
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public abstract static class SingletonAdapterIterator<T> extends ResultSetAdapterIterator<T> {

    public SingletonAdapterIterator(ResultSet pResultSet) {
      super(pResultSet);
    }

    @Override
    protected void doRegisterColumn(int pIndex, String pColumnName) {
      if (pIndex!=1) {
        throw new IllegalArgumentException("Singleton adapters can not be created for result sets with more than one columns");
      }
    }
    
  }

  protected final ResultSet aResultSet;

  protected ResultSetAdapter(ResultSet pResultSet) {
    aResultSet = pResultSet;
  }

  @Override
  public abstract Iterator<T> iterator();
}