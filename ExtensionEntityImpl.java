package sharedApp.extensions;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


import java.sql.Timestamp;

import java.text.DateFormat;

import java.text.ParseException;
import java.text.SimpleDateFormat;


import java.util.Date;
//import java.sql.Date;

import oracle.jbo.AttributeDef;
import oracle.jbo.AttributeList;
import oracle.jbo.JboException;
import oracle.jbo.server.EntityImpl;
import oracle.jbo.server.SequenceImpl;
import oracle.jbo.server.TransactionEvent;


public class ExtensionEntityImpl extends EntityImpl {
    private CallableStatement _st = null;
    private String _stmt = null;

    public ExtensionEntityImpl() {
        super();
    }

    public void setStmt(String stmt) {
        this._stmt = stmt;
        //
        if (this._st != null) {
            try {
                this._st.close();
            } catch (SQLException e) {
                System.out.println("-+- Error close Callable Statement: " + e);
            }
        }
        this._st = getDBTransaction().createCallableStatement(_stmt, 0);
    }

    public String getStmt() {
        return _stmt;
    }

    public CallableStatement getStatement() {
        return _st;
    }

    @Override
    protected void create(AttributeList attributeList) {
        super.create(attributeList);
        for (AttributeDef def : getEntityDef().getAttributeDefs()) {
            String sequenceName = (String)def.getProperty("SequenceName");
            if (sequenceName != null && isPermissionHas()) {
                SequenceImpl s =
                    new SequenceImpl(sequenceName, getDBTransaction());
                //        System.out.println("-+- 1 getted sequence: "+s.getSequenceNumber());
                setAttribute(def.getIndex(), s.getSequenceNumber());
                //        System.out.println("-+- 2 getted sequence: " + s.getSequenceNumber());
                //        in point 2 and point 1 values equals
            }
        }
    }

    protected boolean isPermissionHas() {
        return true;
    }

    @Override
    protected void doDML(int operation, TransactionEvent e) {
        // super.doDML(operation, e);
        if (operation == DML_INSERT)
            callInsertProcedure(e);
        else if (operation == DML_UPDATE)
            callUpdateProcedure(e);
        else if (operation == DML_DELETE)
            callDeleteProcedure(e);
    }

    /* Override in a subclass to perform non-default processing */

    protected void callInsertProcedure(TransactionEvent e) {
        super.doDML(DML_INSERT, e);
    }
    /* Override in a subclass to perform non-default processing */

    protected void callUpdateProcedure(TransactionEvent e) {
        super.doDML(DML_UPDATE, e);
    }
    /* Override in a subclass to perform non-default processing */

    protected void callDeleteProcedure(TransactionEvent e) {
        super.doDML(DML_DELETE, e);
    }

    // In StoredProcTestModuleImpl.java

    public void exec(String proc_name) {
        getDBTransaction().executeCommand("begin " + proc_name + "; end;");
    }


    protected void exec(String stmt, Object[] bindVars) {
        PreparedStatement st = null;
        try {
            // 1. Create a JDBC PreparedStatement for
            st =
 getDBTransaction().createPreparedStatement("begin " + stmt + "; end;", 0);
            if (bindVars != null) {
                // 2. Loop over values for the bind variables passed in, if any
                for (int z = 0; z < bindVars.length; z++) {
                    // 3. Set the value of each bind variable in the statement
                    st.setObject(z + 1, bindVars[z]);
                }
            }
            // 4. Execute the statement
            st.executeUpdate();
        } catch (SQLException e) {
            System.out.println("-+- Error exec for: " + stmt + ", " + e);
            throw new JboException(e);
        } finally {
            if (st != null) {
                try {
                    // 5. Close the statement
                    st.close();
                } catch (SQLException e) {
                    System.out.println("-+- Error close transaction for: " +
                                       stmt);
                }
            }
        }
    }

    // In StoredProcTestModuleImpl.java

    protected Object func(int sqlReturnType, String stmt, Object[] bindVars) {
        // 1. Create a JDBC CallabledStatement
        setStmt(stmt);
        try {
            // 2. Register the first bind variable for the return value
            _st.registerOutParameter(1, sqlReturnType);
            if (bindVars != null) {
                // 3. Loop over values for the bind variables passed in, if any
                for (int z = 0; z < bindVars.length; z++) {
                    // 4. Set the value of user-supplied bind vars in the stmt
                    _st.setObject(z + 2, bindVars[z]);
                }
            }
            // 5. Set the value of user-supplied bind vars in the stmt
            _st.executeUpdate();
            // 6. Return the value of the first bind variable
            return _st.getObject(1);
        } catch (SQLException e) {
            System.out.println("-+- Error execute function: " + stmt + " : " +
                               e);
            return "-+- Error return parameter: " + e;
        }
    }
    /*
    // In StoredProcTestModuleImpl.java

    public String example_func(Number n, Date d, String v) {
      return (String)func(Types.VARCHAR,
                          "begin ? := func_with_three_args(?,?,?); end;",
                          new Object[] { n, d, v });
    }

    //Example

    public void example_exec(String stmt) {
      Number n = 10;
      String v = "20";
      Date dt;
      String str;
      try {
        // 1. Define the PL/SQL block for the statement to invoke
        // 2. Create the CallableStatement for the PL/SQL block
        this.setStmt(stmt);
        // 3. Register the positions and types of the OUT parameters
        getStatement().registerOutParameter(2, Types.DATE);
        getStatement().registerOutParameter(3, Types.VARCHAR);
        // 4. Set the bind values of the IN parameters
        getStatement().setObject(1, n);
        getStatement().setObject(3, v);
        // 5. Execute the statement
        getStatement().executeUpdate();
        // 6. Create a bean to hold the multiple return values
        // 7. Set value of dateValue property using first OUT param
        dt = getStatement().getDate(2);
        // 8. Set value of stringValue property using 2nd OUT param
        str = getStatement().getString(3);
        // 9. Return the result
      } catch (SQLException e) {
        throw new JboException(e);
      } finally {
        if (getStatement() != null) {
          try {
            // 10. Close the JDBC CallableStatement
            getStatement().close();
          } catch (SQLException e) {
            System.out.println("-+- Error close procedure: " + stmt);
          }
        }
      }
    }

    public void callProcWithThreeArgs(String proc_name, Number n, Date d,
                                      String v) {
      exec(proc_name + "(?,?,?)", new Object[] { n, d, v });
    }
*/
}
